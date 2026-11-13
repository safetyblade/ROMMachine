package com.swordfish.lemuroid.app.shared.game.xbox360

import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import java.io.File

/** Same verified package lifecycle as hakuX, in a distinct immutable engine namespace. */
object Xbox360Engine {
    const val CORE = "libcartdeck_xenia.so"
    const val MANIFEST = "cartdeck/xenia.properties"
    const val REVISION = "779680ade075d2d2a4c589c6a8d57a8e26c116c0"

    fun store(files: File) =
        XboxEngineStore(
            File(files, "native-engines/xbox360"),
            CORE,
            setOf(CORE),
            "xenia.properties",
        )

    fun data(files: File): File {
        val root = File(files, "native/xbox360")
        for (name in listOf("content", "cache", "config", "logs", "staging")) {
            val directory = File(root, name)
            check(directory.isDirectory || directory.mkdirs()) { "Xbox 360 storage is unavailable" }
        }
        return root
    }
}
