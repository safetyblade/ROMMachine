package com.swordfish.lemuroid.app.mobile.feature.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverArtworkSearchTest {
    @Test fun onlyPublicWikimediaHttpsImageUrlsAreAccepted() {
        assertTrue(CoverArtworkSearch.isImageUrl("https://upload.wikimedia.org/wikipedia/en/a/ab/cover.jpg"))
        listOf(
            "http://upload.wikimedia.org/cover.jpg",
            "https://upload.wikimedia.org.attacker.example/cover.jpg",
            "https://upload.wikimedia.org:8443/cover.jpg",
            "https://user@upload.wikimedia.org/cover.jpg",
            "https://127.0.0.1/cover.jpg",
            "file:///data/cover.jpg",
        ).forEach { assertFalse(it, CoverArtworkSearch.isImageUrl(it)) }
    }
}
