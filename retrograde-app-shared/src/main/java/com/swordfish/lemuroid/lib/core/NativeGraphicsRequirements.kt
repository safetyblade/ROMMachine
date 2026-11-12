package com.swordfish.lemuroid.lib.core

import com.swordfish.lemuroid.lib.library.CoreID

/** Mirrors the actual Play! libretro GLES shader/context requirement, not a generic emulator guess. */
object NativeGraphicsRequirements {
    fun supportsAdvertisedVersion(
        coreID: CoreID,
        advertisedVersion: Int,
    ): Boolean {
        // Some providers cannot report a version. Keep the native current-context check authoritative.
        if (advertisedVersion == 0) return true
        return coreID != CoreID.PLAY_MODERN || advertisedVersion >= 0x30002
    }
}
