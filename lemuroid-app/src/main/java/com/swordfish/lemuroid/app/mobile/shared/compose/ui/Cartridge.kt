package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckDecalLayer
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckLighting
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckSurfaceDesign
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfMaterials
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.drawCartdeckSurfaceDesign
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfMaterials
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.util.Locale

@Composable
fun CartridgeLibraryTheme(
    appearance: ShelfAppearance = ShelfAppearance(),
    content: @Composable () -> Unit,
) {
    val materials = shelfMaterials(appearance)
    val geometry = remember { CartridgeGeometry() }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalShelfAppearance provides appearance,
        LocalCartridgeGeometry provides geometry,
        LocalShelfMaterials provides materials,
    ) {
        val base = if (materials.roomBackdrop.luminance() > 0.5f) lightColorScheme() else darkColorScheme()
        val primary =
            com.swordfish.lemuroid.app.mobile.feature.shelf.readableColor(
                materials.interactionAccent,
                listOf(materials.shelfBackboard),
                4.5f,
            )
        MaterialTheme(
            colorScheme =
                base.copy(
                    primary = primary,
                    onPrimary =
                        com.swordfish.lemuroid.app.mobile.feature.shelf.readableColor(
                            materials.roomBackdrop,
                            listOf(primary),
                            4.5f,
                        ),
                    background = materials.roomBackdrop,
                    surface = materials.shelfBackboard,
                    surfaceVariant = materials.shelfDeck,
                    onBackground =
                        com.swordfish.lemuroid.app.mobile.feature.shelf.readableColor(
                            materials.sectionText,
                            listOf(materials.roomBackdrop),
                            4.5f,
                        ),
                    onSurface = materials.sectionText,
                    onSurfaceVariant =
                        com.swordfish.lemuroid.app.mobile.feature.shelf.readableColor(
                            materials.sectionText,
                            listOf(materials.shelfDeck),
                            4.5f,
                        ),
                    secondary = materials.interactionAccent,
                    primaryContainer = materials.shelfBackboard,
                    onPrimaryContainer = materials.sectionText,
                    secondaryContainer = materials.shelfBackboard,
                    onSecondaryContainer = materials.interactionAccent,
                    surfaceTint = materials.interactionAccent,
                    outline = materials.sectionDivider,
                    outlineVariant = materials.sectionDivider,
                ),
            typography = CartdeckTypography,
            content = content,
        )
    }
}

fun cartridgeSystemLabel(systemId: String): String =
    when (systemId.lowercase()) {
        "gb" -> "GB"
        "gbc" -> "GBC"
        "gba" -> "GBA"
        "psx" -> "PS1"
        "xbox" -> "Xbox"
        "xbox360" -> "X360"
        "wii" -> "Wii"
        else -> systemId.uppercase()
    }

/** Presentation only: never write a shortened title to the library record. */
fun shelfPresentationTitle(title: String): String =
    title
        .replace(
            Regex("""\s*\((?:USA|Europe|Japan|World|En(?:,[A-Za-z]+)*|Rev[ .][^)]*)\)\s*$""", RegexOption.IGNORE_CASE),
            "",
        )
        .replace(Regex("""\s*\[(?:!|v[0-9.]+)\]\s*$""", RegexOption.IGNORE_CASE), "")
        .trim()
        .ifEmpty { title }

/** Source SVG geometry stays fixed across editions; Green uses generated ASSET-H1 vectors. */
private fun cartridgeAsset(
    edition: String,
    part: String,
): Int =
    when (edition to part) {
        "pink" to "spine" -> R.drawable.cartdeck_pink_spine_base
        "pink" to "front" -> R.drawable.cartdeck_pink_front_base
        "pink" to "overlay" -> R.drawable.cartdeck_pink_front_overlay
        "pink" to "fallback" -> R.drawable.cartdeck_pink_fallback_atlas
        "green" to "spine" -> R.drawable.cartdeck_h1_green_spine_base
        "green" to "front" -> R.drawable.cartdeck_h1_green_front_base
        "green" to "overlay" -> R.drawable.cartdeck_h1_green_front_overlay
        "green" to "fallback" -> R.drawable.cartdeck_h1_green_fallback_atlas
        "pink" to "mark" -> R.drawable.cartdeck_pink_cslot_master
        else ->
            when (part) {
                "spine" -> R.drawable.cartdeck_original_spine_base
                "front" -> R.drawable.cartdeck_original_front_base
                "overlay" -> R.drawable.cartdeck_original_front_overlay
                "fallback" -> R.drawable.cartdeck_original_fallback_atlas
                else -> R.drawable.cartdeck_original_cslot_master
            }
    }

/** Actual canonical cartridge vectors shown against shelf materials, with no game artwork. */
@Composable
fun CartdeckAppearancePreview(
    appearance: ShelfAppearance,
    modifier: Modifier = Modifier,
    height: Dp = 152.dp,
) {
    val materials = shelfMaterials(appearance)
    val edition = appearance.edition
    val scale = height.value / 152f
    androidx.compose.runtime.CompositionLocalProvider(LocalShelfAppearance provides appearance) {
        Box(
            modifier =
                modifier.fillMaxWidth().height(height)
                    .clip(RoundedCornerShape(10.dp))
                    .background(materials.shelfBackboard),
        ) {
            // The miniature shelf is the same original design, not a colour-
            // only placeholder. Patterns stay behind the real cartridge art.
            if (!appearance.highContrast && appearance.patternId != CartdeckSurfaceDesign.PATTERN_NONE) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCartdeckSurfaceDesign(
                        patternId = appearance.patternId,
                        stampId = CartdeckSurfaceDesign.STAMP_NONE,
                        frame = Rect(0f, 0f, size.width, size.height),
                        exclusions = emptyList(),
                        lineColour = materials.interactionAccent.copy(alpha = 0.42f),
                        stampColour = materials.edgeHighlight,
                        cellSize = 26.dp.toPx(),
                        clearance = 2.dp.toPx(),
                    )
                }
            }
            if (!appearance.highContrast && appearance.decal != null) {
                CartdeckDecalLayer(
                    decal = appearance.decal,
                    exclusions = emptyList(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp * scale),
                horizontalArrangement = Arrangement.spacedBy(12.dp * scale),
            ) {
                repeat(3) {
                    Image(
                        painter = cartridgePainter(cartridgeAsset(edition, "spine"), edition),
                        contentDescription = null,
                        modifier = Modifier.size(35.dp * scale, 127.dp * scale),
                    )
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(13.dp * scale)
                    .background(materials.shelfDeck),
            )
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(5.dp * scale)
                    .background(
                        if (appearance.lighting.active(CartdeckLighting.SHELF_RAIL, appearance.highContrast)) {
                            appearance.lighting.tint(materials.interactionAccent)
                        } else {
                            materials.shelfLip
                        },
                    ),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CartridgeSpine(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    pink: Boolean = LocalShelfAppearance.current.edition == "pink",
    width: Dp = 72.dp,
    registerGeometry: Boolean = true,
) {
    val edition = if (pink) "pink" else LocalShelfAppearance.current.edition
    val cartridgeInk =
        when (edition) {
            "pink" -> Color(0xFFFFF3F7)
            "green" -> Color(0xFFF2FFE6)
            else -> Color(0xFFEDF6F2)
        }
    val geometry = LocalCartridgeGeometry.current
    val owner = remember(game.id) { Any() }
    DisposableEffect(game.id, registerGeometry) {
        onDispose { if (registerGeometry) geometry.remove(game.id, owner) }
    }
    var focused by remember { mutableStateOf(false) }
    val scale = width.value / 94f
    Box(
        modifier =
            modifier
                .size(94.dp * scale, 340.dp * scale)
                .onGloballyPositioned {
                    if (registerGeometry) geometry.update(game.id, owner, Rect(it.positionInRoot(), it.size.toSize()))
                }
                .graphicsLayer { alpha = if (registerGeometry && geometry.selectedId == game.id) 0f else 1f }
                .onFocusChanged { focused = it.isFocused }
                .border(
                    if (focused) 3.dp else 0.dp,
                    if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(12.dp),
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = "${game.displayName()}, ${cartridgeSystemLabel(game.systemId)}, spine facing"
                }
                .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
    ) {
        Image(
            cartridgePainter(
                cartridgeAsset(edition, "spine"),
                edition,
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            cartridgePainter(
                cartridgeAsset(edition, "mark"),
                edition,
            ),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 35.dp * scale).size(28.dp * scale),
        )
        // Layout is measured sideways before rotation, keeping the title inside its 46×171 window.
        Box(
            Modifier.offset(24.dp * scale, 82.dp * scale).size(46.dp * scale, 171.dp * scale).clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                shelfPresentationTitle(game.displayName()),
                modifier = Modifier.requiredSize(171.dp * scale, 46.dp * scale).rotate(-90f).padding(horizontal = 6.dp),
                color = cartridgeInk,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            cartridgeSystemLabel(game.systemId),
            modifier = Modifier.offset(17.dp * scale, 276.dp * scale).width(60.dp * scale),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = cartridgeInk,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun cartridgeArtworkRequest(
    context: Context,
    game: Game,
    revision: Int,
    offline: Boolean = false,
): ImageRequest =
    ImageRequest.Builder(context)
        .data(game.displayCoverUrl())
        .networkCachePolicy(if (offline) coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED)
        .memoryCacheKey("${game.id}:${game.displayCoverUrl()}:$revision")
        .memoryCachePolicy(
            if (game.customCoverUri != null) coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED,
        )
        .diskCachePolicy(
            if (game.customCoverUri != null) coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED,
        )
        .build()

@Composable
fun CartridgeFront(
    game: Game,
    modifier: Modifier = Modifier,
    pink: Boolean = LocalShelfAppearance.current.edition == "pink",
    artworkRevision: Int = 0,
    offline: Boolean = false,
) {
    CartridgeFrontArtwork(
        title = game.displayName(),
        systemId = game.systemId,
        artwork = cartridgeArtworkRequest(LocalContext.current, game, artworkRevision, offline),
        modifier = modifier,
        pink = pink,
    )
}

/** A catalogue preview is presentation-only, never a fabricated Game or an install record. */
@Composable
internal fun CatalogueCartridgeFront(
    title: String,
    systemId: String,
    modifier: Modifier = Modifier,
) {
    CartridgeFrontArtwork(
        title, systemId, null, modifier, LocalShelfAppearance.current.edition == "pink",
        cataloguePreview = true,
    )
}

@Composable
private fun CartridgeFrontArtwork(
    title: String,
    systemId: String,
    artwork: ImageRequest?,
    modifier: Modifier,
    pink: Boolean,
    cataloguePreview: Boolean = false,
) {
    val edition = if (pink) "pink" else LocalShelfAppearance.current.edition
    val cartridgeInk =
        when (edition) {
            "pink" -> Color(0xFFFFF3F7)
            "green" -> Color(0xFFF2FFE6)
            else -> Color(0xFFEDF6F2)
        }
    BoxWithConstraints(modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(320f / 390f)) {
        val unit = maxWidth / 320f
        Image(
            cartridgePainter(
                cartridgeAsset(edition, "front"),
                edition,
            ),
            null,
            Modifier.fillMaxSize(),
        )
        val fallback =
            painterResource(
                cartridgeAsset(edition, "fallback"),
            )
        val coverBounds =
            Modifier.offset(unit * 40, unit * 113).size(unit * 240, unit * 200)
                .clip(RoundedCornerShape(unit * 8))
        if (cataloguePreview) {
            CatalogueCoverArtwork(title, systemId, unit, coverBounds)
        } else {
            // Installed cartridges retain custom > indexed > original fallback artwork.
            AsyncImage(
                model = artwork,
                contentDescription = null,
                fallback = fallback,
                error = fallback,
                placeholder = fallback,
                contentScale = ContentScale.Fit,
                modifier = coverBounds,
            )
        }
        Image(
            cartridgePainter(
                cartridgeAsset(edition, "overlay"),
                edition,
            ),
            null,
            Modifier.fillMaxSize(),
        )
        Image(
            cartridgePainter(
                cartridgeAsset(edition, "mark"),
                edition,
            ),
            null,
            Modifier.offset(unit * 40, unit * 38).size(unit * 40),
        )
        Text(
            "CARTDECK",
            modifier = Modifier.offset(unit * 90, unit * 48),
            color =
                when (edition) {
                    "pink" -> Color(0xFF54253F)
                    "green" -> Color(0xFF153629)
                    else -> Color(0xFFEDF6F2)
                },
            style = MaterialTheme.typography.labelLarge,
        )
        // Manufactured nameplate text follows the asset's geometry. The
        // complete accessible title remains in the enclosing cartridge and detail view.
        val textScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val plateStyle =
            MaterialTheme.typography.labelSmall.copy(
                fontSize = (unit.value * 15f / textScale).sp,
                lineHeight = (unit.value * 18f / textScale).sp,
            )
        Box(
            Modifier.offset(unit * 40, unit * 315).size(unit * 240, unit * 18).clipToBounds(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                shelfPresentationTitle(title),
                color = cartridgeInk,
                style = plateStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier.offset(unit * 215, unit * 335).size(unit * 70, unit * 23)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(unit * 4)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                cartridgeSystemLabel(systemId),
                color = MaterialTheme.colorScheme.onSurface,
                style = plateStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Original title-specific CARTDECK artwork: catalogue preview only, never an installed cover. */
internal fun catalogueCoverMotif(title: String, systemId: String): Int =
    ("${title.trim().lowercase(Locale.ROOT)}|${systemId.trim().lowercase(Locale.ROOT)}").hashCode() and
        Int.MAX_VALUE

@Composable
private fun CatalogueCoverArtwork(
    title: String,
    systemId: String,
    unit: Dp,
    modifier: Modifier,
) {
    val seed = catalogueCoverMotif(title, systemId)
    val palette =
        listOf(
            Color(0xFF19F5DA), Color(0xFFFF4FA3), Color(0xFFB69BFF),
            Color(0xFFFFC56A), Color(0xFF76BDFF), Color(0xFFB2E876),
        )
    val accent = palette[seed % palette.size]
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Box(modifier.background(Color(0xFF101424))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val count = 4 + seed % 3
            repeat(count) { index ->
                val offset = (seed ushr (index * 4)) and 7
                val y = h * (0.14f + index * 0.12f)
                drawLine(
                    color = accent.copy(alpha = 0.27f),
                    start = Offset(w * (0.06f + offset * 0.018f), y),
                    end = Offset(w * 0.95f, y - h * (0.04f + offset * 0.012f)),
                    strokeWidth = (w / 110f).coerceAtLeast(1f),
                )
            }
            drawCircle(
                color = accent.copy(alpha = 0.18f),
                radius = w * (0.20f + (seed % 4) * 0.025f),
                center = Offset(w * (0.47f + (seed % 3) * 0.10f), h * 0.54f),
            )
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = unit * 12, vertical = unit * 10),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "CATALOGUE · ${cartridgeSystemLabel(systemId)}",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = (unit.value * 11f / scale).sp,
                        lineHeight = (unit.value * 14f / scale).sp,
                    ),
                color = accent,
                maxLines = 1,
            )
            Text(
                shelfPresentationTitle(title),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (unit.value * 23f / scale).sp,
                        lineHeight = (unit.value * 27f / scale).sp,
                    ),
                color = Color(0xFFF4F5FA),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
