package com.swordfish.lemuroid.app.psp

import com.swordfish.lemuroid.app.shared.game.PPSSPPBundleManifest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PPSSPPBundleManifestTest {
    private fun manifest(bytes: ByteArray): PPSSPPBundleManifest {
        val hash =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return PPSSPPBundleManifest(
            "revision=expected\ncore.sha256=$hash\nassets.sha256=$hash\ncompat.sha256=$hash\n".byteInputStream(),
            "expected",
        )
    }

    @Test
    fun rejectsMismatchedCoreAndAssetContents() {
        val manifest = manifest("matching".toByteArray())
        manifest.verify("core.sha256", "matching".byteInputStream())
        assertTrue(runCatching { manifest.verify("assets.sha256", "different".byteInputStream()) }.isFailure)
    }

    @Test
    fun refusesDifferentSourceRevisionOrIncompleteIntegrityManifest() {
        assertTrue(runCatching { PPSSPPBundleManifest("revision=old".byteInputStream(), "expected") }.isFailure)
        assertTrue(runCatching { PPSSPPBundleManifest("revision=expected".byteInputStream(), "expected") }.isFailure)
    }
}
