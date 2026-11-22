package com.swordfish.lemuroid.app.mobile.feature.main

import com.swordfish.lemuroid.R

/**
 * Stable product identity, independent of dock position. Only mounted routes are exposed.
 * Reserved products are not navigation targets or authorisation decisions.
 */
internal enum class ProductDestination(
    val title: String,
    val route: MainRoute?,
    val mark: Int?,
) {
    SHELF("Shelf", MainRoute.HOME, R.drawable.cartdeck_brand_shelf),
    GEM("GEM", MainRoute.GEM, R.drawable.cartdeck_brand_gem),
    SAFESAVE("SafeSave", MainRoute.SAFESAVE, R.drawable.cartdeck_brand_safesave_wordmark),
    CARTMOD("CARTMOD", MainRoute.CARTMOD, R.drawable.cartdeck_brand_cartmod),
    SETTINGS("Settings", MainRoute.SETTINGS, R.drawable.cartdeck_brand_settings),
    ;

    companion object {
        fun mountedDock() =
            values().mapNotNull { destination ->
                val route = destination.route ?: return@mapNotNull null
                val mark = destination.mark ?: return@mapNotNull null
                Triple(route, destination.title, mark)
            }

        fun isMountedRoot(route: MainRoute?): Boolean = route != null && mountedDock().any { it.first == route }
    }
}

/** Reserved child destinations; no route is registered until its service is verified. */
internal enum class NestedProduct(
    val title: String,
    val parentProduct: ProductDestination,
    val logicalRoute: String,
) {
    MODERN("Modern", ProductDestination.SETTINGS, "settings/controllers/modern"),
    VAULT("Vault", ProductDestination.SAFESAVE, "safesave/vault"),
}
