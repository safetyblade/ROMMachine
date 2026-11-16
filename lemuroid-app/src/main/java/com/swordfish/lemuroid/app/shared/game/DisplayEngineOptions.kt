package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.util.AtomicFile
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOption
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.libretrodroid.Variable
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/** Only options actually reported by the selected installed core are offered. */
class DisplayEngineOptions(private val context: Context) {
    @Serializable data class Option(
        val key: String,
        val title: String,
        val values: List<String>,
        val labels: List<String>,
    )

    private val json = Json { ignoreUnknownKeys = true }

    private fun file(namespace: String): File {
        @Suppress("DEPRECATION")
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        val hash =
            MessageDigest.getInstance("SHA-256").digest("$version:$namespace".toByteArray())
                .joinToString("") { "%02x".format(it) }
        return File(context.filesDir, "cartdeck/display-capabilities/$hash.json")
    }

    fun read(namespace: String): List<Option> =
        runCatching {
            AtomicFile(file(namespace)).openRead().use { json.decodeFromString<List<Option>>(it.reader().readText()) }
        }.getOrDefault(emptyList())

    fun record(
        namespace: String,
        config: SystemCoreConfig,
        variables: Array<Variable>,
    ): List<Option> {
        val exposed = (config.exposedSettings + config.exposedAdvancedSettings).associateBy { it.key }
        val options =
            variables.mapNotNull { variable ->
                val core =
                    runCatching { CoreOption.fromLibretroDroidVariable(variable) }.getOrNull()
                        ?: return@mapNotNull null
                val setting = exposed[core.variable.key] ?: return@mapNotNull null
                if (!isDisplayKey(setting.key)) return@mapNotNull null
                val exposedOption = com.swordfish.lemuroid.app.shared.coreoptions.LemuroidCoreOption(setting, core)
                Option(
                    setting.key,
                    exposedOption.getDisplayName(context),
                    exposedOption.getEntriesValues(),
                    exposedOption.getEntries(context),
                )
            }.filter { it.values.isNotEmpty() }
        val target = file(namespace)
        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
        val atomic = AtomicFile(target)
        val output = atomic.startWrite()
        try {
            output.write(json.encodeToString(options).toByteArray())
            atomic.finishWrite(output)
        } catch (
            error: Throwable,
        ) {
            atomic.failWrite(output)
            throw error
        }
        return options
    }

    companion object {
        fun namespace(
            system: String,
            config: SystemCoreConfig,
        ) = "$system:${config.coreID.name}"

        fun isDisplayKey(key: String): Boolean =
            listOf(
                "screen_layout", "screens_layout", "layout_option", "aspect_ratio", "widescreen",
                "screen_gap", "screen_position", "video_filter", "colorization", "colourization", "palette",
            ).any { it in key.lowercase() }
    }
}
