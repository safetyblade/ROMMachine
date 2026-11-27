package com.swordfish.lemuroid.app.shared.support

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/** A deliberately selected image is decoded and re-encoded without source metadata. */
internal class SupportScreenshot(private val context: Context) {
    private val directory = File(context.noBackupFilesDir, "support-screenshots")

    fun importImage(
        report: SupportReport,
        uri: Uri,
    ): String =
        synchronized(lock) {
            prune()
            check(directory.isDirectory || directory.mkdirs())
            check(directory.listFiles().orEmpty().size < MAX_FILES)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            checkNotNull(context.contentResolver.openInputStream(uri)).use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            require(bounds.outWidth in 1..16_384 && bounds.outHeight in 1..16_384)
            val sample =
                generateSequence(1) { it * 2 }.first {
                    maxOf(bounds.outWidth, bounds.outHeight) / it <= MAX_DIMENSION
                }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val pending = File.createTempFile("support-image-", ".pending", directory)
            try {
                val source = checkNotNull(context.contentResolver.openInputStream(uri))
                val image =
                    source.use {
                        checkNotNull(BitmapFactory.decodeStream(it, null, options))
                    }
                try {
                    pending.outputStream().use {
                            output ->
                        check(image.compress(Bitmap.CompressFormat.PNG, 100, output))
                    }
                } finally {
                    image.recycle()
                }
                check(pending.length() in 1L..MAX_BYTES)
                val digest =
                    MessageDigest.getInstance("SHA-256").digest(pending.readBytes())
                        .joinToString("") { "%02x".format(it) }
                val target = file(report.id, digest)
                if (!target.isFile) check(pending.renameTo(target))
                digest
            } finally {
                pending.delete()
            }
        }

    fun attachment(report: SupportReport): File? =
        report.screenshotDigest?.let { digest ->
            file(report.id, digest).takeIf { it.isFile && it.length() in 1L..MAX_BYTES }
        }

    fun delete(reportId: String) =
        synchronized(lock) {
            val prefix = "${UUID.fromString(reportId)}-"
            directory.listFiles().orEmpty().filter { it.name.startsWith(prefix) }.forEach { it.delete() }
        }

    fun prune() =
        synchronized(lock) {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14)
            directory.listFiles().orEmpty().filter { it.lastModified() < cutoff }.forEach { it.delete() }
        }

    private fun file(
        reportId: String,
        digest: String,
    ): File {
        require(digest.matches(Regex("[a-f0-9]{64}")))
        return File(directory, "${UUID.fromString(reportId)}-$digest.png")
    }

    companion object {
        private val lock = Any()
        private const val MAX_FILES = 16
        private const val MAX_DIMENSION = 720
        private const val MAX_BYTES = 2_400_000L
    }
}
