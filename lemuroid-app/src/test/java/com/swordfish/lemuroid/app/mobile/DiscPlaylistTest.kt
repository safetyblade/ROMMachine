package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.library.DiscPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscPlaylistTest {
    @Test
    fun standardCommentsBlankLinesBomAndSpacesPreserveDiscNames() {
        assertEquals(
            listOf("Disc 1.cue", "Disc 2.chd"),
            DiscPlaylist.parse("\uFEFF#EXTM3U\r\n\n Disc 1.cue \r\n#EXTINF:0,Second disc\nDisc 2.chd\n"),
        )
        assertTrue(
            DiscPlaylist.containsAllDiscs("Disc 1.cue\nDisc 2.chd", setOf("Disc 1.cue", "Disc 2.chd", "track.bin")),
        )
        assertFalse(DiscPlaylist.containsAllDiscs("Disc 1.cue\nDisc 2.chd", setOf("Disc 1.cue")))
    }

    @Test
    fun unsafePathsAndNestedPlaylistsAreRejected() {
        listOf(
            "../game.chd", "/game.chd", "folder/game.chd", "folder\\game.chd", "C:game.chd",
            "game.m3u", "GAME.M3U8", ".", "..", "game\u0000.chd",
        ).forEach {
            assertNull(it, DiscPlaylist.parse(it))
        }
    }

    @Test
    fun entryCountAndTextSizeAreBounded() {
        assertNull(DiscPlaylist.parse("# comments only\n\n"))
        assertEquals(99, DiscPlaylist.parse((1..99).joinToString("\n") { "disc$it.chd" })?.size)
        assertNull(DiscPlaylist.parse((1..100).joinToString("\n") { "disc$it.chd" }))
        assertNull(DiscPlaylist.parse("#" + "a".repeat(65536) + "\ngame.chd"))
    }
}
