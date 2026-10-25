package com.ipafetch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.ui.theme.IpafetchTheme
import com.ipafetch.app.ui.theme.rowShape

/**
 * Tinted text field used everywhere. Focus thickens the accent hairline instead
 * of swapping the whole surface, which keeps long forms calm.
 */
@Composable
fun ExpressiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    isSecret: Boolean = false,
    monospace: Boolean = false,
    singleLine: Boolean = true,
    minHeight: Int = 54,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = IpafetchTheme.colors
    var revealed by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = rowShape(IpafetchTheme.settings.cornerScale)

    Column(modifier) {
        if (label != null) {
            Text(
                label,
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.row)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .heightIn(min = minHeight.dp - 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = colors.textSecondary.copy(alpha = 0.75f), fontSize = 15.sp)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = singleLine,
                        interactionSource = interaction,
                        textStyle = LocalTextStyle.current.copy(
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
                        ),
                        cursorBrush = SolidColor(colors.accent),
                        visualTransformation = when {
                            !isSecret || revealed -> VisualTransformation.None
                            else -> PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isSecret) KeyboardType.Password else keyboardType
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (singleLine) Modifier.height(24.dp) else Modifier.heightIn(min = 60.dp))
                    )
                }
                if (isSecret) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { revealed = !revealed },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (revealed) "Hide value" else "Show value",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                trailing?.invoke()
            }
        }
        Box(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(CircleShape)
                .background(if (focused) colors.accent else Color.Transparent)
        )
    }
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = IpafetchTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.accent else colors.trackOff)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) colors.onAccent else colors.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = IpafetchTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .background(colors.row)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ErrorLine(text: String?, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    Text(
        text = text,
        color = Color(0xFFB3261E),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/** Small label + control column used by the editor. */
@Composable
fun FieldSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Text(
            title,
            color = IpafetchTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun RowSpacer(height: Int) = Spacer(Modifier.height(height.dp))

@Composable
fun RowSpacerWidth(width: Int) = Spacer(Modifier.width(width.dp))

@Composable
fun ColumnGap(space: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(space.dp)) { content() }
}
