package com.swordfish.lemuroid.lib.saves

import com.swordfish.lemuroid.common.kotlin.readBytesUncompressedAtomic
import com.swordfish.lemuroid.common.kotlin.readTextAtomic
import com.swordfish.lemuroid.common.kotlin.runCatchingWithRetry
import com.swordfish.lemuroid.common.kotlin.writeBytesCompressedAtomic
import com.swordfish.lemuroid.common.kotlin.writeTextAtomic
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class StatesManager(private val directoriesManager: DirectoriesManager) {
    suspend fun getSlotSave(
        game: Game,
        coreID: CoreID,
        index: Int,
        namespace: String = coreID.coreName,
    ): SaveState? =
        withContext(Dispatchers.IO) {
            assert(index in 0 until MAX_STATES)
            getSaveState(getSlotSaveFileName(game, index), namespace)
        }

    suspend fun setSlotSave(
        game: Game,
        saveState: SaveState,
        coreID: CoreID,
        index: Int,
        namespace: String = coreID.coreName,
    ) = withContext(Dispatchers.IO) {
        assert(index in 0 until MAX_STATES)
        setSaveState(getSlotSaveFileName(game, index), namespace, saveState)
    }

    suspend fun getAutoSaveInfo(
        game: Game,
        coreID: CoreID,
        namespace: String = coreID.coreName,
    ): SaveInfo =
        withContext(Dispatchers.IO) {
            val autoSaveFile = getStateFile(getAutoSaveFileName(game), namespace)
            val autoSaveHasData = autoSaveFile.length() > 0
            SaveInfo(autoSaveFile.exists() && autoSaveHasData, autoSaveFile.lastModified())
        }

    suspend fun getAutoSave(
        game: Game,
        coreID: CoreID,
        namespace: String = coreID.coreName,
    ) = withContext(Dispatchers.IO) {
        getSaveState(getAutoSaveFileName(game), namespace)
    }

    suspend fun setAutoSave(
        game: Game,
        coreID: CoreID,
        saveState: SaveState,
        namespace: String = coreID.coreName,
    ) = withContext(Dispatchers.IO) {
        setSaveState(getAutoSaveFileName(game), namespace, saveState)
    }

    suspend fun getSavedSlotsInfo(
        game: Game,
        coreID: CoreID,
        namespace: String = coreID.coreName,
    ): List<SaveInfo> =
        withContext(Dispatchers.IO) {
            (0 until MAX_STATES)
                .map { getStateFile(getSlotSaveFileName(game, it), namespace) }
                .map { SaveInfo(it.exists(), it.lastModified()) }
                .toList()
        }

    private suspend fun getSaveState(
        fileName: String,
        coreName: String,
    ): SaveState? {
        return runCatchingWithRetry(FILE_ACCESS_RETRIES) {
            val saveFile = getStateFile(fileName, coreName)
            val metadataFile = getMetadataStateFile(fileName, coreName)
            if (saveFile.exists()) {
                val byteArray = saveFile.readBytesUncompressedAtomic()
                val stateMetadata =
                    runCatching {
                        Json.Default.decodeFromString(
                            SaveState.Metadata.serializer(),
                            metadataFile.readTextAtomic(),
                        )
                    }
                SaveState(byteArray, stateMetadata.getOrNull() ?: SaveState.Metadata())
            } else {
                null
            }
        }.getOrNull()
    }

    private suspend fun setSaveState(
        fileName: String,
        coreName: String,
        saveState: SaveState,
    ) {
        runCatchingWithRetry(FILE_ACCESS_RETRIES) {
            writeStateToDisk(fileName, coreName, saveState.state)
            writeMetadataToDisk(fileName, coreName, saveState.metadata)
        }
    }

    private fun writeMetadataToDisk(
        fileName: String,
        coreName: String,
        metadata: SaveState.Metadata,
    ) {
        val metadataFile = getMetadataStateFile(fileName, coreName)
        metadataFile.writeTextAtomic(Json.encodeToString(SaveState.Metadata.serializer(), metadata))
    }

    private fun writeStateToDisk(
        fileName: String,
        coreName: String,
        stateArray: ByteArray,
    ) {
        val saveFile = getStateFile(fileName, coreName)
        saveFile.writeBytesCompressedAtomic(stateArray)
    }

    private fun getStateFile(
        fileName: String,
        coreName: String,
    ): File {
        val statesDirectories = File(directoriesManager.getStatesDirectory(), coreName)
        statesDirectories.mkdirs()
        return File(statesDirectories, fileName)
    }

    private fun getMetadataStateFile(
        stateFileName: String,
        coreName: String,
    ): File {
        val statesDirectories = File(directoriesManager.getStatesDirectory(), coreName)
        statesDirectories.mkdirs()
        return File(statesDirectories, "$stateFileName.metadata")
    }

    private fun getAutoSaveFileName(game: Game) = "${game.fileName}.state"

    private fun getSlotSaveFileName(
        game: Game,
        index: Int,
    ) = "${game.fileName}.slot${index + 1}"

    companion object {
        const val MAX_STATES = 4
        private const val FILE_ACCESS_RETRIES = 3
    }
}
