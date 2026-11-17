package com.swordfish.lemuroid.app.mobile.feature.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GemWebsiteSourceTest {
    private val source = GemWebsiteSource(name = "Example", home = "https://example.com", category = "Games", template = "")

    @Test fun homeWebsiteRequiresNoQuery() {
        assertEquals("https://example.com", source.searchUri("")?.toASCIIString())
    }

    @Test fun searchEncodesReservedCharactersAndUnicode() {
        val configured = source.copy(template = "https://example.com/search?q={query}")
        assertNull(configured.validationError())
        assertEquals(
            "https://example.com/search?q=Pok%C3%A9mon%20%26%20a%2Fb",
            configured.searchUri("Pokémon & a/b")?.toASCIIString(),
        )
    }

    @Test fun rejectsOffHostTemplatesAndInvalidSchemes() {
        listOf(
            "https://example.org/search?q={query}",
            "http://example.com/search?q={query}",
            "https://example.com/{query}",
            "https://example.com/search?{query}=x",
            "https://example.com/search?q={query}&q2={query}",
            "https://example.com:8080/search?q={query}",
            "https://example.com/search?q={query}#fragment",
        ).forEach { invalid ->
            assertNotNull(invalid, source.copy(template = invalid).validationError())
        }
        listOf("file:///data/local", "https://127.0.0.1", "https://test.local", "https://a@example.com").forEach {
            assertNotNull(it, source.copy(home = it).validationError())
        }
    }
}
