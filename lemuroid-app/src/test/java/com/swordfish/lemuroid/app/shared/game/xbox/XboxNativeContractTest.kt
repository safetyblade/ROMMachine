package com.swordfish.lemuroid.app.shared.game.xbox

import android.view.KeyEvent
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipFile

class XboxNativeContractTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun hardwareButtonsAndAnalogTriggersCanBeHeldTogether() {
        val pad = XboxPadState()
        assertTrue(pad.key(KeyEvent.KEYCODE_BUTTON_A, true))
        pad.key(KeyEvent.KEYCODE_BUTTON_R1, true)
        pad.key(KeyEvent.KEYCODE_BUTTON_L1, true)
        pad.key(KeyEvent.KEYCODE_BUTTON_START, true)
        pad.motion { axis ->
            when (axis) {
                MotionEvent.AXIS_X -> 1f
                MotionEvent.AXIS_Y -> -.5f
                MotionEvent.AXIS_LTRIGGER -> .5f
                MotionEvent.AXIS_RTRIGGER -> 1f
                else -> 0f
            }
        }
        assertEquals(16, pad.buttons)
        assertEquals(255L, pad.analog and 255)
        assertEquals(255L, (pad.analog ushr 32) and 255)
        assertEquals(255L, (pad.analog ushr 40) and 255)
        assertEquals(127L, (pad.analog ushr 48) and 255)
        assertEquals(255L, (pad.analog ushr 56) and 255)
        assertEquals(32767, pad.lx)
        assertTrue(pad.ly > 0)
        pad.clear()
        assertEquals(0, pad.buttons)
        assertEquals(0L, pad.analog)
        assertEquals(0, pad.lx)
        assertFalse(pad.key(KeyEvent.KEYCODE_VOLUME_UP, true))
    }

    @Test fun dpadHatAndKeyReleaseDoNotCancelOtherHeldButtons() {
        val pad = XboxPadState()
        pad.key(KeyEvent.KEYCODE_DPAD_UP, true)
        pad.key(KeyEvent.KEYCODE_BUTTON_SELECT, true)
        pad.motion { if (it == MotionEvent.AXIS_HAT_X) 1f else 0f }
        assertEquals(1 or 8 or 32, pad.buttons)
        pad.key(KeyEvent.KEYCODE_DPAD_UP, false)
        assertEquals(8 or 32, pad.buttons)
    }

    @Test fun upgradingEngineKeepsHddAndMakesRecoverableBackup() {
        val files = temporary.newFolder()
        val revisionA = "a".repeat(40)
        val revisionB = "b".repeat(40)
        val hdd = XboxStorage.prepare(files, revisionA)
        val save = java.io.File(hdd, "Partition1/UDATA/12345678/slot/progress.bin")
        save.parentFile!!.mkdirs()
        save.writeText("authentic-game-data-fixture")
        val identity = java.io.File(hdd, "eeprom.bin").apply { writeText("console-identity-fixture") }
        assertEquals(hdd, XboxStorage.prepare(files, revisionB))
        assertEquals("authentic-game-data-fixture", save.readText())
        assertEquals("console-identity-fixture", identity.readText())
        val backups = java.io.File(files, "native/xbox/backups").listFiles()!!
        assertEquals(1, backups.size)
        ZipFile(backups.single()).use { zip ->
            assertEquals(
                save.readText(),
                zip.getInputStream(
                    zip.getEntry("Partition1/UDATA/12345678/slot/progress.bin"),
                ).bufferedReader().readText(),
            )
            assertEquals(
                identity.readText(),
                zip.getInputStream(zip.getEntry("eeprom.bin")).bufferedReader().readText(),
            )
        }
        XboxStorage.prepare(files, revisionB)
        assertEquals(1, java.io.File(files, "native/xbox/backups").listFiles()!!.size)
    }

    @Test fun sameSourceRevisionWithNewBinaryGetsSeparateUpgradeBackup() {
        val files = temporary.newFolder()
        val first = "a".repeat(40) + "-" + "1".repeat(12)
        val second = "a".repeat(40) + "-" + "2".repeat(12)
        val third = "a".repeat(40) + "-" + "3".repeat(12)
        val hdd = XboxStorage.prepare(files, first)
        java.io.File(hdd, "save").writeText("progress-1")
        XboxStorage.prepare(files, second)
        java.io.File(hdd, "save").writeText("progress-2")
        XboxStorage.prepare(files, third)
        assertEquals(2, java.io.File(files, "native/xbox/backups").listFiles()!!.size)
        XboxStorage.prepare(files, second)
        java.io.File(hdd, "save").writeText("progress-3")
        XboxStorage.prepare(files, third)
        assertEquals(4, java.io.File(files, "native/xbox/backups").listFiles()!!.size)
    }

    @Test fun touchDirectionsDoNotOverwriteHeldFaceButtonsAndUseCorrectUpAxis() {
        val pad = XboxPadState()
        pad.key(KeyEvent.KEYCODE_BUTTON_A, true)
        pad.direction(0, 1f, 1f)
        pad.direction(1, -.5f, .8f)
        pad.direction(2, .4f, -.7f)
        assertEquals(1 or 8, pad.buttons)
        assertEquals(255L, pad.analog and 255)
        assertTrue(pad.lx < 0 && pad.ly > 0 && pad.rx > 0 && pad.ry < 0)
        pad.direction(0, 0f, 0f)
        assertEquals(0, pad.buttons)
        assertEquals(255L, pad.analog and 255)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsafeEngineRevisionCannotAlterStorage() {
        XboxStorage.prepare(temporary.newFolder(), "../engine")
    }
}
