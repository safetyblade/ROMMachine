package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeSpine
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LocalCartridgeGeometry
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** The canonical views share a baseline, pickup geometry and the detail navigation action. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfCartridge(
    game: Game,
    facing: CartridgeFacing,
    slotWidth: Dp,
    footprint: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    registerGeometry: Boolean = true,
    artworkRevision: Int = 0,
) {
    if (facing == CartridgeFacing.SPINE) {
        CartridgeSpine(game, onClick, modifier, onLongClick, width = slotWidth, registerGeometry = registerGeometry)
        return
    }
    val height = slotWidth * (340f / 94f)
    val frontWidth = minOf(footprint, height * (320f / 390f))
    val geometry = LocalCartridgeGeometry.current
    val owner = remember(game.id) { Any() }
    DisposableEffect(game.id, registerGeometry) {
        onDispose { if (registerGeometry) geometry.remove(game.id, owner) }
    }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier.size(frontWidth, frontWidth * (390f / 320f))
            .onGloballyPositioned {
                if (registerGeometry) {
                    geometry.update(
                        game.id,
                        owner,
                        Rect(it.positionInRoot(), it.size.toSize()),
                        front = true,
                    )
                }
            }
            .graphicsLayer { alpha = if (registerGeometry && geometry.selectedId == game.id) 0f else 1f }
            .onFocusChanged { focused = it.isFocused }
            .border(
                if (focused) 3.dp else 0.dp,
                if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${game.displayName()}, ${cartridgeSystemLabel(game.systemId)}, front facing"
            }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
    ) {
        CartridgeFront(game, artworkRevision = artworkRevision)
    }
}
