package com.swordfish.lemuroid.app.mobile.feature.game

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckEdition
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.settings.TouchControllerID
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.inputstate.InputState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import android.graphics.Rect as BitmapRect

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w393dp-h852dp-mdpi")
class PivotArcFamilyRenderingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private data class Mode(
        val system: SystemID,
        val controller: TouchControllerID,
        val native: Boolean = false,
        val analogPrimary: Boolean = false,
        val wiiPointerStick: Boolean = true,
        val standard: Boolean = false,
        val historical: Boolean = false,
    ) {
        val id: String
            get() =
                buildString {
                    append("${system.name}-${controller.name}")
                    if (native) append("-host")
                    if (analogPrimary) append("-analog-primary")
                    if (!wiiPointerStick) append("-touch-ir")
                    if (standard) append("-standard")
                    if (historical) append("-reference")
                }.lowercase()

        val profile: PivotArcSoloProfile?
            get() =
                when {
                    historical -> null
                    standard -> PivotArcSoloProfiles.wiiNunchuk(wiiPointerStick, fixedHolds = false)
                    native -> checkNotNull(PivotArcSoloProfiles.forNativeHost(system, controller)) { id }
                    else -> PivotArcSoloProfiles.forMode(system, controller, analogPrimary, wiiPointerStick)
                }
    }

    private fun modes(): List<Mode> =
        buildList {
            for (system in SystemID.entries) {
                val configurations = GameSystem.findById(system.dbname).systemCoreConfigs
                configurations.flatMap { it.controllerConfigs[0].orEmpty() }
                    .map { it.touchControllerID }.distinct().forEach { add(Mode(system, it)) }
            }
            add(Mode(SystemID.PLAYSTATION_2, TouchControllerID.PSX_DUALSHOCK, native = true))
            add(Mode(SystemID.PLAYSTATION_3, TouchControllerID.PSX_DUALSHOCK, native = true))
            add(Mode(SystemID.WII_U, TouchControllerID.WII_CLASSIC, native = true))
            add(Mode(SystemID.XBOX, TouchControllerID.XBOX, native = true))
            add(Mode(SystemID.XBOX360, TouchControllerID.XBOX360, native = true))
            add(Mode(SystemID.PSP, TouchControllerID.PSP, analogPrimary = true))
            add(Mode(SystemID.WII, TouchControllerID.WII_NUNCHUK, wiiPointerStick = false))
            add(Mode(SystemID.WII, TouchControllerID.WII_NUNCHUK, standard = true))
            add(Mode(SystemID.WII, TouchControllerID.WII_NUNCHUK, wiiPointerStick = false, standard = true))
            add(Mode(SystemID.GBA, TouchControllerID.GBA, historical = true))
            add(Mode(SystemID.PSP, TouchControllerID.PSP, historical = true))
        }

    private val baseline by lazy {
        PivotArcLayoutProfile.parse(File("src/main/assets/pivotarc/candidate-c-baseline.json").readText())
    }

    private fun viewport(
        mode: Mode,
        device: ControlDeckDeviceFixture,
    ): PivotArcBounds {
        if (CartdeckViewportPolicy.portraitMode(mode.system) == CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN) {
            return PivotArcBounds(18f, 18f, device.width - 18f, 18f + device.height * 0.46f)
        }
        val (width, height) =
            CartdeckViewportPolicy.portraitApertureRatio(
                mode.system,
                device.height,
                compactSoloPivot = device.width < 380f,
            )
                .split(':').map(String::toFloat)
        val insets = portraitHardwareInsets(device.width.dp, device.height.dp, width / height != 4f / 3f)
        val side = insets.side.value
        val top = insets.top.value
        return PivotArcBounds(side, top, device.width - side, top + (device.width - 2f * side) * height / width)
    }

    private fun placement(
        mode: Mode,
        device: ControlDeckDeviceFixture,
        left: Boolean = false,
    ): PivotArcPlacement {
        val profile = mode.profile
        val video = viewport(mode, device)
        if (mode.standard) {
            return checkNotNull(PivotArcSoloProfiles.standardWiiNunchuk(checkNotNull(profile), device.safe, video)) {
                mode.id
            }
        }
        val result =
            if (profile != null) {
                PivotArcSoloSolver.resolve(profile, device.safe, video, left, 0f, 0f, false)
            } else {
                check(!PivotArcSoloProfiles.supportsSystem(mode.system) || mode.historical) { mode.id }
                PivotArcPositionSolver.resolve(
                    baseline,
                    checkNotNull(PivotArcLayoutProfile.activeFor(mode.system)),
                    device.safe,
                    video,
                    left,
                    0f,
                    0f,
                    false,
                    requireHardware = !mode.historical,
                )
            }
        return (result as? PivotArcResolution.Fits)?.placement
            ?: error("${mode.id} on ${device.width}×${device.height}, left=$left: $result")
    }

    private fun PivotArcBounds.rect() = Rect(left, top, right, bottom)

    private fun PivotArcPlacement.hardware() =
        controls.mapKeys { pivotArcHardwareSeatKey(it.key) }.mapValues { it.value.rect() }

    @Test
    fun configuredModesFitHandsetsWithIndependentTargetsAndAWordmark() {
        val modes = modes().filterNot { it.historical }
        assertEquals(SystemID.entries.toSet(), modes.map { it.system }.toSet())
        for (device in listOf(
            ControlDeckDeviceFixture(360f, 740f),
            ControlDeckDeviceFixture(393f, 852f),
            ControlDeckDeviceFixture(412f, 915f),
        )) {
            for (mode in modes) {
                val video = viewport(mode, device)
                val right = placement(mode, device)
                for (left in if (mode.standard) listOf(false) else listOf(false, true)) {
                    val placed = placement(mode, device, left)
                    mode.profile?.let {
                        assertEquals(
                            mode.id,
                            it.seats.map {
                                    seat ->
                                seat.id
                            }.toSet(),
                            placed.controls.keys,
                        )
                    }
                    for ((id, target) in placed.controls) {
                        assertTrue(
                            mode.id,
                            target.width >= 47.99f && target.height >= 47.99f && target.inside(device.safe),
                        )
                        assertFalse(mode.id, target.intersects(video))
                        assertTrue(mode.id, placed.controls.filterKeys { it != id }.values.none(target::intersects))
                        if (left) {
                            assertEquals(
                                device.safe.left + device.safe.right - right.controls.getValue(id).right,
                                target.left,
                                0.01f,
                            )
                        }
                    }
                    if (!mode.standard) {
                        val module =
                            PivotArcModuleGeometry.resolve(
                                placed.hardware(),
                                video.rect(),
                                device.screen.rect(),
                                PIVOT_ARC_EDGE_DP,
                                PIVOT_ARC_LABEL_HEIGHT_DP,
                                PIVOT_ARC_DISPLAY_CLEARANCE_DP,
                                mode.profile?.hardwareUtilities ?: PivotArcModuleGeometry.legacyUtilityKeys,
                            )
                        assertTrue(mode.id, module?.wordmark != null)
                    }
                }
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun renderFamilyContactSheet() {
        val modes = modes()
        val device = ControlDeckDeviceFixture(393f, 852f)
        val current = mutableStateOf(modes.first())
        val background = Color(0xFF272233)
        compose.setContent {
            val mode = current.value
            val placement = remember(mode) { placement(mode, device) }
            val video = viewport(mode, device).rect()
            val tokens = compose.activity.resolveCartdeckEdition(CartdeckEditionPreset.ORIGINAL)
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLemuroidPadTheme provides LemuroidPadTheme(),
            ) {
                Box(Modifier.requiredSize(393.dp, 852.dp).background(background).testTag("pivotarc-family")) {
                    CartdeckConsoleShell(
                        tokens,
                        video,
                        device.screen.rect(),
                        false,
                        placement.hardware().values.toList(),
                    )
                    CartdeckHardwareSeats(
                        rootBounds = device.screen.rect(),
                        videoBounds = video,
                        inputBounds = placement.hardware(),
                        tokens = tokens,
                        edition = CartdeckEditionPreset.ORIGINAL,
                        isDualScreen =
                            CartdeckViewportPolicy.portraitMode(mode.system) ==
                                CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN,
                        customColours = false,
                        pivotArc = !mode.standard,
                        measuredStandard = mode.standard,
                        pivotUtilities = mode.profile?.hardwareUtilities ?: PivotArcModuleGeometry.legacyUtilityKeys,
                    )
                    PadKit(
                        onInputEvents = {},
                        hapticFeedbackType = HapticFeedbackType.NONE,
                        simulatedState = remember { mutableStateOf(InputState()) },
                        simulatedControlIds = remember { mutableStateOf(emptySet()) },
                    ) {
                        PivotArcControlGroup(
                            placement,
                            mode.system,
                            emptySet(),
                            tokens.accent,
                            {},
                            touchController = mode.controller,
                            soloProfile = mode.profile,
                        )
                    }
                }
            }
        }

        val tileWidth = 210
        val tileHeight = 476
        val columns = 6
        val sheet =
            Bitmap.createBitmap(
                tileWidth * columns,
                tileHeight * ((modes.size + columns - 1) / columns),
                Bitmap.Config.ARGB_8888,
            )
        val gallery = Canvas(sheet)
        gallery.drawColor(android.graphics.Color.rgb(26, 24, 32))
        val title =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 11f
            }
        val imagePaint = Paint(Paint.FILTER_BITMAP_FLAG)
        val output = File("build/reports/gc01-menu-screens/pivotarc-family").apply { mkdirs() }
        for ((index, mode) in modes.withIndex()) {
            compose.runOnIdle { current.value = mode }
            if (!mode.standard) compose.onNodeWithText("PivotArc").assertIsDisplayed()
            val rect = compose.onNodeWithTag("pivotarc-family").getUnclippedBoundsInRoot()
            compose.runOnIdle {
                val bitmap = Bitmap.createBitmap(393, 852, Bitmap.Config.ARGB_8888)
                val capture = Canvas(bitmap)
                capture.translate(-rect.left.value, -rect.top.value)
                compose.activity.window.decorView.draw(capture)
                val video = viewport(mode, device)
                assertEquals(
                    mode.id,
                    background.toArgb(),
                    bitmap.getPixel(video.centerX.toInt(), video.centerY.toInt()),
                )
                File(
                    output,
                    "${mode.id}.png",
                ).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val x = index % columns * tileWidth
                val y = index / columns * tileHeight
                gallery.drawText(mode.id, x + 4f, y + 16f, title)
                gallery.drawBitmap(bitmap, null, BitmapRect(x + 4, y + 24, x + 206, y + 462), imagePaint)
                bitmap.recycle()
            }
        }
        File(output, "contact-sheet.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sheet.recycle()
    }
}
