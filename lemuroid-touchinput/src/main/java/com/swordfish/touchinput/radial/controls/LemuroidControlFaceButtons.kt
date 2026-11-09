package com.swordfish.touchinput.radial.controls

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.swordfish.touchinput.radial.LocalCartdeckControlBounds
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import gg.padkit.PadKitScope
import gg.padkit.anchors.Anchor
import gg.padkit.controls.ControlFaceButtons
import gg.padkit.ids.Id
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf

context(PadKitScope)
@Composable
fun LemuroidControlFaceButtons(
    modifier: Modifier = Modifier,
    rotationInDegrees: Float = 0f,
    ids: PersistentList<Id.Key>,
    includeComposite: Boolean = true,
    applyPadding: Boolean = true,
    trackPointers: Boolean = true,
    background: @Composable () -> Unit = {},
    idsForegrounds: PersistentMap<Id.Key, @Composable (State<Boolean>) -> Unit>,
) {
    val theme = LocalLemuroidPadTheme.current
    val observe = LocalCartdeckControlBounds.current
    val key = if (ids.size <= 2) "face-ab" else "face-abxy"
    ControlFaceButtons(
        modifier =
            modifier
                .run { if (applyPadding) padding(theme.padding) else modifier }
                .then(
                    if (observe != null) {
                        Modifier.onGloballyPositioned { observe(key, it.boundsInRoot()) }
                    } else {
                        Modifier
                    },
                ),
        includeComposite = includeComposite,
        ids = ids,
        trackPointers = trackPointers,
        rotationInDegrees = rotationInDegrees,
        foreground = { id, pressed -> (idsForegrounds[id]!!)(pressed) },
        background = background,
        foregroundComposite = {},
    )
}

context(PadKitScope)
@Composable
fun LemuroidControlFaceButtons(
    modifier: Modifier = Modifier,
    primaryAnchors: PersistentList<Anchor<Id.Key>>,
    background: @Composable () -> Unit = {},
    applyPadding: Boolean = true,
    trackPointers: Boolean = true,
    idsForegrounds: PersistentMap<Id.Key, @Composable (State<Boolean>) -> Unit>,
) {
    val theme = LocalLemuroidPadTheme.current
    val observe = LocalCartdeckControlBounds.current
    val key = if (primaryAnchors.size <= 2) "face-ab" else "face-abxy"
    ControlFaceButtons(
        modifier =
            modifier
                .run { if (applyPadding) padding(theme.padding) else modifier }
                .then(
                    if (observe != null) {
                        Modifier.onGloballyPositioned { observe(key, it.boundsInRoot()) }
                    } else {
                        Modifier
                    },
                ),
        primaryAnchors = primaryAnchors,
        compositeAnchors = persistentListOf(),
        trackPointers = trackPointers,
        foreground = { id, pressed -> (idsForegrounds[id]!!)(pressed) },
        background = background,
        foregroundComposite = {},
    )
}
