package com.swordfish.lemuroid.app.shared.game.xbox360

import android.app.Application
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class Xbox360ProfilesTest {
    private val fixture = ByteArray(0x194) { (it % 251).toByte() }

    @Test fun localIdentityAndAccountSurviveReopenWithoutReplacingSaves() {
        val root = Files.createTempDirectory("x360-profile").toFile()
        try {
            val first = Xbox360Profiles.provision(root, fixture, Xbox360Profiles.hash(fixture))
            assertTrue(first.created)
            assertTrue(first.xuid.matches(Regex("E0300000[0-7][0-9A-F]{7}")))
            val save =
                File(root, "content/${first.xuid}/454107D9/00000001/progress").apply {
                    parentFile!!.mkdirs()
                    writeText("retained")
                }
            val second = Xbox360Profiles.provision(root, fixture, Xbox360Profiles.hash(fixture))
            assertEquals(first.xuid, second.xuid)
            assertFalse(second.created)
            assertEquals("retained", save.readText())
            assertArrayEquals(fixture, Xbox360Profiles.account(root, first.xuid).readBytes())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun existingNativeProfileAndConfigAreAdoptedWithoutRewriting() {
        val root = Files.createTempDirectory("x360-existing").toFile()
        try {
            val xuid = "E030000012345678"
            Xbox360Profiles.account(root, xuid).apply {
                parentFile!!.mkdirs()
                writeBytes(fixture)
            }
            val config =
                File(root, "config/xenia.config.toml").apply {
                    parentFile!!.mkdirs()
                    writeText("[Profiles]\nlogged_profile_slot_0_xuid = \"$xuid\"\n[GPU]\nuntouched = true\n")
                }
            val original = config.readText()
            val selected = Xbox360Profiles.provision(root, fixture, Xbox360Profiles.hash(fixture))
            assertEquals(xuid, selected.xuid)
            assertFalse(selected.created)
            assertEquals(original, config.readText())
            assertArrayEquals(fixture, Xbox360Profiles.account(root, xuid).readBytes())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun missingBoundProfileDoesNotSilentlyGenerateANewSaveIdentity() {
        val root = Files.createTempDirectory("x360-missing").toFile()
        try {
            val first = Xbox360Profiles.provision(root, fixture, Xbox360Profiles.hash(fixture))
            Xbox360Profiles.account(root, first.xuid).delete()
            assertTrue(
                runCatching { Xbox360Profiles.provision(root, fixture, Xbox360Profiles.hash(fixture)) }.isFailure,
            )
            assertEquals(first.xuid, File(root, "profile/player-one.xuid").readText())
        } finally {
            root.deleteRecursively()
        }
    }
}
