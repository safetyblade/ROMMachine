package com.swordfish.lemuroid.lib.library.pc

import java.io.File
import java.io.FileOutputStream
import java.util.Properties
import java.util.UUID

/** Metadata for a profile-owned install; completion must be reported by the runtime, not import. */
data class PcInstallRecord(
    val profile: UUID,
    val cartridge: UUID,
    val title: String,
    val executable: String,
    val mediaName: String,
    val engine: Engine,
    val stage: Stage,
) {
    enum class Engine { DOSBOX_PURE, WINE }
    enum class Stage { MEDIA_IMPORTED, GUEST_READY, GAME_INSTALLED }

    init {
        require(title.isNotBlank() && title.length <= 256)
        require(mediaName.matches(Regex("[A-Za-z0-9_.-]+")) && !mediaName.contains(".."))
        require(executable.length <= 260 && !executable.contains('\u0000'))
        if (stage == Stage.GAME_INSTALLED) require(executable.isNotBlank())
    }

    fun store(root: File) {
        val workspace = PcWorkspace(root, profile, cartridge)
        workspace.prepareWritableDirectories()
        require(File(workspace.media, mediaName).isFile)
        val file = File(workspace.runtime, "install.properties")
        val temporary = File.createTempFile("install-", ".tmp", workspace.runtime)
        try {
            val props = Properties().apply {
                setProperty("profile", profile.toString())
                setProperty("cartridge", cartridge.toString())
                setProperty("title", title)
                setProperty("executable", executable)
                setProperty("media", mediaName)
                setProperty("engine", engine.name)
                setProperty("stage", stage.name)
            }
            FileOutputStream(temporary).use { props.store(it, null); it.fd.sync() }
            java.nio.file.Files.move(temporary.toPath(), file.toPath(),
                java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } finally { temporary.delete() }
    }

    companion object {
        fun load(root: File, profile: UUID, cartridge: UUID): PcInstallRecord? {
            val workspace = PcWorkspace(root, profile, cartridge)
            val file = File(workspace.runtime, "install.properties")
            if (!file.isFile) return null
            val p = Properties().apply { file.inputStream().use { load(it) } }
            require(p.getProperty("profile") == profile.toString())
            require(p.getProperty("cartridge") == cartridge.toString())
            return PcInstallRecord(profile, cartridge, p.getProperty("title"),
                p.getProperty("executable"), p.getProperty("media"),
                Engine.valueOf(p.getProperty("engine")), Stage.valueOf(p.getProperty("stage")))
        }
    }
}
