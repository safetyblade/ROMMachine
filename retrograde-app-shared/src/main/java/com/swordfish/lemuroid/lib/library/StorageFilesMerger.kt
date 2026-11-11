package com.swordfish.lemuroid.lib.library

import android.net.Uri
import com.swordfish.lemuroid.common.files.readLines
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
        val toBeRemoved = mutableListOf<BaseStorageFile>()

        allFiles.keys
            .asSequence()
            .filter { it.extension == "cue" }
            .forEach {
                val requestedFileNames = extractBinFiles(storageProvider, it.uri).toSet()
                val givenFileNames = allFiles[it]?.map { it.name }?.toSet() ?: setOf()

                if (requestedFileNames != givenFileNames) toBeRemoved.add(it)
            }

        toBeRemoved.forEach { allFiles.remove(it) }
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
                val requestedBinFiles = extractBinFiles(storageProvider, cueFile.uri)

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

    private fun extractBinFiles(
        storageProvider: StorageProvider,
        uri: Uri,
    ): List<String> {
        return runCatching {
            storageProvider.getInputStream(uri)?.readLines()
                ?.mapNotNull { Regex("FILE \"(.*)\"").find(it)?.groupValues?.get(1) }
                ?: listOf()
        }.getOrDefault(listOf())
    }
}
