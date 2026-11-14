package com.swordfish.lemuroid.app.shared.game.ps2next

import java.io.File
import java.util.UUID

/**
 * CARTDECK's separate PS2-NEXT data namespace. This never touches Play! memory cards.
 * The per-cartridge root is stable across changes to the game title, URI and shelf artwork.
 */
object Armsx2Storage {
    data class Paths(
        val globalRoot: File,
        val bios: File,
        val gameRoot: File,
        val resources: File,
        val memcards: File,
    )

    fun paths(
        filesDir: File,
        cartridgeId: String,
    ): Paths {
        require(cartridgeId.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))) {
            "Expected immutable UUID cartridge identity"
        }
        val id = UUID.fromString(cartridgeId).toString()
        val globalRoot = File(filesDir, "native/ps2/armsx2")
        val gameRoot = File(globalRoot, "cartridges/$id")
        return Paths(
            globalRoot = globalRoot,
            bios = File(globalRoot, "bios"),
            gameRoot = gameRoot,
            resources = File(gameRoot, "resources"),
            memcards = File(gameRoot, "memcards"),
        )
    }

    /**
     * Does not create placeholders or "repair" missing BIOS/resources.
     * The ARMSX2 Android core needs real source-licensed resources and a user's own PS2 BIOS.
     */
    fun missingRequirements(
        paths: Paths,
        gameImage: File,
    ): List<String> =
        buildList {
            if (!gameImage.isFile || gameImage.length() == 0L ||
                gameImage.extension.lowercase() !in setOf("iso", "chd")
            ) {
                add("DISC")
            }
            if (paths.bios.listFiles()?.none { it.isFile && it.length() > 0L } != false) {
                add("BIOS")
            }
            if (paths.resources.listFiles()?.none { it.isFile || it.isDirectory } != false) {
                add("RESOURCES")
            }
        }

    fun createPrivateDirectories(paths: Paths) {
        for (directory in listOf(paths.globalRoot, paths.bios, paths.gameRoot, paths.memcards)) {
            check(directory.isDirectory || directory.mkdirs()) {
                "Cannot create app-private ARMSX2 storage"
            }
        }
    }
}
