package com.swordfish.lemuroid.app.shared.game.pc

import android.content.Context
import android.net.Uri
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.pc.PcCartridgeStore
import java.io.File
import java.util.UUID

/** The profile service is not implemented yet. Never accept owner IDs or Junior grants from an Intent. */
object PcManagedSession {
    val localOwner: UUID = UUID.fromString("00000000-0000-4000-8000-000000000001")

    fun resolve(
        context: Context,
        game: Game,
    ): PcCartridgeStore.Entry? {
        if (game.systemId != SystemID.DOS.dbname) return null
        val uri = Uri.parse(game.fileUri)
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        val entry = PcCartridgeStore(context.filesDir).resolve(File(path)) ?: return null
        check(entry.id.toString() == game.cartridgeId) { "PC cartridge identity mismatch" }
        return entry
    }

    /** Future authenticated profile policy must replace this single-local-player boundary. */
    fun permits(
        profile: UUID,
        junior: Boolean,
    ): Boolean = profile == localOwner && !junior
}
