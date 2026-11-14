package com.swordfish.lemuroid.lib.saves

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.util.AtomicFile
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class StatesPreviewManager(
    private val directoriesManager: DirectoriesManager,
    private val binding: CartridgeSaveBinding? = null,
) {
    suspend fun getPreviewForSlot(
        game: Game,
        coreID: CoreID,
        index: Int,
        size: Int,
        namespace: String = coreID.coreName,
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            val screenshotName = getSlotScreenshotName(game, index)
            val file = getPreviewFile(game, screenshotName, namespace)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) null else ThumbnailUtils.extractThumbnail(bitmap, size, size)
        }

    suspend fun setPreviewForSlot(
        game: Game,
        bitmap: Bitmap,
        coreID: CoreID,
        index: Int,
        namespace: String = coreID.coreName,
    ) = withContext(Dispatchers.IO) {
        val screenshotName = getSlotScreenshotName(game, index)
        val file = getPreviewFile(game, screenshotName, namespace)
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)) {
                throw IOException("Cannot encode save-state preview")
            }
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private suspend fun getPreviewFile(
        game: Game,
        fileName: String,
        coreName: String,
    ): File {
        val directory =
            binding?.previewDirectory(game, coreName)
                ?: File(directoriesManager.getStatesPreviewDirectory(), coreName).apply { mkdirs() }
        return File(directory, fileName)
    }

    private fun getSlotScreenshotName(
        game: Game,
        index: Int,
    ) = "${game.saveFileName()}.slot${index + 1}.jpg"

    companion object {
        val PREVIEW_SIZE_DP = 96f
    }
}
