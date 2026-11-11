package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.app.shared.game.PlayStationCoreManifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PlayStationCoreManifestTest {
    private val binary = "test core bytes".toByteArray()
    private val hash =
        MessageDigest.getInstance("SHA-256").digest(binary)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun manifest(
        revision: String = PlayStationCoreManifest.SOURCE_REVISION,
        digest: String = hash,
    ) = "revision=$revision\ncore.sha256=$digest\n".byteInputStream()

    @Test
    fun exactPinnedBinaryIsAccepted() {
        assertTrue(PlayStationCoreManifest.verify(manifest(), binary.inputStream()))
    }

    @Test
    fun tamperedBinaryOrRevisionCannotBeSelected() {
        assertFalse(PlayStationCoreManifest.verify(manifest(), (binary + 1.toByte()).inputStream()))
        assertFalse(PlayStationCoreManifest.verify(manifest(revision = "other"), binary.inputStream()))
        assertFalse(PlayStationCoreManifest.verify(manifest(digest = ""), binary.inputStream()))
    }
}
