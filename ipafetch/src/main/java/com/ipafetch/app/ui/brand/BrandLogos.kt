package com.ipafetch.app.ui.brand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Builds tintable [ImageVector]s from the generated SVG path data. The paths are
 * filled with black so `Icon(tint = ...)` recolours them like any Material icon.
 */
object BrandLogos {

    private val cache = HashMap<String, ImageVector>()

    fun hasMark(brandId: String): Boolean = BrandPaths.paths.containsKey(brandId)

    fun vector(brandId: String): ImageVector? {
        customMarks[brandId]?.let { return it }
        cache[brandId]?.let { return it }
        val data = BrandPaths.paths[brandId] ?: return null
        val built = ImageVector.Builder(
            name = "brand-$brandId",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(data),
            pathFillType = if (BrandPaths.evenOdd.contains(brandId)) PathFillType.EvenOdd else PathFillType.NonZero,
            fill = SolidColor(Color.Black)
        ).build()
        cache[brandId] = built
        return built
    }

    /** Deterministic colour for brands without a bundled mark. */    fun fallbackColor(label: String): Color {
        val palette = listOf(
            Color(0xFF1F5FA8), Color(0xFF2E7D32), Color(0xFFB26A00),
            Color(0xFF7B5BB8), Color(0xFFB23A48), Color(0xFF00796B),
            Color(0xFF455A64), Color(0xFF8E24AA)
        )
        if (label.isBlank()) return palette.first()
        var hash = 0
        for (ch in label) hash = hash * 31 + ch.code
        return palette[((hash % palette.size) + palette.size) % palette.size]
    }

    /**
     * Marks Simple Icons no longer ships. Groq's official glyph is a squared
     * 'G', drawn here as an arc plus a bar so it stays crisp at any size.
     */
    private val customMarks: Map<String, ImageVector> by lazy {
        mapOf("groq" to ImageVector.Builder(
            name = "brand-groq",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes("M20 12a8 8 0 1 0-2.4 5.7"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                fill = null
            )
            addPath(
                pathData = addPathNodes("M12.4 12H20"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                fill = null
            )
        }.build())
    }

    fun initials(label: String): String {
        if (label.isBlank()) return "?"
        val cleaned = label.trim()
        val parts = cleaned.split(' ', '-', '_', '.').filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> (parts[0].first().toString() + parts[1].first()).uppercase()
            else -> cleaned.take(2).uppercase()
        }
    }
}
