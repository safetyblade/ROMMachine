package com.swordfish.lemuroid.app.mobile.feature.settings.bios

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2BiosRecovery
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2Engine
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2Storage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

@Composable
internal fun Armsx2SetupSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val preferences = remember(context) { context.getSharedPreferences("armsx2-runtime", Context.MODE_PRIVATE) }
    var hasBios by remember { mutableStateOf(false) }
    var showNotice by remember { mutableStateOf(preferences.getBoolean("show_free_software_notice", false)) }
    DisposableEffect(lifecycle) {
        fun refresh() {
            scope.launch {
                hasBios =
                    withContext(Dispatchers.IO) {
                        Armsx2Storage.hasUserBios(context.filesDir)
                    }
            }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(observer)
        refresh()
        onDispose { lifecycle.removeObserver(observer) }
    }
    LemuroidCardSettingsGroup(title = { Text("PlayStation 2 setup") }) {
        com.swordfish.lemuroid.app.mobile.shared.compose.ui.EngineSelectionControls(
            com.swordfish.lemuroid.lib.library.GameSystem.findById(
                com.swordfish.lemuroid.lib.library.SystemID.PLAYSTATION_2.dbname,
            ),
            assetsReady = hasBios,
        )
        if (!Armsx2Engine.engineeringBuild) return@LemuroidCardSettingsGroup
        Text(
            "Quick Play uses Play!. Enhanced Compatibility uses ARMSX2 when the required BIOS and runtime are ready. " +
                "Automatic keeps your existing cartridge engine association.",
            Modifier.padding(16.dp),
        )
        LemuroidSettingsMenuLink(
            title = { Text(if (hasBios) "Replace PS2 BIOS for ARMSX2" else "Import PS2 BIOS for ARMSX2") },
            subtitle = {
                Text(
                    if (hasBios) {
                        "BIOS file present. Memory cards are retained."
                    } else {
                        "Requires a BIOS dump from your own PS2."
                    },
                )
            },
            onClick = { context.startActivity(Armsx2BiosRecovery.fromSettings(context)) },
        )
        LemuroidSettingsMenuLink(
            title = { Text("Open BIOS folder") },
            onClick = { com.swordfish.lemuroid.app.shared.game.ps2next.Ps2Onboarding.openFolder(context) },
        )
        Row(Modifier.padding(16.dp)) {
            Text("Show ARMSX2 startup message", Modifier.weight(1f))
            Switch(checked = showNotice, onCheckedChange = {
                showNotice = it
                preferences.edit().putBoolean("show_free_software_notice", it).apply()
            })
        }
        LemuroidSettingsMenuLink(
            title = { Text("ARMSX2 / PCSX2 licence and source") },
            subtitle = { Text("Free and open source, GNU GPLv3. Attribution and dependency notices are retained.") },
            onClick = {
                scope.launch {
                    val licence =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                ZipInputStream(context.assets.open("cartdeck/armsx2/armsx2-licenses.zip")).use { zip ->
                                    var entry = zip.nextEntry
                                    while (entry != null && entry.name != "COPYING.GPLv3") entry = zip.nextEntry
                                    check(entry != null)
                                    zip.bufferedReader().readText().take(65536)
                                }
                            }.getOrDefault("The full licence archive is retained in the verified engine bundle.")
                        }
                    android.app.AlertDialog.Builder(context).setTitle("ARMSX2 / PCSX2 — GNU GPLv3")
                        .setMessage(
                            "Source: github.com/ARMSX2/ARMSX2\n" +
                                "Pinned revision: 7a999782377e61fb96eca65e476b5ec0d182058e\n\n$licence",
                        )
                        .setPositiveButton("Close", null).show()
                }
            },
        )
    }
}
