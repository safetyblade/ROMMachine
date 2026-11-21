package com.swordfish.lemuroid.lib.saves.safesave

import java.io.File

/**
 * SAVE-A01 minimal in-app access point for PSP SafeSave.
 *
 * This is the single entry the app UI calls to (a) find the verified PSP save
 * snapshots belonging to one cartridge UUID and (b) initiate capture/restore.
 * It adds no startup work: discovery happens only when this is called, and
 * only for the one explicitly named savedata group.
 */
class SafeSavePspAccess(private val store: SafeSaveStore, private val index: SafeSaveIndex) {
    /** Verified snapshots for exactly this cartridge, newest last. No filename/title matching. */
    fun verifiedSavesForCartridge(cartridgeUuid: String): List<SafeSaveSnapshotRecord> {
        val uuid = requireCartridgeUuid(cartridgeUuid)
        return index.snapshotsForCartridge(uuid)
            .filter { it.platform == PspSavedataAdapter.PLATFORM }
            .filter { it.verificationState == SafeSaveVerificationState.VERIFIED && store.verify(it.snapshotId) }
    }

    fun capture(
        modernSaveRoot: File,
        groupName: String,
        cartridgeUuid: String,
        engineVersion: String,
        engineQuiesced: Boolean,
    ): SafeSaveStore.CaptureResult =
        store.capture(modernSaveRoot, groupName, cartridgeUuid, engineVersion, engineQuiesced)

    fun restore(
        snapshotId: String,
        modernSaveRoot: File,
        engineQuiesced: Boolean,
    ): SafeSaveStore.RestoreResult = store.restore(snapshotId, modernSaveRoot, engineQuiesced)
}
