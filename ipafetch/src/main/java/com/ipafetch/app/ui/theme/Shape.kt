package com.ipafetch.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii scale with the "Card corners" slider (50%–200%).
 * Base values follow the reference screens: 28dp rows, 999dp pills, 18dp fields.
 */
fun appShapes(cornerScale: Float): Shapes {
    val s = cornerScale.coerceIn(0.5f, 2f)
    fun r(base: Int) = RoundedCornerShape((base * s).dp)
    return Shapes(
        extraSmall = r(8),
        small = r(12),
        medium = r(18),
        large = r(24),
        extraLarge = r(30)
    )
}

/** The single row/panel corner used across the app. */
fun rowShape(cornerScale: Float) = RoundedCornerShape((26 * cornerScale.coerceIn(0.5f, 2f)).dp)

fun pillShape() = RoundedCornerShape(percent = 50)

fun sheetShape(cornerScale: Float) = RoundedCornerShape(
    topStart = (32 * cornerScale.coerceIn(0.5f, 2f)).dp,
    topEnd = (32 * cornerScale.coerceIn(0.5f, 2f)).dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
