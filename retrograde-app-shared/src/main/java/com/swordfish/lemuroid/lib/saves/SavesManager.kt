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

class SavesManager(
    private val directoriesManager: DirectoriesManager,
    private val binding: CartridgeSaveBinding? = null,
) {
    suspend fun getSaveRAM(
        game: Game,
        systemCoreConfig: SystemCoreConfig,
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            val result =
                runCatchingWithRetry(FILE_ACCESS_RETRIES) {
                    val saveFile = getSaveFile(game, systemCoreConfig.coreID)
                    if (saveFile.exists() && saveFile.length() > 0) {
                        saveFile.readBytes()
                    } else {
                        if (systemCoreConfig.coreID == CoreID.PCSX_REARMED_MODERN && !saveFile.exists()) {
                            // Both PCSX engines expose an unchanged raw 128 KiB card.
                            // Seed only an absent candidate slot; never synchronise back
                            // over the fallback, or overwrite newer candidate progress.
                            // GameLoader retained a verified baseline backup before this.
                            val previous = getSaveFile(game, CoreID.PCSX_REARMED)
                            if (previous.isFile && previous.length() == PlayStationMemoryCard.SIZE.toLong()) {
                                val card = previous.readBytes()
                                if (PlayStationMemoryCard.isRawCard(card)) {
                                    saveFile.writeBytesAtomic(card)
                                    return@runCatchingWithRetry card
                                }
                            }
                        }
                        if (isIsolatedEngine(systemCoreConfig.coreID)) {
                            // Never import an unverified foreign save-RAM format implicitly.
                            null
                        } else {
                            val savesMigrator = systemCoreConfig.getSavesMigrator()
                            if (binding == null || binding.mayImportLegacy(game, true)) {
                                savesMigrator?.loadPreviousSaveForGame(game, directoriesManager)
                            } else {
                                null
                            }
                        }
                    }
                }
            result.getOrThrow()
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

                    val saveFile = getSaveFile(game, coreID)
                    saveFile.writeBytesAtomic(data)
                }
            result.getOrThrow()
        }
    }

    suspend fun getSaveRAMInfo(
        game: Game,
        coreID: CoreID? = null,
    ): SaveInfo {
        return withContext(Dispatchers.IO) {
            val saveFile = getSaveFile(game, coreID)
            val fileExists = saveFile.exists() && saveFile.length() > 0
            SaveInfo(fileExists, saveFile.lastModified())
        }
    }

    private suspend fun getSaveFile(
        game: Game,
        coreID: CoreID?,
    ): File {
        val root = directoriesManager.getSavesDirectory()
        val scope = if (isIsolatedEngine(coreID)) coreID!!.coreName else "shared"
        val legacyDirectory =
            if (isIsolatedEngine(coreID)) {
                File(root, "engine-save-ram/$scope").apply { mkdirs() }
            } else {
                root
            }
        val legacy = File(legacyDirectory, getSaveRAMFileName(game))
        return binding?.sramFile(game, legacy, scope) ?: legacy
    }

    private fun isIsolatedEngine(coreID: CoreID?): Boolean =
        coreID in
            setOf(
                CoreID.PCSX_REARMED_MODERN,
                CoreID.PPSSPP_MODERN,
                CoreID.FLYCAST_MODERN,
                CoreID.DOLPHIN_MODERN,
                CoreID.PLAY_MODERN,
            )

    /** This name should make it compatible with RetroArch so that users can freely sync saves across the two application. */
    private fun getSaveRAMFileName(game: Game) = "${game.saveFileName().substringBeforeLast(".")}.srm"

    companion object {
        private const val FILE_ACCESS_RETRIES = 3
    }
}
