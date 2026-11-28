package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guard the cross-host layout and single-trim contract without changing gameplay tests. */
class UniDLayoutSourceContractTest {
    private fun source(name: String): String {
        val relative = "src/main/java/com/swordfish/lemuroid/app/mobile/feature/game/$name"
        return listOf(File(relative), File("lemuroid-app", relative))
            .firstOrNull { it.isFile }
            ?.readText()
            ?: error("Missing source: $name")
    }

    @Test fun bothGameplayHostsUseTheSharedLandscapeGripArcReservation() {
        val common = source("GameScreenLayout.kt")
        assertTrue(common.contains("gripArcLandscape: Boolean = false"))
        assertEquals(132f, GameScreenLayout.gripArcLandscapeSideWidth(520.dp).value, 0.01f)
        assertEquals(177.6f, GameScreenLayout.gripArcLandscapeSideWidth(740.dp).value, 0.01f)
        assertEquals(260f, GameScreenLayout.gripArcLandscapeSideWidth(1280.dp).value, 0.01f)
        assertEquals(2, "Dimension.value(gripSideWidth)".toRegex(RegexOption.LITERAL).findAll(common).count())
        for (name in listOf("MobileGameScreen.kt", "XboxGameControls.kt")) {
            val host = source(name)
            assertTrue(
                "$name must reserve the same GripArc side deck",
                host.contains("gripArcLandscape = gripArcLandscape"),
            )
        }
    }

    @Test fun landscapeHasOneNonOverlappingLowerTrimTreatment() {
        assertTrue(
            source("CartdeckConsoleShell.kt")
                .contains("if (!minimal && !isLandscape) {\n                CartdeckHardwareGeometry.seamSegments("),
        )
    }
}
