package com.omnidex.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidex.app.ui.theme.OmniDexTheme
import com.omnidex.app.ui.theme.rowShape
import kotlin.math.roundToInt

/**
 * The reference switch: a pill track with a knob that carries a check (on) or a
 * cross (off) — no default Material switch anywhere in the app.
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = OmniDexTheme.colors
    val tactile = OmniDexTheme.settings.tactile
    val haptics = LocalHapticFeedback.current
    val track by animateColorAsState(
        targetValue = when {
            !enabled -> colors.trackOff.copy(alpha = 0.5f)
            checked -> colors.accent
            else -> colors.trackOff
        },
        animationSpec = tween(180),
        label = "switch-track"
    )
    val knob by animateDpAsState(
        targetValue = if (checked) 26.dp else 2.dp,
        animationSpec = tween(190),
        label = "switch-knob"
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) Color.White else colors.knobOff,
        animationSpec = tween(180),
        label = "switch-knob-color"
    )
    Box(
        modifier = modifier
            .width(58.dp)
            .height(34.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (tactile) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knob)
                .size(30.dp)
                .clip(CircleShape)
                .background(knobColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                tint = if (checked) colors.accent else colors.textPrimary.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Dotted slider with a tall bar thumb, as in the reference screens.
 */
@Composable
fun DotSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true
) {
    val colors = OmniDexTheme.colors
    var widthPx by remember { mutableStateOf(0f) }
    val span = (range.endInclusive - range.start).takeIf { it != 0f } ?: 1f
    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)

    fun emit(x: Float) {
        if (widthPx <= 0f) return
        val f = (x / widthPx).coerceIn(0f, 1f)
        onValueChange(range.start + f * span)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset -> emit(offset.x) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    emit(change.position.x)
                }
            }
    ) {
        val canvasWidth = if (widthPx > 0f) widthPx else size.width
        val cy = size.height / 2f
        val dotRadius = size.height * 0.085f
        val step = canvasWidth / 14f
        var x = dotRadius + 2f
        while (x <= canvasWidth - dotRadius) {
            val active = (x / canvasWidth) <= fraction + 0.001f
            drawCircle(
                color = if (active) colors.accent else colors.trackOff.copy(alpha = 0.55f),
                radius = dotRadius,
                center = Offset(x, cy)
            )
            x += step
        }
        // Thumb
        val thumbW = size.height * 0.16f
        val thumbH = size.height * 0.72f
        val thumbX = (fraction * canvasWidth).coerceIn(thumbW / 2f, canvasWidth - thumbW / 2f)
        drawRoundRect(
            color = if (enabled) colors.accent else colors.trackOff,
            topLeft = Offset(thumbX - thumbW / 2f, cy - thumbH / 2f),
            size = Size(thumbW, thumbH),
            cornerRadius = CornerRadius(thumbW / 2f, thumbW / 2f)
        )
    }
}

/** Segmented control with tinted selection and optional icons. */
@Composable
fun <T> ExpressiveSegmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector)? = null,
    onSelect: (T) -> Unit
) {
    val colors = OmniDexTheme.colors
    val tactile = OmniDexTheme.settings.tactile
    val haptics = LocalHapticFeedback.current
    val scale = OmniDexTheme.settings.cornerScale
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape(scale))
            .background(colors.row)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) colors.accentSoft else Color.Transparent,
                animationSpec = tween(200),
                label = "segment-bg"
            )
            val contentColor = if (isSelected) colors.accent else colors.textSecondary
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(percent = 42))
                    .background(bg)
                    .clickable {
                        if (tactile) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onSelect(option)
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon(option),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = label(option),
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Horizontally scrolling pill chips (palette style) — the trailing chip peeks. */
@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OmniDexTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) colors.canvas else colors.row)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

/** Row with an inline dropdown menu (Language, auto-lock, swipe actions). */
@Composable
fun DropdownRow(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    var open by remember { mutableStateOf(false) }
    val colors = OmniDexTheme.colors
    Box(modifier) {
        NestedRow(
            label = label,
            value = value,
            leading = leading,
            onClick = { open = true },
            trailing = {
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 14.sp, color = colors.textPrimary) },
                    onClick = { open = false; onSelect(option) }
                )
            }
        }
    }
}

/** Right-aligned value shown on a slider row ("140%"). */
@Composable
fun TrailingValue(text: String) {
    Text(
        text = text,
        color = OmniDexTheme.colors.textPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

/** Rounded tag used on entry rows and detail. */
@Composable
fun TagPill(text: String, modifier: Modifier = Modifier, color: Color = OmniDexTheme.colors.accentSoft) {
    val colors = OmniDexTheme.colors
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = colors.textPrimary, fontSize = 11.sp, maxLines = 1)
    }
}

internal fun percent(fraction: Float): String = "${(fraction * 100).roundToInt()}%"
