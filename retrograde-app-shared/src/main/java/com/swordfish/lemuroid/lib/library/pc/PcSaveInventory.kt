package com.swordfish.lemuroid.lib.library.pc

import java.io.File

/** Inventory seam only. Never auto-export an installed OS, game media, or credentials as game saves. */
object PcSaveInventory {
    enum class Kind {
        GUEST_DISK_RECOVERY_ONLY,
        UNCLASSIFIED_PC_PROGRESS,
    }

    data class Artifact(val file: File, val bytes: Long, val kind: Kind) {
        val autoExportAllowed: Boolean get() = false
    }

    fun inspect(workspace: PcWorkspace): List<Artifact> {
        val disks =
            PcGuestDiskStore(workspace).disks().map {
                Artifact(it, it.length(), Kind.GUEST_DISK_RECOVERY_ONLY)
            }
        val progress =
            workspace.progress.listFiles().orEmpty().filter {
                it.isFile && !it.name.startsWith(".") &&
                    it.canonicalFile == File(workspace.progress.canonicalFile, it.name)
            }.map { Artifact(it, it.length(), Kind.UNCLASSIFIED_PC_PROGRESS) }
        return disks + progress
    }
}
