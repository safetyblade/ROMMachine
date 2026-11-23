package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.R
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class ImmersiveModeRetirementTest {
    @Test
    fun androidTvDoesNotExposeLegacyImmersiveModePreference() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.resources.getXml(R.xml.tv_settings)
        var hasImmersivePreference = false
        parser.use {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    val key = parser.getAttributeResourceValue(
                        "http://schemas.android.com/apk/res/android",
                        "key",
                        0,
                    )
                    if (key == R.string.pref_key_enable_immersive_mode) hasImmersivePreference = true
                }
                parser.next()
            }
        }
        assertFalse(hasImmersivePreference)
    }
}
