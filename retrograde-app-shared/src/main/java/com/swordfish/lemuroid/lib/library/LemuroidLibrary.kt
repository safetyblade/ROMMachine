/*
 * GameLibrary.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.swordfish.lemuroid.lib.library

import androidx.room.withTransaction
import com.swordfish.lemuroid.common.coroutines.batchWithSizeAndTime
import com.swordfish.lemuroid.lib.bios.BiosManager
import com.swordfish.lemuroid.lib.library.covers.CustomCoverManager
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.DataFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.metadata.GameMetadata
import com.swordfish.lemuroid.lib.library.metadata.GameMetadataProvider
import com.swordfish.lemuroid.lib.storage.BaseStorageFile
import com.swordfish.lemuroid.lib.storage.GroupedStorageFiles
import com.swordfish.lemuroid.lib.storage.RomFiles
import com.swordfish.lemuroid.lib.storage.StorageFile
import com.swordfish.lemuroid.lib.storage.StorageProvider
import com.swordfish.lemuroid.lib.storage.StorageProviderRegistry
import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity
import com.swordfish.lemuroid.lib.storage.scanner.SerialScanner
import dagger.Lazy
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

class LemuroidLibrary(
    private val retrogradedb: RetrogradeDatabase,
    private val storageProviderRegistry: Lazy<StorageProviderRegistry>,
    private val gameMetadataProvider: Lazy<GameMetadataProvider>,
    private val biosManager: BiosManager,
    private val customCoverManager: CustomCoverManager,
) {
    data class IndexSummary(
        val added: Int,
        val removed: Int,
        val skipped: Int,
        val discProblems: Int = 0,
        val relocations: List<CartridgeSourceMatch.Relocation> = emptyList(),
    )

    /**
     * A scan is authoritative ONLY after every enabled provider and directory was
     * enumerated successfully. In particular, never prune on an IO/SAF failure
     * or coroutine cancellation: prior games and saves must remain discoverable.
     */
    suspend fun indexLibrary(): IndexSummary {
        val previousGames = retrogradedb.gameDao().selectShelfGames().first()
        // Two quick refreshes (or a backwards wall-clock adjustment) must not
        // reuse an index marker and leave removed games on the shelf.
        val startedAtMs = maxOf(System.currentTimeMillis(), (previousGames.maxOfOrNull { it.lastIndexedAt } ?: 0L) + 1)
        val before = previousGames.map { it.systemId to it.fileUri }.toSet()
        val skipped = AtomicInteger(0)
        val discProblems = AtomicInteger(0)

        try {
            indexProviders(startedAtMs, skipped, discProblems)
        } catch (error: Exception) {
            Timber.e(error, "Library refresh failed; existing games will not be pruned")
            throw error
        }

        // Match only already-enumerated unique stable SAF documents. Never change
        // a cartridge identity while scanning an incomplete provider or by filename.
        val relocations = reconcileVerifiedSourceChanges(previousGames, startedAtMs)

        // Cleanup follows completed enumeration, never finally/catch.
        cleanUp(startedAtMs)
        val after =
            retrogradedb.gameDao().selectShelfGames().first()
                .map { it.systemId to it.fileUri }.toSet()
        Timber.i("Library indexing completed in: ${System.currentTimeMillis() - startedAtMs} ms")
        return IndexSummary(
            added = (after - before).size,
            removed = (before - after).size,
            skipped = skipped.get(),
            discProblems = discProblems.get(),
            relocations = relocations,
        )
    }

    private suspend fun reconcileVerifiedSourceChanges(
        before: List<Game>,
        scannedAt: Long,
    ): List<CartridgeSourceMatch.Relocation> {
        val now = retrogradedb.gameDao().selectShelfGames().first()
        val relocations = CartridgeSourceMatch.uniqueRelocations(before, now, scannedAt)
        if (relocations.isEmpty()) return emptyList()
        val byOldId = before.associateBy { it.id }
        val byUri = now.associateBy { it.fileUri }
        val accepted = mutableListOf<CartridgeSourceMatch.Relocation>()
        relocations.forEach { match ->
            val original = byOldId[match.gameId] ?: return@forEach
            val candidate = byUri[match.currentUri] ?: return@forEach
            if (candidate.cartridgeId == original.cartridgeId) return@forEach
            retrogradedb.withTransaction {
                // Preserve original Room integer ID, UUID, art, favourites and aliases;
                // re-parent current disc files before removing the transient new record.
                retrogradedb.dataFileDao().transferToExistingGame(candidate.id, original.id)
                retrogradedb.gameDao().delete(listOf(candidate))
                retrogradedb.gameDao().update(
                    original.copy(
                        fileUri = candidate.fileUri,
                        fileName = candidate.fileName,
                        lastIndexedAt = scannedAt,
                    ),
                )
            }
            accepted += match
        }
        return accepted
    }

    @OptIn(FlowPreview::class)
    private suspend fun indexProviders(
        startedAtMs: Long,
        skipped: AtomicInteger,
        discProblems: AtomicInteger,
    ) {
        val gameMetadata = gameMetadataProvider.get()
        val enabledProviders = storageProviderRegistry.get().enabledProviders
        enabledProviders.asFlow()
            .flatMapConcat { indexSingleProvider(it, startedAtMs, gameMetadata, skipped, discProblems) }
            .collect()
    }

    @OptIn(FlowPreview::class)
    private fun indexSingleProvider(
        provider: StorageProvider,
        startedAtMs: Long,
        gameMetadata: GameMetadataProvider,
        skipped: AtomicInteger,
        discProblems: AtomicInteger,
    ): Flow<Unit> {
        return provider.listBaseStorageFiles()
            .flatMapConcat { StorageFilesMerger.mergeDataFiles(provider, it).asFlow() }
            .batchWithSizeAndTime(MAX_BUFFER_SIZE, MAX_TIME)
            .flatMapMerge { processBatch(it, provider, startedAtMs, gameMetadata, skipped, discProblems) }
    }

    private suspend fun processBatch(
        batch: List<GroupedStorageFiles>,
        provider: StorageProvider,
        startedAtMs: Long,
        gameMetadata: GameMetadataProvider,
        skipped: AtomicInteger,
        discProblems: AtomicInteger,
    ) = flow<Unit> {
        val entries = batch.map { fetchEntriesFromDatabase(it) }

        val existingEntries = entries.filterIsInstance<ScanEntry.GameFile>()
        handleExistingEntries(existingEntries, startedAtMs)

        val newEntries =
            entries.filterIsInstance<ScanEntry.File>()
                .map { buildEntryFromMetadata(it.file, provider, gameMetadata, startedAtMs) }

        // Includes unrecognised files (and BIOS candidates); no false claim that
        // every skipped item was a playable ROM.
        skipped.addAndGet(newEntries.count { it is ScanEntry.File })
        discProblems.addAndGet(
            newEntries.count {
                it is ScanEntry.File && it.file.primaryFile.extension in setOf("gdi", "cue", "m3u")
            },
        )
        handleNewEntries(newEntries, startedAtMs, provider)
    }

    private fun fetchEntriesFromDatabase(storageFile: GroupedStorageFiles): ScanEntry {
        Timber.d("Retrieving scan entry for uri: ${storageFile.primaryFile}")
        val game = retrogradedb.gameDao().selectByFileUri(storageFile.primaryFile.uri.toString())
        return buildScanEntry(storageFile, game)
    }

    private fun buildScanEntry(
        storageFile: GroupedStorageFiles,
        game: Game?,
    ): ScanEntry {
        return if (game != null) {
            ScanEntry.GameFile(storageFile, game)
        } else {
            ScanEntry.File(storageFile)
        }
    }

    private fun handleExistingEntries(
        entries: List<ScanEntry.GameFile>,
        startedAtMs: Long,
    ) {
        updateGames(entries, startedAtMs)
        updateDataFiles(entries, startedAtMs)
    }

    private fun updateGames(
        entries: List<ScanEntry.GameFile>,
        startedAtMs: Long,
    ) {
        val updatedGames =
            entries
                .map { entry ->
                    // SAF document IDs can remain stable when a file is renamed.
                    // Keep the database ID (and all player metadata/save identity),
                    // but launch the current filename. ZIP entries retain their
                    // internal ROM name instead of becoming the archive filename.
                    val primary = entry.file.primaryFile
                    val fileName = if (primary.extension == "zip") entry.game.fileName else primary.name
                    entry.game.copy(fileName = fileName, lastIndexedAt = startedAtMs)
                }

        updatedGames
            .forEach { Timber.d("Updating game: $it") }

        retrogradedb.gameDao().update(updatedGames)
    }

    private fun updateDataFiles(
        entries: List<ScanEntry.GameFile>,
        startedAtMs: Long,
    ) {
        val dataFiles =
            entries.flatMap { (storageFile, game) ->
                storageFile.dataFiles.map { convertIntoDataFile(game.id, it, startedAtMs) }
            }

        dataFiles
            .forEach { Timber.d("Updating data file: $it") }

        retrogradedb.dataFileDao().insert(dataFiles)
    }

    private fun convertIntoDataFile(
        gameId: Int,
        baseStorageFile: BaseStorageFile,
        startedAtMs: Long,
    ): DataFile {
        return DataFile(
            gameId = gameId,
            fileUri = baseStorageFile.uri.toString(),
            fileName = baseStorageFile.name,
            lastIndexedAt = startedAtMs,
            path = baseStorageFile.path,
        )
    }

    private fun handleNewEntries(
        entries: List<ScanEntry>,
        startedAtMs: Long,
        provider: StorageProvider,
    ) {
        val gameFiles =
            entries
                .filterIsInstance<ScanEntry.GameFile>()

        val unknownFiles =
            entries
                .filterIsInstance<ScanEntry.File>()
                .flatMap { it.file.allFiles() }

        handleNewGames(gameFiles, startedAtMs)
        handleUnknownFiles(provider, unknownFiles, startedAtMs)
    }

    private fun handleNewGames(
        pairs: List<ScanEntry.GameFile>,
        startedAtMs: Long,
    ) {
        val games =
            pairs
                .map { it.game }

        games.forEach { Timber.d("Insert: $it") }

        val gameIds = retrogradedb.gameDao().insert(games)
        val dataFiles =
            pairs
                .map { it.file.dataFiles }
                .zip(gameIds)
                .flatMap { (files, gameId) ->
                    files.map {
                        convertIntoDataFile(gameId.toInt(), it, startedAtMs)
                    }
                }

        retrogradedb.dataFileDao().insert(dataFiles)
    }

    private fun handleUnknownFiles(
        provider: StorageProvider,
        files: List<BaseStorageFile>,
        startedAtMs: Long,
    ) {
        files.forEach { baseStorageFile ->
            val storageFile = safeStorageFile(provider, baseStorageFile)
            if (storageFile != null && biosManager.isBiosCandidate(storageFile)) {
                // Opening all unknown game files here leaked descriptors on
                // every refresh and could abort large Android SAF scans.
                // BIOS import is the only consumer, and its stream is owned
                // and closed at this call site, including on exceptions.
                provider.getInputStream(storageFile.uri)?.use { inputStream ->
                    biosManager.tryAddBiosAfter(storageFile, inputStream, startedAtMs)
                }
            }
        }
    }

    private suspend fun buildEntryFromMetadata(
        groupedStorageFile: GroupedStorageFiles,
        provider: StorageProvider,
        metadataProvider: GameMetadataProvider,
        startedAtMs: Long,
    ): ScanEntry {
        // New imports must retain the full GDI family. Existing entries use the
        // existing-record branch and survive a temporarily missing companion.
        val names = groupedStorageFile.allFiles().map { it.name }.toSet()
        for (cue in groupedStorageFile.allFiles().filter { it.extension == "cue" }) {
            val requested = StorageFilesMerger.cueFileNames(provider, cue)
            if (requested == null ||
                requested.any {
                    DiscReferences.resolve(it, names) == null
                }
            ) {
                return ScanEntry.File(groupedStorageFile)
            }
        }
        for (playlist in groupedStorageFile.allFiles().filter { it.extension.equals("m3u", ignoreCase = true) }) {
            if (playlist.size !in 1..DiscPlaylist.MAX_BYTES) return ScanEntry.File(groupedStorageFile)
            val text =
                provider.getInputStream(playlist.uri)?.use { it.bufferedReader().readText() }
                    ?: throw java.io.IOException("Cannot read disc playlist ${playlist.name}")
            if (!DiscPlaylist.containsAllDiscs(text, names)) return ScanEntry.File(groupedStorageFile)
        }
        for (descriptor in groupedStorageFile.allFiles().filter { it.extension.equals("gdi", ignoreCase = true) }) {
            if (descriptor.size !in 1..65536) return ScanEntry.File(groupedStorageFile)
            val text =
                provider.getInputStream(descriptor.uri)?.use { it.bufferedReader().readText() }
                    ?: throw java.io.IOException("Cannot read GDI descriptor ${descriptor.name}")
            val identity = NextGenDiscIdentity.probe(descriptor.name, gdiText = text, availableTrackNames = names)
            if (identity.platform != NextGenDiscIdentity.Platform.DREAMCAST) return ScanEntry.File(groupedStorageFile)
        }
        // Descriptor identity owns the cartridge. Do not let an arbitrary track's
        // database filename override a positively identified Dreamcast disc/playlist.
        val primary = groupedStorageFile.primaryFile

        fun isDreamcast(file: BaseStorageFile): Boolean {
            val parsed = safeStorageFile(provider, file) ?: return false
            if (file.extension != "cue") return parsed.systemID == SystemID.DREAMCAST
            if (parsed.systemID != null && parsed.systemID != SystemID.DREAMCAST) return false
            val requested = StorageFilesMerger.cueFileNames(provider, file) ?: return false
            val matched = requested.mapNotNull { DiscReferences.resolve(it, names) }
            var boot = false
            val dataTracks =
                groupedStorageFile.dataFiles
                    .filter { it.name in matched && it.extension in setOf("bin", "raw") }.take(3)
            for (track in dataTracks) {
                val header =
                    provider.getInputStream(track.uri)?.use { input ->
                        val bytes = ByteArray(352840)
                        var count = 0
                        while (count < bytes.size) {
                            val read = input.read(bytes, count, bytes.size - count)
                            if (read <= 0) break
                            count += read
                        }
                        bytes.copyOf(count)
                    } ?: throw java.io.IOException("Cannot read disc track")
                // Positive legacy evidence beats a misplaced Dreamcast folder.
                val legacy =
                    SerialScanner.extractInfo(
                        track.name,
                        java.io.ByteArrayInputStream(header),
                    ).systemID
                if (legacy != null && legacy != SystemID.DREAMCAST) return false
                boot = boot || NextGenDiscIdentity.hasDreamcastBootSector(header)
            }
            return parsed.systemID == SystemID.DREAMCAST || boot
        }

        val dreamcast =
            when (primary.extension) {
                "cue" -> isDreamcast(primary)
                "m3u" -> {
                    val text = provider.getInputStream(primary.uri)?.use { it.bufferedReader().readText() }.orEmpty()
                    val requested = DiscPlaylist.parse(text).orEmpty()
                    requested.isNotEmpty() &&
                        requested.all { name ->
                            val actual = DiscReferences.resolve(name, names)
                            groupedStorageFile.dataFiles.firstOrNull { it.name == actual }?.let(::isDreamcast) == true
                        }
                }
                else -> false
            }
        if (dreamcast && GameSystem.all().any { it.id == SystemID.DREAMCAST }) {
            val parsed = safeStorageFile(provider, primary) ?: return ScanEntry.File(groupedStorageFile)
            val identified = parsed.copy(systemID = SystemID.DREAMCAST)
            val metadata = metadataProvider.retrieveMetadata(identified)
            val game = convertGameMetadataToGame(groupedStorageFile, identified, metadata, startedAtMs)
            return buildScanEntry(groupedStorageFile, game)
        }
        val game =
            sortedFilesForScanning(groupedStorageFile).asFlow()
                .mapNotNull { safeStorageFile(provider, it) }
                .mapNotNull { storageFile ->
                    val metadata = metadataProvider.retrieveMetadata(storageFile)
                    convertGameMetadataToGame(groupedStorageFile, storageFile, metadata, startedAtMs)
                }
                .firstOrNull()

        return buildScanEntry(groupedStorageFile, game)
    }

    private fun safeStorageFile(
        provider: StorageProvider,
        baseStorageFile: BaseStorageFile,
    ): StorageFile? {
        // A file read/parse failure makes enumeration incomplete. Do not
        // silently skip it and then delete its previously indexed game.
        return provider.getStorageFile(baseStorageFile)
    }

    private suspend fun cleanUp(startedAtMs: Long) {
        // Filesystem BIOS handling is upstream behaviour; the two library
        // record tables must be pruned as one transaction or not at all.
        removeDeletedBios(startedAtMs)
        retrogradedb.withTransaction {
            removeDeletedGames(startedAtMs)
            removeDeletedDataFiles(startedAtMs)
        }
    }

    private fun removeDeletedBios(startedAtMs: Long) {
        biosManager.deleteBiosBefore(startedAtMs)
    }

    private fun sortedFilesForScanning(groupedStorageFile: GroupedStorageFiles): List<BaseStorageFile> {
        val files = groupedStorageFile.dataFiles.sortedBy { it.name }
        return if (groupedStorageFile.primaryFile.extension.lowercase(java.util.Locale.US) in setOf("gdi", "m3u")) {
            listOf(groupedStorageFile.primaryFile) + files
        } else {
            files + listOf(groupedStorageFile.primaryFile)
        }
    }

    private fun convertGameMetadataToGame(
        groupedStorageFile: GroupedStorageFiles,
        storageFile: StorageFile,
        gameMetadata: GameMetadata?,
        lastIndexedAt: Long,
    ): Game? {
        if (gameMetadata == null) {
            return null
        }

        val gameSystem = GameSystem.findById(gameMetadata.system!!)

        // If the databased matched a data file (as with bin/cue) we force link the primary filename
        val fileName =
            if (groupedStorageFile.dataFiles.isNotEmpty()) {
                groupedStorageFile.primaryFile.name
            } else {
                storageFile.name
            }

        return Game(
            fileName = fileName,
            fileUri = groupedStorageFile.primaryFile.uri.toString(),
            title = gameMetadata.name ?: groupedStorageFile.primaryFile.name,
            systemId = gameSystem.id.dbname,
            developer = gameMetadata.developer,
            coverFrontUrl = gameMetadata.thumbnail,
            lastIndexedAt = lastIndexedAt,
        )
    }

    private fun removeDeletedDataFiles(startedAtMs: Long) {
        Timber.d("Deleting data files from db before: $startedAtMs")
        val dataFiles = retrogradedb.dataFileDao().selectByLastIndexedAtLessThan(startedAtMs)
        retrogradedb.dataFileDao().delete(dataFiles)
    }

    private fun removeDeletedGames(startedAtMs: Long) {
        Timber.d("Deleting games from db before: $startedAtMs")
        val games = retrogradedb.gameDao().selectByLastIndexedAtLessThan(startedAtMs)

        // A refresh removes stale library rows, not user-created artwork,
        // save RAM or states. Explicit delete/backup policy is separate.
        retrogradedb.gameDao().delete(games)
    }

    fun getGameFiles(
        game: Game,
        dataFiles: List<DataFile>,
        allowVirtualFiles: Boolean,
    ): RomFiles {
        val provider = storageProviderRegistry.get()
        return provider.getProvider(game).getGameRomFiles(game, dataFiles, allowVirtualFiles)
    }

    private sealed class ScanEntry {
        data class GameFile(val file: GroupedStorageFiles, val game: Game) : ScanEntry()

        data class File(val file: GroupedStorageFiles) : ScanEntry()
    }

    companion object {
        // We batch database updates to avoid unnecessary UI updates.
        const val MAX_BUFFER_SIZE = 200
        const val MAX_TIME = 5000
    }
}
