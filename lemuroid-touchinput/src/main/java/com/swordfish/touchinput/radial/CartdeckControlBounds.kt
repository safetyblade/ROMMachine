package com.swordfish.touchinput.radial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect

/**
 * Optional read-only P1-C2B PadKit bounds observer, in root pixels.
 * Does not register gestures or modify a control's PadKit identity/position.
 */
val LocalCartdeckControlBounds =
    compositionLocalOf<((String, Rect) -> Unit)?> { null }
