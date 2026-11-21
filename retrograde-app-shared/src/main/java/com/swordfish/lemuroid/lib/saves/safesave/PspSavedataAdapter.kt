package com.swordfish.lemuroid.lib.saves.safesave

import java.io.File

/**
 * SAVE-A01 PSP-only adapter for PPSSPP Modern.
 *
 * Verified layout (GameLoader / PSPSavedataSeed source): the modern save root
 * holds `PSP/SAVEDATA/<group>/...`, one directory per native savedata group.
 * This adapter handles exactly ONE such group. It never scans other consoles,
 * never touches `PSP/PPSSPP_STATE`, and rejects symlinks the same way
 * [com.swordfish.lemuroid.lib.saves.PSPSavedataSeed] does.
 */
object PspSavedataAdapter {
    const val PLATFORM = "PSP"
    const val ENGINE_ID = "ppsspp_modern"
    const val SAVE_TYPE = "PSP_SAVEDATA"

    private val GROUP_NAME = Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")

    fun savedataRoot(modernSaveRoot: File): File = File(File(modernSaveRoot, "PSP"), "SAVEDATA")

    data class DiscoveredGroup(
        val groupName: String,
        val directory: File,
        val files: List<File>,
        val totalBytes: Long,
        val latestModified: Long,
    )

    /**
     * Discover one explicitly named group. The group name selects a directory;
     * it is never used to infer which cartridge owns the save.
     */
    fun discover(
        modernSaveRoot: File,
        groupName: String,
    ): DiscoveredGroup {
        require(GROUP_NAME.matches(groupName)) { "Unsafe PSP savedata group name" }
        val root = savedataRoot(modernSaveRoot)
        val group = File(root, groupName)
        require(group.isDirectory) { "PSP savedata group not found: $groupName" }
        val groupCanonical = group.canonicalFile
        val files = mutableListOf<File>()
        group.walkTopDown().forEach { entry ->
            if (entry == group) return@forEach
            check(
                entry.canonicalFile.path == groupCanonical.path ||
                    entry.canonicalFile.path.startsWith(groupCanonical.path + File.separator),
            ) {
                "PSP savedata symlinks are not supported"
            }
            if (entry.isFile) files += entry
        }
        require(files.isNotEmpty()) { "PSP savedata group is empty: $groupName" }
        return DiscoveredGroup(
            groupName = groupName,
            directory = group,
            files = files.sortedBy { it.relativeTo(group).path },
            totalBytes = files.sumOf { it.length() },
            latestModified = files.maxOf { it.lastModified() },
        )
    }

    /** Relative path of [file] inside its savedata group, with traversal rejected. */
    fun relativePath(
        group: File,
        file: File,
    ): String {
        val relative = file.relativeTo(group).path
        require(relative.isNotBlank() && !relative.startsWith("..") && '\\' !in relative) {
            "Unsafe PSP savedata relative path"
        }
        return relative
    }
}
