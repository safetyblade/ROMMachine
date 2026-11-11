package com.swordfish.lemuroid.lib.saves

/** PCSX ReARMed's libretro SRAM is one raw 128 KiB PlayStation memory card. */
object PlayStationMemoryCard {
    const val SIZE = 128 * 1024

    fun isRawCard(data: ByteArray): Boolean =
        data.size == SIZE && data[0] == 'M'.code.toByte() && data[1] == 'C'.code.toByte()
}
