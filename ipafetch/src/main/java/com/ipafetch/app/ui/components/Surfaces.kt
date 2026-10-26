package com.ipafetch.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.ui.theme.IpafetchTheme
import com.ipafetch.app.ui.theme.rowShape

/**
 * Canvas with the theme gradient plus two very soft accent blooms.
 * Blur is real RenderEffect blur on API 31+ and a no-op below that.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = IpafetchTheme.colors
    val settings = IpafetchTheme.settings
    val glow = if (settings.progressiveBlur) 1f else 0.5f
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(colors.canvas, colors.canvasBottom))
            )
    ) {
        if (settings.gradientBackground && !settings.reduceEffects) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 0.dp)
                    .size(240.dp)
                    .blur(if (settings.progressiveBlur) 90.dp else 0.dp)
                    .background(colors.accent.copy(alpha = 0.22f * glow), CircleShape)
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .size(200.dp)
                    .blur(if (settings.progressiveBlur) 100.dp else 0.dp)
                    .background(colors.accent.copy(alpha = 0.14f * glow), CircleShape)
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(280.dp)
                    .blur(if (settings.progressiveBlur) 120.dp else 0.dp)
                    .background(colors.accent.copy(alpha = 0.07f * glow), CircleShape)
            )
        }
        content()
    }
}

/** Collapsible section header: tinted icon, accent label, chevron. */
@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IpafetchTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = title,
            color = colors.accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            tint = colors.accent.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/** A group of expressive rows; collapses with the section header. */
@Composable
fun SettingsGroup(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(tween(220)) + fadeIn(tween(180)),
        exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/**
 * The signature row: a tinted rounded slab. Optional leading icon, supporting
 * text, and trailing content (a switch, a value, a chevron).
 */
@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    nested: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = IpafetchTheme.colors
    val settings = IpafetchTheme.settings
    val glass = settings.progressiveBlur && !settings.reduceEffects
    val scale = IpafetchTheme.settings.cornerScale
    val shape = rowShape(scale)
    val base = when {
        glass -> colors.glassRow
        nested -> colors.rowInset
        else -> colors.row
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(base)
            .then(if (glass) Modifier.border(1.dp, colors.glassSheen, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = contentPadding, vertical = 13.dp)
            .defaultMinSize(minHeight = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { leading() }
                Spacer(Modifier.size(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (title != null) {
                    Text(
                        text = title,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (supporting != null) {
                    Text(
                        text = supporting,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            if (trailing != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = trailing
                )
            }
        }
        if (content != null) {
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/** Secondary label/value row used inside a [SettingsRow] (e.g. dropdowns). */
@Composable
fun NestedRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = IpafetchTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape(IpafetchTheme.settings.cornerScale))
            .background(colors.rowInset)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(label, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.size(10.dp))
            }
            Text(
                text = value,
                color = colors.textPrimary,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailing != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = trailing
                )
            }
        }
    }
}

/** Frosted slab used by the floating navigation bar and floating buttons. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = IpafetchTheme.colors
    val settings = IpafetchTheme.settings
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (settings.reduceEffects) colors.row else colors.glassTop,
                        if (settings.reduceEffects) colors.row else colors.glassBottom
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(colors.glassBorder, Color.Transparent)
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

/** Small rounded icon plate (section icons, type badges). */
@Composable
fun IconPlate(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    background: Color = tint.copy(alpha = 0.16f),
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2.8f))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

/** Content colour that respects the local surface. */
@Composable
fun muted(): Color = IpafetchTheme.colors.textSecondary

@Suppress("unused")
@Composable
private fun ContentColorBox(content: @Composable () -> Unit) {
    val c = LocalContentColor.current
    remember(c) { c }
    content()
}
