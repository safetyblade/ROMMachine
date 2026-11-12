package com.swordfish.lemuroid.app.shared.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckEdition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Branded pre-game cartridge loader. No invented progress percentage and no
 * delay before gameplay: this composable disappears as soon as the game loads.
 * Read appearance off the UI thread; never mutate the shelf or game files.
 */
@Composable
internal fun CartdeckLoadingScreen(message: String?) {
    val context = LocalContext.current.applicationContext
    val appearance by produceState(initialValue = ShelfAppearance(), key1 = context) {
        value = withContext(Dispatchers.IO) {
            runCatching { ShelfStore(context).read().appearance }.getOrDefault(ShelfAppearance())
        }
    }
    val edition = when (appearance.edition.lowercase()) {
        "pink" -> CartdeckEditionPreset.PINK
        "green" -> CartdeckEditionPreset.GREEN
        else -> CartdeckEditionPreset.ORIGINAL
    }
    val base = remember(context, edition) { context.resolveCartdeckEdition(edition) }
    val chassis = appearance.custom["chassis"]?.let { Color(it.toInt()) } ?: base.chassis
    val panel = appearance.custom["secondary"]?.let { Color(it.toInt()) } ?: base.secondary
    val bezel = appearance.custom["bezel"]?.let { Color(it.toInt()) } ?: base.bezel
    val accent = appearance.custom["accent"]?.let { Color(it.toInt()) } ?: base.accent
    val ink = appearance.custom["onChassis"]?.let { Color(it.toInt()) } ?: base.onChassis
    val button = appearance.custom["button"]?.let { Color(it.toInt()) } ?: base.button
    val slot = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier.fillMaxSize().background(chassis),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val gap = 18.dp.toPx()
            val width = size.width
            // Subtle manufacturing rails share the current CARTDECK palette.
            drawLine(
                bezel, Offset(gap, gap), Offset(gap, size.height - gap),
                strokeWidth = 3.dp.toPx(),
            )
            drawLine(
                bezel, Offset(width - gap, gap), Offset(width - gap, size.height - gap),
                strokeWidth = 3.dp.toPx(),
            )
            drawLine(
                accent.copy(alpha = 0.62f),
                Offset(gap, gap), Offset(gap + 35.dp.toPx(), gap),
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                accent.copy(alpha = 0.62f),
                Offset(width - gap - 35.dp.toPx(), gap), Offset(width - gap, gap),
                strokeWidth = 2.dp.toPx(),
            )
        }
        Column(
            modifier =
                Modifier.fillMaxWidth(0.84f).widthIn(max = 420.dp)
                    .clip(slot).background(panel).border(2.dp, bezel, slot)
                    .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Text(
                text = "CARTDECK",
                color = ink,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Canvas(Modifier.size(width = 105.dp, height = 128.dp)) {
                // Stylised cartridge physically entering the C-slot.
                val w = size.width
                val h = size.height
                val inset = 5.dp.toPx()
                drawRoundRect(
                    bezel,
                    topLeft = Offset(inset, 0f),
                    size = Size(w - 2 * inset, h),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
                drawRoundRect(
                    panel,
                    topLeft = Offset(2 * inset, inset),
                    size = Size(w - 4 * inset, h - 2 * inset),
                    cornerRadius = CornerRadius(9.dp.toPx()),
                )
                drawRoundRect(
                    button,
                    topLeft = Offset(w * 0.20f, h * 0.30f),
                    size = Size(w * 0.60f, h * 0.46f),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
                drawRoundRect(
                    accent,
                    topLeft = Offset(w * 0.27f, h * 0.40f),
                    size = Size(w * 0.46f, 3.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
                drawLine(
                    accent,
                    Offset(w * 0.16f, h - 8.dp.toPx()),
                    Offset(w * 0.84f, h - 8.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                )
                drawRoundRect(
                    ink.copy(alpha = 0.30f),
                    topLeft = Offset(2 * inset, inset),
                    size = Size(w - 4 * inset, h - 2 * inset),
                    cornerRadius = CornerRadius(9.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            Text(
                "Preparing cartridge",
                color = ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (appearance.reducedMotion) {
                    Canvas(Modifier.size(22.dp)) {
                        drawCircle(accent, radius = size.minDimension / 3f)
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = accent,
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    text = message?.takeIf { it.isNotBlank() } ?: "Initialising game",
                    color = ink,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start,
                    maxLines = 3,
                )
            }
            Text(
                "Loading time depends on the game and emulator.",
                color = ink.copy(alpha = 0.80f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
