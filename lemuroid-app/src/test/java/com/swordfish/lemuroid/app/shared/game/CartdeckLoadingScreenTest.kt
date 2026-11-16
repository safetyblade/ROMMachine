package com.swordfish.lemuroid.app.shared.game

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w800dp-h600dp-mdpi")
class CartdeckLoadingScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun shortLandscapeFocusesVisibleCancelAndKeepsCleanupStateTruthful() {
        val cancelling = mutableStateOf(false)
        var requests = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(700.dp, 260.dp)) {
                    CartdeckLoadingScreen(
                        "Preparing content",
                        title = "Fixture · Wii U",
                        onCancel = {
                            requests++
                            cancelling.value = true
                        },
                        cancelling = cancelling.value,
                    )
                }
            }
        }
        compose.onNodeWithText("Cancel and return to Shelf").assertIsFocused().assertIsDisplayed().performClick()
        compose.onNodeWithText("Cancelling — finishing safely").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, requests) }
    }
}
