package com.swordfish.lemuroid.app.shared.support

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.ps2next.Ps2Onboarding
import com.swordfish.lemuroid.app.shared.gamecrash.GameCrashActivity
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SupportScreensTest {
    @Test
    fun recoveryKeepsShelfActionVisibleInCompactLandscape() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val report =
            SupportReport.create(
                "failure recovery",
                "Game stopped responding",
                "failure=ENGINE_PROCESS_ENDED",
                gameTitle = "Conflict: Desert Storm",
                system = "PS2",
                engine = "Play!",
            )
        GameCrashActivity.launchReport(host, report)
        val intent = shadowOf(host).nextStartedActivity
        val controller = Robolectric.buildActivity(GameCrashActivity::class.java, intent)
        controller.get().setTheme(R.style.LemuroidMaterialTheme_Game)
        val activity = controller.setup().get()
        try {
            capture(activity.window.decorView, "support-recovery-portrait", 393, 852)
            capture(activity.window.decorView, "support-recovery-compact-landscape", 640, 360)
            val shelf = activity.findViewById<Button>(R.id.returnToShelf)
            assertTrue(shelf.isShown && shelf.height > 0 && shelf.isFocusable)
            assertEquals("Return to shelf", shelf.text.toString())
            shelf.performClick()
            assertTrue(activity.isFinishing)
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun rendersReportEditorAndFirstLaunchBiosOnboarding() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val report = SupportReport.create("settings", "Application problem", null)
        val support = Robolectric.buildActivity(SupportActivity::class.java, SupportActivity.intent(host, report))
        support.get().setTheme(R.style.LemuroidMaterialTheme_Game)
        try {
            capture(support.setup().get().window.decorView, "support-report-portrait", 393, 852)
        } finally {
            support.destroy()
        }

        host.setTheme(R.style.LemuroidMaterialTheme_Game)
        val game =
            Game(
                id = 24,
                fileName = "fixture.iso",
                fileUri = "content://fixture/game",
                title = "Fixture",
                systemId = SystemID.PLAYSTATION_2.dbname,
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 1,
            )
        var continued = false
        assertTrue(Ps2Onboarding.showIfNeeded(host, game, EngineSelectionPreferences(host)) { continued = true })
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        try {
            capture(checkNotNull(dialog.window).decorView, "support-ps2-first-launch", 393, 510)
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(continued)
        } finally {
            dialog.dismiss()
        }
    }

    private fun capture(
        view: View,
        name: String,
        width: Int,
        height: Int,
    ) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            val output = File("build/reports/gc01-menu-screens", "$name.png")
            check(output.parentFile?.isDirectory == true || output.parentFile?.mkdirs() == true)
            output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }
}
