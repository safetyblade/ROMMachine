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
        for (descriptor in descriptors) {
            if (descriptor.size !in 1..65536) continue
            // IO failures propagate so an incomplete scan cannot prune existing games.
            val text =
                storageProvider.getInputStream(descriptor.uri)?.use { it.bufferedReader().readText() }
                    ?: throw java.io.IOException("Cannot read GDI descriptor ${descriptor.name}")
            // Group every present member of a structurally valid descriptor,
            // even while another track is missing. Completeness is checked by
            // discovery/loading; missing media must not turn the other tracks
            // into separate cartridges during refresh.
            val result = NextGenDiscIdentity.probe(descriptor.name, gdiText = text)
            if (result.platform != NextGenDiscIdentity.Platform.DREAMCAST) continue
            val matched =
                result.companionTracks.mapNotNull {
                    DiscReferences.resolve(
                        it,
                        originals.map {
                                file ->
                            file.name
                        },
                    )
                }
            val tracks = originals.filter { it.name in matched }
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
                if (requested == null ||
                    requested.any {
                        DiscReferences.resolve(
                            it,
                            companions.map {
                                    file ->
                                file.name
                            },
                        ) == null
                    }
                ) {
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
                val matched =
                    requested.mapNotNull {
                        DiscReferences.resolve(
                            it,
                            allFiles.keys.map {
                                    file ->
                                file.name
                            },
                        )
                    }
                val dataFiles = allFiles.filter { it.key.name in matched }
                // Never consume a partial family: standalone discs and an existing
                // playlist record must remain discoverable during an incomplete scan.
                if (matched.size != requested.size) return@forEach
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

    private fun mergeBinCueFiles(
        allFiles: MutableMap<BaseStorageFile, List<BaseStorageFile>>,
        storageProvider: StorageProvider,
    ) {
        val originals = allFiles.keys.toList()
        for (cue in originals.filter { it.extension.equals("cue", true) }) {
            val requested = cueFileNames(storageProvider, cue) ?: continue
            // Like GDI, present tracks remain subordinate even when a member is missing.
            val matched = requested.mapNotNull { DiscReferences.resolve(it, originals.map { file -> file.name }) }
            val tracks = originals.filter { it.name in matched && it != cue }
            allFiles[cue] = tracks
            tracks.forEach { allFiles.remove(it) }
        }
    }

    fun cueFileNames(
        storageProvider: StorageProvider,
        cue: BaseStorageFile,
    ): List<String>? {
        if (cue.size !in 1..65536) return null
        val text =
            storageProvider.getInputStream(cue.uri)?.use { it.bufferedReader().readText() }
                ?: throw java.io.IOException("Cannot read CUE descriptor ${cue.name}")
        return DiscReferences.cue(text)?.takeIf { names -> names.none { it.equals(cue.name, true) } }
    }
}
