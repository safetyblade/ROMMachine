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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfMaterials

/** Product dock is available only for real destinations. Arrangement has priority. */
internal fun showProductDock(
    currentRoute: MainRoute?,
    arrangingShelf: Boolean,
    cartmodAvailable: Boolean,
): Boolean = cartmodAvailable && !arrangingShelf && currentRoute in setOf(MainRoute.HOME, MainRoute.CARTMOD)

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
    cartmodAvailable: Boolean = false,
) {
    AnimatedVisibility(
        visible =
            showShelfEditDock(currentRoute, arrangingShelf) ||
                showProductDock(currentRoute, arrangingShelf, cartmodAvailable),
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
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        enabled = currentRoute != MainRoute.HOME,
                        onClick = {
                            if (!navController.popBackStack(MainRoute.HOME.route, false)) {
                                navController.navigate(MainRoute.HOME.route) { launchSingleTop = true }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.cartdeck_product_shelf))
                    }
                    TextButton(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        enabled = currentRoute != MainRoute.CARTMOD,
                        onClick = {
                            navController.navigate(MainRoute.CARTMOD.route) { launchSingleTop = true }
                        },
                    ) {
                        Text(stringResource(R.string.cartdeck_cartmod_title))
                    }
                }
            }
        }
    }
}

