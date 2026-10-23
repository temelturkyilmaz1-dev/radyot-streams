package com.radyo.turkiyeradyolari

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class EqualizerSpectrumView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val center = height / 2f
        paint.strokeWidth = resources.displayMetrics.density * 3f
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(0xffff3cac.toInt(), 0xff14e0ff.toInt(), 0xff78ff44.toInt(), 0xffffd23f.toInt()),
            null, Shader.TileMode.CLAMP
        )
        val count = 38
        val gap = width.toFloat() / count
        repeat(count) { index ->
            val wave = (0.22f + 0.78f * kotlin.math.abs(sin(index * .71))).toFloat()
            val half = height * .39f * wave
            val x = gap * (index + .5f)
            canvas.drawLine(x, center - half, x, center + half, paint)
        }
    }
}
