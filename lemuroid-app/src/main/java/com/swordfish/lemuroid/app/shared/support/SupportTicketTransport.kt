package com.swordfish.lemuroid.app.shared.support

import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Client contract for the future private CARTDECK intake. Credentials belong to the server. */
internal object SupportTicketTransport {
    // Configured SUP-06 intake endpoint. Only a validated receipt confirms delivery.
    private const val INTAKE_ENDPOINT = "https://prestwich.dev/api/support/intake"
    private const val MAX_UPLOAD_BYTES = 4_000_000L
    private const val MAX_RECEIPT_BYTES = 4096
    val enabled: Boolean get() = INTAKE_ENDPOINT.isNotBlank()

    fun send(
        report: SupportReport,
        archive: File,
    ): String {
        check(enabled)
        val endpoint = URL(INTAKE_ENDPOINT)
        require(
            endpoint.protocol == "https" && endpoint.host == "prestwich.dev" &&
                endpoint.userInfo == null && endpoint.ref == null && endpoint.query == null,
        )
        val length = archive.length()
        require(length in 1..MAX_UPLOAD_BYTES && archive.name == report.packageName)
        val connection = endpoint.openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/zip")
            connection.setRequestProperty("Idempotency-Key", report.id)
            connection.setFixedLengthStreamingMode(length)
            archive.inputStream().use { input ->
                connection.outputStream.use { output -> input.copyTo(output) }
            }
            val status = connection.responseCode
            check(status in setOf(200, 201, 202)) { "Support intake returned HTTP $status." }
            val receipt =
                connection.inputStream.use { input ->
                    val bytes = ByteArray(MAX_RECEIPT_BYTES + 1)
                    var size = 0
                    while (size < bytes.size) {
                        val read = input.read(bytes, size, bytes.size - size)
                        if (read <= 0) break
                        size += read
                    }
                    require(size in 1..MAX_RECEIPT_BYTES)
                    bytes.decodeToString(endIndex = size)
                }
            return validateReceipt(report.id, receipt)
        } finally {
            connection.disconnect()
        }
    }

    internal fun validateReceipt(
        reportId: String,
        receipt: String,
    ): String {
        val response = JSONObject(receipt)
        check(response.optBoolean("accepted") && response.optString("reportId") == reportId)
        return response.getString("reference").also {
            require(it.matches(Regex("[A-Za-z0-9][A-Za-z0-9-]{2,63}")))
        }
    }
}
