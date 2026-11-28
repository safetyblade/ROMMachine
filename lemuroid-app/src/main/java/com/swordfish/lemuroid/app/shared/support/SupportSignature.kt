package com.swordfish.lemuroid.app.shared.support

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R

@Composable
internal fun SupportSignature(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                background = Color(SupportDesign.midnight),
                surface = Color(SupportDesign.panel),
                surfaceVariant = Color(SupportDesign.panel),
                primary = Color(SupportDesign.cyan),
                onPrimary = Color(SupportDesign.midnight),
                secondary = Color(SupportDesign.pink),
                onBackground = Color(SupportDesign.text),
                onSurface = Color(SupportDesign.text),
                onSurfaceVariant = Color(SupportDesign.muted),
            ),
        content = content,
    )
}

@Composable
internal fun SupportSignatureHeader(
    title: String,
    detail: String,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(
                painterResource(R.drawable.cartdeck_wordmark_native),
                contentDescription = "CARTDECK",
                modifier = Modifier.width(160.dp).height(34.dp),
                contentScale = ContentScale.Fit,
            )
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
