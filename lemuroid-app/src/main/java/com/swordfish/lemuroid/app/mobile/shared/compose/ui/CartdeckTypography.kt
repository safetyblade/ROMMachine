package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.R

// Adobe Source Sans 3, unmodified static faces under SIL OFL 1.1.
internal val CartdeckFontFamily =
    FontFamily(
        Font(R.font.source_sans_3_regular, FontWeight.Normal),
        Font(R.font.source_sans_3_medium, FontWeight.Medium),
        Font(R.font.source_sans_3_semibold, FontWeight.SemiBold),
        Font(R.font.source_sans_3_bold, FontWeight.Bold),
        Font(R.font.source_sans_3_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    )

internal val CartdeckTypography =
    Typography().let { base ->
        val headline = base.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = CartdeckFontFamily),
            displayMedium = base.displayMedium.copy(fontFamily = CartdeckFontFamily),
            displaySmall = base.displaySmall.copy(fontFamily = CartdeckFontFamily),
            headlineLarge = headline.copy(fontFamily = CartdeckFontFamily, fontSynthesis = FontSynthesis.None),
            headlineMedium = base.headlineMedium.copy(fontFamily = CartdeckFontFamily),
            headlineSmall =
                base.headlineSmall.copy(
                    fontFamily = CartdeckFontFamily,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            titleLarge =
                base.titleLarge.copy(
                    fontFamily = CartdeckFontFamily,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            titleMedium =
                base.titleMedium.copy(
                    fontFamily = CartdeckFontFamily,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            titleSmall = base.titleSmall.copy(fontFamily = CartdeckFontFamily),
            bodyLarge = base.bodyLarge.copy(fontFamily = CartdeckFontFamily, fontSize = 16.sp, lineHeight = 22.sp),
            bodyMedium = base.bodyMedium.copy(fontFamily = CartdeckFontFamily, fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = base.bodySmall.copy(fontFamily = CartdeckFontFamily, fontSize = 12.sp, lineHeight = 16.sp),
            labelLarge =
                base.labelLarge.copy(
                    fontFamily = CartdeckFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            labelMedium = base.labelMedium.copy(fontFamily = CartdeckFontFamily, fontSize = 13.sp, lineHeight = 18.sp),
            labelSmall = base.labelSmall.copy(fontFamily = CartdeckFontFamily, fontSize = 12.sp, lineHeight = 16.sp),
        )
    }
