package com.swordfish.lemuroid.app.mobile.feature.product

/** Logical identifiers only; neither navigation routes nor authorisation tokens. */
internal enum class ProductCapability {
    SHELF,
    GEM,
    RECOMMENDATIONS,
    CATALOGUE,
    RATINGS,
    WEB_SOURCES,
    SAVE_INVENTORY,
    SAVE_EXPORT,
    SAVE_IMPORT,
    SAVE_RECOVERY,
    VAULT,
    CARTMOD,
    PC_SETUP,
    PATCH,
    CARTRIDGE_TOOLS,
    SETTINGS,
    MODERN,
    PROFILES,
    JUNIOR,
    MANAGER,
}

internal enum class ProductScreen(
    val stableId: String,
    val label: String,
    val parentId: String?,
    val capability: ProductCapability,
) {
    SHELF("shelf", "Shelf", null, ProductCapability.SHELF),
    SHELF_FILTERS("shelf.filters", "Filters", "shelf", ProductCapability.SHELF),
    SHELF_COVERS("shelf.covers", "Search for covers", "shelf", ProductCapability.SHELF),
    GEM("gem", "GEM", null, ProductCapability.GEM),
    GEM_FOR_YOU("gem.for-you", "For You", "gem", ProductCapability.RECOMMENDATIONS),
    GEM_BROWSE("gem.browse", "Browse", "gem", ProductCapability.CATALOGUE),
    GEM_RATINGS("gem.my-ratings", "My Ratings", "gem", ProductCapability.RATINGS),
    GEM_SEARCH("gem.search", "Search", "gem", ProductCapability.WEB_SOURCES),
    GEM_SOURCES("gem.search.sources", "My Sources", "gem.search", ProductCapability.WEB_SOURCES),
    SAFESAVE("safesave", "SafeSave", null, ProductCapability.SAVE_INVENTORY),
    SAVE_INVENTORY("safesave.inventory", "Inventory", "safesave", ProductCapability.SAVE_INVENTORY),
    SAVE_EXPORT("safesave.export", "Export saves", "safesave", ProductCapability.SAVE_EXPORT),
    SAVE_IMPORT("safesave.import", "Import saves", "safesave", ProductCapability.SAVE_IMPORT),
    SAVE_RECOVERY("safesave.recovery", "Recovery", "safesave", ProductCapability.SAVE_RECOVERY),
    VAULT("safesave.vault", "Vault", "safesave", ProductCapability.VAULT),
    VAULT_ACCOUNT("safesave.vault.account", "Account & access", "safesave.vault", ProductCapability.VAULT),
    VAULT_SNAPSHOTS("safesave.vault.snapshots", "Snapshots", "safesave.vault", ProductCapability.VAULT),
    VAULT_STORAGE("safesave.vault.storage", "Storage & retention", "safesave.vault", ProductCapability.VAULT),
    VAULT_RECOVERY("safesave.vault.recovery", "Recovery", "safesave.vault", ProductCapability.VAULT),
    CARTMOD("cartmod", "CARTMOD", null, ProductCapability.CARTMOD),
    PC_SETUP("cartmod.pc-setup", "Set up a PC game", "cartmod", ProductCapability.PC_SETUP),
    PC_INSTALLATIONS("cartmod.installations", "Manage installations", "cartmod", ProductCapability.PC_SETUP),
    PATCH_STUDIO("cartmod.patch-studio", "Mods & patches", "cartmod", ProductCapability.PATCH),
    CARTRIDGE_TOOLS("cartmod.tools", "Cartridge tools", "cartmod", ProductCapability.CARTRIDGE_TOOLS),
    FAQS("cartmod.faqs", "FAQs", "cartmod", ProductCapability.CARTMOD),
    SETTINGS("settings", "Settings", null, ProductCapability.SETTINGS),
    APPEARANCE("settings.appearance", "Appearance", "settings", ProductCapability.SETTINGS),
    CONTROLLERS("settings.controllers", "Controllers", "settings", ProductCapability.SETTINGS),
    MODERN("settings.controllers.modern", "Modern", "settings.controllers", ProductCapability.MODERN),
    DISPLAY("settings.display", "Display & gameplay", "settings", ProductCapability.SETTINGS),
    LIBRARY("settings.library", "Shelf & library", "settings", ProductCapability.SETTINGS),
    FAMILY("settings.family", "Profiles & family", "settings", ProductCapability.PROFILES),
    HELP("settings.help", "Help & guides", "settings", ProductCapability.SETTINGS),
    SYSTEM("settings.system", "System & advanced", "settings", ProductCapability.SETTINGS),
    PLAYERS("players", "Who's playing?", null, ProductCapability.PROFILES),
    PLAYROOM("junior.playroom", "My Playroom", null, ProductCapability.JUNIOR),
    GROWN_UPS("junior.grown-ups", "Grown-Ups", null, ProductCapability.MANAGER),
}

internal object ProductFramework {
    val adultOrder =
        listOf(
            ProductScreen.SHELF,
            ProductScreen.GEM,
            ProductScreen.SAFESAVE,
            ProductScreen.CARTMOD,
            ProductScreen.SETTINGS,
        )

    // Callers must supply actual mounted routes, verified services and authorised
    // session. This registry does not grant access at the service boundary.
    fun adultDestinations(
        mounted: Set<ProductScreen>,
        verifiedCapabilities: Set<ProductCapability>,
        adultAllowed: Boolean,
    ): List<ProductScreen> =
        if (!adultAllowed) {
            emptyList()
        } else {
            adultOrder.filter { it in mounted && it.capability in verifiedCapabilities }
        }
}
