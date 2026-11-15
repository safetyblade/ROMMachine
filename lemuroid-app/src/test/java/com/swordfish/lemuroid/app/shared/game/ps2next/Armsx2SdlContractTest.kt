package com.swordfish.lemuroid.app.shared.game.ps2next

import android.app.Application
import kr.co.iefriends.pcsx2.Armsx2JniContract
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class Armsx2SdlContractTest {
    private fun fixture() = JSONObject(javaClass.getResource("/ps2-sdl-contract.json")!!.readText())

    @Test
    fun completePinnedNativeRegistrationAndCallbackContractMatchesCompiledClasses() {
        val source = fixture()
        assertEquals("7a999782377e61fb96eca65e476b5ec0d182058e", source.getString("armsx2_pin"))
        val expected = source.getJSONArray("legacy").let { rows -> List(rows.length()) { rows.getString(it) } }
        assertEquals(expected, Armsx2JniContract.pinnedEntries().toList())
        Armsx2JniContract.verify()
    }

    @Test
    fun allExistingModernNativeBindingsRemainAvailable() {
        val rows = fixture().getJSONArray("existing_modern")
        Armsx2JniContract.verify(Array(rows.length()) { rows.getString(it) })
    }

    @Test
    fun mismatchBecomesReadableJavaErrorBeforeAnyNativeLoad() {
        val error =
            runCatching {
                Armsx2JniContract.verify(arrayOf("N|S|org.libsdl.app.SDLControllerManager|onNativePadDown|(IIII)Z"))
            }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("onNativePadDown|(IIII)Z"))
    }

    @Test
    fun nonNativePlaceholderCannotSatisfyNativeRegistration() {
        val error =
            runCatching {
                Armsx2JniContract.verify(arrayOf("N|S|org.libsdl.app.SDLControllerManager|usesLegacyControllerAbi|()Z"))
            }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }
}
