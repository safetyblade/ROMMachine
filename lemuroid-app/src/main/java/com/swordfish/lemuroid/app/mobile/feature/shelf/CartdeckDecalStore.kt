package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * App-private cosmetic assets. No persistable URI grants, storage permission,
 * cloud requests, ROM directory access or save-state dependencies.
 */
internal class CartdeckDecalStore(private val context: Context) {
    private val folder = File(context.filesDir, "cartdeck/decals")
    private val pending = File(context.cacheDir, "cartdeck/decal-pending")

    companion object {
        const val MAX_IMPORT_BYTES = 8 * 1024 * 1024
        const val MAX_DECODE_DIMENSION = 8192
        const val MAX_DECODE_PIXELS = 40_000_000L
        const val MAX_OUTPUT_DIMENSION = 1024
    }

    private fun assetFile(id: String): File {
        require(runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)) {
            "Unsafe decal identifier"
        }
        return File(folder, "$id.png")
    }

    private fun pendingFile(id: String): File = File(pending, assetFile(id).name)

    suspend fun importImage(uri: Uri): String =
        withContext(Dispatchers.IO) {
            val bytes =
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val output = ByteArrayOutputStream()
                    val block = ByteArray(8192)
                    while (true) {
                        val count = stream.read(block)
                        if (count < 0) break
                        require(output.size() + count <= MAX_IMPORT_BYTES) { "Decal is larger than 8 MB" }
                        output.write(block, 0, count)
                    }
                    output.toByteArray()
                } ?: error("Could not open selected image")
            require(isSupportedImage(bytes)) { "Use a PNG, WebP or JPEG image" }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(
                bounds.outWidth > 0 && bounds.outHeight > 0 &&
                    bounds.outWidth <= MAX_DECODE_DIMENSION &&
                    bounds.outHeight <= MAX_DECODE_DIMENSION &&
                    bounds.outWidth.toLong() * bounds.outHeight <= MAX_DECODE_PIXELS,
            ) { "Image dimensions are unsupported" }
            val options = BitmapFactory.Options()
            while (
                bounds.outWidth / options.inSampleSize > MAX_OUTPUT_DIMENSION ||
                bounds.outHeight / options.inSampleSize > MAX_OUTPUT_DIMENSION
            ) {
                options.inSampleSize *= 2
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: error("Image could not be decoded")
            try {
                val normalized = ByteArrayOutputStream()
                require(decoded.compress(Bitmap.CompressFormat.PNG, 100, normalized)) { "Image encoding failed" }
                val payload = normalized.toByteArray()
                require(payload.size <= MAX_IMPORT_BYTES) { "The processed image is too large" }
                val currentBytes =
                    (folder.listFiles()?.sumOf { it.length() } ?: 0L) +
                        (pending.listFiles()?.sumOf { it.length() } ?: 0L)
                require(currentBytes + payload.size <= 64L * 1024 * 1024) {
                    "CARTDECK personal artwork storage is full"
                }
                val id = UUID.randomUUID().toString()
                val file = pendingFile(id)
                require(pending.isDirectory || pending.mkdirs()) { "Could not create decal staging storage" }
                val atomic = AtomicFile(file)
                val output = atomic.startWrite()
                try {
                    output.write(payload)
                    atomic.finishWrite(output)
                } catch (error: Throwable) {
                    atomic.failWrite(output)
                    throw error
                }
                id
            } finally {
                decoded.recycle()
            }
        }

    suspend fun readImage(id: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val owned = assetFile(id)
                val file = if (owned.isFile) owned else pendingFile(id)
                if (!file.isFile || file.length() <= 0 || file.length() > MAX_IMPORT_BYTES) return@runCatching null
                val image = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching null
                image.asImageBitmap()
            }.getOrNull()
        }

    /** Move selected artwork from temporary cache into durable private storage. */
    fun promote(id: String) {
        val source = pendingFile(id)
        val destination = assetFile(id)
        if (destination.isFile) return
        require(source.isFile) { "The selected artwork is no longer available" }
        require(folder.isDirectory || folder.mkdirs()) { "Cannot create decal storage" }
        // Use AtomicFile for a crash-safe cross-directory commit.
        val atomic = AtomicFile(destination)
        val output = atomic.startWrite()
        try {
            source.inputStream().use { it.copyTo(output) }
            atomic.finishWrite(output)
            source.delete()
        } catch (error: Throwable) {
            atomic.failWrite(output)
            throw error
        }
    }

    /** Discard only editor-created staging images, never shared saved-profile assets. */
    fun discard(id: String) {
        runCatching { pendingFile(id).delete() }
    }

    /** Safe after ShelfStore has successfully committed the new metadata. */
    fun pruneUnused(saved: ShelfAppearance) {
        val retained = buildSet {
            saved.decal?.assetId?.let(::add)
            saved.profiles.mapNotNull { it.decal?.assetId }.forEach(::add)
        }
        folder.listFiles()?.forEach { file ->
            val id = file.name.removeSuffix(".png")
            if (file.extension == "png" && id !in retained) {
                // Called only AFTER AtomicFile committed the new appearance.
                // Pending editor work lives in a separate staging directory.
                file.delete()
            }
        }
        pending.listFiles()?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000L) file.delete()
        }
    }

    private fun isSupportedImage(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val png =
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        val jpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        val webp =
            bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()) &&
                bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray())
        return png || jpeg || webp
    }
}
