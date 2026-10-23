package com.radyo.turkiyeradyolari

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color

object StationVisualColors {
    fun forRadio(context: Context, radio: Radio): Int = runCatching {
        val res = StationLogoOverrides.localLogoFor(radio)
            ?: CatalogLogoFallbacks.localLogoFor(radio) ?: radio.logoRes
        val bitmap = BitmapFactory.decodeResource(context.resources, res) ?: return@runCatching Color.BLACK
        var red = 0L; var green = 0L; var blue = 0L; var count = 0L
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)
        for (y in 0 until bitmap.height step stepY) for (x in 0 until bitmap.width step stepX) {
            val pixel = bitmap.getPixel(x, y)
            if (Color.alpha(pixel) < 100) continue
            val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
            if (r > 242 && g > 242 && b > 242) continue
            red += r; green += g; blue += b; count++
        }
        bitmap.recycle()
        if (count == 0L) Color.BLACK else Color.rgb(
            (red / count / 2).toInt().coerceIn(0, 92),
            (green / count / 2).toInt().coerceIn(0, 92),
            (blue / count / 2).toInt().coerceIn(0, 92)
        )
    }.getOrDefault(Color.BLACK)
}
