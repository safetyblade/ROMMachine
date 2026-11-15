package com.swordfish.lemuroid.app.ps3

import com.swordfish.lemuroid.lib.storage.ps3.Ps3Metadata
import com.swordfish.lemuroid.lib.storage.ps3.Ps3Storage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Ps3StorageTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun realVirtualUsersAndEnginesNeverShareSavedata() {
        val a = Ps3Storage(temporary.root, "armsx3")
        val b = Ps3Storage(temporary.root, "rpcs3-libretro")
        val first = a.create("Player One")
        val second = a.create("Player Two")
        b.create("Separate Engine")
        assertNotEquals(a.guest, b.guest)
        assertNotEquals(first.id, second.id)
        a.select(second.id)
        a.rename(first.id, "Renamed")
        val reopened = Ps3Storage(temporary.root, "armsx3")
        assertEquals(second.id, reopened.profiles().single { it.selected }.id)
        assertEquals("Renamed", reopened.profiles().single { it.id == first.id }.name)
        assertEquals("Separate Engine", b.profiles().single().name)
    }

    @Test fun restoreBacksUpOriginalAndRejectsTraversalWithoutDeletingSave() {
        val storage = Ps3Storage(temporary.root, "armsx3")
        val user = storage.create("Player").id
        val path = File(storage.guest, "dev_hdd0/home/$user/savedata/BLUS12345SAVE").apply { mkdirs() }
        File(path, "PARAM.SFO").writeText("original-metadata")
        File(path, "PARAM.PFD").writeText("original-signature")
        File(path, "PROGRESS").writeText("level 5")
        val backup = ByteArrayOutputStream()
        storage.export(user, "BLUS12345SAVE", backup)
        File(path, "PROGRESS").writeText("level 9")
        storage.restore(user, "BLUS12345SAVE", ByteArrayInputStream(backup.toByteArray()), true)
        assertEquals("level 5", File(path, "PROGRESS").readText())
        assertTrue(storage.backups.walkTopDown().any { it.name == "PROGRESS" && it.readText() == "level 9" })
        val malicious = ByteArrayOutputStream()
        ZipOutputStream(malicious).use {
            it.putNextEntry(ZipEntry("BLUS12345SAVE/../../escape"))
            it.write(1)
            it.closeEntry()
        }
        assertThrows(IllegalArgumentException::class.java) {
            storage.restore(user, "BLUS12345SAVE", ByteArrayInputStream(malicious.toByteArray()), true)
        }
        assertEquals("level 5", File(path, "PROGRESS").readText())
        storage.deleteSave(user, "BLUS12345SAVE", true)
        assertFalse(path.exists())
        assertTrue(storage.backups.walkTopDown().any { it.name == "PROGRESS" })
    }

    @Test fun existingNativeUserIsDiscoveredAndNeverOverwrittenByCreatingAnother() {
        val storage = Ps3Storage(temporary.root, "armsx3")
        val original = File(storage.guest, "dev_hdd0/home/00000001").apply { mkdirs() }
        File(original, "localusername").writeText("Original User")
        assertEquals("Original User", storage.profiles().single().name)
        val next = storage.create("New User")
        assertEquals("00000002", next.id)
        assertEquals("Original User", File(original, "localusername").readText())
    }

    @Test fun sfoRejectsPspAndVitaAndRetainsPs3TitleIdentity() {
        assertNull(Ps3Metadata.sfo(byteArrayOf()))
        assertNull(Ps3Metadata.sfo(sfo("UG")))
        assertEquals("BLUS12345", Ps3Metadata.sfo(sfo("DG"))?.id)
        assertEquals("Homebrew Test", Ps3Metadata.sfo(sfo("HG"))?.name)
        val malformed = sfo("DG").also { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putInt(16, Int.MAX_VALUE) }
        assertNull(Ps3Metadata.sfo(malformed))
    }

    @Test fun failedFirmwareReplacementRestoresFlashWithoutChangingUserData() {
        val storage = Ps3Storage(temporary.root, "armsx3")
        val user = storage.create("Player").id
        val flash = File(storage.guest, "dev_flash/sys/external/liblv2.sprx")
        flash.parentFile!!.mkdirs()
        flash.writeText("working firmware")
        val save = File(storage.guest, "dev_hdd0/home/$user/progress").apply { writeText("retained") }
        storage.beginFirmwareReplacement()
        assertFalse(storage.firmwareInstalled())
        storage.markFirmwareInstallStarted()
        assertTrue(storage.firmwareInstallInterrupted())
        flash.parentFile!!.mkdirs()
        flash.writeText("partial replacement")
        storage.finishFirmwareReplacement(false)
        assertEquals("working firmware", flash.readText())
        assertEquals("retained", save.readText())
        assertFalse(storage.hasPendingFirmwareReplacement())
    }

    @Test fun licencesRequireOriginalSizeAndStayWithTheirUserAndEngine() {
        val storage = Ps3Storage(temporary.root, "armsx3")
        val first = storage.create("One").id
        val second = storage.create("Two").id
        val name = "UP0001-BLUS12345_00-CONTENT000000001.rap"
        storage.importRap(first, name, ByteArray(16) { it.toByte() })
        assertTrue(File(storage.guest, "dev_hdd0/home/$first/exdata/$name").isFile)
        assertFalse(File(storage.guest, "dev_hdd0/home/$second/exdata/$name").exists())
        assertThrows(IllegalArgumentException::class.java) { storage.importRap(first, name, ByteArray(17)) }
        assertThrows(IllegalArgumentException::class.java) { storage.importRap(first, "../$name", ByteArray(16)) }
    }

    private fun sfo(category: String): ByteArray {
        val entries = listOf("TITLE_ID" to "BLUS12345", "TITLE" to "Homebrew Test", "CATEGORY" to category)
        val keys = entries.joinToString("") { it.first + "\u0000" }.toByteArray()
        val values = entries.joinToString("") { it.second + "\u0000" }.toByteArray()
        val keyStart = 20 + 16 * entries.size
        val valueStart = keyStart + keys.size
        val b = ByteBuffer.allocate(valueStart + values.size).order(ByteOrder.LITTLE_ENDIAN)
        b.putInt(0x46535000).putInt(0x101).putInt(keyStart).putInt(valueStart).putInt(entries.size)
        var k = 0
        var v = 0
        entries.forEach { (key, value) ->
            b.putShort(k.toShort()).putShort(0x204).putInt(value.length + 1).putInt(value.length + 1).putInt(v)
            k += key.length + 1
            v += value.length + 1
        }
        b.put(keys).put(values)
        return b.array()
    }
}
