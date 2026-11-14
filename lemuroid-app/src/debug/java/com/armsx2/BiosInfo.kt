package com.armsx2

/**
 * Exact data type requested by pinned ARMSX2 JNI BiosInfo constructor:
 * (int version, int region, String description, String zone).
 */
class BiosInfo(
    @JvmField val version: Int,
    @JvmField val region: Int,
    @JvmField val description: String,
    @JvmField val zone: String,
)
