package com.swordfish.lemuroid.app.mobile.feature.cartmod.riivolution

import java.io.File

/**
 * CARTMOD / Riivolution mod scaffold — data model.
 *
 * SCAFFOLD ONLY: nothing in this package is wired to game boot, the Dolphin
 * core, or any UI. It exists so a later, separately gated slice (see #212)
 * can build on a tested scanner/stager instead of starting from zero.
 *
 * A Riivolution mod, as distributed for real Wii discs, is a folder tree:
 * one or more Riivolution XML configuration files (root element <wiidisc>)
 * plus the replacement files those XMLs reference through
 * <file external="..."> entries. On a real SD card the XMLs sit in a
 * `riivolution/` folder and the replacement files sit beside them.
 */
data class RiivolutionPatchFileRef(
    /** Path of the file on the emulated disc, e.g. "/files/Stage/Final.arc". */
    val discPath: String,
    /** Path of the replacement file, relative to the Riivolution folder. */
    val externalPath: String,
)

data class RiivolutionPatchSection(
    /** The patch id from <patch id="...">. */
    val id: String,
    val fileRefs: List<RiivolutionPatchFileRef>,
)

data class RiivolutionModPackage(
    /** Directory the mod was scanned from. */
    val rootDir: File,
    /** The Riivolution XML configuration file for this mod. */
    val xmlFile: File,
    /** Game id from <id game="...">, or null when the XML does not declare one. */
    val gameId: String?,
    val patches: List<RiivolutionPatchSection>,
) {
    /** Every replacement file the XMLs reference, relative to [rootDir]'s Riivolution folder. */
    val referencedExternalPaths: List<String>
        get() = patches.flatMap { patch -> patch.fileRefs.map { it.externalPath } }.distinct()
}

/** Result of scanning a candidate folder. */
sealed class RiivolutionScanResult {
    data class Found(val mod: RiivolutionModPackage) : RiivolutionScanResult()

    data class Rejected(val reason: String) : RiivolutionScanResult()
}

/**
 * One file copy the stager would perform to lay a mod out for Dolphin.
 * Nothing is copied by the scaffold; this is a plan a later slice executes.
 */
data class RiivolutionStagedFile(
    val source: File,
    /** Destination, relative to the Dolphin user directory. */
    val destinationRelativePath: String,
)

/**
 * Preset descriptor mirroring the shape of standalone Dolphin's saveable
 * Riivolution presets (.json): a named set of patches with enabled flags.
 * Scaffold-level: per-patch option choices are not modelled yet.
 */
data class RiivolutionPreset(
    val name: String,
    val gameId: String?,
    val patches: List<RiivolutionPresetPatch>,
) {
    fun toJson(): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"name\": ").append(jsonString(name)).append(",\n")
        if (gameId != null) {
            sb.append("  \"gameId\": ").append(jsonString(gameId)).append(",\n")
        }
        sb.append("  \"patches\": [")
        patches.forEachIndexed { index, patch ->
            if (index > 0) sb.append(",")
            sb.append("\n    { \"id\": ").append(jsonString(patch.id))
            sb.append(", \"enabled\": ").append(patch.enabled).append(" }")
        }
        if (patches.isNotEmpty()) sb.append("\n  ")
        sb.append("]\n}")
        return sb.toString()
    }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

data class RiivolutionPresetPatch(
    val id: String,
    val enabled: Boolean = true,
)

/** The full staging plan for one mod: file copies plus the preset descriptor. */
data class RiivolutionStagingPlan(
    val mod: RiivolutionModPackage,
    val files: List<RiivolutionStagedFile>,
    val preset: RiivolutionPreset,
    /** Referenced replacement files that were not present in the scanned folder. */
    val missingExternalPaths: List<String>,
)
