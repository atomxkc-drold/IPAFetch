package com.ipafetch.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Accent sources shown as swatches in Settings.
 * Hues are picked to match the reference expression: mono, blue, green,
 * orange, purple, red, plus the wallpaper-derived "dynamic" source.
 */
enum class AccentPalette(
    val key: String,
    val label: String,
    val hue: Float,
    val saturation: Float,
    val swatch: Color
) {
    MONO("mono", "Mono", 262f, 0.10f, Color(0xFFB9B2C6)),
    BLUE("blue", "Blue", 214f, 0.58f, Color(0xFF1F5FA8)),
    GREEN("green", "Green", 138f, 0.52f, Color(0xFF2E7D32)),
    ORANGE("orange", "Orange", 35f, 0.95f, Color(0xFFFF9A00)),
    PURPLE("purple", "Purple", 283f, 0.58f, Color(0xFF7B1FA2)),
    RED("red", "Red", 5f, 0.72f, Color(0xFFE53935)),
    DYNAMIC("dynamic", "Dynamic", 214f, 0.58f, Color(0xFF546E7A));

    companion object {
        val swatches = listOf(MONO, BLUE, GREEN, ORANGE, PURPLE, RED)
        fun from(key: String?): AccentPalette = entries.firstOrNull { it.key == key } ?: BLUE
    }
}

/** Material "palette style" variants. */
enum class PaletteStyle(
    val key: String,
    val label: String,
    val chroma: Float,
    val hueShift: Float
) {
    TONAL("tonal", "Tonal spot", 1.00f, 0f),
    NEUTRAL("neutral", "Neutral", 0.42f, 0f),
    VIBRANT("vibrant", "Vibrant", 1.35f, 0f),
    EXPRESSIVE("expressive", "Expressive", 1.15f, 28f);

    companion object {
        fun from(key: String?): PaletteStyle = entries.firstOrNull { it.key == key } ?: TONAL
    }
}

/**
 * A compact HCT-flavoured tonal palette: hue stays fixed, saturation follows
 * the palette style's chroma, lightness is the M3 tone value.
 */
private fun tone(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    // Slightly damp mid-tone saturation so large surfaces stay calm.
    val softened = s * (0.72f + 0.28f * (1f - kotlin.math.abs(l - 0.5f) * 2f))
    return Color.hsl(h, softened, l)
}

/** Main colour scheme builder. */
object Palette {

    fun scheme(
        accent: AccentPalette,
        style: PaletteStyle,
        dark: Boolean,
        pureBlack: Boolean = false,
        dynamicSeed: Color? = null
    ): ColorScheme {
        val hue = (dynamicSeed?.let { hueOf(it) } ?: accent.hue) + style.hueShift
        val chroma = style.chroma * if (accent == AccentPalette.MONO) 0.5f else 1f
        val sat = accent.saturation * chroma
        val neutralSat = (sat * 0.06f).coerceIn(0.004f, 0.05f)

        return if (!dark) {
            val surface = tone(hue, neutralSat, 0.968f)
            val variant = tone(hue, (neutralSat * 2.4f).coerceIn(0.012f, 0.09f), 0.885f)
            lightColorScheme(
                primary = tone(hue, sat, 0.335f),
                onPrimary = tone(hue, 0.06f, 1f),
                primaryContainer = tone(hue, sat * 0.55f, 0.87f),
                onPrimaryContainer = tone(hue, sat * 0.8f, 0.16f),
                secondary = tone(hue + 24f, sat * 0.45f, 0.40f),
                onSecondary = tone(hue, 0.05f, 1f),
                secondaryContainer = tone(hue + 24f, sat * 0.28f, 0.88f),
                onSecondaryContainer = tone(hue + 24f, sat * 0.55f, 0.18f),
                tertiary = tone(hue - 55f, sat * 0.5f, 0.42f),
                onTertiary = tone(hue, 0.05f, 1f),
                tertiaryContainer = tone(hue - 55f, sat * 0.3f, 0.89f),
                onTertiaryContainer = tone(hue - 55f, sat * 0.6f, 0.18f),
                error = Color(0xFFB3261E),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFF9DEDC),
                onErrorContainer = Color(0xFF410E0B),
                background = surface,
                onBackground = tone(hue, neutralSat * 3f, 0.11f),
                surface = surface,
                onSurface = tone(hue, neutralSat * 3f, 0.11f),
                surfaceVariant = variant,
                onSurfaceVariant = tone(hue, neutralSat * 3.4f, 0.30f),
                outline = tone(hue, (neutralSat * 3.4f).coerceIn(0.02f, 0.13f), 0.53f),
                outlineVariant = tone(hue, (neutralSat * 3f).coerceIn(0.015f, 0.10f), 0.78f),
                scrim = Color(0xFF000000),
                inverseSurface = tone(hue, neutralSat * 3f, 0.20f),
                inverseOnSurface = tone(hue, neutralSat, 0.95f),
                inversePrimary = tone(hue, sat * 0.7f, 0.80f),
                surfaceTint = tone(hue, sat, 0.40f)
            )
        } else {
            val surface = if (pureBlack) Color(0xFF000000) else tone(hue, neutralSat, 0.085f)
            val variant = if (pureBlack) Color(0xFF141118)
            else tone(hue, (neutralSat * 2.4f).coerceIn(0.012f, 0.09f), 0.165f)
            darkColorScheme(
                primary = tone(hue, sat * 0.85f, 0.78f),
                onPrimary = tone(hue, sat, 0.18f),
                primaryContainer = tone(hue, sat * 0.7f, 0.34f),
                onPrimaryContainer = tone(hue, sat * 0.5f, 0.92f),
                secondary = tone(hue + 24f, sat * 0.45f, 0.80f),
                onSecondary = tone(hue + 24f, sat * 0.6f, 0.18f),
                secondaryContainer = tone(hue + 24f, sat * 0.4f, 0.30f),
                onSecondaryContainer = tone(hue + 24f, sat * 0.35f, 0.92f),
                tertiary = tone(hue - 55f, sat * 0.55f, 0.82f),
                onTertiary = tone(hue - 55f, sat * 0.6f, 0.18f),
                tertiaryContainer = tone(hue - 55f, sat * 0.45f, 0.32f),
                onTertiaryContainer = tone(hue - 55f, sat * 0.35f, 0.92f),
                error = Color(0xFFF2B8B5),
                onError = Color(0xFF601410),
                errorContainer = Color(0xFF8C1D18),
                onErrorContainer = Color(0xFFF9DEDC),
                background = surface,
                onBackground = tone(hue, neutralSat * 3f, 0.93f),
                surface = surface,
                onSurface = tone(hue, neutralSat * 3f, 0.93f),
                surfaceVariant = variant,
                onSurfaceVariant = tone(hue, neutralSat * 3.4f, 0.79f),
                outline = tone(hue, (neutralSat * 3.4f).coerceIn(0.02f, 0.13f), 0.56f),
                outlineVariant = tone(hue, (neutralSat * 3f).coerceIn(0.015f, 0.10f), 0.27f),
                scrim = Color(0xFF000000),
                inverseSurface = tone(hue, neutralSat, 0.92f),
                inverseOnSurface = tone(hue, neutralSat * 3f, 0.16f),
                inversePrimary = tone(hue, sat, 0.38f),
                surfaceTint = tone(hue, sat, 0.78f)
            )
        }
    }

    private fun hueOf(color: Color): Float {
        val max = maxOf(color.red, color.green, color.blue)
        val min = minOf(color.red, color.green, color.blue)
        val d = max - min
        if (d == 0f) return 214f
        val h = when (max) {
            color.red -> ((color.green - color.blue) / d) % 6f
            color.green -> ((color.blue - color.red) / d) + 2f
            else -> ((color.red - color.green) / d) + 4f
        }
        return (h * 60f + 360f) % 360f
    }
}

/**
 * Extra surfaces the expressive layout needs on top of [ColorScheme].
 * Rows are tinted versions of the canvas; [shading] scales the tint strength.
 */
data class AppColors(
    val canvas: Color,
    val canvasBottom: Color,
    val row: Color,
    val rowInset: Color,
    val rowStrong: Color,
    val hairline: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val trackOff: Color,
    val knobOff: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorder: Color
) {
    companion object {
        fun of(
            scheme: ColorScheme,
            shading: Float,
            gradientBackground: Boolean,
            dark: Boolean
        ): AppColors {
            val canvas = scheme.background
            val tint = scheme.surfaceTint
            val neutral = if (dark) Color(0xFF2A2733) else Color(0xFFD6CFDF)
            val strength = (shading * 0.62f).coerceIn(0.25f, 1f)
            val row = lerp(canvas, if (dark) lerp(neutral, tint, 0.18f) else neutral, strength)
            val rowStrong = lerp(row, if (dark) Color.White else Color.Black, if (dark) 0.06f else 0.05f)
            val rowInset = lerp(row, tint, 0.10f)
            val accent = scheme.primary
            val canvasBottom = when {
                gradientBackground && dark -> lerp(canvas, tint, 0.16f)
                gradientBackground -> lerp(canvas, tint, 0.09f)
                else -> canvas
            }
            return AppColors(
                canvas = canvas,
                canvasBottom = canvasBottom,
                row = row,
                rowInset = rowInset,
                rowStrong = rowStrong,
                hairline = lerp(row, scheme.onSurface, 0.10f),
                accent = accent,
                onAccent = scheme.onPrimary,
                accentSoft = lerp(row, accent, 0.16f),
                textPrimary = scheme.onSurface,
                textSecondary = scheme.onSurfaceVariant,
                trackOff = lerp(row, scheme.onSurfaceVariant, 0.22f),
                knobOff = lerp(row, scheme.onSurfaceVariant, 0.45f),
                glassTop = scheme.surface.copy(alpha = if (dark) 0.72f else 0.76f),
                glassBottom = scheme.surface.copy(alpha = if (dark) 0.58f else 0.62f),
                glassBorder = scheme.onSurface.copy(alpha = if (dark) 0.14f else 0.10f)
            )
        }
    }
}

/** Semantic accent colours used by brand chips and type badges. */
object TypeColors {
    val token = Color(0xFF1F5FA8)
    val api = Color(0xFF2E7D32)
    val json = Color(0xFF7B5BB8)
    val link = Color(0xFFB26A00)
    val note = Color(0xFF5A6270)
}
