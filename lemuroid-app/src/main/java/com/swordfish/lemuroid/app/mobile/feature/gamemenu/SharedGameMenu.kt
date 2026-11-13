package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink

@Composable
internal fun GameMenuTheme(
    appearance: ShelfAppearance? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val persisted by produceState(initialValue = ShelfAppearance(), key1 = context) {
        value = ShelfStore(context).read().appearance
    }
    val saved = appearance ?: persisted
    AppTheme { CartridgeLibraryTheme(saved, content) }
}

/** The established right-side panel, shared by the Activity menu and native overlay. */
@Composable
internal fun GameMenuPanel(
    title: String,
    console: String,
    page: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onResume: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onBack)
    BoxWithConstraints(
        Modifier.fillMaxSize().testTag("game-menu").onPreviewKeyEvent { event ->
            when (event.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_BUTTON_B -> {
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) onBack()
                    true
                }
                KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_MENU -> {
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) onResume()
                    true
                }
                else -> false
            }
        },
    ) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                .clickable(onClick = onResume),
        )
        val panelWidth = minOf(if (maxWidth < 480.dp) maxWidth else maxWidth * 0.8f, 400.dp)
        Surface(
            modifier =
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(panelWidth)
                    .pointerInput(Unit) { detectTapGestures { } },
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Box(Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(
                            if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            if (canGoBack) "Back" else "Resume",
                        )
                    }
                    Column(Modifier.padding(start = 56.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)) {
                        Text(page, style = MaterialTheme.typography.titleLarge)
                        Text("$console · $title", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider()
                content()
            }
        }
    }
}

@Composable
internal fun GameMenuLink(
    label: String,
    icon: Int = R.drawable.ic_menu_settings,
    onClick: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        LemuroidSettingsMenuLink(
            title = { Text(label) },
            icon = { Icon(painterResource(icon), contentDescription = null) },
            onClick = onClick,
        )
    }
}

@Composable
internal fun SharedGameMenuHome(
    capabilities: GameMenuCapabilities,
    onAction: (GameMenuAction) -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Button(
                modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp).focusRequester(focus),
                onClick = { onAction(GameMenuAction.RESUME) },
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(GameMenuAction.RESUME.label)
            }
            capabilities.homeActions().filterNot {
                it == GameMenuAction.RESUME || it == GameMenuAction.RETURN_TO_SHELF
            }.forEach { action ->
                GameMenuLink(
                    action.label,
                    when (action) {
                        GameMenuAction.CONTROLS -> R.drawable.ic_menu_controls
                        GameMenuAction.STATES -> R.drawable.ic_menu_save
                        else -> R.drawable.ic_menu_settings
                    },
                ) { onAction(action) }
            }
        }
        HorizontalDivider()
        GameMenuLink(GameMenuAction.RETURN_TO_SHELF.label, R.drawable.ic_menu_quit) {
            onAction(GameMenuAction.RETURN_TO_SHELF)
        }
    }
}
