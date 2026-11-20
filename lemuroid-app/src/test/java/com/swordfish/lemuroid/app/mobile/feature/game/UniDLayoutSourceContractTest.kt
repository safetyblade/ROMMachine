package com.swordfish.lemuroid.app.mobile.feature.game

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
        assertTrue(common.contains("Dimension.value(180.dp)"))
        for (name in listOf("MobileGameScreen.kt", "XboxGameControls.kt")) {
            val host = source(name)
            assertTrue("$name must reserve the same GripArc side deck", host.contains("gripArcLandscape = gripArcLandscape"))
        }
    }

    @Test fun landscapeHasOneNonOverlappingLowerTrimTreatment() {
        assertTrue(
            source("CartdeckConsoleShell.kt")
                .contains("if (!minimal && !isLandscape) {\n                CartdeckHardwareGeometry.seamSegments("),
        )
    }
}
