package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Native Android/Compose fixtures, not handset or stakeholder visual acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShelfFacingRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture(
        name: String,
        appearance: ShelfAppearance = ShelfAppearance(),
        custom: Boolean = false,
        arrange: Boolean = false,
    ) {
        val context = compose.activity
        val cover =
            if (custom) {
                val bitmap = Bitmap.createBitmap(120, 200, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.CYAN)
                val file = File(context.cacheDir, "shelf-test-cover.png")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                Uri.fromFile(file).toString()
            } else {
                null
            }
        val games =
            (1..5).map {
                Game(
                    id = it, title = "Atlas $it", systemId = "gb", fileName = "$it.gb", fileUri = "content://test/$it",
                    developer = null, coverFrontUrl = null,
                    customCoverUri = if (it == 2) cover else null, lastIndexedAt = 0,
                )
            }
        val layout =
            ShelfLayout(appearance = appearance).reconcile(
                games.associate {
                    shelfGameKey(it.systemId, it.fileUri) to it.displayName()
                },
            )
                .setFacing("game:${shelfGameKey("gb", "content://test/2")}", CartridgeFacing.FRONT)
                .insertSpace(ShelfLayout.UNSORTED, "game:${shelfGameKey("gb", "content://test/5")}")
        var current by mutableStateOf(layout)
        var records by mutableStateOf(games)
        var revision by mutableStateOf(0)
        compose.setContent {
            CartridgeLibraryTheme(appearance) {
                ShelfBoard(
                    current,
                    records,
                    {},
                    {},
                    Modifier,
                    arrange = arrange,
                    onEdit = { current = it(current) },
                    artworkRevision = revision,
                )
            }
        }
        compose.waitForIdle()
        if (arrange) compose.onNodeWithContentDescription("Atlas 2, GB, front facing").performClick()
        // Compose AlertDialog is not an android.app.Dialog under Robolectric.
        // Assert the arrangement action is available, then capture the host view.
        if (arrange) compose.onNodeWithText("Show spine").assertIsDisplayed()
        val view =
            org.robolectric.shadows.ShadowDialog.getLatestDialog()?.window?.decorView
                ?: compose.activity.window.decorView

        fun hasColor(color: Int): Boolean {
            var found = false
            compose.runOnIdle {
                val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(image))
                found =
                    (0 until image.width step 4).any { x ->
                        (0 until image.height step 4).any { y -> image.getPixel(x, y) == color }
                    }
                image.recycle()
            }
            return found
        }
        if (custom) compose.waitUntil(10_000) { hasColor(android.graphics.Color.CYAN) }
        compose.runOnIdle {
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            val colors =
                (0..15).flatMap {
                        y ->
                    (0..15).map {
                            x ->
                        image.getPixel(x * (image.width - 1) / 15, y * (image.height - 1) / 15)
                    }
                }.toSet()
            assertTrue("Actual UI must be rendered", colors.size > 2)
            val directory = File("build/reports/shelf-face-01").apply { mkdirs() }
            File(
                directory,
                "$name.png",
            ).outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            image.recycle()
        }
        if (custom) {
            // Replace the existing managed URI, exactly as Change Cover can do.
            val replacement = Bitmap.createBitmap(120, 200, Bitmap.Config.ARGB_8888)
            replacement.eraseColor(android.graphics.Color.MAGENTA)
            File(Uri.parse(cover).path!!).outputStream().use {
                replacement.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            replacement.recycle()
            compose.runOnIdle { revision++ }
            compose.waitUntil(
                10_000,
            ) { hasColor(android.graphics.Color.MAGENTA) && !hasColor(android.graphics.Color.CYAN) }
            compose.runOnIdle {
                records = records.map { it.copy(customCoverUri = null) }
                revision++
            }
            compose.waitUntil(10_000) { !hasColor(android.graphics.Color.MAGENTA) }
            compose.onNodeWithContentDescription("Atlas 2, GB, front facing").assertExists()
        }
    }

    @Test
    @Config(qualifiers = "w393dp-h850dp-mdpi")
    fun originalMixed() = capture("01-original")

    @Test
    @Config(qualifiers = "w393dp-h850dp-mdpi")
    fun pinkMixed() = capture("02-pink", ShelfAppearance(edition = "pink"))

    @Test
    @Config(qualifiers = "w393dp-h850dp-mdpi")
    fun greenMixed() = capture("03-green", ShelfAppearance(edition = "green"))

    @Test
    @Config(qualifiers = "w393dp-h850dp-mdpi")
    fun stealthMixed() =
        capture(
            "04-stealth-acid",
            CartdeckCuratedDesigns.apply(ShelfAppearance(), CartdeckCuratedDesigns.STEALTH_ACID_ID),
        )

    @Test
    @Config(qualifiers = "w240dp-h640dp-mdpi")
    fun compact() = capture("05-compact")

    @Test
    @Config(qualifiers = "w850dp-h393dp-land-mdpi")
    fun landscape() = capture("06-landscape")

    @Test
    @Config(qualifiers = "w800dp-h1280dp-mdpi")
    fun tablet() = capture("07-tablet")

    @Test
    @Config(qualifiers = "w393dp-h850dp-mdpi")
    fun missingArt() = capture("08-fallback")

    @Test
    @Config(qualifiers = "w393dp-h850dp-mdpi")
    fun customArt() = capture("09-custom", custom = true)

    @Test
    @Config(qualifiers = "w393dp-h850dp-mdpi")
    fun arrangeSelected() = capture("10-arrange", arrange = true)
}
