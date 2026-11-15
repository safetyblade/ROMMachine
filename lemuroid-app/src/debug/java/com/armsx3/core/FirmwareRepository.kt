package com.armsx3.core

object FirmwareRepository {
    @Volatile var installed: String? = null

    @Volatile var compiled: String? = null

    @JvmStatic fun onFirmwareInstalled(version: String) {
        installed = version
    }

    @JvmStatic fun onFirmwareCompiled(version: String) {
        compiled = version
    }
}
