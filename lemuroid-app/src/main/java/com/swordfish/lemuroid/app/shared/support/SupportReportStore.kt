package com.swordfish.lemuroid.app.shared.support

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SupportReportStore(context: Context) {
    private val screenshots = SupportScreenshot(context)
    private val submissions = SupportSubmissionStore(context)
    private val directory = File(context.noBackupFilesDir, "support-reports")
    private val attachments = File(context.cacheDir, "support-packages")

    fun list(): List<SupportReport> = synchronized(lock) { prune(System.currentTimeMillis()) }

    fun save(report: SupportReport) =
        synchronized(lock) {
            val bytes = report.json.toByteArray()
            require(bytes.size <= MAX_REPORT_BYTES)
            check(directory.isDirectory || directory.mkdirs())
            val file = AtomicFile(reportFile(report.id))
            val output = file.startWrite()
            try {
                output.write(bytes)
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            }
            prune(System.currentTimeMillis())
            Unit
        }

    fun delete(id: String) =
        synchronized(lock) {
            AtomicFile(reportFile(id)).delete()
            screenshots.delete(id)
            submissions.delete(id)
            attachments.listFiles().orEmpty().filter { it.name.startsWith("$id-") }.forEach {
                it.deleteRecursively()
            }
            Unit
        }

    fun packageReport(report: SupportReport): File =
        synchronized(lock) {
            UUID.fromString(report.id)
            check(attachments.isDirectory || attachments.mkdirs())
            prune(System.currentTimeMillis())
            val fingerprint =
                MessageDigest.getInstance("SHA-256")
                    .digest(report.json.toByteArray()).joinToString("") { "%02x".format(it) }
            val folder = File(attachments, "${report.id}-$fingerprint")
            val file = File(folder, report.packageName)
            if (file.isFile) return@synchronized file
            check(folder.exists() || attachments.listFiles().orEmpty().size < MAX_PACKAGES)
            check(folder.isDirectory || folder.mkdir())
            val pending = File.createTempFile("support-", ".pending", folder)
            try {
                ZipOutputStream(pending.outputStream().buffered()).use { zip ->
                    for ((name, value) in listOf("report.txt" to report.text, "report.json" to report.json)) {
                        val bytes = value.toByteArray()
                        require(bytes.size <= MAX_REPORT_BYTES)
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                    if (report.screenshotDigest != null) {
                        val screenshot = checkNotNull(screenshots.attachment(report))
                        zip.putNextEntry(ZipEntry("screenshot.png"))
                        screenshot.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
                check(pending.renameTo(file))
                file
            } finally {
                pending.delete()
            }
        }

    private fun reportFile(id: String): File = File(directory, "${UUID.fromString(id)}.json")

    private fun prune(now: Long): List<SupportReport> {
        screenshots.prune()
        val cutoff = now - RETENTION
        val reports =
            directory.listFiles().orEmpty().filter { it.extension == "json" }.mapNotNull { file ->
                val report =
                    runCatching {
                        require(file.length() in 1..MAX_REPORT_BYTES.toLong())
                        SupportReport.parse(file.readText()).also { require(reportFile(it.id) == file) }
                    }.getOrNull()
                if (report == null || report.createdAtMillis < cutoff) {
                    file.delete()
                    null
                } else {
                    report
                }
            }.sortedByDescending { it.createdAtMillis }
        reports.drop(MAX_REPORTS).forEach { AtomicFile(reportFile(it.id)).delete() }
        attachments.listFiles().orEmpty().forEach {
            if (it.lastModified() < now - ATTACHMENT_RETENTION) it.deleteRecursively()
        }
        return reports.take(MAX_REPORTS)
    }

    companion object {
        private val lock = Any()
        private const val MAX_PACKAGES = 24
        private const val MAX_REPORTS = 10
        private const val MAX_REPORT_BYTES = 400_000
        private val RETENTION = TimeUnit.DAYS.toMillis(14)
        private val ATTACHMENT_RETENTION = TimeUnit.DAYS.toMillis(1)
    }
}
