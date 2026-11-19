package com.swordfish.lemuroid.app.gemrating

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Local catalogue selection, independent of scanner identity and saves. */
class GemLocalCorrections(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("gem_catalogue_corrections_v1", Context.MODE_PRIVATE)

    data class Custom(val title: String, val genre: String?)

    fun custom(game: Game): Custom? {
        val id = game.cartridgeId
        if (id.isBlank()) return null
        val title = preferences.getString("$id:custom-title", null) ?: return null
        return Custom(title, preferences.getString("$id:custom-genre", null))
    }

    fun saveCustom(
        game: Game,
        title: String,
        genre: String,
    ): Boolean {
        val id = game.cartridgeId
        if (id.isBlank() || title.isBlank() || title.length > 512 || genre.length > 128) return false
        return preferences.edit()
            .remove(id)
            .putString("$id:custom-title", title.trim())
            .putString("$id:custom-genre", genre.trim().ifBlank { null })
            .commit()
    }

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
        val id = game.cartridgeId
        return preferences.edit().putString(id, release.releaseId)
            .remove("$id:custom-title").remove("$id:custom-genre").commit()
    }

    fun clear(game: Game): Boolean {
        if (game.cartridgeId.isBlank()) return false
        val id = game.cartridgeId
        return preferences.edit().remove(id).remove("$id:custom-title").remove("$id:custom-genre").commit()
    }
}
