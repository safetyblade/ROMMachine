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
    pink: Boolean,
): androidx.compose.ui.graphics.painter.Painter {
    val source = ImageVector.vectorResource(resource)
    val appearance = LocalShelfAppearance.current
    val tokens =
        LocalContext.current.resolveCartdeckEdition(
            if (pink) CartdeckEditionPreset.PINK else CartdeckEditionPreset.ORIGINAL,
        )
    val vector =
        remember(source, appearance, tokens) {
            val chassis = appearance.custom["chassis"]?.let { Color(it.toInt()) } ?: tokens.chassis
            val accent = appearance.custom["accent"]?.let { Color(it.toInt()) } ?: tokens.accent
            if (!appearance.custom.containsKey("chassis") && !appearance.custom.containsKey("accent")) {
                source
            } else {
                val secondary = lerp(chassis, Color.Black, 0.22f)
                val colors =
                    mapOf(
                        tokens.chassis to chassis,
                        tokens.secondary to secondary,
                        tokens.bezel to lerp(chassis, Color.Black, 0.6f),
                        tokens.glass to lerp(chassis, Color.Black, 0.8f),
                        tokens.accent to accent,
                        tokens.onChassis to readableColor(tokens.onChassis, listOf(chassis), 4.5f),
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
