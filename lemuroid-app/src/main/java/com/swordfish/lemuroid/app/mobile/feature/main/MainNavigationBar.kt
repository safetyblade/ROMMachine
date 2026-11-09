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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfMaterials
import com.swordfish.lemuroid.app.mobile.feature.shelf.readableColor

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
    onAddSection: () -> Unit = {},
    onUndoShelf: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = currentRoute?.showBottomNavigation != false,
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
                            .heightIn(min = 72.dp),
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
                        onClick = onAddSection,
                    ) {
                        Text(stringResource(R.string.cartdeck_add_section), maxLines = 1)
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
                CartridgeNavigationTabs(currentRoute, navController)
            }
        }
    }
}

@Composable
private fun CartridgeNavigationTabs(
    currentRoute: MainRoute?,
    navController: NavHostController,
) {
    val materials = LocalShelfMaterials.current
    val inactive =
        readableColor(
            materials.edgeHighlight,
            listOf(materials.shelfBackboard),
            3f,
        )
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = materials.shelfBackboard,
        tonalElevation = 0.dp,
    ) {
        MainNavigationRoutes.values().forEach { destination ->
            val selected = currentRoute?.root == destination.route
            val iconDrawable = if (selected) destination.selectedIcon else destination.unselectedIcon
            val label = stringResource(destination.titleId)
            NavigationBarItem(
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(iconDrawable, contentDescription = null)
                        Box(
                            modifier =
                                Modifier
                                    .width(24.dp)
                                    .height(3.dp)
                                    .background(if (selected) materials.interactionAccent else Color.Transparent),
                        )
                    }
                },
                label = { Text(label, maxLines = 1) },
                selected = selected,
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = materials.interactionAccent,
                        selectedTextColor = materials.sectionText,
                        unselectedIconColor = inactive,
                        unselectedTextColor = inactive,
                        indicatorColor = Color.Transparent,
                    ),
                modifier =
                    Modifier.semantics {
                        contentDescription = label
                    },
                onClick = {
                    navController.navigate(destination.route.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
            )
        }
    }
}
