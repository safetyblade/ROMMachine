/*
 * GameLoader.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.swordfish.lemuroid.lib.game

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.swordfish.lemuroid.lib.bios.BiosManager
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.core.NativeCoreProvisioner
import com.swordfish.lemuroid.lib.core.NativeGraphicsRequirements
import com.swordfish.lemuroid.lib.core.assetsmanager.DolphinAssetsProvisioner
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.LemuroidLibrary
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.migration.DesmumeMigrationHandler
import com.swordfish.lemuroid.lib.saves.EmulatorMigrationBackup
import com.swordfish.lemuroid.lib.saves.SaveState
import com.swordfish.lemuroid.lib.saves.SavesCoherencyEngine
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import com.swordfish.lemuroid.lib.storage.RomFiles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.File

class GameLoader(
    private val lemuroidLibrary: LemuroidLibrary,
    private val statesManager: StatesManager,
    private val savesManager: SavesManager,
    private val coreVariablesManager: CoreVariablesManager,
    private val retrogradeDatabase: RetrogradeDatabase,
    private val savesCoherencyEngine: SavesCoherencyEngine,
    private val directoriesManager: DirectoriesManager,
    private val biosManager: BiosManager,
    private val desmumeMigrationHandler: DesmumeMigrationHandler,
) {
    sealed class LoadingState {
        object LoadingCore : LoadingState()

        object LoadingGame : LoadingState()

        class Ready(val gameData: GameData) : LoadingState()
    }

    fun load(
        appContext: Context,
        game: Game,
        loadSave: Boolean,
        systemCoreConfig: SystemCoreConfig,
        directLoad: Boolean,
    ): Flow<LoadingState> =
        flow {
            try {
                emit(LoadingState.LoadingCore)

                val system = GameSystem.findById(game.systemId)

                if (!isArchitectureSupported(systemCoreConfig)) {
                    throw GameLoaderException(GameLoaderError.UnsupportedArchitecture)
                }

                val advertisedGles =
                    (appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                        ?.deviceConfigurationInfo?.reqGlEsVersion ?: 0
                if (!NativeGraphicsRequirements.supportsAdvertisedVersion(systemCoreConfig.coreID, advertisedGles)) {
                    throw GameLoaderException(GameLoaderError.GLIncompatible)
                }

                val coreLibrary =
                    runCatching {
                        findLibrary(appContext, systemCoreConfig.coreID)!!.absolutePath
                    }.getOrElse {
                        val error =
                            if (it is NativeCoreProvisioner.DisabledException) {
                                GameLoaderError.NativeEngineDisabled
                            } else {
                                GameLoaderError.LoadCore
                            }
                        throw GameLoaderException(error, it)
                    }

                emit(LoadingState.LoadingGame)

                if (isIsolatedCandidate(systemCoreConfig.coreID)) {
                    // Fail closed: never start an experimental engine before a verified
                    // copy of the old saves, states and PSP savedata has been retained.
                    val previousCore =
                        if (system.id == SystemID.PSX) CoreID.PCSX_REARMED else CoreID.PPSSPP
                    val oldSystemDirectory = directoriesManager.getSystemDirectory()
                    val sources =
                        mutableListOf(
                            EmulatorMigrationBackup.Source(
                                directoriesManager.getSavesDirectory(),
                                "saves",
                            ),
                            EmulatorMigrationBackup.Source(
                                File(directoriesManager.getStatesDirectory(), previousCore.coreName),
                                "states/${previousCore.coreName}",
                            ),
                            EmulatorMigrationBackup.Source(
                                File(directoriesManager.getStatesPreviewDirectory(), previousCore.coreName),
                                "state-previews/${previousCore.coreName}",
                            ),
                        )
                    if (system.id == SystemID.PSP) {
                        sources += EmulatorMigrationBackup.Source(File(oldSystemDirectory, "PPSSPP"), "system/PPSSPP")
                        sources += EmulatorMigrationBackup.Source(File(oldSystemDirectory, "PSP"), "system/PSP")
                    }
                    EmulatorMigrationBackup.preserve(
                        File(appContext.filesDir, "emulator-baseline-backups/${system.id.dbname}"),
                        sources,
                    )
                }

                val missingBiosFiles = biosManager.getMissingBiosFiles(systemCoreConfig, game)
                if (missingBiosFiles.isNotEmpty()) {
                    throw GameLoaderException(GameLoaderError.MissingBiosFiles(missingBiosFiles))
                }

                val gameFiles =
                    runCatching {
                        val useVFS = systemCoreConfig.supportsLibretroVFS && directLoad
                        val dataFiles = retrogradeDatabase.dataFileDao().selectDataFilesForGame(game.id)
                        lemuroidLibrary.getGameFiles(game, dataFiles, useVFS)
                    }.getOrElse {
                        if (system.id == SystemID.DREAMCAST &&
                            game.fileName.substringAfterLast('.', "").lowercase(java.util.Locale.US) in
                            setOf("gdi", "cue", "m3u")
                        ) {
                            throw GameLoaderException(
                                GameLoaderError.DiscFamily(
                                    "Disc family is incomplete. Restore the original descriptor and all " +
                                        "tracks in one folder, restore folder access, and refresh the shelf.",
                                ),
                                it,
                            )
                        }
                        throw it
                    }

                val saveRAM =
                    runCatching {
                        val data = savesManager.getSaveRAM(game, systemCoreConfig)
                        desmumeMigrationHandler.resolveSaveData(game, systemCoreConfig.coreID, data)
                    }.getOrElse { throw GameLoaderException(GameLoaderError.Saves) }
                val saveRAMData = saveRAM.data

                val quickSaveData =
                    runCatching {
                        val shouldDiscardSave =
                            !savesCoherencyEngine.shouldDiscardAutoSaveState(
                                game,
                                systemCoreConfig.coreID,
                                saveRAM.timestampOverride,
                                systemCoreConfig.stateNamespace,
                            )

                        if (systemCoreConfig.statesSupported && loadSave && shouldDiscardSave) {
                            statesManager.getAutoSave(game, systemCoreConfig.coreID, systemCoreConfig.stateNamespace)
                        } else {
                            null
                        }
                    }.getOrElse { throw GameLoaderException(GameLoaderError.Saves) }

                val coreVariables =
                    coreVariablesManager.getOptionsForCore(system.id, systemCoreConfig)
                        .toTypedArray()

                var systemDirectory =
                    if (systemCoreConfig.coreID == CoreID.PPSSPP_MODERN) {
                        // Modern PPSSPP assets are version-matched and isolated from 1.15.
                        File(appContext.filesDir, "system-engines/ppsspp_modern").apply { mkdirs() }
                    } else if (isNativeExpansion(systemCoreConfig.coreID)) {
                        File(directoriesManager.getSystemDirectory(), "native/${system.id.dbname}").apply { mkdirs() }
                    } else {
                        directoriesManager.getSystemDirectory()
                    }
                if (systemCoreConfig.coreID == CoreID.DOLPHIN_MODERN) {
                    systemDirectory = DolphinAssetsProvisioner.install(appContext, systemDirectory, File(coreLibrary))
                }
                if (systemCoreConfig.coreID == CoreID.FLYCAST_MODERN) {
                    check(File(systemDirectory, "dc").run { isDirectory || mkdirs() })
                }
                val savesDirectory =
                    if (isIsolatedCandidate(systemCoreConfig.coreID)) {
                        File(directoriesManager.getSavesDirectory(), "engines/${systemCoreConfig.coreID.coreName}")
                            .apply { mkdirs() }
                    } else if (isNativeExpansion(systemCoreConfig.coreID)) {
                        File(directoriesManager.getSavesDirectory(), "native/${system.id.dbname}").apply { mkdirs() }
                    } else {
                        directoriesManager.getSavesDirectory()
                    }

                if (systemCoreConfig.coreID == CoreID.PPSSPP_MODERN) {
                    // The baseline has already been backed up above. Seed standard PSP
                    // savedata only; candidate progress and incompatible states stay isolated.
                    com.swordfish.lemuroid.lib.saves.PSPSavedataSeed.seedIfAbsent(
                        directoriesManager.getSavesDirectory(),
                        savesDirectory,
                    )
                }

                emit(
                    LoadingState.Ready(
                        GameData(
                            game,
                            coreLibrary,
                            gameFiles,
                            quickSaveData,
                            saveRAMData,
                            coreVariables,
                            systemDirectory,
                            savesDirectory,
                        ),
                    ),
                )
            } catch (e: GameLoaderException) {
                Timber.e(e, "Error while preparing game")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error while preparing game")
                throw GameLoaderException(GameLoaderError.Generic)
            }
        }

    private fun isNativeExpansion(coreID: CoreID): Boolean =
        coreID in setOf(CoreID.FLYCAST_MODERN, CoreID.DOLPHIN_MODERN, CoreID.PLAY_MODERN)

    private fun isIsolatedCandidate(coreID: CoreID): Boolean =
        coreID == CoreID.PCSX_REARMED_MODERN || coreID == CoreID.PPSSPP_MODERN

    private fun isArchitectureSupported(systemCoreConfig: SystemCoreConfig): Boolean {
        val supportedOnlyArchitectures = systemCoreConfig.supportedOnlyArchitectures ?: return true
        return Build.SUPPORTED_ABIS.toSet().intersect(supportedOnlyArchitectures).isNotEmpty()
    }

    private fun findLibrary(
        context: Context,
        coreID: CoreID,
    ): File? {
        if (isNativeExpansion(coreID)) return NativeCoreProvisioner.select(context, coreID)
        val files =
            sequenceOf(
                File(context.applicationInfo.nativeLibraryDir),
                context.filesDir,
            )

        return files
            .flatMap { it.walkBottomUp() }
            .firstOrNull { it.name == coreID.libretroFileName }
    }

    @Suppress("ArrayInDataClass")
    data class GameData(
        val game: Game,
        val coreLibrary: String,
        val gameFiles: RomFiles,
        val quickSaveData: SaveState?,
        val saveRAMData: ByteArray?,
        val coreVariables: Array<CoreVariable>,
        val systemDirectory: File,
        val savesDirectory: File,
    )
}
