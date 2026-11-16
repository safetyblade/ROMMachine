package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import com.swordfish.lemuroid.app.shared.input.ControllerMenuEdge
import com.swordfish.lemuroid.app.shared.input.handle

@Composable
internal fun GameMenuTheme(
    appearance: ShelfAppearance? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // ShelfStore.read is suspend; load the saved palette without blocking composition.
    // Never overwrite a corrupt or newer shelf file while displaying the menu.
    // A supplied live appearance takes precedence immediately. The saved
    // palette is only loaded when the caller has no explicit appearance;
    // produceState's initialValue is not reapplied on key changes.
    val persisted =
        produceState(initialValue = ShelfAppearance(), key1 = context, key2 = appearance == null) {
            if (appearance == null) {
                try {
                    value = ShelfStore(context).read().appearance
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    timber.log.Timber.e(error, "Unable to read CARTDECK menu appearance; retained shelf data")
                }
            }
        }.value
    AppTheme { CartridgeLibraryTheme(appearance ?: persisted, content) }
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
    statusMessage: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backFocus = remember { FocusRequester() }
    val menuView = LocalView.current
    val menuEdge = remember { ControllerMenuEdge() }
    val backEdge = remember { ControllerMenuEdge() }
    // Nested routes start with a reachable Back control. Home keeps its
    // separate first-action focus, so opening a menu selects Resume.
    LaunchedEffect(page, canGoBack) {
        if (canGoBack) backFocus.requestFocus()
    }
    BackHandler(onBack = onBack)
    BoxWithConstraints(
        Modifier.fillMaxSize().testTag("game-menu").onPreviewKeyEvent { event ->
            when (event.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_BUTTON_A -> {
                    val raw = event.nativeKeyEvent
                    menuView.dispatchKeyEvent(
                        KeyEvent(
                            raw.downTime,
                            raw.eventTime,
                            raw.action,
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            raw.repeatCount,
                        ),
                    )
                    true
                }
                KeyEvent.KEYCODE_BUTTON_B -> {
                    if (backEdge.handle(event.nativeKeyEvent)) onBack()
                    true
                }
                KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_MENU -> {
                    if (menuEdge.handle(event.nativeKeyEvent)) onResume()
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
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart).focusRequester(backFocus),
                    ) {
                        Icon(
                            if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            if (canGoBack) "Back" else "Resume",
                        )
                    }
                    Column(Modifier.padding(start = 56.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)) {
                        Text(page, style = MaterialTheme.typography.titleLarge)
                        Text("$console · $title", style = MaterialTheme.typography.bodyMedium)
                        if (!statusMessage.isNullOrBlank()) {
                            Text(statusMessage, style = MaterialTheme.typography.bodySmall)
                        }
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier =
            modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
                .heightIn(min = 48.dp).onFocusChanged { focused = it.hasFocus }
                .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (focused) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        contentColor =
            if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.5f,
                )
            },
        border =
            BorderStroke(
                if (focused) 2.dp else 1.dp,
                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leading != null) {
                Icon(
                    leading,
                    null,
                    Modifier.size(22.dp),
                )
            } else {
                Icon(painterResource(icon), null, Modifier.size(22.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
internal fun SharedGameMenuHome(
    capabilities: GameMenuCapabilities,
    canResume: Boolean = true,
    onAction: (GameMenuAction) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(canResume) { firstFocus.requestFocus() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        GameMenuLink(
            GameMenuAction.RESUME.label,
            enabled = canResume,
            modifier = if (canResume) Modifier.focusRequester(firstFocus) else Modifier,
            leading = Icons.Default.PlayArrow,
        ) { onAction(GameMenuAction.RESUME) }
        if (!canResume && capabilities.advanced) {
            GameMenuLink(
                "Recovery options",
                modifier = Modifier.focusRequester(firstFocus),
                leading = Icons.Default.MoreHoriz,
            ) {
                onAction(GameMenuAction.ADVANCED)
            }
        }
        capabilities.homeActions().filterNot { it == GameMenuAction.RESUME || it == GameMenuAction.ADVANCED }.forEach {
                action ->
            GameMenuLink(
                action.label,
                modifier =
                    if (!canResume && !capabilities.advanced && action == GameMenuAction.RETURN_TO_SHELF) {
                        Modifier.focusRequester(firstFocus)
                    } else {
                        Modifier
                    },
                leading =
                    when (action) {
                        GameMenuAction.STATES -> Icons.Default.Save
                        GameMenuAction.CONTROLS -> Icons.Default.Gamepad
                        GameMenuAction.DISPLAY -> Icons.Default.AspectRatio
                        GameMenuAction.SETTINGS -> Icons.Default.Tune
                        GameMenuAction.RETURN_TO_SHELF -> Icons.Default.Logout
                        else -> Icons.Default.PlayArrow
                    },
            ) { onAction(action) }
        }
        if (capabilities.advanced) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            GameMenuLink(
                GameMenuAction.ADVANCED.label,
                leading = Icons.Default.MoreHoriz,
            ) { onAction(GameMenuAction.ADVANCED) }
        }
    }
}
