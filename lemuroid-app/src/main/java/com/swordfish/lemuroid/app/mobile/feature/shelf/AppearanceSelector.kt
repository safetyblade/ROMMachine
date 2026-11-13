package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckAppearancePreview
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckEdition
import java.util.UUID

/** Runtime swatches are resolved from the same generated resources used by the cartridges. */
@Composable
private fun presetRoles(id: String): Map<String, Color> {
    val ctx = LocalContext.current
    val t =
        ctx.resolveCartdeckEdition(
            when (id) {
                "pink" -> CartdeckEditionPreset.PINK
                "green" -> CartdeckEditionPreset.GREEN
                else -> CartdeckEditionPreset.ORIGINAL
            },
        )
    val s = canonicalShelfMaterials(id)
    return mapOf(
        "chassis" to t.chassis, "secondary" to t.secondary, "bezel" to t.bezel,
        "glass" to t.glass, "button" to t.button, "accent" to t.accent,
        "onChassis" to t.onChassis, "onButton" to t.onButton, "focus" to t.focus,
        "roomBackdrop" to s.roomBackdrop, "shelfBackboard" to s.shelfBackboard,
        "shelfDeck" to s.shelfDeck, "shelfLip" to s.shelfLip, "shelfGroove" to s.shelfGroove,
        "edgeHighlight" to s.edgeHighlight, "shelfShadow" to s.shelfShadow,
        "sectionDivider" to s.sectionDivider, "vacantCell" to s.vacantCell,
        "sectionText" to s.sectionText, "focusOutline" to s.focusOutline,
        "interactionAccent" to s.interactionAccent,
    )
}

private val roles =
    linkedMapOf(
        "Console hardware" to
            listOf(
                "chassis" to "Chassis", "secondary" to "Side rails", "bezel" to "Bezel",
                "glass" to "Screen surround", "button" to "Buttons", "accent" to "Accent",
            ),
        "Cartridge shelf" to
            listOf(
                "roomBackdrop" to "Room", "shelfBackboard" to "Back panel",
                "shelfDeck" to "Rail deck", "shelfLip" to "Rail lip",
                "shelfGroove" to "Groove", "edgeHighlight" to "Machined edge",
                "shelfShadow" to "Shadow", "sectionDivider" to "Divider",
                "vacantCell" to "Empty slot",
            ),
        "Lighting" to
            listOf(
                "focus" to "Cartridge focus", "focusOutline" to "Shelf focus",
                "interactionAccent" to "Active lighting",
            ),
        "Text & contrast" to
            listOf(
                "onChassis" to "Shell lettering", "onButton" to "Button lettering",
                "sectionText" to "Shelf lettering",
            ),
    )

private fun colourHex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

private fun longColour(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL

/** Curated choices in the controlled design/cartdeck-v07/material-swatches.json. */
private fun suggestions(role: String): List<Color> {
    val group =
        when (role) {
            "chassis", "secondary" -> "chassis"
            "bezel", "button", "glass" -> "button"
            "shelfBackboard", "roomBackdrop", "shelfGroove", "shelfShadow", "vacantCell" -> "back"
            "shelfDeck", "shelfLip", "sectionDivider" -> "deck"
            else -> "accent"
        }
    val hex =
        when (group) {
            "chassis" -> listOf("#303B49", "#F2A9C9", "#9BC7A8", "#28435C", "#D9D1C8", "#9F91C6")
            "button" -> listOf("#111923", "#793653", "#194437", "#26374A", "#51443E", "#413450")
            "back" -> listOf("#14212D", "#382536", "#163127", "#142B3F", "#332D42", "#3F332A")
            "deck" -> listOf("#293847", "#69445D", "#315443", "#334A61", "#665747", "#5C4F71")
            else -> listOf("#4ED8C4", "#FFE5F1", "#D4F47D", "#F2C879", "#A3C5FF", "#F5B0A1")
        }
    return hex.map { Color(android.graphics.Color.parseColor(it)) }
}

@Composable
private fun EditionOption(
    id: String,
    checked: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val title = id.replaceFirstChar { it.uppercase() }
    val samples = presetRoles(id)
    Column(
        modifier =
            modifier.border(
                if (checked) 2.dp else 1.dp,
                if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp),
            ).clickable(onClick = onClick)
                .semantics {
                    selected = checked
                    contentDescription = title + " edition"
                }
                .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        CartdeckAppearancePreview(ShelfAppearance(edition = id), height = 68.dp)
        Text(if (checked) "✓ " + title else title, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("chassis", "shelfBackboard", "shelfDeck", "accent").forEach { key ->
                Box(
                    Modifier.size(13.dp).background(samples.getValue(key), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
            }
        }
    }
}

/** First-party designs are immutable selections; only Save as new creates a user-owned copy. */
@Composable
private fun CuratedDesignOption(
    design: CartdeckCuratedDesign,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val preview = ShelfAppearance(edition = design.baseEdition, custom = design.colours)
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .border(
                    if (checked) 2.dp else 1.dp,
                    if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(12.dp),
                ).clickable(onClick = onClick)
                .semantics {
                    selected = checked
                    contentDescription = design.title + ", custom design"
                }.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            if (checked) "✓ 001 · " + design.title else "001 · " + design.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(design.description, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Handheld", style = MaterialTheme.typography.labelSmall)
                CartdeckConsoleHardwarePreview(preview, height = 96.dp)
            }
            Column(Modifier.weight(1f)) {
                Text("Shelf", style = MaterialTheme.typography.labelSmall)
                CartdeckAppearancePreview(preview, height = 96.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("chassis", "secondary", "accent", "focus", "onChassis").forEach { role ->
                Box(
                    Modifier.size(23.dp)
                        .background(Color(design.colours.getValue(role).toInt()), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
            }
        }
        Text(
            "Colour finish available now. Patterns, stamps and LED effects follow in later phases.",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ColourPicker(
    role: String,
    current: Color,
    defaults: List<Color>,
    onChoose: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var advanced by remember(role) { mutableStateOf(false) }
    var typed by remember(role) { mutableStateOf(colourHex(current)) }
    val valid = typed.matches(Regex("#[0-9a-fA-F]{6}"))
    val chips = (listOf(current) + defaults + suggestions(role)).distinct()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose " + role.replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase()) },
        text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState())) {
                chips.chunked(3).forEach { chunk ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        chunk.forEach { color ->
                            Column(
                                modifier =
                                    Modifier.padding(5.dp).clickable { onChoose(color) }
                                        .semantics { contentDescription = "Colour " + colourHex(color) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(48.dp).background(color, CircleShape)
                                        .border(
                                            if (color == current) 3.dp else 1.dp,
                                            if (color == current) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            CircleShape,
                                        ),
                                )
                                Text(colourHex(color), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                TextButton(onClick = { advanced = !advanced }) {
                    Text(if (advanced) "Hide precise colour" else "Precise colour (hex)")
                }
                if (advanced) {
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it.take(7) },
                        label = { Text("Hex") },
                        isError = !valid,
                        singleLine = true,
                    )
                    TextButton(enabled = valid, onClick = {
                        onChoose(Color(android.graphics.Color.parseColor(typed)))
                    }) { Text("Use colour") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** Live console preview using the SAME saved chassis, button, bezel and accent roles as gameplay. */
@Composable
private fun CartdeckConsoleHardwarePreview(
    appearance: ShelfAppearance,
    height: Dp,
) {
    val preset = presetRoles(appearance.edition)

    fun role(id: String): Color = appearance.custom[id]?.let { Color(it.toInt()) } ?: preset.getValue(id)
    val chassis = role("chassis")
    val secondary = role("secondary")
    val bezel = role("bezel")
    val button = role("button")
    val accent = role("accent")
    val ink = role("onChassis")
    Box(
        modifier =
            Modifier.fillMaxWidth().height(height)
                .background(chassis, RoundedCornerShape(10.dp))
                .border(2.dp, bezel, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(9.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                secondary,
                topLeft = Offset(w * 0.12f, h * 0.07f),
                size = Size(w * 0.76f, h * 0.40f),
                cornerRadius = CornerRadius(7.dp.toPx()),
            )
            drawRoundRect(
                bezel,
                topLeft = Offset(w * 0.17f, h * 0.11f),
                size = Size(w * 0.66f, h * 0.31f),
                cornerRadius = CornerRadius(5.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
            val d = h * 0.13f
            val dx = w * 0.31f
            val dy = h * 0.73f
            drawRoundRect(
                accent,
                topLeft = Offset(dx - d * 1.25f, dy - d * 0.35f),
                size = Size(d * 2.5f, d * 0.7f),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            drawRoundRect(
                accent,
                topLeft = Offset(dx - d * 0.35f, dy - d * 1.25f),
                size = Size(d * 0.7f, d * 2.5f),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            for ((cx, cy) in listOf(
                0.67f to 0.62f,
                0.78f to 0.75f,
                0.56f to 0.75f,
                0.67f to 0.88f,
            )) {
                drawCircle(bezel, radius = d * 0.68f, center = Offset(w * cx, h * cy))
                drawCircle(button, radius = d * 0.52f, center = Offset(w * cx, h * cy))
                drawCircle(
                    ink.copy(alpha = 0.20f),
                    radius = d * 0.48f,
                    center = Offset(w * cx, h * cy),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
    }
}

/** All changes are a local draft; only Apply calls the atomic ShelfStore writer. */
@Composable
fun ShelfAppearanceEditor(
    appearance: ShelfAppearance,
    onDismiss: () -> Unit,
    onSave: (ShelfAppearance) -> Unit,
) {
    var edition by remember { mutableStateOf(appearance.edition) }
    var highContrast by remember { mutableStateOf(appearance.highContrast) }
    var reducedMotion by remember { mutableStateOf(appearance.reducedMotion) }
    val overrides = remember { mutableStateMapOf<String, Long>().apply { putAll(appearance.custom) } }
    val named = remember { mutableStateListOf<NamedAppearance>().apply { addAll(appearance.profiles) } }
    var selectedId by remember { mutableStateOf(appearance.activeProfileId) }
    var selectedDesignId by remember { mutableStateOf(appearance.curatedDesignId) }
    var modified by remember { mutableStateOf(false) }
    var expandedEditor by remember { mutableStateOf(false) }
    var expandedAccessibility by remember { mutableStateOf(false) }
    val expandedGroups = remember { mutableStateListOf<String>() }
    var picker by remember { mutableStateOf<String?>(null) }
    var nameAction by remember { mutableStateOf<String?>(null) }
    var typedName by remember { mutableStateOf("") }
    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf<String?>(null) }

    val base = presetRoles(edition)
    val original = presetRoles("original")
    val pink = presetRoles("pink")
    val green = presetRoles("green")
    val draft =
        ShelfAppearance(
            edition,
            overrides.toMap(),
            highContrast,
            reducedMotion,
            named.toList(),
            if (modified) null else selectedId,
            if (modified) null else selectedDesignId,
        )

    fun choosePreset(id: String) {
        edition = id
        overrides.clear()
        selectedId = null
        selectedDesignId = null
        modified = false
        warning = null
    }

    fun chooseProfile(item: NamedAppearance) {
        edition = item.baseEdition
        overrides.clear()
        overrides.putAll(item.custom)
        selectedId = item.id
        selectedDesignId = null
        modified = false
        warning = null
    }

    fun chooseCuratedDesign(design: CartdeckCuratedDesign) {
        edition = design.baseEdition
        overrides.clear()
        overrides.putAll(design.colours)
        selectedId = null
        selectedDesignId = design.id
        modified = false
        warning = null
    }

    fun changeColour(
        role: String,
        value: Color,
    ) {
        val proposed = overrides.toMap() + (role to longColour(value))
        val surface = shelfMaterials(ShelfAppearance(edition, proposed, highContrast, reducedMotion))
        val unsafe =
            when (role) {
                "sectionText" -> colorContrast(value, surface.shelfBackboard) < 4.5f
                "focusOutline" ->
                    colorContrast(value, surface.shelfBackboard) < 3f ||
                        colorContrast(value, surface.vacantCell) < 3f
                "onChassis" ->
                    colorContrast(
                        value,
                        Color(proposed["chassis"]?.toInt() ?: base.getValue("chassis").toArgb()),
                    ) < 4.5f
                "onButton" ->
                    colorContrast(
                        value,
                        Color(proposed["button"]?.toInt() ?: base.getValue("button").toArgb()),
                    ) < 4.5f
                else -> false
            }
        if (unsafe) {
            warning = "This colour has insufficient contrast on that surface."
            return
        }
        overrides[role] = longColour(value)
        modified = true
        warning = null
        picker = null
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "CARTDECK Appearance",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Customise your console hardware and cartridge shelf together.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("original", "pink", "green").forEach { id ->
                            EditionOption(
                                id,
                                checked = edition == id && selectedId == null,
                                modifier = Modifier.weight(1f),
                                onClick = { choosePreset(id) },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Console hardware", style = MaterialTheme.typography.labelMedium)
                            CartdeckConsoleHardwarePreview(draft, height = 118.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Cartridge shelf", style = MaterialTheme.typography.labelMedium)
                            CartdeckAppearancePreview(draft, height = 118.dp)
                        }
                    }
                    TextButton(onClick = { expandedEditor = !expandedEditor }) {
                        Text(if (expandedEditor) "Hide colours" else "Customise colours")
                    }
                    if (expandedEditor) {
                        roles.forEach { (group, tokens) ->
                            TextButton(onClick = {
                                if (group in expandedGroups) {
                                    expandedGroups.remove(group)
                                } else {
                                    expandedGroups.add(group)
                                }
                            }) { Text(if (group in expandedGroups) "▾ " + group else "▸ " + group) }
                            if (group in expandedGroups) {
                                tokens.forEach { (key, label) ->
                                    val color = overrides[key]?.let { Color(it.toInt()) } ?: base.getValue(key)
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            Modifier.weight(1f).clickable { picker = key }
                                                .semantics {
                                                    contentDescription = "$label, ${colourHex(color)}, change colour"
                                                }.padding(vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Box(
                                                Modifier.size(44.dp).background(color, RoundedCornerShape(8.dp))
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline,
                                                        RoundedCornerShape(8.dp),
                                                    ),
                                            )
                                            Column {
                                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                                Text(colourHex(color), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        if (overrides.containsKey(key)) {
                                            TextButton(onClick = {
                                                overrides.remove(key)
                                                modified = true
                                            }) { Text("Reset") }
                                        }
                                    }
                                }
                            }
                        }
                        TextButton(onClick = {
                            overrides.clear()
                            modified = true
                        }) {
                            Text("Reset custom colours")
                        }
                    }
                    HorizontalDivider()
                    TextButton(onClick = { expandedNames = !expandedNames }) {
                        Text(if (expandedNames) "▾ My appearances" else "▸ My appearances")
                    }
                    if (expandedNames) {
                        named.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(onClick = { chooseProfile(item) }, modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (selectedId == item.id && !modified) "✓ " + item.name else item.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                TextButton(onClick = {
                                    renameTargetId = item.id
                                    typedName = item.name
                                    nameAction = "rename"
                                }) { Text("Rename") }
                                TextButton(onClick = {
                                    val copy =
                                        item.copy(
                                            id = UUID.randomUUID().toString(),
                                            name = item.name.take(50) + " Copy",
                                            createdAt = System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis(),
                                        )
                                    named.add(copy)
                                    chooseProfile(copy)
                                }) { Text("Copy") }
                                TextButton(onClick = {
                                    named.remove(item)
                                    if (selectedId == item.id) {
                                        selectedId = null
                                        modified = true
                                    }
                                }) { Text("Delete") }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            typedName = ""
                            nameAction = "create"
                        }) { Text("Save as new") }
                        if (selectedId != null && modified) {
                            TextButton(onClick = {
                                val i = named.indexOfFirst { it.id == selectedId }
                                if (i >= 0) {
                                    named[i] =
                                        named[i].copy(
                                            baseEdition = edition, custom = overrides.toMap(),
                                            updatedAt = System.currentTimeMillis(),
                                        )
                                    modified = false
                                }
                            }) { Text("Update selected") }
                        }
                    }
                    HorizontalDivider()
                    TextButton(onClick = { expandedAccessibility = !expandedAccessibility }) {
                        Text(if (expandedAccessibility) "▾ Accessibility" else "▸ Accessibility")
                    }
                    if (expandedAccessibility) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(highContrast, { highContrast = it })
                            Text("High contrast")
                        }
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(reducedMotion, { reducedMotion = it })
                            Text("Reduce motion")
                        }
                    }
                    warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(10.dp))
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        onSave(draft)
                        onDismiss()
                    }) { Text("Apply") }
                }
            }
        }
    }

    picker?.let { role ->
        ColourPicker(
            role,
            overrides[role]?.let { Color(it.toInt()) } ?: base.getValue(role),
            listOf(original.getValue(role), pink.getValue(role), green.getValue(role)),
            onChoose = { changeColour(role, it) },
            onDismiss = { picker = null },
        )
    }
    if (nameAction != null) {
        val collision =
            named.any {
                it.name.equals(typedName.trim(), ignoreCase = true) &&
                    (nameAction != "rename" || it.id != renameTargetId)
            }
        AlertDialog(
            onDismissRequest = { nameAction = null },
            title = { Text(if (nameAction == "rename") "Rename appearance" else "Save custom appearance") },
            text = {
                Column {
                    OutlinedTextField(
                        value = typedName,
                        onValueChange = { typedName = it.take(60) },
                        label = { Text("Name") },
                        singleLine = true,
                    )
                    if (collision) Text("Name already used", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = typedName.trim().isNotEmpty() && !collision, onClick = {
                    if (nameAction == "rename") {
                        val i = named.indexOfFirst { it.id == renameTargetId }
                        if (i >= 0) {
                            named[i] =
                                named[i].copy(
                                    name = typedName.trim(),
                                    updatedAt = System.currentTimeMillis(),
                                )
                        }
                    } else {
                        val item =
                            NamedAppearance(
                                name = typedName.trim(),
                                baseEdition = edition,
                                custom = overrides.toMap(),
                            )
                        named.add(item)
                        selectedId = item.id
                        modified = false
                    }
                    nameAction = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { nameAction = null }) { Text("Cancel") } },
        )
    }
}
