package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ShelfViewModel(context: Context) : ViewModel() {
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ShelfViewModel(context.applicationContext) as T
    }

    data class State(val layout: ShelfLayout? = null, val error: String? = null, val canUndo: Boolean = false)

    private val store = ShelfStore(context.applicationContext)
    private val serial = Mutex()
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private val undo = ArrayDeque<ShelfLayout>()
    private var identities = emptyMap<String, String>()

    init {
        reload()
        // The worker atomically updates shelf-layout-v1.json only after a
        // complete scan. Observe completed work to reload its new contents,
        // including when refresh was started outside the Home screen.
        viewModelScope.launch {
            WorkManager.getInstance(context.applicationContext)
                .getWorkInfosForUniqueWorkFlow(LibraryIndexScheduler.LIBRARY_INDEX_WORK_ID)
                .map { jobs ->
                    jobs.filter { it.state == WorkInfo.State.SUCCEEDED }.map { it.id }.toSet()
                }
                .distinctUntilChanged()
                .collect { completed ->
                    if (completed.isNotEmpty()) reload(clearUndo = true)
                }
        }
    }

    fun reload(clearUndo: Boolean = false) {
        viewModelScope.launch {
            serial.withLock {
                runCatching { store.read() }.onSuccess {
                    if (clearUndo) undo.clear()
                    mutableState.value = State(it, canUndo = undo.isNotEmpty())
                }
                    .onFailure {
                        mutableState.value =
                            State(error = "Saved shelf could not be loaded. Retry without resetting it.")
                    }
            }
        }
    }

    fun reconcile(games: List<Game>) {
        identities = games.associate { shelfGameKey(it.systemId, it.fileUri) to it.displayName() }
        edit(recordUndo = false) { it.reconcile(identities) }
    }

    fun edit(
        recordUndo: Boolean = true,
        transform: (ShelfLayout) -> ShelfLayout,
    ) {
        viewModelScope.launch {
            serial.withLock {
                if (mutableState.value.layout == null) return@withLock
                runCatching { store.update(transform) }.onSuccess { (before, after) ->
                    if (recordUndo && before != after) {
                        undo.addLast(before)
                        while (undo.size > 10) undo.removeFirst()
                    }
                    mutableState.value = State(after, canUndo = undo.isNotEmpty())
                }.onFailure {
                        error ->
                    mutableState.value =
                        mutableState.value.copy(
                            error = error.message ?: "Shelf change failed. Previous layout is safe.",
                        )
                }
            }
        }
    }

    fun undo() {
        viewModelScope.launch {
            serial.withLock {
                val previous = undo.lastOrNull() ?: return@withLock
                runCatching { store.update { previous.copy(revision = it.revision).reconcile(identities) } }
                    .onSuccess {
                            (_, after) ->
                        undo.removeLast()
                        mutableState.value = State(after, canUndo = undo.isNotEmpty())
                    }
                    .onFailure {
                        mutableState.value =
                            mutableState.value.copy(error = "Undo failed. Previous layout is safe.")
                    }
            }
        }
    }
}
