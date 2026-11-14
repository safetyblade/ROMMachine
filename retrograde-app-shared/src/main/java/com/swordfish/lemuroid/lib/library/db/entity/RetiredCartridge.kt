package com.swordfish.lemuroid.lib.library.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * C2A: durable identity for a cartridge removed by a fully successful scan.
 * A retired cartridge is NOT a launchable Game. Retaining its old source
 * and frozen save aliases does not grant access to a missing ROM.
 */
@Entity(
    tableName = "retired_cartridges",
    indices = [Index("systemId"), Index("fileUri")],
)
data class RetiredCartridge(
    @PrimaryKey val cartridgeId: String,
    val formerGameId: Int,
    val fileName: String,
    val fileUri: String,
    val title: String,
    val systemId: String,
    val developer: String?,
    val coverFrontUrl: String?,
    val lastPlayedAt: Long?,
    val isFavorite: Boolean,
    val customName: String?,
    val customCoverUri: String?,
    val legacyFileName: String?,
    val legacySourceUri: String?,
    val retiredAt: Long,
) {
    fun restore(candidate: Game): Game {
        require(candidate.systemId == systemId) { "Cartridge system mismatch" }
        return candidate.copy(
            cartridgeId = cartridgeId,
            title = title,
            developer = developer,
            coverFrontUrl = coverFrontUrl ?: candidate.coverFrontUrl,
            lastPlayedAt = lastPlayedAt,
            isFavorite = isFavorite,
            customName = customName,
            customCoverUri = customCoverUri,
            legacyFileName = legacyFileName,
            legacySourceUri = legacySourceUri,
        )
    }

    fun displayName(): String = customName ?: title

    companion object {
        fun fromGame(
            game: Game,
            retiredAt: Long,
        ) =
            RetiredCartridge(
                cartridgeId = game.cartridgeId,
                formerGameId = game.id,
                fileName = game.fileName,
                fileUri = game.fileUri,
                title = game.title,
                systemId = game.systemId,
                developer = game.developer,
                coverFrontUrl = game.coverFrontUrl,
                lastPlayedAt = game.lastPlayedAt,
                isFavorite = game.isFavorite,
                customName = game.customName,
                customCoverUri = game.customCoverUri,
                legacyFileName = game.legacyFileName,
                legacySourceUri = game.legacySourceUri,
                retiredAt = retiredAt,
            )
    }
}
