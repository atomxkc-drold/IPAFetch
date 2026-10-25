package com.ipafetch.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class AppFont(val key: String, val label: String, val detail: String) {
    SYSTEM("system", "System default", "Default System Font"),
    SANS("sans", "Sans serif", "Roboto"),
    SERIF("serif", "Serif", "Noto Serif"),
    MONO("mono", "Monospace", "For keys and JSON");

    companion object {
        fun from(key: String?): AppFont = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

private fun TextStyle.withFamily(family: FontFamily): TextStyle = copy(fontFamily = family)

/**
 * Type ramp tuned for the settings/vault layout: 32sp display, 15sp section
 * labels, 16sp row titles and 13sp supporting text.
 */
fun appTypography(font: AppFont): Typography {
    val family = when (font) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.SANS -> FontFamily.SansSerif
        AppFont.SERIF -> FontFamily.Serif
        AppFont.MONO -> FontFamily.Monospace
    }
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = family, fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        displayMedium = base.displayMedium.copy(fontFamily = family, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
        displaySmall = base.displaySmall.copy(fontFamily = family, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
        headlineLarge = base.headlineLarge.copy(fontFamily = family, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = family, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = family, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = family, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium.copy(fontFamily = family, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
        titleSmall = base.titleSmall.copy(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = family, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = base.bodySmall.copy(fontFamily = family, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = base.labelLarge.copy(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(fontFamily = family, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = family, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
    )
}

/** Compact 15sp section header used above every settings group. */
val sectionLabelStyle = TextStyle(
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.1.sp
)
