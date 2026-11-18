package com.swordfish.lemuroid.app.shared

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shortcuts.ShortcutsGenerator
import com.swordfish.lemuroid.app.shared.game.GameLauncher
import com.swordfish.lemuroid.app.shared.main.BusyActivity
import com.swordfish.lemuroid.common.displayToast
import com.swordfish.lemuroid.lib.library.covers.CustomCoverManager
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameInteractor(
    private val activity: BusyActivity,
    private val retrogradeDb: RetrogradeDatabase,
    private val useLeanback: Boolean,
    private val shortcutsGenerator: ShortcutsGenerator,
    private val gameLauncher: GameLauncher,
    private val customCoverManager: CustomCoverManager,
) {
    fun onGamePlay(game: Game) {
        if (!ensureNotBusy()) {
            return
        }
        if (!ensureNotificationsPermissionAvailable()) {
            return
        }
        gameLauncher.launchGameAsync(activity.activity(), game, true, useLeanback)
    }

    fun onGameRestart(game: Game) {
        if (!ensureNotBusy()) {
            return
        }
        if (!ensureNotificationsPermissionAvailable()) {
            return
        }
        gameLauncher.launchGameAsync(activity.activity(), game, false, useLeanback)
    }

    fun onFavoriteToggle(
        game: Game,
        isFavorite: Boolean,
    ) {
        GlobalScope.launch {
            retrogradeDb.gameDao().update(game.copy(isFavorite = isFavorite))
        }
    }

    fun onCreateShortcut(game: Game) {
        GlobalScope.launch {
            shortcutsGenerator.pinShortcutForGame(game)
        }
    }

    fun supportShortcuts(): Boolean {
        return shortcutsGenerator.supportShortcuts()
    }

    fun onSetCustomName(
        game: Game,
        customName: String,
    ) {
        GlobalScope.launch {
            retrogradeDb.gameDao().update(game.copy(customName = customName))
        }
    }

    fun onClearCustomName(game: Game) {
        GlobalScope.launch {
            retrogradeDb.gameDao().update(game.copy(customName = null))
        }
    }

    /** Copy and validate local artwork before committing the cover field only. */
    fun onSetCustomCoverUri(
        game: Game,
        sourceUri: String,
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            val changed =
                replaceArtwork(game) {
                    customCoverManager.stageLocalCover(game.id, Uri.parse(sourceUri))
                }
            if (!changed) showCoverFailure()
        }
    }

    /** Reset the database reference first; remove only the old owned artwork after success. */
    fun onClearCustomCoverUri(game: Game) {
        GlobalScope.launch(Dispatchers.Main) {
            if (!replaceArtwork(game) { null }) showCoverFailure()
        }
    }

    suspend fun useOnlineCover(
        game: Game,
        bitmap: Bitmap,
    ): Boolean =
        replaceArtwork(game) {
            customCoverManager.stageReplacement(game.id) { file ->
                file.outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                    it.fd.sync()
                }
            }
        }

    /** All artwork changes share the canonical identity and compare-and-set commit boundary. */
    private suspend fun replaceArtwork(
        game: Game,
        stage: () -> String?,
    ): Boolean =
        withContext(Dispatchers.IO + NonCancellable) {
            if (game.cartridgeId.isBlank()) return@withContext false
            val dao = retrogradeDb.gameDao()
            val current = runCatching { dao.selectById(game.id) }.getOrNull() ?: return@withContext false
            if (current.cartridgeId != game.cartridgeId || current.customCoverUri != game.customCoverUri) {
                return@withContext false
            }
            val staged =
                try {
                    stage()
                } catch (_: Exception) {
                    return@withContext false
                }
            val changed =
                runCatching {
                    dao.replaceCoverIfCurrent(game.id, game.cartridgeId, current.customCoverUri, staged) == 1
                }.getOrDefault(false)
            if (changed) {
                if (current.customCoverUri != staged) {
                    runCatching { customCoverManager.discardOwnedCover(game.id, current.customCoverUri) }
                }
            } else {
                runCatching { customCoverManager.discardOwnedCover(game.id, staged) }
            }
            changed
        }

    private fun showCoverFailure() {
        Toast.makeText(
            activity.activity(),
            "Artwork could not be changed. Existing cover retained.",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun ensureNotificationsPermissionAvailable(): Boolean {
        if (useLeanback || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        val permissionResult =
            ContextCompat.checkSelfPermission(
                activity.activity(),
                Manifest.permission.POST_NOTIFICATIONS,
            )

        if (permissionResult == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        activity.activity().displayToast(R.string.game_interactor_notification_permission_required)
        return false
    }

    private fun ensureNotBusy(): Boolean {
        if (activity.isBusy()) {
            activity.activity().displayToast(R.string.game_interactory_busy)
            return false
        }
        return true
    }
}
