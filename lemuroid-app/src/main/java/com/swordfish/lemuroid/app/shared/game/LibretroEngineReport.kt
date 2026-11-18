package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.os.Build
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.io.File
import java.io.Serializable
import java.security.MessageDigest
import java.util.Properties

/**
 * Private, current-session metadata transferred to the existing menu.
 * The binary path is used only for an on-demand hash and is never printed or persisted.
 */
data class LibretroReportSession(
    val coreFilePath: String,
    val mediaRoute: String,
    val lowLatencyAudioRequested: Boolean,
    val skipDuplicateFrames: Boolean,
    val coreFrameObserved: Boolean = false,
    val graphics: String? = null,
    val reportedOptions: ArrayList<String> = arrayListOf(),
    val performanceSnapshot: String? = null,
    val compatibilityMode: String? = null,
    val renderingSnapshot: String? = null,
) : Serializable

object LibretroEngineReport {
    private const val UNAVAILABLE = "unavailable"
    private val optionKey = Regex("[A-Za-z0-9_.-]{1,100}")
    private val confidentialKey =
        Regex("password|secret|token|key_file|key_path|directory|path", RegexOption.IGNORE_CASE)
    private val digestPattern = Regex("[a-fA-F0-9]{64}")
    private val revisionPattern = Regex("[a-fA-F0-9]{40}")

    internal fun safeField(value: String?): String {
        if (value.isNullOrBlank()) return UNAVAILABLE
        if (value.any { it.isISOControl() || it == '/' || it == '\\' }) return "[redacted]"
        return value.take(200)
    }

    fun safeOptions(options: List<Pair<String, String>>): ArrayList<String> =
        ArrayList(
            options.asSequence()
                .filter { (key, _) -> optionKey.matches(key) && !confidentialKey.containsMatchIn(key) }
                .distinctBy { it.first }
                .take(256)
                .sortedBy { it.first }
                .map { (key, value) -> "$key=${safeField(value)}" }
                .toList(),
        )

    internal fun sha256(file: File): String? =
        runCatching {
            if (!file.isFile) return@runCatching null
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }.getOrNull()

    /** The APK manifest may describe a different binary than the selected rollback core. */
    internal fun verifiedRevision(
        actualDigest: String?,
        manifest: Properties?,
    ): String? {
        if (actualDigest == null || !digestPattern.matches(actualDigest) || manifest == null) return null
        val expected = manifest.getProperty("core.sha256") ?: return null
        if (!actualDigest.equals(expected, ignoreCase = true)) return null
        val revision = manifest.getProperty("revision") ?: manifest.getProperty("upstream.revision")
        return revision?.takeIf { revisionPattern.matches(it) }
    }

    /** Called only after navigating to Engine Report, on Dispatchers.IO. */
    fun build(
        context: Context,
        game: Game,
        config: SystemCoreConfig,
        session: LibretroReportSession?,
    ): String {
        val file = session?.coreFilePath?.let(::File)
        val digest = file?.takeIf { it.name == config.coreID.libretroFileName }?.let(::sha256)
        val manifest =
            runCatching {
                val engine =
                    NativeEngineRegistry.engines(GameSystem.findById(game.systemId))
                        .firstOrNull { it.core == config.coreID }
                engine?.manifest?.let { path ->
                    context.assets.open(path).use { stream -> Properties().apply { load(stream) } }
                }
            }.getOrNull()
        return render(
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            system = game.systemId,
            title = game.displayName(),
            core = config.coreID.coreName,
            hostVersion = com.swordfish.lemuroid.lib.BuildConfig.NATIVE_HOST_VERSION,
            mediaFormat = game.fileName.substringAfterLast('.', "").takeIf { it.matches(Regex("[A-Za-z0-9]{1,12}")) },
            session = session,
            digest = digest,
            revision = verifiedRevision(digest, manifest),
            androidSdk = Build.VERSION.SDK_INT,
            device = Build.MODEL,
        )
    }

    internal fun render(
        appVersion: String,
        system: String,
        title: String,
        core: String,
        hostVersion: String,
        mediaFormat: String?,
        session: LibretroReportSession?,
        digest: String?,
        revision: String?,
        androidSdk: Int,
        device: String?,
    ): String =
        buildString {
            appendLine("CARTDECK Engine Report (current session)")
            appendLine("apk_version=${safeField(appVersion)}")
            appendLine("console=${safeField(system)}")
            appendLine("game=${safeField(title)}")
            appendLine("configured_core=${safeField(core)}")
            appendLine("host=LibretroDroid")
            appendLine("host_build=${safeField(hostVersion)}")
            appendLine("selected_core_file_sha256=${digest?.takeIf { digestPattern.matches(it) } ?: UNAVAILABLE}")
            appendLine(
                "source_revision_verified_by_digest=${revision?.takeIf { revisionPattern.matches(it) } ?: UNAVAILABLE}",
            )
            appendLine("android_sdk=$androidSdk")
            appendLine("device=${safeField(device)}")
            appendLine("media_format=${safeField(mediaFormat)}")
            appendLine("media_route=${safeField(session?.mediaRoute)}")
            appendLine("graphics=${safeField(session?.graphics)}")
            appendLine("core_video_callback_observed=${session?.coreFrameObserved?.toString() ?: UNAVAILABLE}")
            appendLine("low_latency_audio_requested=${session?.lowLatencyAudioRequested?.toString() ?: UNAVAILABLE}")
            appendLine("skip_duplicate_frames=${session?.skipDuplicateFrames?.toString() ?: UNAVAILABLE}")
            if (core.equals("citra", ignoreCase = true)) {
                appendLine("citra_compatibility_mode=${safeField(session?.compatibilityMode)}")
                appendLine("citra_package_pin=fee2e824525daa22bcf318f96127fe43fa8a15ad")
                appendLine("citra_package_pin_is_not_upstream_source_revision=true")
                appendLine("render_probe_codes=0 not attempted, 1 sampled dark, 2 sampled colour, 3 incomplete, 4 unreadable")
                appendLine("render_probe_scope=five sample pixels per target at three checkpoints, not a full image or display swap")
                val rendering = session?.renderingSnapshot?.takeIf { it.isNotBlank() }
                if (rendering == null) {
                    appendLine("render_probe=not collected")
                } else {
                    rendering.split(';')
                        .filter { it.matches(Regex("[a-z_]+=[0-9,]+")) }
                        .take(16)
                        .forEach { appendLine(safeField(it)) }
                }
            }
            val performance = session?.performanceSnapshot?.takeIf { it.isNotBlank() }
            if (performance == null) {
                appendLine("performance_counters=not collected")
            } else {
                appendLine("performance_counters=bounded Dreamcast QA capture (first 4096 core runs)")
                appendLine("run_us_bins_upper=4000,8000,12000,17000,25000,34000,50000,overflow")
                performance.split(';')
                    .filter { it.matches(Regex("[a-z_]+=[0-9,]+")) }
                    .take(12)
                    .forEach { appendLine(safeField(it)) }
                appendLine("last_fps_milli and last_sample_rate zero means no runtime timing change observed")
                appendLine("timing_scope=core run duration, not Android display presentation")
                appendLine("audio_scope=frontend FIFO stereo frames, not hardware underruns")
                appendLine("rtt_and_media_timing=not collected")
            }
            appendLine("gameplay_acceptance=not established by this report")
            appendLine("frontend_reported_core_options (internal game-specific overrides may differ):")
            val options = session?.reportedOptions.orEmpty()
            if (options.isEmpty()) appendLine(UNAVAILABLE)
            options.forEach { appendLine(safeField(it)) }
        }
}
