package com.swordfish.lemuroid.app.shared.input

/**
 * The active Wii core's advertised controller inventory is authoritative.
 * A physical Bluetooth pad never proves a Wii title accepts Classic mode or
 * supplies an IR pointer, Nunchuk motion, accelerometer or real Wii Remote.
 */
object WiiControllerCompatibility {
    enum class Variant {
        CLASSIC,
        SIDEWAYS_REMOTE,
        REMOTE_WITH_NUNCHUK,
        OTHER,
    }

    fun variant(coreControllerName: String): Variant {
        val name = coreControllerName.lowercase()
        return when {
            "classic" in name -> Variant.CLASSIC
            "nunchuk" in name || "nunchuck" in name -> Variant.REMOTE_WITH_NUNCHUK
            "sideways" in name || "horizontal" in name -> Variant.SIDEWAYS_REMOTE
            else -> Variant.OTHER
        }
    }

    /** Saved exact cartridge/controller choice wins; unknown games stay at core default. */
    fun selectVerifiedOrFallback(
        supportedNames: List<String>,
        previouslySelectedName: String?,
    ): String? {
        if (supportedNames.isEmpty()) return null
        return supportedNames.firstOrNull { it == previouslySelectedName }
            ?: supportedNames.first()
    }

    fun help(variant: Variant): String =
        when (variant) {
        Variant.CLASSIC -> "Use this where the Wii game itself supports Classic Controller input."
        Variant.SIDEWAYS_REMOTE -> "Sideways Remote maps directional and face inputs; not all games support it."
        Variant.REMOTE_WITH_NUNCHUK ->
            "Nunchuk buttons and sticks require explicit original-input mapping. IR/motion are separate capabilities."
        Variant.OTHER ->
            "This core profile may need original Wii Remote buttons, pointer or motion. Check game controls."
    }
}
