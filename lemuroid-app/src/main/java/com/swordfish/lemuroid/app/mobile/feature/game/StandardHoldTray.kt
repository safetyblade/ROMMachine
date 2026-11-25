package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.product.gemFocusTarget
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.NativeControlInventory

internal val standardHoldTraySystems =
    listOf(
        SystemID.GB,
        SystemID.GBC,
        SystemID.GBA,
        SystemID.GG,
        SystemID.LYNX,
        SystemID.NGP,
        SystemID.NGC,
        SystemID.WS,
        SystemID.WSC,
        SystemID.PSP,
        SystemID.NDS,
        SystemID.NINTENDO_3DS,
    )

private val wonderSwanHoldInputs =
    listOf(
        GripArcInput("A / X3", KeyEvent.KEYCODE_BUTTON_A),
        GripArcInput("B / X4", KeyEvent.KEYCODE_BUTTON_B),
        GripArcInput("X1", KeyEvent.KEYCODE_BUTTON_Y),
        GripArcInput("X2", KeyEvent.KEYCODE_BUTTON_X),
        GripArcInput("Start", KeyEvent.KEYCODE_BUTTON_START),
    )

internal fun standardHoldTrayInputs(system: SystemID): List<GripArcInput> =
    if (system == SystemID.WS || system == SystemID.WSC) wonderSwanHoldInputs else GripArcInputCatalog.forSystem(system)

private class StandardHoldTrayStore(context: Context) {
    val preferences = context.getSharedPreferences("cartdeck_standard_hold_tray_v1", Context.MODE_PRIVATE)

    fun read(system: SystemID): List<Int> {
        val allowed = standardHoldTrayInputs(system).map { it.code }
        return preferences.getString(system.dbname, "").orEmpty().split(',')
            .mapNotNull(String::toIntOrNull).filter { it in allowed }.distinct().take(3)
    }

    fun write(
        system: SystemID,
        keys: List<Int>,
    ) {
        preferences.edit().putString(system.dbname, keys.joinToString(",")).apply()
    }
}

@Composable
internal fun StandardHoldTraySettings() {
    val context = LocalContext.current
    val store = remember(context) { StandardHoldTrayStore(context) }
    var system by remember { mutableStateOf(SystemID.GBA) }
    var choosingSystem by remember { mutableStateOf(false) }
    var keys by remember(system) { mutableStateOf(store.read(system)) }
    Column {
        Text("Standard Hold Tray")
        TextButton({ choosingSystem = true }, Modifier.gemFocusTarget()) {
            Text(cartridgeSystemLabel(system.dbname))
        }
        DropdownMenu(choosingSystem, { choosingSystem = false }) {
            standardHoldTraySystems.forEach {
                DropdownMenuItem(
                    text = { Text(cartridgeSystemLabel(it.dbname)) },
                    onClick = {
                        system = it
                        choosingSystem = false
                    },
                )
            }
        }
        Text("Choose up to three buttons. The tray appears when there is room.")
        if (system == SystemID.WS || system == SystemID.WSC) {
            Text("X1 and X2 are available with vertical WonderSwan controls.")
        }
        standardHoldTrayInputs(system).forEach { input ->
            val selected = input.code in keys
            FilterChip(
                selected = selected,
                enabled = selected || keys.size < 3,
                onClick = {
                    keys = if (selected) keys - input.code else keys + input.code
                    store.write(system, keys)
                },
                label = { Text("Hold ${input.label}") },
                modifier = Modifier.gemFocusTarget(),
            )
        }
    }
}

internal fun findStandardHoldTray(
    safe: PivotArcBounds,
    occupied: List<PivotArcBounds>,
    slots: Int,
): List<PivotArcBounds> {
    if (slots !in 1..3 || !safe.isFiniteArea() || occupied.isEmpty() || occupied.any { !it.isFiniteArea() }) {
        return emptyList()
    }
    val width = 48f * slots + 4f * (slots - 1)
    for (top in listOf(safe.bottom - 48f, safe.top)) {
        for (left in listOf(safe.centerX - width / 2, safe.left, safe.right - width)) {
            val buttons =
                List(slots) {
                    val x = left + it * 52f
                    PivotArcBounds(x, top, x + 48f, top + 48f)
                }
            if (buttons.all { button -> button.inside(safe) && occupied.none { button.intersects(it.inset(4f)) } }) {
                return buttons
            }
        }
    }
    return emptyList()
}

@Composable
internal fun StandardHoldTray(
    system: SystemID,
    native: NativeControlInventory?,
    root: Rect?,
    viewport: Rect?,
    measured: Map<String, Rect>,
    held: Set<Int>,
    onKeys: (Set<Int>) -> Unit,
    onToggle: (Int) -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { StandardHoldTrayStore(context) }
    var keys by remember(system, store) { mutableStateOf(store.read(system)) }
    DisposableEffect(system, store) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == system.dbname) keys = store.read(system)
            }
        store.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { store.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val drawing = WindowInsets.safeDrawing
    val gestures = WindowInsets.safeGestures
    val nativeKeys = native?.controls.orEmpty().mapNotNull { it.keyCode }.toSet()
    val selected = if (system in standardHoldTraySystems) keys.filter { it in nativeKeys } else emptyList()
    val seats = native?.controls.orEmpty().map { it.seat }.toSet()
    val measuredControls = seats.isNotEmpty() && seats.all { it in measured }
    val placement =
        if (root != null && viewport != null && measuredControls && selected.isNotEmpty()) {
            val scale = density.density
            val safe =
                PivotArcBounds(
                    maxOf(drawing.getLeft(density, direction), gestures.getLeft(density, direction)) / scale + 8f,
                    maxOf(drawing.getTop(density), gestures.getTop(density)) / scale + 8f,
                    root.width / scale -
                        maxOf(drawing.getRight(density, direction), gestures.getRight(density, direction)) / scale - 8f,
                    root.height / scale - maxOf(drawing.getBottom(density), gestures.getBottom(density)) / scale - 8f,
                )
            val occupied =
                (measured.values + viewport).map {
                    PivotArcBounds(
                        (it.left - root.left) / scale,
                        (it.top - root.top) / scale,
                        (it.right - root.left) / scale,
                        (it.bottom - root.top) / scale,
                    )
                }
            findStandardHoldTray(safe, occupied, selected.size)
        } else {
            emptyList()
        }
    val activeKeys = if (placement.isEmpty()) emptySet() else selected.toSet()
    DisposableEffect(activeKeys) {
        onKeys(activeKeys)
        onDispose { onKeys(emptySet()) }
    }
    val catalog = standardHoldTrayInputs(system).associateBy { it.code }
    selected.zip(placement).forEach { (key, bounds) ->
        val label =
            if (system == SystemID.WS || system == SystemID.WSC) {
                native?.controls?.firstOrNull { it.keyCode == key }?.label ?: catalog.getValue(key).label
            } else {
                catalog.getValue(key).label
            }
        val shortLabel =
            when (label) {
                "Option 1" -> "O1"
                "Option 2" -> "O2"
                "Circle" -> "○"
                "Cross" -> "×"
                "Triangle" -> "△"
                "Square" -> "□"
                else -> label.take(2)
            }
        FilterChip(
            selected = key in held,
            onClick = { onToggle(key) },
            label = { Text(shortLabel, maxLines = 1) },
            modifier =
                Modifier.offset(bounds.left.dp, bounds.top.dp).size(48.dp).gemFocusTarget()
                    .semantics { contentDescription = "Hold $label" },
        )
    }
}
