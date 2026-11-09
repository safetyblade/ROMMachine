package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.readableColor

/** Retint hardware paths, never the independently loaded cover image. */
@Composable
internal fun cartridgePainter(
    resource: Int,
    edition: String,
): androidx.compose.ui.graphics.painter.Painter {
    val source = ImageVector.vectorResource(resource)
    val appearance = LocalShelfAppearance.current
    val tokens =
        LocalContext.current.resolveCartdeckEdition(
            when (edition) {
                "pink" -> CartdeckEditionPreset.PINK
                "green" -> CartdeckEditionPreset.GREEN
                else -> CartdeckEditionPreset.ORIGINAL
            },
        )
    val vector =
        remember(source, appearance, tokens) {
            fun role(name: String, base: Color) =
                appearance.custom[name]?.let { Color(it.toInt()) } ?: base
            val chassis = role("chassis", tokens.chassis)
            val accent = role("accent", tokens.accent)
            val edited = appearance.custom.keys.any {
                it in setOf(
                    "chassis", "secondary", "bezel", "glass", "button",
                    "accent", "onChassis", "onButton", "focus",
                )
            }
            val recolourCanonicalMark = edition == "green" &&
                resource == com.swordfish.lemuroid.R.drawable.cartdeck_original_cslot_master
            if (!edited && !recolourCanonicalMark) {
                source
            } else {
                val secondary = role("secondary", lerp(chassis, Color.Black, 0.22f))
                val colors =
                    mapOf(
                        tokens.chassis to chassis,
                        tokens.secondary to secondary,
                        tokens.bezel to role(
                            "bezel",
                            if (appearance.custom.containsKey("chassis")) lerp(chassis, Color.Black, 0.6f) else tokens.bezel,
                        ),
                        tokens.glass to role(
                            "glass",
                            if (appearance.custom.containsKey("chassis")) lerp(chassis, Color.Black, 0.8f) else tokens.glass,
                        ),
                        tokens.button to role("button", tokens.button),
                        tokens.accent to accent,
                        tokens.onChassis to readableColor(role("onChassis", tokens.onChassis), listOf(chassis), 4.5f),
                        tokens.onButton to readableColor(
                            role("onButton", tokens.onButton),
                            listOf(role("button", tokens.button)),
                            4.5f,
                        ),
                        tokens.focus to role("focus", tokens.focus),
                        // The unchanged canonical C-slot master is Original teal;
                        // remap only its semantic accent rather than duplicating geometry.
                        Color(0xFF4ED8C4) to accent,
                    )

                fun tint(brush: androidx.compose.ui.graphics.Brush?) =
                    if (brush is SolidColor) {
                        SolidColor(
                            colors[brush.value] ?: brush.value,
                        )
                    } else {
                        brush
                    }
                val builder =
                    ImageVector.Builder(
                        defaultWidth = source.defaultWidth,
                        defaultHeight = source.defaultHeight,
                        viewportWidth = source.viewportWidth,
                        viewportHeight = source.viewportHeight,
                    )

                fun append(group: VectorGroup) {
                    for (node in group) when (node) {
                        is VectorGroup -> {
                            builder.addGroup(
                                name = node.name,
                                rotate = node.rotation,
                                pivotX = node.pivotX,
                                pivotY = node.pivotY,
                                scaleX = node.scaleX,
                                scaleY = node.scaleY,
                                translationX = node.translationX,
                                translationY = node.translationY,
                                clipPathData = node.clipPathData,
                            )
                            append(node)
                            builder.clearGroup()
                        }
                        is VectorPath ->
                            builder.addPath(
                                pathData = node.pathData, pathFillType = node.pathFillType, name = node.name,
                                fill =
                                    tint(
                                        node.fill,
                                    ),
                                fillAlpha = node.fillAlpha,
                                stroke =
                                    tint(
                                        node.stroke,
                                    ),
                                strokeAlpha = node.strokeAlpha,
                                strokeLineWidth = node.strokeLineWidth,
                                strokeLineCap = node.strokeLineCap,
                                strokeLineJoin = node.strokeLineJoin,
                                strokeLineMiter = node.strokeLineMiter,
                                trimPathStart = node.trimPathStart,
                                trimPathEnd = node.trimPathEnd,
                                trimPathOffset = node.trimPathOffset,
                            )
                    }
                }
                append(source.root)
                builder.build()
            }
        }
    return rememberVectorPainter(vector)
}
