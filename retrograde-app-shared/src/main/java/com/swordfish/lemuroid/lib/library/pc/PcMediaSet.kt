package com.swordfish.lemuroid.lib.library.pc

import java.io.File
import java.io.InputStream
import java.util.UUID

/** Explicit selection only. CUE input is restricted to a single local binary track file. */
object PcMediaSet {
    private val fileLine = Regex("""^\s*FILE\s+"([^"]+)"\s+BINARY\s*$""", RegexOption.IGNORE_CASE)

    fun cueTrack(cue: String): String {
        require(cue.length <= 65536)
        val declarations = cue.lineSequence().filter {
            it.trimStart().startsWith("FILE ", ignoreCase = true)
        }.toList()
        require(declarations.size == 1)
        val name = requireNotNull(fileLine.matchEntire(declarations.single())).groupValues[1]
        require(name.matches(Regex("[A-Za-z0-9 _().-]+[.]bin", RegexOption.IGNORE_CASE)))
        require(!name.contains(".."))
        return name
    }

    /** Copies a verified user-selected pair and rewrites the descriptor to a generated local name. */
    fun importCue(workspace: PcWorkspace, cue: String, selectedTrackName: String, track: InputStream): File {
        require(cueTrack(cue) == selectedTrackName)
        val binary = workspace.importMedia("bin", track)
        val descriptor = File(workspace.media, "${UUID.randomUUID()}.cue")
        try {
            descriptor.writeText(cue.lineSequence().joinToString("\n") {
                if (fileLine.matches(it)) "FILE \"${binary.name}\" BINARY" else it
            } + "\n")
            check(descriptor.setReadOnly())
            return descriptor
        } catch (error: Exception) {
            descriptor.delete()
            binary.delete()
            throw error
        }
    }
}
