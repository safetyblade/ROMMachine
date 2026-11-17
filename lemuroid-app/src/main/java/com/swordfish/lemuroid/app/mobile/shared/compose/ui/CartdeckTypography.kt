package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// B1 role hierarchy. System sans stays until bundled font packaging is approved.
internal val CartdeckTypography =
    Typography().let { base ->
        base.copy(
            headlineLarge =
                base.headlineLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            headlineSmall =
                base.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
            titleLarge =
                base.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
            titleMedium =
                base.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
            bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            labelLarge = base.labelLarge.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
            labelMedium = base.labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
            labelSmall = base.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        )
    }
