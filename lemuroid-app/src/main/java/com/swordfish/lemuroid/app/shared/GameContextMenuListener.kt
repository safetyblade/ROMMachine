package com.swordfish.lemuroid.app.shared

import android.content.Context
import android.content.Intent
import android.view.ContextMenu
import android.view.Menu
import android.view.View
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.ControllerSettingsActivity
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.DisplaySettingsActivity
import com.swordfish.lemuroid.lib.library.db.entity.Game

class GameContextMenuListener(
    private val gameInteractor: GameInteractor,
    private val game: Game,
) : View.OnCreateContextMenuListener {
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?,
    ) {
        menu.add(R.string.game_context_menu_resume).setOnMenuItemClickListener {
            gameInteractor.onGamePlay(game)
            true
        }

        menu.add(R.string.game_context_menu_restart).setOnMenuItemClickListener {
            gameInteractor.onGameRestart(game)
            true
        }

        addCartridgeSetupEntries(menu, v.context, game)

        if (game.isFavorite) {
            menu.add(R.string.game_context_menu_remove_from_favorites).setOnMenuItemClickListener {
                gameInteractor.onFavoriteToggle(game, false)
                true
            }
        } else {
            menu.add(R.string.game_context_menu_add_to_favorites).setOnMenuItemClickListener {
                gameInteractor.onFavoriteToggle(game, true)
                true
            }
        }

        if (gameInteractor.supportShortcuts()) {
            menu.add(R.string.game_context_menu_create_shortcut).setOnMenuItemClickListener {
                gameInteractor.onCreateShortcut(game)
                true
            }
        }
    }
}

/** The TV/legacy library keeps its existing cartridge context-menu navigation. */
internal fun addCartridgeSetupEntries(
    menu: Menu,
    context: Context,
    game: Game,
) {
    val setup = menu.addSubMenu("Setup")
    setup.add("Display").intent = Intent(context, DisplaySettingsActivity::class.java).putExtra("cartdeck.game", game)
    setup.add("Controls").intent = Intent(context, ControllerSettingsActivity::class.java).putExtra("cartdeck.game", game)
}
