package com.ipafetch.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.ipafetch.app.domain.CustomLogos
import com.ipafetch.app.ui.brand.BrandCatalog
import com.ipafetch.app.ui.brand.BrandLogos
import com.ipafetch.app.ui.theme.IpafetchTheme

/**
 * A provider mark on a tinted plate: bundled vector, user-uploaded image, or a
 * deterministic monogram. Every entry gets a real tile.
 */
@Composable
fun BrandTile(
    brandId: String,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tinted: Boolean = true
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(size / 3.2f)

    if (CustomLogos.isCustom(brandId)) {
        val file = remember(brandId) { CustomLogos.fileFor(context, brandId) }
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(IpafetchTheme.colors.rowStrong),
            contentAlignment = Alignment.Center
        ) {
            if (file != null) {
                AsyncImage(
                    model = file,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size).clip(shape)
                )
            } else {
                Text(
                    text = BrandLogos.initials(label),
                    color = Color.White,
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        return
    }

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
            .clip(shape)
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

/** Real launcher icon for an installed app (used by the "used by" pills). */
@Composable
fun AppIcon(packageName: String, size: Dp = 18.dp) {
    val context = LocalContext.current
    val px = (size.value * 2).toInt().coerceAtLeast(24)
    val bitmap = remember(packageName, px) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(px, px)
                .asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(size).clip(RoundedCornerShape(size / 3f))
        )
    }
}
