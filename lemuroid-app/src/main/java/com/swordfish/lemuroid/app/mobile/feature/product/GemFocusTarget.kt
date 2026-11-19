package com.swordfish.lemuroid.app.mobile.feature.product

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** BRAND-01: independent focus indication, never a colour-only selected state. */
@Composable
internal fun Modifier.gemFocusTarget(): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this.heightIn(min = 48.dp)
        .onFocusChanged { focused = it.hasFocus }
        .border(
            if (focused) 2.dp else 0.dp,
            if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
            RoundedCornerShape(8.dp),
        )
}
