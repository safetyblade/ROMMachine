package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.R

/**
 * Brief CARTDECK identity on initial phone launch. The normal app is composed
 * underneath so ROM scanning, navigation and saved state are not delayed.
 * This does not replace Android's OS-managed cold-start splash screen.
 */
@Composable
internal fun CartdeckLaunchSignature() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF303B49)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.cartdeck_mark_foreground),
                contentDescription = null,
                modifier = Modifier.size(112.dp),
            )
            Text(
                text = stringResource(R.string.cartdeck_launch_title),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEDF6F2),
                letterSpacing = 2.sp,
            )
            Text(
                text = stringResource(R.string.cartdeck_launch_byline),
                fontSize = 13.sp,
                color = Color(0xFF4ED8C4),
            )
        }
    }
}
