package com.swordfish.lemuroid.app.gemrating

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Local catalogue selection, independent of scanner identity and saves. */
class GemLocalCorrections(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("gem_catalogue_corrections_v1", Context.MODE_PRIVATE)

    fun read(game: Game): String? = read(game.cartridgeId)

    fun read(cartridgeId: String): String? {
        if (cartridgeId.isBlank()) return null
        return preferences.getString(cartridgeId, null)
    }

    fun select(
        game: Game,
        release: GemCatalogueIndex.Release,
        catalogue: GemCataloguePackage.Loaded,
    ): Boolean {
        if (game.cartridgeId.isBlank() || game.systemId != release.systemId) return false
        if (catalogue.index.find(release.releaseId) != release) return false
        return preferences.edit().putString(game.cartridgeId, release.releaseId).commit()
    }

    fun clear(game: Game): Boolean {
        if (game.cartridgeId.isBlank()) return false
        return preferences.edit().remove(game.cartridgeId).commit()
    }
}
