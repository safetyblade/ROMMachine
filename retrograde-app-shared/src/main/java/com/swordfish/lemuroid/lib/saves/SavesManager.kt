package com.swordfish.lemuroid.lib.saves

import com.swordfish.lemuroid.common.kotlin.runCatchingWithRetry
import com.swordfish.lemuroid.common.kotlin.writeBytesAtomic
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.migrators.getSavesMigrator
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SavesManager(private val directoriesManager: DirectoriesManager) {
    suspend fun getSaveRAM(
        game: Game,
        systemCoreConfig: SystemCoreConfig,
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            val result =
                runCatchingWithRetry(FILE_ACCESS_RETRIES) {
                    val saveFile = getSaveFile(getSaveRAMFileName(game), systemCoreConfig.coreID)
                    if (saveFile.exists() && saveFile.length() > 0) {
                        saveFile.readBytes()
                    } else {
                        if (isIsolatedEngine(systemCoreConfig.coreID)) {
                            // Never import an unverified foreign save-RAM format implicitly.
                            null
                        } else {
                            val savesMigrator = systemCoreConfig.getSavesMigrator()
                            savesMigrator?.loadPreviousSaveForGame(game, directoriesManager)
                        }
                    }
                }
            result.getOrNull()
        }
    }

    suspend fun setSaveRAM(
        game: Game,
        data: ByteArray,
        coreID: CoreID? = null,
    ) {
        withContext(Dispatchers.IO) {
            val result =
                runCatchingWithRetry(FILE_ACCESS_RETRIES) {
                    if (data.isEmpty()) {
                        return@runCatchingWithRetry
                    }

                    val saveFile = getSaveFile(getSaveRAMFileName(game), coreID)
                    saveFile.writeBytesAtomic(data)
                }
            result.getOrNull()
        }
    }

    suspend fun getSaveRAMInfo(game: Game, coreID: CoreID? = null): SaveInfo {
        return withContext(Dispatchers.IO) {
            val saveFile = getSaveFile(getSaveRAMFileName(game), coreID)
            val fileExists = saveFile.exists() && saveFile.length() > 0
            SaveInfo(fileExists, saveFile.lastModified())
        }
    }

    private suspend fun getSaveFile(fileName: String, coreID: CoreID?): File {
        return withContext(Dispatchers.IO) {
            val root = directoriesManager.getSavesDirectory()
            val directory =
                if (isIsolatedEngine(coreID)) {
                    File(root, "engine-save-ram/${coreID!!.coreName}").apply { mkdirs() }
                } else {
                    root // Existing save paths are never renamed or overwritten by migration.
                }
            File(directory, fileName)
        }
    }

    private fun isIsolatedEngine(coreID: CoreID?): Boolean =
        coreID == CoreID.PCSX_REARMED_MODERN || coreID == CoreID.PPSSPP_MODERN

    /** This name should make it compatible with RetroArch so that users can freely sync saves across the two application. */
    private fun getSaveRAMFileName(game: Game) = "${game.fileName.substringBeforeLast(".")}.srm"

    companion object {
        private const val FILE_ACCESS_RETRIES = 3
    }
}
