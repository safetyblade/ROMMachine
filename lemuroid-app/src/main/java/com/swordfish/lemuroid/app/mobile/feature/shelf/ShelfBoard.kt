package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.app.mobile.feature.game.rememberGripArcProfile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.floor
import kotlin.math.roundToInt

private data class ShelfItem(val key: String, val section: ShelfSection, val row: ShelfRow? = null)

private data class DropPosition(val section: String, val before: String?)

/** Lazy vertical rows: there is deliberately no horizontally scrolling container. */
@Composable
fun ShelfBoard(
    layout: ShelfLayout,
    games: List<Game>,
    onSelect: (Game) -> Unit,
    onLongClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    arrange: Boolean = false,
    onEdit: ((ShelfLayout) -> ShelfLayout) -> Unit = {},
    onSectionEdit: (ShelfSection) -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    artworkRevision: Int = 0,
) {
    val colors = LocalShelfMaterials.current
    val decorationPattern =
        if (layout.appearance.highContrast) CartdeckSurfaceDesign.PATTERN_NONE else layout.appearance.patternId
    val decorationStamp =
        if (layout.appearance.highContrast) CartdeckSurfaceDesign.STAMP_NONE else layout.appearance.stampId
    val personalDecal = if (layout.appearance.highContrast) null else layout.appearance.decal
    val byKey = remember(games) { games.associateBy { shelfGameKey(it.systemId, it.fileUri) } }
    val positions = remember { mutableStateMapOf<String, Pair<Rect, DropPosition>>() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    var hover by remember { mutableStateOf<DropPosition?>(null) }
    var menu by remember { mutableStateOf<Pair<ShelfSection, ShelfCell>?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val systemReduced =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val currentLayout by rememberUpdatedState(layout)
    val turn = remember { Animatable(0f) }
    var turning by remember { mutableStateOf<String?>(null) }
    val focusRequests = remember { mutableStateMapOf<String, FocusRequester>() }

    fun flip(cell: ShelfCell) {
        if (turning != null || dragging != null) return
        val facing = if (cell.facing == CartridgeFacing.SPINE) CartridgeFacing.FRONT else CartridgeFacing.SPINE
        scope.launch {
            turning = cell.id
            try {
                if (!(currentLayout.appearance.reducedMotion || systemReduced)) turn.animateTo(90f, tween(110))
                onEdit { it.setFacing(cell.id, facing) }
                // The atomic writer publishes asynchronously. Exchange content edge-on,
                // then reveal only after the new footprint has been measured.
                withTimeoutOrNull(3000) {
                    snapshotFlow {
                        currentLayout.sections.flatMap { it.cells }.firstOrNull { it.id == cell.id }?.facing
                    }.first { it == facing }
                }
                withFrameNanos { }
                if (!(currentLayout.appearance.reducedMotion || systemReduced)) {
                    turn.snapTo(-90f)
                    turn.animateTo(0f, tween(110))
                }
                withTimeoutOrNull(3000) {
                    snapshotFlow { focusRequests[cell.id] }.first { it != null }
                }?.requestFocus()
            } finally {
                turn.snapTo(0f)
                turning = null
            }
        }
    }

    fun hitTest(): DropPosition? {
        val hit = positions.values.lastOrNull { it.first.contains(pointer) } ?: return null
        val target = hit.second
        val cells = currentLayout.sections.firstOrNull { it.id == target.section }?.cells.orEmpty()
        val index = cells.indexOfFirst { it.id == target.before }
        // Both halves of the complete footprint are insertion targets. Explicit
        // vacancies keep the existing swap semantics instead of being consumed.
        return if (index >= 0 && cells[index].gameKey != null && pointer.x > hit.first.center.x) {
            target.copy(before = cells.getOrNull(index + 1)?.id)
        } else {
            target
        }
    }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scroll = rememberLazyListState()
    var anchor by rememberSaveable { mutableStateOf<String?>(null) }
    var anchorOffset by rememberSaveable { mutableStateOf(0) }
    BoxWithConstraints(modifier.background(colors.roomBackdrop).onGloballyPositioned { viewport = it.boundsInRoot() }) {
        // The same saved design carries into landscape, tablet and controller-
        // first library navigation. This layer is decorative and non-touchable.
        if (
            decorationPattern != CartdeckSurfaceDesign.PATTERN_NONE ||
            decorationStamp != CartdeckSurfaceDesign.STAMP_NONE
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCartdeckSurfaceDesign(
                    patternId = decorationPattern,
                    stampId = decorationStamp,
                    frame = Rect(0f, 0f, size.width, size.height),
                    exclusions = emptyList(),
                    lineColour = colors.interactionAccent,
                    stampColour = colors.edgeHighlight,
                    cellSize = 32.dp.toPx(),
                    clearance = 12.dp.toPx(),
                )
            }
        }
        if (personalDecal != null) {
            // Controller-first/TV-sized library: theme identity is visible in
            // navigation, never on top of the running game or native input.
            CartdeckDecalLayer(
                decal = personalDecal,
                exclusions = emptyList(),
            )
        }
        val columns = maxOf(1, floor((maxWidth.value - 24f) / 72f).toInt())
        val width = ((maxWidth.value - 24f - 4f * (columns - 1)) / columns).dp.coerceAtLeast(48.dp)
        val items =
            remember(layout, columns) {
                layout.sections.flatMap { section ->
                    val rows = section.rows(columns).map { ShelfItem(it.key, section, it) }
                    listOf(ShelfItem("header:${section.id}", section)) + rows
                }
            }
        val headerOffset = if (header == null) 0 else 1
        LaunchedEffect(columns, layout.revision) {
            (turning ?: anchor)?.let { id ->
                val index = items.indexOfFirst { it.row?.cells?.any { cell -> cell.id == id } == true }
                if (index >= 0) scroll.scrollToItem(index + headerOffset, anchorOffset)
            }
        }
        LaunchedEffect(scroll, items) {
            snapshotFlow { scroll.firstVisibleItemIndex to scroll.firstVisibleItemScrollOffset }.collect {
                    (index, offset) ->
                items.getOrNull(index - headerOffset)?.row?.cells?.firstOrNull()?.let {
                    anchor = it.id
                    anchorOffset = offset
                }
            }
        }
        LaunchedEffect(dragging) {
            if (dragging != null) {
                while (isActive) {
                    withFrameNanos { }
                    val edge = with(density) { 72.dp.toPx() }
                    val delta =
                        when {
                            pointer.y < viewport.top + edge -> (pointer.y - viewport.top - edge) * 0.18f
                            pointer.y > viewport.bottom - edge -> (pointer.y - viewport.bottom + edge) * 0.18f
                            else -> 0f
                        }.coerceIn(-32f, 32f)
                    if (delta != 0f) scroll.scrollBy(delta)
                    hover =
                        hitTest()
                }
            }
        }
        LazyColumn(state = scroll, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            if (header != null) item(key = "library-header") { header() }
            items(items.size, key = { items[it].key }) { index ->
                val item = items[index]
                if (item.row == null) {
                    DropSurface("header:${item.section.id}", DropPosition(item.section.id, null), positions) {
                        Row(
                            Modifier.fillMaxWidth().background(colors.roomBackdrop)
                                .drawBehind {
                                    if (decorationPattern != CartdeckSurfaceDesign.PATTERN_NONE) {
                                        drawCartdeckSurfaceDesign(
                                            patternId = decorationPattern,
                                            stampId = CartdeckSurfaceDesign.STAMP_NONE,
                                            frame = Rect(0f, 0f, size.width, size.height),
                                            exclusions = emptyList(),
                                            lineColour = colors.interactionAccent.copy(alpha = 0.45f),
                                            stampColour = colors.edgeHighlight,
                                            cellSize = 28.dp.toPx(),
                                            clearance = 0f,
                                        )
                                    }
                                }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                item.section.title,
                                Modifier.weight(1f),
                                color = readableColor(colors.sectionText, listOf(colors.roomBackdrop), 4.5f),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (arrange) TextButton(onClick = { onSectionEdit(item.section) }) { Text("Edit section") }
                        }
                        HorizontalDivider(color = colors.sectionDivider)
                    }
                } else {
                    val row = item.row
                    Column(
                        Modifier.fillMaxWidth().background(
                            Brush.verticalGradient(
                                listOf(colors.shelfShadow, colors.shelfBackboard, colors.shelfBackboard),
                            ),
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            val cells =
                                if (row.intentionalEmpty) {
                                    List(columns) {
                                        row.cells.first()
                                    }
                                } else {
                                    row.cells + List(columns - row.cells.sumOf { it.span(columns) }) { null }
                                }
                            cells.forEachIndexed { column, cell ->
                                val next =
                                    row.cells.lastOrNull()?.let {
                                            last ->
                                        item.section.cells.dropWhile { it.id != last.id }.drop(1).firstOrNull()?.id
                                    }
                                val cellKey =
                                    if (row.intentionalEmpty) {
                                        "${row.key}:$column"
                                    } else {
                                        cell?.id ?: "${row.key}:padding:$column"
                                    }
                                val target =
                                    DropPosition(
                                        item.section.id,
                                        if (cell == null) next else cell.id,
                                    )
                                val game = cell?.gameKey?.let(byKey::get)
                                val span = if (row.intentionalEmpty) 1 else cell?.span(columns) ?: 1
                                val footprint = width * span + 4.dp * (span - 1)
                                val canDrag = arrange && !row.intentionalEmpty && turning == null
                                val editingActions =
                                    Modifier.semantics {
                                        if (arrange && cell != null) {
                                            customActions =
                                                listOfNotNull(
                                                    if (cell.gameKey != null) {
                                                        CustomAccessibilityAction(
                                                            cell.facing.actionLabel(),
                                                        ) {
                                                            flip(cell)
                                                            true
                                                        }
                                                    } else {
                                                        null
                                                    },
                                                    CustomAccessibilityAction("Move earlier") {
                                                        onEdit { it.moveBy(cell.id, -1) }
                                                        true
                                                    },
                                                    CustomAccessibilityAction("Move later") {
                                                        onEdit { it.moveBy(cell.id, 1) }
                                                        true
                                                    },
                                                    CustomAccessibilityAction("Move to section") {
                                                        menu = item.section to cell
                                                        true
                                                    },
                                                    CustomAccessibilityAction("Move up") {
                                                        onEdit { it.moveBy(cell.id, -columns) }
                                                        true
                                                    },
                                                    CustomAccessibilityAction("Move down") {
                                                        onEdit { it.moveBy(cell.id, columns) }
                                                        true
                                                    },
                                                    CustomAccessibilityAction("Insert space before") {
                                                        onEdit { it.insertSpace(item.section.id, cell.id) }
                                                        true
                                                    },
                                                )
                                        }
                                    }
                                key(cellKey) {
                                    val focusRequester = remember(cell?.facing) { FocusRequester() }
                                    DisposableEffect(cellKey, focusRequester) {
                                        onDispose {
                                            if (focusRequests[cellKey] === focusRequester) focusRequests.remove(cellKey)
                                        }
                                    }
                                    DropSurface(cellKey, target, positions) {
                                        Box(
                                            Modifier.width(footprint).height(width * (340f / 94f))
                                                .border(
                                                    if (hover == target && dragging != null) 3.dp else 0.dp,
                                                    if (hover == target) {
                                                        colors.focusOutline
                                                    } else {
                                                        androidx.compose.ui.graphics.Color.Transparent
                                                    },
                                                )
                                                .then(
                                                    if (canDrag && cell != null) {
                                                        Modifier.pointerInput(cell.id) {
                                                            detectDragGestures(
                                                                onDragStart = { start ->
                                                                    dragging = cell.id
                                                                    val origin = positions[cellKey]?.first?.topLeft
                                                                    pointer = (origin ?: Offset.Zero) + start

                                                                    haptics.performHapticFeedback(
                                                                        HapticFeedbackType.LongPress,
                                                                    )
                                                                },
                                                                onDrag = {
                                                                        change,
                                                                        amount,
                                                                    ->
                                                                    change.consume()
                                                                    pointer += amount
                                                                    hover =
                                                                        hitTest()
                                                                },
                                                                onDragCancel = {
                                                                    dragging = null
                                                                    hover = null
                                                                },
                                                                onDragEnd = {
                                                                    val id = dragging
                                                                    val destination = hover
                                                                    if (id != null && destination != null) {
                                                                        // Padded columns are visual only.
                                                                        // Only explicit Insert space reserves gaps.
                                                                        onEdit {
                                                                            it.moveCell(
                                                                                id,
                                                                                destination.section,
                                                                                destination.before,
                                                                            )
                                                                        }
                                                                    }
                                                                    haptics.performHapticFeedback(
                                                                        HapticFeedbackType.LongPress,
                                                                    )
                                                                    dragging = null
                                                                    hover = null
                                                                },
                                                            )
                                                        }
                                                    } else {
                                                        Modifier
                                                    },
                                                )
                                                .then(if (game == null) editingActions else Modifier),
                                        ) {
                                            if (game != null) {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                                                    ShelfCartridge(
                                                        game,
                                                        cell.facing,
                                                        width,
                                                        footprint,
                                                        {
                                                            if (arrange) menu = item.section to cell else onSelect(game)
                                                        },
                                                        Modifier
                                                            .focusRequester(focusRequester)
                                                            .onGloballyPositioned {
                                                                focusRequests[cell.id] = focusRequester
                                                            }
                                                            .then(editingActions)
                                                            .alpha(if (dragging == cell.id) 0.25f else 1f)
                                                            .graphicsLayer {
                                                                rotationY = if (turning == cell.id) turn.value else 0f
                                                                cameraDistance = 12f * density.density
                                                            },
                                                        artworkRevision = artworkRevision,
                                                        onLongClick = {
                                                            if (arrange) {
                                                                menu = item.section to cell
                                                            } else {
                                                                onLongClick(
                                                                    game,
                                                                )
                                                            }
                                                        },
                                                    )
                                                    if (rememberGripArcProfile(game).value.mode.hasGrip) {
                                                        Text(
                                                            "ARC",
                                                            modifier =
                                                                Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(top = 4.dp, end = 4.dp)
                                                                    .background(colors.shelfBackboard)
                                                                    .padding(horizontal = 3.dp, vertical = 1.dp),
                                                            color = colors.sectionText,
                                                            fontSize = 8.sp,
                                                        )
                                                    }
                                                }
                                            } else {
                                                val reserved = cell != null
                                                Box(
                                                    Modifier.fillMaxSize().padding(
                                                        horizontal = 4.dp,
                                                    ).background(
                                                        if (reserved) colors.vacantCell else colors.shelfBackboard,
                                                    ).border(
                                                        if (reserved) 1.dp else 0.dp,
                                                        colors.sectionDivider,
                                                    ).semantics {
                                                        contentDescription =
                                                            if (cell?.gameKey != null) {
                                                                "Missing cartridge: ${cell.titleHint}"
                                                            } else if (reserved) {
                                                                "Reserved empty shelf space"
                                                            } else {
                                                                "Available shelf space"
                                                            }
                                                    },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (cell?.gameKey != null && !arrange) {
                                                        // Unavailable cartridges reserve their original shelf cell.
                                                        // No launcher or misleading edit action is exposed until
                                                        // an independently verified source is restored.
                                                        Text(
                                                            "Unavailable",
                                                            color = colors.sectionText,
                                                            fontSize = 10.sp,
                                                        )
                                                    } else if (arrange) {
                                                        TextButton(onClick = {
                                                            if (cell != null) {
                                                                menu = item.section to cell
                                                            } else {
                                                                onEdit {
                                                                    it.insertSpace(
                                                                        item.section.id,
                                                                        next,
                                                                    )
                                                                }
                                                            }
                                                        }) {
                                                            Text(
                                                                if (cell?.gameKey != null) {
                                                                    cell.titleHint
                                                                } else if (reserved) {
                                                                    "Empty"
                                                                } else {
                                                                    "+ Space"
                                                                },
                                                                color =
                                                                    readableColor(
                                                                        colors.sectionText,
                                                                        listOf(
                                                                            if (reserved) {
                                                                                colors.vacantCell
                                                                            } else {
                                                                                colors.shelfBackboard
                                                                            },
                                                                        ),
                                                                        4.5f,
                                                                    ),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        ShelfRail(lighting = layout.appearance.lighting, highContrast = layout.appearance.highContrast)
                    }
                }
            }
        }
        val dragged = layout.sections.flatMap { it.cells }.firstOrNull { it.id == dragging }
        dragged?.gameKey?.let(byKey::get)?.let { game ->
            val footprint = width * dragged.span(columns) + 4.dp * (dragged.span(columns) - 1)
            ShelfCartridge(
                game,
                dragged.facing,
                width,
                footprint,
                {},
                Modifier.offset {
                    IntOffset(
                        (
                            pointer.x - viewport.left -
                                with(density) {
                                    footprint.toPx() / 2
                                }
                        ).roundToInt(),
                        (pointer.y - viewport.top - with(density) { width.toPx() }).roundToInt(),
                    )
                }.graphicsLayer {
                    alpha = 0.9f
                    shadowElevation = 16f
                    scaleX = 1.04f
                    scaleY = 1.04f
                },
                artworkRevision = artworkRevision,
                registerGeometry = false,
            )
        }
    }
    menu?.let { (section, cell) ->
        AlertDialog(onDismissRequest = {
            menu = null
        }, title = { Text(if (cell.gameKey == null) "Shelf space" else cell.titleHint) }, text = {
            Column {
                if (cell.gameKey != null) {
                    TextButton(onClick = {
                        menu = null
                        flip(cell)
                    }, enabled = turning == null) { Text(cell.facing.actionLabel()) }
                }
                layout.sections.forEach {
                        target ->
                    TextButton(onClick = {
                        onEdit { it.moveCell(cell.id, target.id) }
                        menu = null
                    }) { Text("Move to ${target.title}") }
                }
                TextButton(onClick = {
                    onEdit { it.moveBy(cell.id, -1) }
                    menu = null
                }) { Text("Move earlier") }
                TextButton(onClick = {
                    onEdit { it.moveBy(cell.id, 1) }
                    menu = null
                }) { Text("Move later") }
                TextButton(onClick = {
                    onEdit { it.insertSpace(section.id, cell.id) }
                    menu = null
                }) { Text("Insert space before") }
                if (cell.gameKey == null) {
                    TextButton(onClick = {
                        onEdit { it.removeSpace(cell.id) }
                        menu = null
                    }) { Text("Remove empty space") }
                }
            }
        }, confirmButton = { TextButton(onClick = { menu = null }) { Text("Done") } })
    }
}

@Composable
private fun DropSurface(
    key: String,
    target: DropPosition,
    positions: MutableMap<String, Pair<Rect, DropPosition>>,
    content: @Composable () -> Unit,
) {
    DisposableEffect(key) { onDispose { positions.remove(key) } }
    Column(Modifier.onGloballyPositioned { positions[key] = it.boundsInRoot() to target }) { content() }
}

@Composable
fun ShelfRail(
    lighting: CartdeckLighting = CartdeckLighting(),
    highContrast: Boolean = false,
) {
    val colors = LocalShelfMaterials.current
    val railColour =
        if (lighting.active(CartdeckLighting.SHELF_RAIL, highContrast)) {
            lighting.tint(colors.interactionAccent)
        } else {
            colors.edgeHighlight
        }
    Column(Modifier.fillMaxWidth().height(22.dp).graphicsLayer { shadowElevation = 6.dp.toPx() }) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(colors.shelfGroove))
        Box(Modifier.fillMaxWidth().height(2.dp).background(railColour))
        Box(
            Modifier.fillMaxWidth().height(
                9.dp,
            ).background(Brush.verticalGradient(listOf(colors.shelfDeck, colors.shelfLip))),
        )
        Box(Modifier.fillMaxWidth().height(6.dp).background(colors.shelfShadow))
    }
}

fun ShelfLayout.moveBy(
    cellId: String,
    delta: Int,
): ShelfLayout {
    val section = sections.first { it.cells.any { cell -> cell.id == cellId } }
    val index = section.cells.indexOfFirst { it.id == cellId }
    val target = (index + delta).coerceIn(0, section.cells.lastIndex)
    val before = if (target > index) section.cells.getOrNull(target + 1)?.id else section.cells[target].id
    return moveCell(cellId, section.id, before)
}

internal fun CartridgeFacing.actionLabel(): String = if (this == CartridgeFacing.SPINE) "Show front" else "Show spine"
