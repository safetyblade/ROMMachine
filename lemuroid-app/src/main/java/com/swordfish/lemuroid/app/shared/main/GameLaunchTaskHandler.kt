package com.swordfish.lemuroid.app.shared.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.GameSessionCheckpoint
import com.swordfish.lemuroid.app.shared.game.NativeSessionJournal
import com.swordfish.lemuroid.app.shared.game.PS2RecoveryActivity
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2BiosRecovery
import com.swordfish.lemuroid.app.shared.gamecrash.GameCrashActivity
import com.swordfish.lemuroid.app.shared.savesync.SaveSyncWork
import com.swordfish.lemuroid.app.shared.storage.cache.CacheCleanerWork
import com.swordfish.lemuroid.app.shared.support.SupportReportStore
import com.swordfish.lemuroid.ext.feature.review.ReviewManager
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class GameLaunchTaskHandler(
    private val reviewManager: ReviewManager,
    private val retrogradeDb: RetrogradeDatabase,
) {
    fun handleGameStart(context: Context) {
        cancelBackgroundWork(context)
    }

    suspend fun handleGameFinish(
        enableRatingFlow: Boolean,
        activity: Activity,
        resultCode: Int,
        data: Intent?,
    ) {
        rescheduleBackgroundWork(activity.applicationContext)
        if (data?.getBooleanExtra(PS2RecoveryActivity.RECOVERY_DISPLAYED, false) == true) return
        val (nativeCrash, report) =
            withContext(Dispatchers.IO) {
                val context = activity.applicationContext
                val checkpoint = GameSessionCheckpoint(context)
                val record = checkpoint.read()
                val crash =
                    if (resultCode != Activity.RESULT_OK) {
                        runCatching {
                            NativeSessionJournal.consumeCrash(
                                context,
                                expectedPid = record?.getProperty("pid")?.toIntOrNull(),
                                notBefore = record?.getProperty("started")?.toLongOrNull(),
                            )
                        }.getOrNull()
                    } else {
                        null
                    }
                val captured =
                    record?.takeUnless { resultCode == Activity.RESULT_OK }?.let {
                        runCatching {
                            checkpoint.failure(
                                it,
                                listOfNotNull(data?.getStringExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR), crash)
                                    .joinToString("\n").ifBlank { null },
                                restart = false,
                                confirmedFailure = resultCode != Activity.RESULT_CANCELED || crash != null,
                            )
                        }.getOrNull()
                    }
                if (captured != null) runCatching { SupportReportStore(context).save(captured) }
                if (record != null) runCatching { checkpoint.clear(record.getProperty("session")) }
                crash to captured
            }
        if (report != null) {
            val game = data?.getSerializableExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME) as? Game
            GameCrashActivity.launchReport(activity, report, Armsx2BiosRecovery.fromResult(activity, data), game)
            return
        }
        if (resultCode == Activity.RESULT_CANCELED && nativeCrash != null) {
            handleUnsuccessfulGameFinish(
                activity,
                "The game stopped unexpectedly.",
                nativeCrash,
            )
            return
        }
        when (resultCode) {
            Activity.RESULT_OK -> handleSuccessfulGameFinish(activity, enableRatingFlow, data)
            BaseGameActivity.RESULT_ERROR ->
                handleUnsuccessfulGameFinish(
                    activity,
                    "The game could not start or stopped unexpectedly.",
                    data?.getStringExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR) ?: nativeCrash,
                    Armsx2BiosRecovery.fromResult(activity, data),
                )
            BaseGameActivity.RESULT_UNEXPECTED_ERROR ->
                handleUnsuccessfulGameFinish(
                    activity,
                    "The game stopped unexpectedly.",
                    data?.getStringExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR),
                )
        }
    }

    suspend fun recoverInterruptedGame(activity: Activity) {
        if (PS2RecoveryActivity.restoreInterruptedLaunch(activity)) return
        val report =
            withContext(Dispatchers.IO) {
                val context = activity.applicationContext
                val checkpoint = GameSessionCheckpoint(context)
                runCatching { SupportReportStore(context).list() }
                val record = checkpoint.read() ?: return@withContext null
                runCatching {
                    checkpoint.failure(record, null, restart = true, confirmedFailure = false)?.also {
                        runCatching { SupportReportStore(context).save(it) }
                        runCatching { checkpoint.clear(record.getProperty("session")) }
                    }
                }.getOrNull()
            } ?: return
        if (!activity.isFinishing && !activity.isDestroyed) {
            GameCrashActivity.launchReport(activity, report)
        }
    }

    private fun cancelBackgroundWork(context: Context) {
        SaveSyncWork.cancelAutoWork(context)
        SaveSyncWork.cancelManualWork(context)
        CacheCleanerWork.cancelCleanCacheLRU(context)
    }

    private fun rescheduleBackgroundWork(context: Context) {
        // Let's slightly delay the sync. Maybe the user wants to play another game.
        SaveSyncWork.enqueueAutoWork(context, 5)
        CacheCleanerWork.enqueueCleanCacheLRU(context)
    }

    private fun handleUnsuccessfulGameFinish(
        activity: Activity,
        message: String,
        messageDetail: String?,
        biosRecovery: Intent? = null,
    ) {
        GameCrashActivity.launch(activity, message, messageDetail, biosRecovery)
    }

    private suspend fun handleSuccessfulGameFinish(
        activity: Activity,
        enableRatingFlow: Boolean,
        data: Intent?,
    ) {
        val duration =
            data?.extras?.getLong(BaseGameActivity.PLAY_GAME_RESULT_SESSION_DURATION)
                ?: 0L
        val game = data?.extras?.getSerializable(BaseGameActivity.PLAY_GAME_RESULT_GAME) as? Game ?: return

        updateGamePlayedTimestamp(game)
        if (enableRatingFlow) {
            displayReviewRequest(activity, duration)
        }
    }

    private suspend fun displayReviewRequest(
        activity: Activity,
        durationMillis: Long,
    ) {
        delay(500)
        reviewManager.launchReviewFlow(activity, durationMillis)
    }

    private suspend fun updateGamePlayedTimestamp(game: Game) {
        retrogradeDb.gameDao().update(game.copy(lastPlayedAt = System.currentTimeMillis()))
    }
}
