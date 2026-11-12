package com.omnidex.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidex.app.domain.AppCatalog
import com.omnidex.app.ui.brand.AppIcon
import com.omnidex.app.ui.theme.OmniDexTheme
import com.omnidex.app.ui.theme.rowShape

/** One selectable option in [AppAutocompleteField]. */
data class AppChoice(
    val packageName: String,
    val label: String
)

/**
 * The "Used by application" control: a rounded pill that holds the chosen apps
 * (each with its launcher icon) and doubles as a search box. Typing filters the
 * installed apps and an animated panel drops down under the pill; tapping a row
 * adds that app to the pill.
 *
 * The panel is drawn inline instead of using a [androidx.compose.material3.DropdownMenu]
 * popup so it can grow downward out of the pill with an expand animation and
 * stays inside the scrolling form.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppAutocompleteField(
    query: String,
    onQueryChange: (String) -> Unit,
    choices: List<AppChoice>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    maxPanelHeight: Int = 260,
    emptyLabel: String = "No matching apps",
    placeholder: String = "Search installed apps",
    pillText: String? = null,
    onPillClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = OmniDexTheme.colors
    val context = LocalContext.current

    Column(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(rowShape(OmniDexTheme.settings.cornerScale))
                .background(colors.row)
                .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
        ) {
            if (pillText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (onPillClick != null) Modifier.clickable(onClick = onPillClick) else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leading?.let {
                        it()
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        pillText,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    trailing?.invoke()
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(10.dp))
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(colors.accent),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(placeholder, color = colors.textSecondary, fontSize = 15.sp)
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                    )
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onQueryChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        trailing?.invoke()
                    }
                }
            }

            if (selected.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selected.forEach { pkg ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.accentSoft)
                                .clickable { onToggle(pkg) }
                                .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(pkg, size = 16.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                AppCatalog.labelFor(context, pkg),
                                color = colors.textPrimary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(220)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(160)) + fadeOut(tween(120))
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.row)
                    .heightIn(max = maxPanelHeight.dp)
            ) {
                if (choices.isEmpty()) {
                    Text(
                        emptyLabel,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(choices, key = { it.packageName }) { choice ->
                            val chosen = choice.packageName in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggle(choice.packageName) }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(choice.packageName, size = 26.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        choice.label,
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                    Text(
                                        choice.packageName,
                                        color = colors.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                if (chosen) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Small chevron shown when a dropdown-style control is collapsed. */
@Composable
internal fun FieldChevron() {
    Icon(
        imageVector = Icons.Rounded.ArrowDropDown,
        contentDescription = null,
        tint = OmniDexTheme.colors.textSecondary,
        modifier = Modifier.size(20.dp)
    )
}
