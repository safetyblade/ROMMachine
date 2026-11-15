package com.swordfish.lemuroid.app.ps3

import com.armsx3.core.FirmwareRepository
import com.armsx3.core.GameInfo
import com.armsx3.core.GameRepository
import com.armsx3.core.ProgressRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/** Missing descriptors abort ART inside JNI_OnLoad before Kotlin can catch the error. */
class Ps3NativeCallbackAbiTest {
    @Test fun pinnedArmsx3InstallerCallbacksAreStaticAndHaveNativeDescriptors() {
        val progress =
            ProgressRepository::class.java.getDeclaredMethod(
                "onProgressEvent",
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                String::class.java,
            )
        assertTrue(Modifier.isStatic(progress.modifiers))
        assertEquals(Boolean::class.javaPrimitiveType, progress.returnType)
        for (name in listOf("onFirmwareInstalled", "onFirmwareCompiled")) {
            val callback = FirmwareRepository::class.java.getDeclaredMethod(name, String::class.java)
            assertTrue(Modifier.isStatic(callback.modifiers))
            assertEquals(Void.TYPE, callback.returnType)
        }
        GameInfo::class.java.getConstructor(
            String::class.java,
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
        )
        val games =
            GameRepository::class.java.getDeclaredMethod(
                "add",
                Array<GameInfo>::class.java,
                Long::class.javaPrimitiveType,
            )
        assertTrue(Modifier.isStatic(games.modifiers))
        assertEquals(Void.TYPE, games.returnType)
    }
}
