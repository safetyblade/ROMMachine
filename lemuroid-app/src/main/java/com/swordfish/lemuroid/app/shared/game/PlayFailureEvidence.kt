package com.swordfish.lemuroid.app.shared.game

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.swordfish.lemuroid.BuildConfig
import java.util.Properties

internal object PlayFailureEvidence {
    fun build(
        context: Context,
        record: Properties,
        code: String,
    ): String {
        val manager = context.getSystemService(ActivityManager::class.java)
        val exit =
            if (Build.VERSION.SDK_INT >= 30) {
                findExit(
                    context,
                    record,
                )?.let(SessionExitEvidence::render)
            } else {
                null
            }
        val provenance =
            runCatching {
                context.assets.open("cartdeck/play-modern.properties").use {
                        input ->
                    Properties().apply { load(input) }
                }
            }.getOrDefault(Properties())
        val revision = provenance.getProperty("revision", provenance.getProperty("upstream.revision", "unavailable"))
        val checksum = provenance.getProperty("core.sha256", "unavailable")
        val stage = record.getProperty("stage", "REQUESTED")
        val error = record.getProperty("error", "none")
        val abis = Build.SUPPORTED_ABIS.joinToString()
        val gles = manager.deviceConfigurationInfo.reqGlEsVersion.toString(16)
        val host = com.swordfish.lemuroid.lib.BuildConfig.NATIVE_HOST_VERSION
        val selectedVersion = record.getProperty("selectedVersion", "unavailable")
        val selectedSHA = record.getProperty("selectedSHA256", "unavailable")
        val glVendor = record.getProperty("glVendor", "unavailable before frontend draw")
        val glRenderer = record.getProperty("glRenderer", "unavailable before frontend draw")
        val glVersion = record.getProperty("glVersion", "unavailable before frontend draw")
        val report =
            listOf(
                "CARTDECK PS2 launch diagnostic",
                "app=${BuildConfig.VERSION_NAME}; versionCode=${BuildConfig.VERSION_CODE}",
                "engine=Play!; host=$host; bundledEngineRevision=$revision",
                "bundledEngineSHA256=$checksum",
                "selectedEngineVersion=$selectedVersion; selectedEngineSHA256=$selectedSHA",
                "device=${Build.MANUFACTURER} ${Build.MODEL}; sdk=${Build.VERSION.SDK_INT}; abi=$abis",
                "advertisedGLES=$gles; lastVerifiedStage=$stage",
                "GL_VENDOR=$glVendor; GL_RENDERER=$glRenderer; GL_VERSION=$glVersion",
                "failure=$code; containedError=$error",
                exit ?: "androidProcessExit=NOT_CAPTURED",
                "Core frame submission alone does not prove gameplay, sound, input or saves.",
            ).joinToString("\n")
        return report
    }

    fun exitReason(
        context: Context,
        record: Properties,
    ): Int? = if (Build.VERSION.SDK_INT >= 30) findExit(context, record)?.reason else null

    @RequiresApi(30)
    private fun findExit(
        context: Context,
        record: Properties,
    ): ApplicationExitInfo? =
        SessionExitEvidence.find(
            context,
            "${context.packageName}:game",
            record.getProperty("pid")?.toIntOrNull() ?: 0,
            record.getProperty("started")?.toLongOrNull() ?: 0,
        )
}
