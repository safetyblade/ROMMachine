package com.swordfish.lemuroid.app.shared.game.ps2next

import android.content.Context
import android.content.Intent
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Explicit BIOS-only route; never infer a recovery action from an arbitrary error string. */
object Armsx2BiosRecovery {
    const val REQUIRED = "cartdeck.ps2.bios.required"
    const val SETUP = "cartdeck.ps2.bios.setup"
    const val ACTIVITY = "com.swordfish.lemuroid.app.debug.Armsx2RuntimeProbeActivity"

    fun fromResult(
        context: Context,
        result: Intent?,
    ): Intent? {
        if (result?.getBooleanExtra(REQUIRED, false) != true) return null
        val game = result.getSerializableExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME) as? Game ?: return null
        if (game.systemId != SystemID.PLAYSTATION_2.dbname) return null
        return Intent().setClassName(context, ACTIVITY).putExtra("cartdeck.game", game).putExtra(SETUP, true)
    }

    fun fromSettings(context: Context): Intent = Intent().setClassName(context, ACTIVITY).putExtra(SETUP, true)

    fun validate(
        context: Context,
        target: Intent?,
    ): Intent? {
        val component = target?.component ?: return null
        val game = target.getSerializableExtra("cartdeck.game") as? Game
        return target.takeIf {
            component.packageName == context.packageName && component.className == ACTIVITY &&
                it.getBooleanExtra(SETUP, false) && (game == null || game.systemId == SystemID.PLAYSTATION_2.dbname)
        }
    }
}
