package com.swordfish.lemuroid.lib.storage.local

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.DataFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.io.File
import java.security.MessageDigest

object GameCacheUtils {
    fun getDataFileForGame(
        folderName: String,
        context: Context,
        game: Game,
        dataFile: DataFile,
    ): File {
        val gamesCacheDir = getCacheDirForGame(folderName, game, context)
        return File(gamesCacheDir, dataFile.fileName)
    }

    fun getCacheFileForGame(
        folderName: String,
        context: Context,
        game: Game,
    ): File {
        val gamesCacheDir = getCacheDirForGame(folderName, game, context)
        return File(gamesCacheDir, game.fileName)
    }

    private fun getCacheDirForGame(
        folderName: String,
        game: Game,
        context: Context,
    ): File {
        // Different disc folders can contain an identically named game.iso.
        // Scope disposable ROM cache to the indexed URI, not just the system.
        // No shelf identifiers, installed saves or ROM locations are changed.
        val digest =
            MessageDigest.getInstance("SHA-256")
                .digest(game.fileUri.toByteArray(Charsets.UTF_8))
                .take(8)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val gamesCachePath = buildPath(folderName, game.systemId, digest)
        val gamesCacheDir = File(context.cacheDir, gamesCachePath)
        gamesCacheDir.mkdirs()
        return gamesCacheDir
    }

    private fun buildPath(vararg chunks: String): String {
        return chunks.joinToString(separator = File.separator)
    }
}
