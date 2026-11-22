package com.swordfish.lemuroid.app.mobile.feature.product

import java.net.URI
import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomebrewDirectoryTest {
    @Test fun listedSourcesAreHttpsAndRestrictedToReviewedDomains() {
        val expected = setOf("itch.io", "hh.gbdev.io", "pdroms.de", "www.gamebrew.org")
        assertEquals(6, HomebrewDirectory.links.size)
        assertEquals(
            expected,
            HomebrewDirectory.links.map { URI(it.url).host }.toSet(),
        )
        assertTrue(HomebrewDirectory.links.all { URI(it.url).scheme == "https" })
        assertEquals(
            HomebrewDirectory.links.size,
            HomebrewDirectory.links.map { it.url }.distinct().size,
        )
    }

    @Test fun keywordSearchUsesExternalSiteScopedSearch() {
        val uri = HomebrewDirectory.searchUri("PSP puzzle")!!
        assertEquals("www.google.com", uri.host)
        val term = URLDecoder.decode(uri.rawQuery.substringAfter("q="), "UTF-8")
        assertTrue(term.contains("PSP puzzle"))
        listOf("site:itch.io", "site:hh.gbdev.io", "site:pdroms.de", "site:gamebrew.org").forEach {
            assertTrue(term.contains(it))
        }
        assertTrue(term.contains("homebrew game"))
    }

    @Test fun rejectsEmptyOrControlQueriesAndStripsSearchOperators() {
        assertNull(HomebrewDirectory.searchUri(""))
        assertNull(HomebrewDirectory.searchUri("   "))
        assertNull(HomebrewDirectory.searchUri("!".repeat(4)))
        assertNull(HomebrewDirectory.searchUri("x".repeat(121)))
        assertNull(HomebrewDirectory.searchUri("test\nconsole"))
        val search = HomebrewDirectory.searchUri("PSP site:untrusted.example")
        assertNotNull(search)
        val decoded = URLDecoder.decode(search!!.rawQuery.substringAfter("q="), "UTF-8")
        assertFalse(decoded.contains("site:untrusted.example"))
        assertTrue(decoded.contains("PSP siteuntrustedexample"))
    }
}
