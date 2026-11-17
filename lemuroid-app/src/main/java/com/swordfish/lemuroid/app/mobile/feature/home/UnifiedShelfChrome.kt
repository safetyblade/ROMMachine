package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Shelf-only chrome. No navigation to legacy filter screens and no write to shelf storage. */
@Composable
internal fun UnifiedShelfChrome(
    modifier: Modifier,
    games: List<Game>,
    arrange: Boolean,
    content: @Composable (ShelfQuery, Modifier) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var favourites by rememberSaveable { mutableStateOf(false) }
    var systems by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var recent by rememberSaveable { mutableStateOf(false) }
    var sortName by rememberSaveable { mutableStateOf(ShelfSort.ARRANGEMENT.name) }
    var filters by remember { mutableStateOf(false) }
    val savedQuery = ShelfQuery(text, favourites, systems.toSet(), recent, ShelfSort.valueOf(sortName))
    val query = if (arrange) ShelfQuery() else savedQuery
    val focus = LocalFocusManager.current

    fun clear() {
        text = ""
        favourites = false
        systems = arrayListOf()
        recent = false
        sortName = ShelfSort.ARRANGEMENT.name
    }
    BackHandler(enabled = !arrange && !filters && savedQuery.active) {
        focus.clearFocus()
        clear()
    }
    Column(modifier.fillMaxSize()) {
        if (!arrange) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                singleLine = true,
                placeholder = { Text("Search collection") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )
            if (query.active) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        buildList {
                            if (favourites) add("Favourites")
                            if (recent) add("Played")
                            if (systems.isNotEmpty()) add(systems.joinToString { cartridgeSystemLabel(it) })
                            if (query.sort != ShelfSort.ARRANGEMENT) add(sortLabel(query.sort))
                            if (text.isNotBlank()) add("Search")
                        }.joinToString(" · "),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                    )
                    TextButton(onClick = ::clear) { Text("Clear") }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content(
                query,
                Modifier.fillMaxSize().then(if (arrange) Modifier else Modifier.padding(bottom = 60.dp)),
            )
            if (!arrange) {
                FilledTonalButton(
                    onClick = {
                        focus.clearFocus()
                        filters = true
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(if (query.active) "Filters •" else "Filters")
                }
            }
        }
    }
    if (filters && !arrange) {
        AlertDialog(
            onDismissRequest = { filters = false },
            title = { Text("Library filters") },
            text = {
                Column(
                    Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = favourites, onCheckedChange = { favourites = it })
                        Text("Favourites only")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recent, onCheckedChange = { recent = it })
                        Text("Previously played")
                    }
                    Text("Consoles")
                    games.groupingBy { it.systemId }.eachCount().toSortedMap().forEach { (id, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = id in systems,
                                onCheckedChange = { checked ->
                                    systems = ArrayList(if (checked) systems + id else systems - id)
                                },
                            )
                            Text("${cartridgeSystemLabel(id)} ($count)")
                        }
                    }
                    Text("Sort by")
                    ShelfSort.values().forEach { sort ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = sortName == sort.name, onClick = { sortName = sort.name })
                            Text(sortLabel(sort))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { filters = false }) { Text("Done") } },
            dismissButton = { TextButton(onClick = ::clear) { Text("Clear all") } },
        )
    }
}

private fun sortLabel(sort: ShelfSort): String =
    when (sort) {
        ShelfSort.ARRANGEMENT -> "My arrangement"
        ShelfSort.TITLE_ASC -> "Title A–Z"
        ShelfSort.TITLE_DESC -> "Title Z–A"
        ShelfSort.SYSTEM -> "Console"
        ShelfSort.LAST_PLAYED -> "Last played"
    }
