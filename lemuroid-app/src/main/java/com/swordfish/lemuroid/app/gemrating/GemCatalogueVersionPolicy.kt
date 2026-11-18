package com.swordfish.lemuroid.app.gemrating

/** Stable catalogue IDs cannot be reassigned, including IDs retired in earlier releases. */
internal object GemCatalogueVersionPolicy {
    private fun identity(release: GemCataloguePackage.Release): String =
        GemProfileBackup.digest(
            listOf(release.sourceId, release.sourceRecordId, release.systemId, release.title)
                .joinToString("") { "${it.length}:$it" }.encodeToByteArray(),
        )

    fun extend(
        history: Map<String, String>,
        releases: List<GemCataloguePackage.Release>,
    ): Map<String, String> {
        val next = history.toMutableMap()
        for (release in releases) {
            val fingerprint = identity(release)
            val existing = next.putIfAbsent(release.releaseId, fingerprint)
            require(existing == null || existing == fingerprint) { "Catalogue release identity reassignment refused" }
        }
        require(next.size <= 200_000) { "Catalogue identity history exceeds limit" }
        return next.toSortedMap()
    }
}
