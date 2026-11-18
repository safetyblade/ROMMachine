package com.swordfish.lemuroid.app.gemrating

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Local catalogue selection, independent of scanner identity and saves. */
class GemLocalCorrections(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("gem_catalogue_corrections_v1", Context.MODE_PRIVATE)

    fun read(game: Game): String? =
        game.cartridgeId.takeIf { it.isNotBlank() }?.let { preferences.getString(it, null) }

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
