package com.swordfish.lemuroid.app.shared.game.xbox360

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.shared.game.GameProcessLock

@Composable
fun Xbox360Troubleshooting() {
    val context = LocalContext.current
    var report by remember { mutableStateOf(Xbox360Reports.read(context.filesDir)) }
    var show by remember { mutableStateOf(false) }
    TextButton(onClick = {
        show = !show
        report = Xbox360Reports.read(context.filesDir)
    }) { Text("Troubleshooting") }
    if (show) {
        Text(
            "Uses the device's system Vulkan driver. Gameplay and title compatibility require device testing. " +
                "Saves remain in Xbox 360 storage when an engine is changed.",
        )
        Text(report)
        TextButton(onClick = {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("CARTDECK Xbox 360", report))
        }) { Text("Copy engine/GPU report") }
        val store = Xbox360Engine.store(context.filesDir)
        if (store.canRollback()) {
            TextButton(onClick = {
                if (!GameProcessLock.isHeldByAnotherProcess(context) && store.rollback()) {
                    report = "Previous engine selected for the next launch. All Xbox 360 content and saves retained."
                } else {
                    report = "Close the active game before changing its engine. Saves retained."
                }
            }) { Text("Previous Engine") }
        }
    }
}
