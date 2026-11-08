package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Original/Pink tokens from v03. Library-only: gameplay themes remain independent. */
@Composable
fun CartridgeLibraryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = Color(0xFF4ED8C4),
                onPrimary = Color(0xFF09131B),
                background = Color(0xFF0D1721),
                surface = Color(0xFF1C2430),
                surfaceVariant = Color(0xFF303B49),
                onBackground = Color(0xFFEDF6F2),
                onSurface = Color(0xFFEDF6F2),
                onSurfaceVariant = Color(0xFFEDF6F2),
                secondary = Color(0xFF4ED8C4),
                primaryContainer = Color(0xFF303B49),
                onPrimaryContainer = Color(0xFFEDF6F2),
                secondaryContainer = Color(0xFF303B49),
                onSecondaryContainer = Color(0xFF4ED8C4),
                tertiary = Color(0xFF4ED8C4),
                onTertiary = Color(0xFF09131B),
                surfaceTint = Color(0xFF4ED8C4),
            ),
        content = content,
    )
}

fun cartridgeSystemLabel(systemId: String): String =
    when (systemId.lowercase()) {
        "gb" -> "GB"
        "gbc" -> "GBC"
        "gba" -> "GBA"
        "psx" -> "PS1"
        else -> systemId.uppercase()
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CartridgeSpine(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    pink: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val ink = if (pink) Color(0xFF54253F) else Color(0xFFEDF6F2)
    Box(
        modifier =
            modifier
                .size(94.dp * scale, 340.dp * scale)
                .onFocusChanged { focused = it.isFocused }
                .border(
                    if (focused) 3.dp else 0.dp,
                    if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(12.dp),
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = "${game.displayName()}, ${cartridgeSystemLabel(game.systemId)}"
                }
                .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
    ) {
        Image(
            painterResource(if (pink) R.drawable.cartdeck_pink_spine_base else R.drawable.cartdeck_original_spine_base),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painterResource(
                if (pink) R.drawable.cartdeck_pink_cslot_master else R.drawable.cartdeck_original_cslot_master,
            ),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 35.dp * scale).size(28.dp * scale),
        )
        // Layout is measured sideways before rotation, keeping the title inside its 46×171 window.
        Box(
            Modifier.offset(24.dp * scale, 82.dp * scale).size(46.dp * scale, 171.dp * scale),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                game.displayName(),
                modifier = Modifier.requiredSize(171.dp * scale, 46.dp * scale).rotate(-90f).padding(horizontal = 6.dp),
                color = if (pink) Color(0xFFFFF3F7) else ink,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            cartridgeSystemLabel(game.systemId),
            modifier = Modifier.offset(17.dp * scale, 276.dp * scale).width(60.dp * scale),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (pink) Color(0xFFFFF3F7) else ink,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CartridgeFront(
    game: Game,
    modifier: Modifier = Modifier,
    pink: Boolean = false,
    artworkRevision: Int = 0,
) {
    BoxWithConstraints(modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(320f / 390f)) {
        val unit = maxWidth / 320f
        Image(
            painterResource(if (pink) R.drawable.cartdeck_pink_front_base else R.drawable.cartdeck_original_front_base),
            null,
            Modifier.fillMaxSize(),
        )
        val fallback =
            painterResource(
                if (pink) R.drawable.cartdeck_pink_fallback_atlas else R.drawable.cartdeck_original_fallback_atlas,
            )
        // Only existing local user artwork: no new scraping or bundled game art.
        AsyncImage(
            model =
                ImageRequest.Builder(LocalContext.current)
                    .data(game.customCoverUri)
                    .memoryCacheKey("${game.customCoverUri}:$artworkRevision")
                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                    .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                    .build(),
            contentDescription = null,
            fallback = fallback,
            error = fallback,
            placeholder = fallback,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.offset(
                    unit * 40,
                    unit * 113,
                ).size(unit * 240, unit * 200).clip(RoundedCornerShape(unit * 8)),
        )
        Image(
            painterResource(
                if (pink) R.drawable.cartdeck_pink_front_overlay else R.drawable.cartdeck_original_front_overlay,
            ),
            null,
            Modifier.fillMaxSize(),
        )
        Image(
            painterResource(
                if (pink) R.drawable.cartdeck_pink_cslot_master else R.drawable.cartdeck_original_cslot_master,
            ),
            null,
            Modifier.offset(unit * 40, unit * 38).size(unit * 40),
        )
        Text(
            "CARTDECK",
            modifier = Modifier.offset(unit * 90, unit * 48),
            color = if (pink) Color(0xFF54253F) else Color(0xFFEDF6F2),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            cartridgeSystemLabel(game.systemId),
            modifier = Modifier.offset(unit * 215, unit * 334).width(unit * 78),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (pink) Color(0xFFFFF3F7) else Color(0xFFEDF6F2),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "Original Arc-Rail")
@Preview(name = "Large text", fontScale = 1.8f)
@Composable
private fun OriginalCartridgePreview() {
    CartridgeLibraryTheme { CartridgeFront(previewCartridge()) }
}

@Preview(name = "Pink Arc-Rail")
@Composable
private fun PinkCartridgePreview() {
    CartridgeLibraryTheme { CartridgeFront(previewCartridge(), pink = true) }
}

private fun previewCartridge() =
    Game(
        id = 1,
        title = "Atlas — an original cartridge",
        fileName = "atlas.gb",
        fileUri = "",
        systemId = "gb",
        developer = null,
        coverFrontUrl = null,
        lastIndexedAt = 0,
    )
