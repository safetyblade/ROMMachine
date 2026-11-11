package com.swordfish.lemuroid.lib.library

import com.swordfish.lemuroid.lib.storage.BaseStorageFile
import com.swordfish.lemuroid.lib.storage.GroupedStorageFiles
import com.swordfish.lemuroid.lib.storage.StorageProvider
import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity

object StorageFilesMerger {
    /** Merge files which belong to the same game. This includes bin/cue files and m3u playlists.*/
    fun mergeDataFiles(
        storageProvider: StorageProvider,
        files: List<BaseStorageFile>,
    ): List<GroupedStorageFiles> {
        val allFiles =
            files
                .associateWith { listOf<BaseStorageFile>() }
                .toMutableMap()

        mergeGdiFiles(allFiles, storageProvider)
        mergeBinCueFiles(allFiles, storageProvider)
        removeInvalidBinCuePairs(allFiles, storageProvider)
        mergeM3UPlaylists(allFiles, storageProvider)
        removeInvalidM3UPlaylists(allFiles, storageProvider)

        return allFiles.map { GroupedStorageFiles(it.key, it.value) }
    }

    private fun mergeGdiFiles(
        allFiles: MutableMap<BaseStorageFile, List<BaseStorageFile>>,
        storageProvider: StorageProvider,
    ) {
        val originals = allFiles.keys.toList()
        val descriptors = originals.filter { it.extension.equals("gdi", ignoreCase = true) }
        val names = originals.map { it.name }.toSet()
        for (descriptor in descriptors) {
            if (descriptor.size !in 1..65536) continue
            // IO failures propagate so an incomplete scan cannot prune existing games.
            val text =
                storageProvider.getInputStream(descriptor.uri)?.use { it.bufferedReader().readText() }
                    ?: throw java.io.IOException("Cannot read GDI descriptor ${descriptor.name}")
            val result = NextGenDiscIdentity.probe(descriptor.name, gdiText = text, availableTrackNames = names)
            if (result.platform != NextGenDiscIdentity.Platform.DREAMCAST) continue
            val tracks = originals.filter { it.name in result.companionTracks }
            allFiles[descriptor] = tracks
            tracks.forEach { allFiles.remove(it) }
        }
    }

    private fun removeInvalidM3UPlaylists(
        allFiles: MutableMap<BaseStorageFile, List<BaseStorageFile>>,
        storageProvider: StorageProvider,
    ) {
        allFiles.keys.toList()
            .filter { it.extension.equals("m3u", ignoreCase = true) }
            .forEach { playlist ->
                val requested = readPlaylist(storageProvider, playlist)
                val companions = allFiles[playlist].orEmpty()
                if (requested == null || !companions.map { it.name }.containsAll(requested)) {
                    // Keep the primary in enumeration so existing database references
                    // survive a temporarily invalid/missing disc. Do not hide its discs.
                    companions.forEach { allFiles.putIfAbsent(it, emptyList()) }
                    allFiles[playlist] = emptyList()
                }
            }
    }

    private fun mergeM3UPlaylists(
        allFiles: MutableMap<BaseStorageFile, List<BaseStorageFile>>,
        storageProvider: StorageProvider,
    ) {
        val toBeRemoved = mutableListOf<BaseStorageFile>()
        allFiles.keys.toList()
            .filter { it.extension.equals("m3u", ignoreCase = true) }
            .forEach { playlist ->
                val requested = readPlaylist(storageProvider, playlist) ?: return@forEach
                val dataFiles = allFiles.filter { it.key.name in requested }
                // Never consume a partial family: standalone discs and an existing
                // playlist record must remain discoverable during an incomplete scan.
                if (!dataFiles.keys.map { it.name }.containsAll(requested)) return@forEach
                allFiles[playlist] = dataFiles.flatMap { listOf(it.key) + it.value }.distinct()
                toBeRemoved.addAll(dataFiles.keys)
            }
        toBeRemoved.forEach { allFiles.remove(it) }
    }

    private fun readPlaylist(
        storageProvider: StorageProvider,
        playlist: BaseStorageFile,
    ): List<String>? {
        if (playlist.size !in 1..DiscPlaylist.MAX_BYTES) return null
        // Read errors propagate; an incomplete scan must not prune existing games.
        val text =
            storageProvider.getInputStream(playlist.uri)?.use { it.bufferedReader().readText() }
                ?: throw java.io.IOException("Cannot read disc playlist ${playlist.name}")
        return DiscPlaylist.parse(text)
    }

    private fun removeInvalidBinCuePairs(
        allFiles: MutableMap<BaseStorageFile, List<BaseStorageFile>>,
        storageProvider: StorageProvider,
    ) {
        // Keep incomplete primaries in enumeration so an existing cartridge
        // survives a temporarily missing track. New imports validate the family.
        for (cue in allFiles.keys.toList().filter { it.extension == "cue" }) {
            val requested = cueFileNames(storageProvider, cue)
            if (requested == null || !allFiles[cue].orEmpty().map { it.name }.containsAll(requested)) {
                allFiles[cue].orEmpty().forEach { allFiles.putIfAbsent(it, emptyList()) }
                allFiles[cue] = emptyList()
            }
        }
    }

    private fun mergeBinCueFiles(
        allFiles: MutableMap<BaseStorageFile, List<BaseStorageFile>>,
        storageProvider: StorageProvider,
    ) {
        val toBeRemoved = mutableListOf<BaseStorageFile>()

        allFiles.keys
            .asSequence()
            .filter { it.extension == "cue" }
            .forEach { cueFile ->
                val requestedBinFiles = cueFileNames(storageProvider, cueFile) ?: return@forEach
                if (!allFiles.keys.map { it.name }.containsAll(requestedBinFiles)) return@forEach

                val binFiles =
                    allFiles
                        .filter { it.key.name in requestedBinFiles }

                allFiles[cueFile] = (allFiles[cueFile] ?: listOf()) +
                    binFiles.flatMap {
                        listOf(it.key) + it.value
                    }
                toBeRemoved.addAll(binFiles.keys)
            }

        toBeRemoved.forEach { allFiles.remove(it) }
    }

    fun cueFileNames(
        storageProvider: StorageProvider,
        cue: BaseStorageFile,
    ): List<String>? {
        if (cue.size !in 1..65536) return null
        val text =
            storageProvider.getInputStream(cue.uri)?.use { it.bufferedReader().readText() }
                ?: throw java.io.IOException("Cannot read CUE descriptor ${cue.name}")
        if (text.length > 65536) return null
        val names =
            Regex(
                """^\s*FILE\s+"([^"]+)"\s+\S+\s*$""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
            )
                .findAll(text.removePrefix("\uFEFF")).map { it.groupValues[1] }.toList()
        val directives =
            Regex("""^\s*FILE\b""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
                .findAll(text.removePrefix("\uFEFF")).count()
        if (directives != names.size || names.isEmpty() ||
            names.any {
                it.isBlank() || '/' in it || '\\' in it || ':' in it || it == ".." ||
                    '\u0000' in it || it.equals(cue.name, ignoreCase = true)
            }
        ) {
            return null
        }
        return names.distinct()
    }
}
