package com.swordfish.lemuroid.app.shared.support

import android.os.Build
import com.swordfish.lemuroid.BuildConfig
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** Immutable snapshot shared by preview, clipboard and package rendering. */
class SupportReport private constructor(private val data: JSONObject) {
    val id: String get() = data.getString("id")
    val createdAtMillis: Long get() = data.getLong("createdAtMillis")

    operator fun get(key: String): String = data.optString(key, NOT_CAPTURED)

    val packageName: String get() = "CARTDECK-CD-$id.zip"
    val json: String get() = data.toString(2)

    val text: String
        get() =
            buildString {
                appendLine("CARTDECK SUPPORT REPORT")
                for (key in data.keys()) {
                    if (key != "diagnostic") appendLine("$key: ${data.optString(key, NOT_CAPTURED)}")
                }
                appendLine()
                append(data.getString("diagnostic"))
            }

    fun withPlayerDetails(
        description: String,
        category: String,
        frequency: String,
    ): SupportReport =
        SupportReport(
            JSONObject(json)
                .put("description", sanitise(description.take(2_000)))
                .put("category", sanitise(category.take(100)))
                .put("frequency", sanitise(frequency.take(40))),
        )

    companion object {
        const val NOT_CAPTURED = "NOT_CAPTURED"
        const val NOT_SUPPORTED = "NOT_SUPPORTED"
        const val UNKNOWN = "UNKNOWN"
        const val NOT_APPLICABLE = "NOT_APPLICABLE"

        fun create(
            source: String,
            category: String,
            diagnostic: String?,
            gameTitle: String? = null,
            system: String? = null,
            engine: String? = null,
            session: String? = null,
            serial: String? = null,
        ): SupportReport =
            SupportReport(
                JSONObject()
                    .put("id", UUID.randomUUID().toString())
                    .put("schemaVersion", 1)
                    .put("createdAtMillis", System.currentTimeMillis())
                    .put("createdAt", utcTimestamp())
                    .put("source", source)
                    .put("category", category)
                    .put("game", gameTitle?.let(::sanitise) ?: NOT_CAPTURED)
                    .put("console", system?.take(100)?.let(::sanitise) ?: NOT_CAPTURED)
                    .put("engine", engine?.take(200)?.let(::sanitise) ?: NOT_CAPTURED)
                    .put("session", session ?: NOT_CAPTURED)
                    .put("discSerial", serial ?: NOT_CAPTURED)
                    .put("version", BuildConfig.VERSION_NAME)
                    .put("versionCode", BuildConfig.VERSION_CODE)
                    .put("buildRevision", BuildConfig.SUPPORT_SOURCE_REVISION)
                    .put("buildChannel", "${BuildConfig.FLAVOR}-${BuildConfig.BUILD_TYPE}")
                    .put("manufacturer", Build.MANUFACTURER ?: UNKNOWN)
                    .put("deviceModel", Build.MODEL ?: UNKNOWN)
                    .put("androidVersion", Build.VERSION.RELEASE ?: UNKNOWN)
                    .put("androidApi", Build.VERSION.SDK_INT)
                    .put("abis", Build.SUPPORTED_ABIS.joinToString())
                    .put("description", NOT_CAPTURED)
                    .put("frequency", NOT_CAPTURED)
                    .put("diagnostic", diagnostic?.let(::sanitise) ?: NOT_CAPTURED),
            )

        private fun utcTimestamp(): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

        internal fun parse(json: String): SupportReport {
            val values = JSONObject(json)
            require(values.getInt("schemaVersion") == 1)
            UUID.fromString(values.getString("id"))
            values.getLong("createdAtMillis")
            values.getString("diagnostic")
            return SupportReport(values)
        }

        private val privateData =
            Regex(
                """(?i)(?:https?|content|file|smb)://[^\s]+|(?:[a-z]:\\|(?<![\w/])/(?=[^\s/]+/))[^\s]+|""" +
                    """[\w.+-]+@[\w.-]+\.[a-z]{2,}|""" +
                    """\b(?:authorization|password|api[_ -]?key|access[_ -]?token|refresh[_ -]?token|token|bearer)""" +
                    """\b[\s:=]+[^\r\n;]+|""" +
                    """\b(?:gh[pousr]_|github_pat_|sk-)[A-Za-z0-9_-]+""",
            )

        internal fun sanitise(value: String): String =
            value.take(96_000)
                .replace(privateData, "[redacted]")
                .filter { !it.isISOControl() || it == '\n' || it == '\t' }
    }
}
