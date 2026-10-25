package com.ipafetch.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.ipafetch.app.data.AppSettings

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("LocalAppColors accessed outside IpafetchTheme")
}

/** Current settings, available to any component that needs one of the live knobs. */
val LocalAppSettings = staticCompositionLocalOf<AppSettings> { AppSettings() }

object IpafetchTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
    val settings: AppSettings
        @Composable get() = LocalAppSettings.current
}

@Composable
fun IpafetchTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    val dynamicSeed = remember(settings.accent, dark) {
        if (settings.accent == AccentPalette.DYNAMIC.key && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            scheme.primary
        } else null
    }

    val accent = AccentPalette.from(settings.accent)
    val style = PaletteStyle.from(settings.paletteStyle)

    val scheme = remember(accent, style, dark, settings.pureBlack, dynamicSeed) {
        Palette.scheme(
            accent = accent,
            style = style,
            dark = dark,
            pureBlack = settings.pureBlack,
            dynamicSeed = dynamicSeed
        )
    }
    val colors = remember(scheme, settings.surfaceShading, settings.gradientBackground, dark) {
        AppColors.of(scheme, settings.surfaceShading, settings.gradientBackground, dark)
    }

    val baseDensity = LocalDensity.current
    val density = remember(baseDensity, settings.uiScale) {
        Density(baseDensity.density * settings.uiScale.coerceIn(0.75f, 1.3f), baseDensity.fontScale)
    }

    val typography = remember(settings.font) { appTypography(AppFont.from(settings.font)) }

    CompositionLocalProvider(
        LocalDensity provides density,
        LocalAppColors provides colors,
        LocalAppSettings provides settings
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = appShapes(settings.cornerScale),
            content = content
        )
    }
}
