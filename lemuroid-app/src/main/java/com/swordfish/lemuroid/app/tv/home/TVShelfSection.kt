package com.swordfish.lemuroid.app.tv.home

import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfLayout
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.lib.library.db.entity.Game

internal data class TVShelfSection(
    val id: String,
    val title: String,
    val games: List<Game>,
)

/** Read-only TV projection; never writes the owner's layout or cartridge records. */
internal fun tvShelfSections(
    layout: ShelfLayout,
    games: List<Game>,
): List<TVShelfSection> {
    val byKey = games.associateBy { shelfGameKey(it.systemId, it.fileUri) }
    val projected = layout.reconcile(byKey.mapValues { it.value.displayName() })
    return projected.visibleSections(byKey.keys).mapNotNull { section ->
        val cartridges = section.cells.mapNotNull { byKey[it.gameKey] }
        if (cartridges.isEmpty()) null else TVShelfSection(section.id, section.title, cartridges)
    }
}
