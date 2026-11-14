package com.swordfish.lemuroid.app.shared.game.wiiu

import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Cartridge identity owns NAND; disposable media staging never owns saves. */
object WiiuStorage {
    fun cartridgeRoot(
        files: File,
        cartridgeId: String,
    ): File = File(files, "native/wiiu/cartridges/${identity(cartridgeId)}")

    fun stagingRoot(
        cache: File,
        cartridgeId: String,
        sourceUri: String,
    ): File {
        val digest =
            MessageDigest.getInstance("SHA-256").digest(sourceUri.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 255) }
        return File(cache, "native/wiiu/${identity(cartridgeId)}/$digest")
    }

    fun component(name: String): String {
        require(
            name.isNotBlank() && name !in setOf(".", "..") &&
                name.none { it == '/' || it == '\\' || it.code < 32 },
        ) { "Unsafe Wii U content filename" }
        return name
    }

    private fun identity(id: String): String {
        require(id.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))) {
            "Wii U requires an immutable cartridge UUID"
        }
        return UUID.fromString(id).toString()
    }
}
