package com.swordfish.lemuroid.app.shared.library

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.shared.NotificationsManager
import com.swordfish.lemuroid.app.utils.android.createSyncForegroundInfo
import com.swordfish.lemuroid.lib.injection.AndroidWorkerInjection
import com.swordfish.lemuroid.lib.injection.WorkerKey
import com.swordfish.lemuroid.lib.library.LemuroidLibrary
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import dagger.Binds
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class LibraryIndexWork(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    @Inject
    lateinit var lemuroidLibrary: LemuroidLibrary

    @Inject
    lateinit var retrogradeDb: RetrogradeDatabase

    override suspend fun doWork(): Result {
        AndroidWorkerInjection.inject(this)

        val notificationsManager = NotificationsManager(applicationContext)

        val foregroundInfo =
            createSyncForegroundInfo(
                NotificationsManager.LIBRARY_INDEXING_NOTIFICATION_ID,
                notificationsManager.libraryIndexingNotification(),
            )

        setForegroundAsync(foregroundInfo)

        var phase = "recovery"
        val runId = java.util.UUID.randomUUID().toString()
        return try {
            val summary =
                withContext(Dispatchers.IO) {
                    refreshLock.withLock {
                        val journal = LibraryRefreshJournal(applicationContext)
                        val shelf = ShelfStore(applicationContext)
                        journal.reconcile(
                            retrogradeDb.gameDao().selectShelfGames().first(),
                            shelf,
                            retrogradeDb.retiredCartridgeDao().selectAll(),
                        )
                        if (inputData.getBoolean("recoverOnly", false)) {
                            return@withLock LemuroidLibrary.IndexSummary(0, 0, 0)
                        }
                        phase = "journal_prepare"
                        journal.prepare(
                            retrogradeDb.gameDao().selectShelfGames().first(),
                            retrogradeDb.retiredCartridgeDao().selectAll(),
                        )
                        val report = lemuroidLibrary.indexLibrary { phase = it }
                        phase = "shelf_reconciliation"
                        journal.reconcile(
                            retrogradeDb.gameDao().selectShelfGames().first(),
                            shelf,
                            retrogradeDb.retiredCartridgeDao().selectAll(),
                        )
                        report
                    }
                }

            phase = "core_scheduling"
            LibraryIndexScheduler.scheduleCoreUpdate(applicationContext)
            Result.success(
                workDataOf(
                    "added" to summary.added,
                    "removed" to summary.removed,
                    "skipped" to summary.skipped,
                    "discProblems" to summary.discProblems,
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.e(error, "ROM folder refresh failed during %s; recovery journal retained if pending", phase)
            // Preserve the full exception in private debug logs, but return
            // only a sanitized category/message to the player.
            Result.failure(
                workDataOf(
                    "errorCode" to LibraryRefreshFailure.code(error),
                    "report" to LibraryRefreshFailure.report(runId, phase, error),
                    "error" to LibraryRefreshFailure.userMessage(error),
                ),
            )
        }
    }

    companion object {
        internal val refreshLock = Mutex()
    }

    @dagger.Module(subcomponents = [Subcomponent::class])
    abstract class Module {
        @Binds
        @IntoMap
        @WorkerKey(LibraryIndexWork::class)
        abstract fun bindMyWorkerFactory(builder: Subcomponent.Builder): AndroidInjector.Factory<out ListenableWorker>
    }

    @dagger.Subcomponent
    interface Subcomponent : AndroidInjector<LibraryIndexWork> {
        @dagger.Subcomponent.Builder
        abstract class Builder : AndroidInjector.Builder<LibraryIndexWork>()
    }
}
