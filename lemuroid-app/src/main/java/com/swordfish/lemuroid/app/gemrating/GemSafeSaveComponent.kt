package com.swordfish.lemuroid.app.gemrating

import kotlinx.serialization.Serializable

/**
 * Optional personal-metadata component for the future SafeSave PLAYER/ALL_PLAYERS archive.
 * The archive coordinator owns authorization, quiescence, manifest and group transaction.
 * This adapter never accesses engine files or pretends to be a complete .cartsave archive.
 */
object GemSafeSaveComponent {
    @Serializable
    data class Descriptor(
        val componentType: String = "cartdeck.gem.profile",
        val schemaVersion: Int = 1,
        val ownerPlayerId: String,
        val path: String,
        val byteLength: Int,
        val sha256: String,
    )

    data class Export(val descriptor: Descriptor, val bytes: ByteArray)

    fun capture(
        repository: GemRatingRepository,
        session: GemRatingRepository.Session,
    ): Export {
        val snapshot = repository.snapshot(session)
        val bytes = GemProfileBackup.encode(snapshot)
        return Export(
            Descriptor(
                ownerPlayerId = session.profileId,
                path = "components/gem/${session.profileId}/profile.gembackup",
                byteLength = bytes.size,
                sha256 = GemProfileBackup.digest(bytes),
            ),
            bytes,
        )
    }

    fun prepare(
        repository: GemRatingRepository,
        target: GemRatingRepository.Session,
        descriptor: Descriptor,
        bytes: ByteArray,
    ): GemProfileBackup.Preview {
        GemRatingRepository.requireUuid(descriptor.ownerPlayerId)
        require(descriptor.componentType == "cartdeck.gem.profile" && descriptor.schemaVersion == 1)
        require(descriptor.path == "components/gem/${descriptor.ownerPlayerId}/profile.gembackup")
        require(bytes.size == descriptor.byteLength && bytes.size <= GemProfileBackup.MAX_BYTES)
        require(GemProfileBackup.digest(bytes) == descriptor.sha256)
        val source = GemProfileBackup.decode(bytes)
        require(source.profileId == descriptor.ownerPlayerId)
        return repository.previewRestore(target, source)
    }

    fun apply(
        repository: GemRatingRepository,
        target: GemRatingRepository.Session,
        preview: GemProfileBackup.Preview,
        replaceConflicts: Boolean,
        restorePreferences: Boolean,
        allowCopy: Boolean,
    ): GemRatingRepository.Snapshot =
        repository.restore(target, preview, replaceConflicts, restorePreferences, allowCopy)
}
