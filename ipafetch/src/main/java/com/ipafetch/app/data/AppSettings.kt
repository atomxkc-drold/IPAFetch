package com.ipafetch.app.data

import com.ipafetch.app.ui.theme.AccentPalette
import com.ipafetch.app.ui.theme.PaletteStyle
import kotlinx.serialization.Serializable

/**
 * Every knob from the Settings screen. Values persist as plain (non-secret)
 * preferences; nothing here touches the vault.
 */
@Serializable
data class AppSettings(
    val themeMode: String = "system",
    val pureBlack: Boolean = false,
    val accent: String = AccentPalette.BLUE.key,
    val paletteStyle: String = PaletteStyle.TONAL.key,
    val surfaceShading: Float = 1.4f,
    val gradientBackground: Boolean = true,
    val progressiveBlur: Boolean = true,
    val matchIconColors: Boolean = true,
    val reduceEffects: Boolean = false,
    val language: String = "English",
    val font: String = "system",
    val uiScale: Float = 0.9f,
    val cornerScale: Float = 1.5f,
    val buttonsAtTop: Boolean = true,
    val alwaysPhoneLayout: Boolean = true,
    val showSourceInApp: Boolean = true,
    val highlightTargets: Boolean = true,
    val tactile: Boolean = true,
    val rightSwipe: String = SwipeAction.COPY.key,
    val leftSwipe: String = SwipeAction.DELETE.key,
    val biometric: Boolean = true,
    val autoLockSeconds: Int = 60,
    val clipboardClear: Boolean = true,
    val allowScreenshots: Boolean = false,
    val knownTags: List<String> = emptyList(),
    val compactRows: Boolean = false
) {
    val isDarkPreferred: Boolean get() = themeMode == "dark"
}

enum class SwipeAction(val key: String, val label: String) {
    COPY("copy", "Copy value"),
    REVEAL("reveal", "Reveal value"),
    OPEN("open", "Open console"),
    DELETE("delete", "Delete entry"),
    EDIT("edit", "Edit entry"),
    NONE("none", "Nothing");

    companion object {
        fun from(key: String?): SwipeAction = entries.firstOrNull { it.key == key } ?: NONE
        val selectable = listOf(COPY, REVEAL, OPEN, EDIT, DELETE, NONE)
    }
}

/** Auto-lock choices shown as a dropdown in Settings → Security. */
val autoLockOptions = listOf(0 to "Immediately", 30 to "30 seconds", 60 to "1 minute", 300 to "5 minutes", 900 to "15 minutes")
