package com.ipafetch.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.data.AppSettings
import com.ipafetch.app.data.SwipeAction
import com.ipafetch.app.data.autoLockOptions
import com.ipafetch.app.ui.components.AppBackground
import com.ipafetch.app.ui.components.ChipRow
import com.ipafetch.app.ui.components.DotSlider
import com.ipafetch.app.ui.components.DropdownRow
import com.ipafetch.app.ui.components.ExpressiveSegmented
import com.ipafetch.app.ui.components.ExpressiveSwitch
import com.ipafetch.app.ui.components.RoundIconButton
import com.ipafetch.app.ui.components.ScreenHeader
import com.ipafetch.app.ui.components.SectionHeader
import com.ipafetch.app.ui.components.SettingsGroup
import com.ipafetch.app.ui.components.SettingsRow
import com.ipafetch.app.ui.theme.AccentPalette
import com.ipafetch.app.ui.theme.AppFont
import com.ipafetch.app.ui.theme.IpafetchTheme
import com.ipafetch.app.ui.theme.PaletteStyle

private const val THEMES = "themes"
private const val APPEARANCE = "appearance"
private const val INTERACTION = "interaction"
private const val SECURITY = "security"
private const val DATA = "data"

@Composable
fun SettingsScreen(
    settings: AppSettings,
    entryCount: Int,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val colors = IpafetchTheme.colors
    val expanded = remember {
        mutableStateMapOf(
            THEMES to true,
            APPEARANCE to true,
            INTERACTION to true,
            SECURITY to false,
            DATA to false
        )
    }
    val allOpen = expanded.values.all { it }

    AppBackground(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 126.dp)
            ) {
                ScreenHeader(
                    title = "Settings",
                    subtitle = "$entryCount items in the vault",
                    trailing = {
                        RoundIconButton(
                            icon = if (allOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (allOpen) "Collapse all" else "Expand all",
                            onClick = {
                                val target = !allOpen
                                listOf(THEMES, APPEARANCE, INTERACTION, SECURITY, DATA).forEach {
                                    expanded[it] = target
                                }
                            },
                            tint = colors.textSecondary
                        )
                    }
                )

                Spacer(Modifier.height(10.dp))
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    // -------------------------------------------------- Themes
                    SectionHeader(
                        icon = Icons.Rounded.Palette,
                        title = "Themes",
                        expanded = expanded[THEMES] == true,
                        onToggle = { expanded[THEMES] = !(expanded[THEMES] ?: true) }
                    )
                    SettingsGroup(expanded[THEMES] == true) {
                        ThemeModeRow(settings, onUpdate)

                        SettingsRow(
                            title = "Use pure black dark theme",
                            supporting = "Only applies in dark mode",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.pureBlack,
                                    onCheckedChange = { value -> onUpdate { it.copy(pureBlack = value) } }
                                )
                            }
                        )

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(colors.row)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                "Accent and dynamic color source",
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            AccentSwatches(settings.accent) { key ->
                                onUpdate { it.copy(accent = key) }
                            }
                        }

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(colors.row)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text("Palette style", color = colors.textSecondary, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            ChipRow(
                                options = PaletteStyle.entries,
                                selected = PaletteStyle.from(settings.paletteStyle),
                                label = { it.label },
                                onSelect = { style -> onUpdate { it.copy(paletteStyle = style.key) } }
                            )
                        }

                        SliderRow(
                            title = "Surface shading",
                            valueLabel = "${(settings.surfaceShading * 100).toInt()}%",
                            supporting = "Adjust how strongly theme colors tint cards and surfaces.",
                            value = settings.surfaceShading,
                            range = 0.6f..2.0f,
                            onChange = { value -> onUpdate { it.copy(surfaceShading = value) } }
                        )

                        SettingsRow(
                            title = "Gradient background",
                            supporting = "Tinted vertical wash behind every screen",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.gradientBackground,
                                    onCheckedChange = { value -> onUpdate { it.copy(gradientBackground = value) } }
                                )
                            }
                        )

                        SettingsRow(
                            title = "Progressive blur",
                            supporting = "Frosted navigation bar and accent blooms",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.progressiveBlur,
                                    onCheckedChange = { value -> onUpdate { it.copy(progressiveBlur = value) } }
                                )
                            }
                        )

                        SettingsRow(
                            title = "Match app page to icon colors",
                            supporting = "Tints vault rows with the current accent",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.matchIconColors,
                                    onCheckedChange = { value -> onUpdate { it.copy(matchIconColors = value) } }
                                )
                            }
                        )

                        SettingsRow(
                            title = "Reduce visual effects",
                            supporting = "Drops shadows, blur and background blooms",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.reduceEffects,
                                    onCheckedChange = { value -> onUpdate { it.copy(reduceEffects = value) } }
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // ---------------------------------------------- Appearance
                    SectionHeader(
                        icon = Icons.Rounded.Style,
                        title = "Appearance",
                        expanded = expanded[APPEARANCE] == true,
                        onToggle = { expanded[APPEARANCE] = !(expanded[APPEARANCE] ?: true) }
                    )
                    SettingsGroup(expanded[APPEARANCE] == true) {
                        SettingsRow {
                            DropdownRow(
                                label = "Language",
                                value = settings.language,
                                options = listOf("English", "System default"),
                                onSelect = { value -> onUpdate { it.copy(language = value) } }
                            )
                        }

                        SettingsRow(
                            leading = { Icon(Icons.Rounded.TextFields, null, tint = colors.accent, modifier = Modifier.size(20.dp)) },
                            title = "Font in use",
                            supporting = AppFont.from(settings.font).detail,
                            trailing = {
                                Text(
                                    AppFont.from(settings.font).label,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                val order = AppFont.entries
                                val next = order[(order.indexOf(AppFont.from(settings.font)) + 1) % order.size]
                                onUpdate { it.copy(font = next.key) }
                            }
                        )

                        SliderRow(
                            title = "UI scale",
                            valueLabel = "${(settings.uiScale * 100).toInt()}%",
                            supporting = "Scales text and spacing together",
                            value = settings.uiScale,
                            range = 0.8f..1.2f,
                            onChange = { value -> onUpdate { it.copy(uiScale = value) } }
                        )

                        SliderRow(
                            title = "Card corners",
                            valueLabel = "${(settings.cornerScale * 100).toInt()}%",
                            supporting = null,
                            value = settings.cornerScale,
                            range = 0.6f..1.8f,
                            onChange = { value -> onUpdate { it.copy(cornerScale = value) } }
                        )

                        SettingsRow(
                            title = "Update buttons at top of app page",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.buttonsAtTop,
                                    onCheckedChange = { value -> onUpdate { it.copy(buttonsAtTop = value) } }
                                )
                            }
                        )
                        SettingsRow(
                            title = "Always use phone layout",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.alwaysPhoneLayout,
                                    onCheckedChange = { value -> onUpdate { it.copy(alwaysPhoneLayout = value) } }
                                )
                            }
                        )
                        SettingsRow(
                            title = "Show source webpage in app view",
                            supporting = "Opens console links inside the app instead of the browser",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.showSourceInApp,
                                    onCheckedChange = { value -> onUpdate { it.copy(showSourceInApp = value) } }
                                )
                            }
                        )
                        SettingsRow(
                            title = "Highlight less obvious touch targets",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.highlightTargets,
                                    onCheckedChange = { value -> onUpdate { it.copy(highlightTargets = value) } }
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // --------------------------------------------- Interaction
                    SectionHeader(
                        icon = Icons.Rounded.Gesture,
                        title = "Interaction",
                        expanded = expanded[INTERACTION] == true,
                        onToggle = { expanded[INTERACTION] = !(expanded[INTERACTION] ?: true) }
                    )
                    SettingsGroup(expanded[INTERACTION] == true) {
                        SettingsRow(
                            title = "Enable tactile feedback",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.tactile,
                                    onCheckedChange = { value -> onUpdate { it.copy(tactile = value) } }
                                )
                            }
                        )
                        SettingsRow {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DropdownRow(
                                    label = "Right swipe action",
                                    value = SwipeAction.from(settings.rightSwipe).label,
                                    options = SwipeAction.selectable.map { it.label },
                                    onSelect = { label ->
                                        onUpdate {
                                            it.copy(
                                                rightSwipe = (SwipeAction.selectable.firstOrNull { s -> s.label == label }
                                                    ?: SwipeAction.NONE).key
                                            )
                                        }
                                    }
                                )
                                DropdownRow(
                                    label = "Left swipe action",
                                    value = SwipeAction.from(settings.leftSwipe).label,
                                    options = SwipeAction.selectable.map { it.label },
                                    onSelect = { label ->
                                        onUpdate {
                                            it.copy(
                                                leftSwipe = (SwipeAction.selectable.firstOrNull { s -> s.label == label }
                                                    ?: SwipeAction.NONE).key
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ------------------------------------------------ Security
                    SectionHeader(
                        icon = Icons.Rounded.Shield,
                        title = "Security",
                        expanded = expanded[SECURITY] == true,
                        onToggle = { expanded[SECURITY] = !(expanded[SECURITY] ?: true) }
                    )
                    SettingsGroup(expanded[SECURITY] == true) {
                        SettingsRow(
                            title = "Biometric unlock",
                            supporting = "Keeps the master key inside the Keystore",
                            leading = { Icon(Icons.Rounded.Fingerprint, null, tint = colors.accent, modifier = Modifier.size(20.dp)) },
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.biometric,
                                    onCheckedChange = { value -> onUpdate { it.copy(biometric = value) } }
                                )
                            }
                        )
                        SettingsRow {
                            DropdownRow(
                                label = "Auto-lock",
                                value = autoLockOptions.firstOrNull { it.first == settings.autoLockSeconds }?.second
                                    ?: "${settings.autoLockSeconds}s",
                                options = autoLockOptions.map { it.second },
                                onSelect = { label ->
                                    val seconds = autoLockOptions.firstOrNull { it.second == label }?.first ?: 60
                                    onUpdate { it.copy(autoLockSeconds = seconds) }
                                },
                                leading = {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        SettingsRow(
                            title = "Clear clipboard after 45s",
                            supporting = "Copied secrets are wiped automatically",
                            trailing = {
                                ExpressiveSwitch(
                                    checked = settings.clipboardClear,
                                    onCheckedChange = { value -> onUpdate { it.copy(clipboardClear = value) } }
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // ---------------------------------------------------- Data
                    SectionHeader(
                        icon = Icons.Rounded.Widgets,
                        title = "Vault data",
                        expanded = expanded[DATA] == true,
                        onToggle = { expanded[DATA] = !(expanded[DATA] ?: true) }
                    )
                    SettingsGroup(expanded[DATA] == true) {
                        SettingsRow(
                            title = "Export encrypted vault",
                            supporting = "JSON file, passphrase protected",
                            leading = { Icon(Icons.Rounded.Upload, null, tint = colors.accent, modifier = Modifier.size(20.dp)) },
                            onClick = onExport
                        )
                        SettingsRow(
                            title = "Import vault",
                            supporting = "Merge an encrypted backup",
                            leading = { Icon(Icons.Rounded.Download, null, tint = colors.accent, modifier = Modifier.size(20.dp)) },
                            onClick = onImport
                        )
                        SettingsRow(
                            title = "Reset vault",
                            supporting = "Deletes every entry and the master key",
                            leading = { Icon(Icons.Rounded.SettingsBrightness, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp)) },
                            onClick = onReset
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        "IPAFetch · local vault · AES-256-GCM + PBKDF2",
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            ) { bottomBar() }
        }
    }
}

@Composable
private fun ThemeModeRow(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit
) {
    ExpressiveSegmented(
        options = listOf("system", "light", "dark"),
        selected = settings.themeMode,
        label = { mode -> mode.replaceFirstChar { it.uppercase() } },
        icon = { mode ->
            when (mode) {
                "light" -> Icons.Rounded.LightMode
                "dark" -> Icons.Rounded.DarkMode
                else -> Icons.Rounded.AutoAwesome
            }
        },
        onSelect = { mode -> onUpdate { it.copy(themeMode = mode) } }
    )
}

@Composable
private fun AccentSwatches(selected: String, onSelect: (String) -> Unit) {
    val colors = IpafetchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Swatch(
            color = colors.rowInset,
            selected = selected == AccentPalette.DYNAMIC.key || selected == AccentPalette.MONO.key,
            onClick = {
                onSelect(
                    if (selected == AccentPalette.DYNAMIC.key) AccentPalette.MONO.key else AccentPalette.DYNAMIC.key
                )
            },
            icon = Icons.Rounded.Palette
        )
        AccentPalette.swatches.forEach { accent ->
            Swatch(
                color = accent.swatch,
                selected = selected == accent.key,
                onClick = { onSelect(accent.key) }
            )
        }
    }
}

@Composable
private fun Swatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val colors = IpafetchTheme.colors
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(3.dp, colors.textPrimary.copy(alpha = 0.55f), CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = "Dynamic color", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    supporting: String?,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    val colors = IpafetchTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.row)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = colors.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(valueLabel, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        if (supporting != null) {
            Text(supporting, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(6.dp))
        DotSlider(value = value, onValueChange = onChange, range = range)
    }
}
