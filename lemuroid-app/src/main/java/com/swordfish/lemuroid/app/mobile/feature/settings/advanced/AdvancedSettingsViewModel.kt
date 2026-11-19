package com.swordfish.lemuroid.app.mobile.feature.settings.advanced

import android.content.Context
import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.shared.settings.SettingsInteractor
import com.swordfish.lemuroid.lib.storage.cache.CacheCleaner
import com.swordfish.lemuroid.lib.storage.cache.StorageUsageReview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdvancedSettingsViewModel(
    private val appContext: Context,
    private val settingsInteractor: SettingsInteractor,
) : ViewModel() {
    class Factory(
        private val appContext: Context,
        private val settingsInteractor: SettingsInteractor,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdvancedSettingsViewModel(appContext, settingsInteractor) as T
        }
    }

    data class CacheState(
        val default: String,
        val values: List<String>,
        val displayNames: List<String>,
    )

    data class State(val cache: CacheState)

    data class StorageState(
        val report: StorageUsageReview.Report? = null,
        val busy: Boolean = false,
        val message: String? = null,
    )

    private val storage = MutableStateFlow(StorageState())
    val storageState = storage.asStateFlow()

    fun reviewStorage(clear: Boolean = false) {
        if (storage.value.busy) return
        storage.value = storage.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try {
                val (report, message) =
                    withContext(Dispatchers.IO) {
                        var message: String? = null
                        if (clear) {
                            val cleared =
                                GameProcessLock.withIdleAccess(appContext) {
                                    CacheCleaner.cleanAll(appContext)
                                }
                            message =
                                if (cleared) {
                                    "Storage refreshed. Only staged ROM copies were eligible for deletion."
                                } else {
                                    "A game or storage operation is active. Return to Shelf and retry."
                                }
                        }
                        val coroutine = currentCoroutineContext()
                        val report =
                            StorageUsageReview.inspect(
                                java.io.File(appContext.applicationInfo.dataDir),
                                appContext.cacheDir,
                                appContext.filesDir,
                                appContext.getExternalFilesDir(null),
                            ) { coroutine.ensureActive() }
                        report to message
                    }
                storage.value = StorageState(report = report, message = message)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                storage.value = storage.value.copy(busy = false, message = "Storage review could not finish. Retry.")
            }
        }
    }

    val uiState =
        initializeState(appContext)
            .stateIn(viewModelScope, started = SharingStarted.Lazily, null)

    private fun initializeState(appContext: Context): Flow<State?> =
        flow {
            val supportedCacheValues = CacheCleaner.getSupportedCacheLimits()

            val default = CacheCleaner.getDefaultCacheLimit().toString()

            val displayNames =
                supportedCacheValues
                    .map { getSizeLabel(appContext, it) }

            val values =
                supportedCacheValues
                    .map { it.toString() }

            emit(State(CacheState(default, values, displayNames)))
        }

    private fun getSizeLabel(
        appContext: Context,
        size: Long,
    ): String {
        return if (size == 0L) "Reload every session" else Formatter.formatShortFileSize(appContext, size)
    }

    fun resetAllSettings() {
        settingsInteractor.resetAllSettings()
    }
}
