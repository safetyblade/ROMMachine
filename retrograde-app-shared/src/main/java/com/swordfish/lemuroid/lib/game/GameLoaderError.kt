package com.swordfish.lemuroid.lib.game

class GameLoaderException(val error: GameLoaderError, cause: Throwable? = null) :
    RuntimeException("Game Loader error: $error", cause)

sealed class GameLoaderError {
    object GLIncompatible : GameLoaderError()

    object Generic : GameLoaderError()

    data class DiscFamily(val recovery: String) : GameLoaderError()

    object LoadCore : GameLoaderError()

    object NativeEngineDisabled : GameLoaderError()

    object LoadGame : GameLoaderError()

    object Saves : GameLoaderError()

    object UnsupportedArchitecture : GameLoaderError()

    data class MissingBiosFiles(val missingFiles: List<String>) : GameLoaderError()
}
