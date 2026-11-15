package com.armsx3.core

/** Exact pinned native callback ABI; installer and compiler progress are not gameplay evidence. */
object ProgressRepository {
    @Volatile var message: String = ""

    @Volatile var value: Long = 0

    @Volatile var maximum: Long = 0

    @JvmStatic fun onProgressEvent(
        id: Long,
        current: Long,
        max: Long,
        text: String?,
    ): Boolean {
        value = current
        maximum = max
        if (text != null) message = text.take(1024)
        return true
    }
}
