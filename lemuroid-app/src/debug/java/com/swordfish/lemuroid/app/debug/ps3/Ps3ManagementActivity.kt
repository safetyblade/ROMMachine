package com.swordfish.lemuroid.app.debug.ps3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.swordfish.lemuroid.app.mobile.feature.settings.ps3.Ps3Management
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage

class Ps3ManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LemuroidSettingsPage { Ps3Management(intent.getStringExtra("engine") ?: "armsx3") } }
    }
}
