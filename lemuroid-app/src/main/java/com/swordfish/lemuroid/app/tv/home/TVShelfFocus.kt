package com.swordfish.lemuroid.app.tv.home

import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ObjectAdapter
import com.swordfish.lemuroid.lib.library.db.entity.Game

internal data class TVShelfFocus(val rowKey: String, val cartridgeId: String?)

internal fun tvShelfRowKey(
    rowId: Long,
    sections: List<TVShelfSection>,
): String? =
    if (rowId >= 0) {
        "standard:$rowId"
    } else {
        sections.getOrNull((-1L - rowId).toInt())?.let { "section:${it.id}" }
    }

internal fun locateTVShelfFocus(
    rows: ObjectAdapter,
    sections: List<TVShelfSection>,
    focus: TVShelfFocus,
): Pair<Int, Int?>? {
    for (index in 0 until rows.size()) {
        val row = rows.get(index) as? ListRow ?: continue
        if (tvShelfRowKey(row.headerItem.id, sections) != focus.rowKey) continue
        val item =
            if (focus.cartridgeId == null) {
                null
            } else {
                (0 until row.adapter.size()).firstOrNull {
                    (row.adapter.get(it) as? Game)?.cartridgeId == focus.cartridgeId
                }
            }
        return index to item
    }
    return null
}
