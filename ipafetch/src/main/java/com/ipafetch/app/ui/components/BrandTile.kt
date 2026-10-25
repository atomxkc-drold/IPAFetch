package com.ipafetch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.ui.brand.BrandCatalog
import com.ipafetch.app.ui.brand.BrandLogos
import com.ipafetch.app.ui.theme.IpafetchTheme

/**
 * A brand mark on a tinted plate. Uses the bundled vector when available and a
 * deterministic monogram otherwise, so every entry has a real logo tile.
 */
@Composable
fun BrandTile(
    brandId: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tinted: Boolean = true
) {
    val brand = BrandCatalog.by(brandId)
    val plate = when {
        brand != null -> if (tinted) brand.tint else brand.tint.copy(alpha = 0.18f)
        else -> BrandLogos.fallbackColor(label)
    }
    val content = if (brand != null && tinted) Color.White else if (brand != null) brand.tint else Color.White
    val mark = BrandLogos.vector(brandId)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.2f))
            .background(plate),
        contentAlignment = Alignment.Center
    ) {
        if (mark != null) {
            Icon(
                imageVector = mark,
                contentDescription = brand?.name ?: label,
                tint = content,
                modifier = Modifier.size(size * 0.52f)
            )
        } else {
            Text(
                text = BrandLogos.initials(brand?.name ?: label),
                color = content,
                fontSize = (size.value * 0.32f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Neutral outlined variant used in pickers where colour would be noisy. */
@Composable
fun BrandChip(brandId: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = IpafetchTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.accentSoft else colors.row)
            .then(
                if (selected) Modifier
                else Modifier
            )
            .size(58.dp),
        contentAlignment = Alignment.Center
    ) {
        BrandTile(brandId = brandId, label = label, size = 38.dp, tinted = !selected)
    }
}
