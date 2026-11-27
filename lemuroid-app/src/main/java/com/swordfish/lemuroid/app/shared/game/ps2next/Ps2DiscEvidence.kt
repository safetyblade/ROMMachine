package com.swordfish.lemuroid.app.shared.game.ps2next

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity

internal object Ps2DiscEvidence {
    /** On-demand descriptor reads. This never stages media, hashes the disc or exports the URI. */
    fun serial(
        context: Context,
        game: Game,
    ): String? {
        if (game.systemId != SystemID.PLAYSTATION_2.dbname || !game.fileName.endsWith(".iso", ignoreCase = true)) {
            return null
        }
        return runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(Uri.parse(game.fileUri), "r") ?: return null
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
                NextGenDiscIdentity.probePs2Iso(game.fileName, it.channel).discSerial
            }
        }.getOrNull()
    }
}
