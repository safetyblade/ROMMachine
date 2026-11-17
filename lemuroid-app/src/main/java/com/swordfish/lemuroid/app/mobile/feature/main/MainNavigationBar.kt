package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfMaterials

/** Editing replaces the library's navigation dock; non-Home routes never inherit it. */
internal fun showShelfEditDock(
    currentRoute: MainRoute?,
    arrangingShelf: Boolean,
): Boolean = currentRoute == MainRoute.HOME && arrangingShelf

@Composable
fun MainNavigationBar(
    currentRoute: MainRoute?,
    navController: NavHostController,
    arrangingShelf: Boolean = false,
    canUndoShelf: Boolean = false,
    onDoneArrange: () -> Unit = {},
    onEditTools: () -> Unit = {},
    onUndoShelf: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = currentRoute == MainRoute.HOME || currentRoute == MainRoute.CARTMOD,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        val materials = LocalShelfMaterials.current
        Column(
            modifier = Modifier.fillMaxWidth().background(materials.shelfBackboard),
        ) {
            // A continuous manufactured lip, shared by browsing and arranging.
            Box(
                modifier = Modifier.fillMaxWidth().height(2.dp).background(materials.shelfLip),
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(1.dp).background(materials.edgeHighlight),
            )
            if (showShelfEditDock(currentRoute, arrangingShelf)) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        onClick = onDoneArrange,
                    ) {
                        Text(stringResource(R.string.cartdeck_done))
                    }
                    TextButton(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        onClick = onEditTools,
                    ) {
                        Text("Tools", maxLines = 1)
                    }
                    TextButton(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        enabled = canUndoShelf,
                        onClick = onUndoShelf,
                    ) {
                        Text(stringResource(R.string.cartdeck_undo))
                    }
                }
            } else {
                ProductDock(currentRoute, navController)
            }
        }
    }
}

/** GEM and SafeSave join only when their actual routes are ready. */
@Composable
private fun ProductDock(
    currentRoute: MainRoute?,
    navController: NavHostController,
) {
    val materials = LocalShelfMaterials.current
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        listOf(MainRoute.HOME to "Shelf", MainRoute.CARTMOD to "CARTMOD").forEach { (route, title) ->
            val active = currentRoute == route
            val ink = if (active) materials.interactionAccent else materials.sectionText
            TextButton(
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { selected = active },
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            ) {
                Icon(
                    if (route == MainRoute.HOME) Icons.Filled.Home else Icons.Filled.VideogameAsset,
                    contentDescription = null,
                    tint = ink,
                )
                Text(title, Modifier.padding(start = 8.dp), color = ink, maxLines = 1)
            }
        }
    }
}
