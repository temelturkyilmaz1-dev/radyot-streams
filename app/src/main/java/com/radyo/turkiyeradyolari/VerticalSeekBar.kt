package com.radyo.turkiyeradyolari

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar

class VerticalSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatSeekBar(context, attrs) {
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val centerX = width / 2f
        val top = 10f * density
        val bottom = height - 10f * density
        val fraction = if (max > 0) progress.toFloat() / max else 0f
        val thumbY = bottom - (bottom - top) * fraction
        val gradientColors = intArrayOf(
            Color.rgb(255, 230, 128),
            Color.rgb(255, 193, 7),
            Color.rgb(255, 128, 0),
            Color.rgb(239, 57, 38),
            Color.rgb(128, 0, 18)
        )
        val gradient = LinearGradient(
            centerX,
            bottom,
            centerX,
            top,
            gradientColors,
            floatArrayOf(0f, 0.27f, 0.52f, 0.76f, 1f),
            Shader.TileMode.CLAMP
        )

        trackPaint.shader = gradient
        trackPaint.alpha = if (isEnabled) 70 else 40
        trackPaint.strokeWidth = 5f * density
        fillPaint.shader = gradient
        fillPaint.alpha = if (isEnabled) 255 else 100
        fillPaint.strokeWidth = 6f * density
        thumbPaint.color = gradientColorAt(fraction)
        thumbPaint.alpha = if (isEnabled) 255 else 120

        canvas.drawLine(centerX, top, centerX, bottom, trackPaint)
        canvas.drawLine(centerX, thumbY, centerX, bottom, fillPaint)
        canvas.drawCircle(centerX, thumbY, 7f * density, thumbPaint)
    }

    private fun gradientColorAt(fraction: Float): Int {
        val stops = floatArrayOf(0f, 0.27f, 0.52f, 0.76f, 1f)
        val colors = intArrayOf(
            Color.rgb(255, 230, 128),
            Color.rgb(255, 193, 7),
            Color.rgb(255, 128, 0),
            Color.rgb(239, 57, 38),
            Color.rgb(128, 0, 18)
        )
        val safe = fraction.coerceIn(0f, 1f)
        val upper = stops.indexOfFirst { safe <= it }.let { if (it < 1) 1 else it }
        val lower = upper - 1
        val local = ((safe - stops[lower]) / (stops[upper] - stops[lower])).coerceIn(0f, 1f)
        return Color.rgb(
            lerp(Color.red(colors[lower]), Color.red(colors[upper]), local),
            lerp(Color.green(colors[lower]), Color.green(colors[upper]), local),
            lerp(Color.blue(colors[lower]), Color.blue(colors[upper]), local)
        )
    }

    private fun lerp(start: Int, end: Int, amount: Float) =
        (start + (end - start) * amount).toInt()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        val fraction = (1f - event.y / height).coerceIn(0f, 1f)
        progress = (fraction * max).toInt()
        invalidate()
        return true
    }
}
