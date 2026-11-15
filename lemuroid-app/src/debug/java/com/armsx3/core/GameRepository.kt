package com.armsx3.core

/** Native installer callback ABI. CARTDECK indexes the actual installed PARAM.SFO files afterwards. */
class GameInfo(val path: String, val name: String, val iconPath: String, val flags: Int)

object GameRepository {
    @Volatile var installed: List<GameInfo> = emptyList()

    @JvmStatic fun add(
        games: Array<GameInfo>,
        progressId: Long,
    ) {
        installed = games.take(10000)
    }
}
