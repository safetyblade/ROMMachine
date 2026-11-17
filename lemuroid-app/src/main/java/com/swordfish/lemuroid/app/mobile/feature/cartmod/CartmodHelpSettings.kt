package com.swordfish.lemuroid.app.mobile.feature.cartmod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink

internal val pcSetupGuide =
    listOf(
        "Open a PC cartridge. Boot and install Windows from your own installation media.",
        "Run the installed operating system, install the game and shut Windows down normally.",
        "In Windows, create a Startup shortcut to the installed EXE with the game folder as Start in.",
        "For new imports with an EXE path, copy the CARTPLAY shortcut from the content drive to Startup.",
        "Set the installed OS as auto-start in the core menu when supported.",
        "To recover setup, remove the guest startup shortcut and reopen the core menu.",
    )

@Composable
internal fun CartmodHelpSettings(onOpenHelp: () -> Unit) {
    var guide by rememberSaveable { mutableStateOf<String?>(null) }
    LemuroidCardSettingsGroup(title = { Text("Help & guides") }) {
        LemuroidSettingsMenuLink(
            title = { Text("How to use CARTMOD") },
            onClick = { guide = "How to use CARTMOD" },
        )
        LemuroidSettingsMenuLink(
            title = { Text("Install Windows and a PC game") },
            onClick = { guide = "Install Windows and a PC game" },
        )
        LemuroidSettingsMenuLink(title = { Text("About CARTDECK and supported systems") }, onClick = onOpenHelp)
    }
    guide?.let { title ->
        AlertDialog(
            onDismissRequest = { guide = null },
            title = { Text(title) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (title == "How to use CARTMOD") {
                        Text("Choose an available preparation task and select game media you have permission to use.")
                        Text("Review the setup, then return to Shelf. Importing a game does not verify playability.")
                    } else {
                        Text("These steps depend on the installed PC runtime. Boot and game launch are not guaranteed.")
                        pcSetupGuide.forEachIndexed { index, step -> Text("${index + 1}. $step") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { guide = null }) { Text("Done") } },
        )
    }
}

private val faqArticles =
    listOf(
        "What is emulation?" to "An emulator reproduces parts of a gaming system in software.",
        "Does CARTDECK include games?" to "Use game files and media you have permission to use.",
        "What is a game patch?" to "A patch changes a specific version of a game. The correct base is required.",
        "Why does a PC cartridge say Setup needed?" to "Import is separate from installation and verified launch.",
        "Will a modified edition share my saves?" to "Do not assume save compatibility. Keep your original progress.",
        "What if setup fails?" to "Check the actual status and keep the original game and saves before retrying.",
    )

@Composable
internal fun CartmodFaq() {
    var open by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    LemuroidSettingsMenuLink(
        title = { Text("FAQs") },
        subtitle = { Text("Emulation, modding and troubleshooting") },
        onClick = { open = true },
    )
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("FAQs") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search articles") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    val matches =
                        faqArticles.filter { (q, a) ->
                            q.contains(query, ignoreCase = true) || a.contains(query, ignoreCase = true)
                        }
                    if (matches.isEmpty()) Text("No matching articles")
                    matches.forEach { (q, a) ->
                        Text(q, style = MaterialTheme.typography.titleMedium)
                        Text(a)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Done") } },
        )
    }
}
