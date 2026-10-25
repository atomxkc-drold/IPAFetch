package com.ipafetch.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.domain.EntryCategories
import com.ipafetch.app.domain.VaultEntry
import com.ipafetch.app.ui.brand.BrandCatalog
import com.ipafetch.app.ui.components.AppBackground
import com.ipafetch.app.ui.components.BrandTile
import com.ipafetch.app.ui.components.ScreenHeader
import com.ipafetch.app.ui.theme.IpafetchTheme
import com.ipafetch.app.ui.theme.rowShape

@Composable
fun CategoriesScreen(
    entries: List<VaultEntry>,
    onOpenCategory: (String) -> Unit,
    onOpenProvider: (String) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val colors = IpafetchTheme.colors
    val byCategory = entries.groupBy { it.category.ifBlank { EntryCategories.OTHER } }

    AppBackground(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ScreenHeader(
                        title = "Categories",
                        subtitle = "${entries.size} items · ${BrandCatalog.categories.size} groups"
                    )
                    Spacer(Modifier.height(6.dp))
                }
                items(EntryCategories.ALL) { category ->
                    val count = byCategory[category]?.size ?: 0
                    CategoryCard(
                        category = category,
                        count = count,
                        entries = byCategory[category].orEmpty(),
                        onClick = { onOpenCategory(category) }
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Providers",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )
                }
                items(BrandCatalog.ALL) { brand ->
                    val brandCount = entries.count { it.brandId == brand.id }
                    BrandRow(
                        name = brand.name,
                        brandId = brand.id,
                        detail = if (brandCount == 0) brand.category else "$brandCount ${if (brandCount == 1) "entry" else "entries"}",
                        onClick = { onOpenProvider(brand.name) }
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
private fun CategoryCard(
    category: String,
    count: Int,
    entries: List<VaultEntry>,
    onClick: () -> Unit
) {
    val colors = IpafetchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape(IpafetchTheme.settings.cornerScale))
            .background(colors.row)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                category,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                if (count == 0) "Empty" else "$count ${if (count == 1) "entry" else "entries"}",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
        if (entries.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                entries.map { it.brandId }.distinct().take(3).forEach { id ->
                    BrandTile(brandId = id, label = category, size = 24.dp)
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            Icons.Rounded.ArrowForward,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun BrandRow(name: String, brandId: String, detail: String, onClick: () -> Unit) {
    val colors = IpafetchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape(IpafetchTheme.settings.cornerScale))
            .background(colors.row)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandTile(brandId = brandId, label = name, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = colors.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = colors.textSecondary, fontSize = 11.sp)
        }
        Icon(
            Icons.Rounded.ArrowForward,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(15.dp)
        )
    }
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    EntryCategories.AI -> Icons.Rounded.Memory
    EntryCategories.CLOUD -> Icons.Rounded.Cloud
    EntryCategories.DEV -> Icons.Rounded.Code
    EntryCategories.PAYMENTS -> Icons.Rounded.CreditCard
    EntryCategories.COMMS -> Icons.Rounded.Forum
    EntryCategories.DATA -> Icons.Rounded.Storage
    EntryCategories.DESIGN -> Icons.Rounded.Brush
    else -> Icons.Rounded.Widgets
}
