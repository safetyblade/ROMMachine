package com.swordfish.lemuroid.app.mobile.feature.cartmod.riivolution

import java.io.File

/**
 * CARTMOD / Riivolution mod scaffold — scanner and stager.
 *
 * SCAFFOLD ONLY: not wired to game boot, the Dolphin core, or any UI.
 * See #212 for the gate this foundation is staged for.
 *
 * Recognition rule: a folder is a Riivolution mod when it contains a
 * `riivolution/` subfolder (the on-SD layout) or itself holds an XML file
 * whose root element is <wiidisc>. The XML is read with deliberately light
 * extraction (ids and file references only); full Riivolution semantics
 * belong to Dolphin's own DiscIO parser, not to this scanner.
 */
object RiivolutionModScanner {

    /** Dolphin's Riivolution load directory, relative to the Dolphin user dir. */
    const val DOLPHIN_RIIVOLUTION_RELATIVE_DIR = "Load/Riivolution"

    private val gameIdRegex = Regex("""<id\s+[^>]*game\s*=\s*"([^"]+)"""")
    private val patchStartRegex = Regex("""<patch\s+[^>]*id\s*=\s*"([^"]+)"""")
    private val fileRefRegex =
        Regex("""<file\s+[^>]*disc\s*=\s*"([^"]+)"[^>]*external\s*=\s*"([^"]+)"""")
    private val fileRefAltRegex =
        Regex("""<file\s+[^>]*external\s*=\s*"([^"]+)"[^>]*disc\s*=\s*"([^"]+)"""")

    fun scan(modRoot: File): RiivolutionScanResult {
        if (!modRoot.isDirectory) {
            return RiivolutionScanResult.Rejected("not a directory: ${modRoot.path}")
        }
        val riivolutionDir = File(modRoot, "riivolution")
            .takeIf { it.isDirectory }
            ?: modRoot
        val xmlFiles = riivolutionDir.listFiles { f -> f.isFile && f.extension.equals("xml", true) }
            ?.sortedBy { it.name }
            .orEmpty()
        if (xmlFiles.isEmpty()) {
            return RiivolutionScanResult.Rejected("no Riivolution XML found in ${riivolutionDir.path}")
        }
        for (xml in xmlFiles) {
            val text = runCatching { xml.readText() }.getOrNull() ?: continue
            if (!text.contains("<wiidisc")) continue
            return RiivolutionScanResult.Found(parseMod(modRoot, riivolutionDir, xml, text))
        }
        return RiivolutionScanResult.Rejected("XML files present but none is a Riivolution <wiidisc> config")
    }

    private fun parseMod(
        modRoot: File,
        riivolutionDir: File,
        xml: File,
        text: String,
    ): RiivolutionModPackage {
        val gameId = gameIdRegex.find(text)?.groupValues?.get(1)
        val patches = mutableListOf<RiivolutionPatchSection>()
        val patchStarts = patchStartRegex.findAll(text).toList()
        patchStarts.forEachIndexed { index, match ->
            val end = patchStarts.getOrNull(index + 1)?.range?.first ?: text.length
            val body = text.substring(match.range.first, end)
            val refs = mutableListOf<RiivolutionPatchFileRef>()
            fileRefRegex.findAll(body).forEach { m ->
                refs += RiivolutionPatchFileRef(discPath = m.groupValues[1], externalPath = m.groupValues[2])
            }
            fileRefAltRegex.findAll(body).forEach { m ->
                refs += RiivolutionPatchFileRef(discPath = m.groupValues[2], externalPath = m.groupValues[1])
            }
            patches += RiivolutionPatchSection(id = match.groupValues[1], fileRefs = refs)
        }
        // The package root for staging is the folder holding the XML's tree:
        // replacement files resolve relative to the riivolution/ folder.
        val effectiveRoot = if (riivolutionDir == modRoot) modRoot else modRoot
        return RiivolutionModPackage(
            rootDir = effectiveRoot,
            xmlFile = xml,
            gameId = gameId,
            patches = patches,
        )
    }

    /**
     * Compute where each file of [mod] belongs under a Dolphin user
     * directory ([dolphinUserDir], e.g. `.../User`), and the preset
     * descriptor a future boot slice would hand to Dolphin.
     * Pure computation: nothing is read from or written to [dolphinUserDir].
     */
    fun stagingPlan(mod: RiivolutionModPackage, dolphinUserDir: File): RiivolutionStagingPlan {
        val riivolutionDir = File(mod.rootDir, "riivolution").takeIf { it.isDirectory } ?: mod.rootDir
        val files = mutableListOf<RiivolutionStagedFile>()
        files += RiivolutionStagedFile(
            source = mod.xmlFile,
            destinationRelativePath = "$DOLPHIN_RIIVOLUTION_RELATIVE_DIR/${mod.xmlFile.name}",
        )
        val missing = mutableListOf<String>()
        for (externalPath in mod.referencedExternalPaths) {
            val source = File(riivolutionDir, externalPath)
            if (source.isFile) {
                files += RiivolutionStagedFile(
                    source = source,
                    destinationRelativePath = "$DOLPHIN_RIIVOLUTION_RELATIVE_DIR/$externalPath",
                )
            } else {
                missing += externalPath
            }
        }
        val preset = RiivolutionPreset(
            name = mod.xmlFile.nameWithoutExtension,
            gameId = mod.gameId,
            patches = mod.patches.map { RiivolutionPresetPatch(id = it.id, enabled = true) },
        )
        return RiivolutionStagingPlan(
            mod = mod,
            files = files,
            preset = preset,
            missingExternalPaths = missing,
        )
    }
}
