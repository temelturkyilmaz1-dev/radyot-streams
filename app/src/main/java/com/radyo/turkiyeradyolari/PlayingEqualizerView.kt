package com.radyo.turkiyeradyolari

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PlayingEqualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var phase = 0f
    private var animator: ValueAnimator? = null
    private var playing = false

    fun setAccentColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setPlaying(value: Boolean) {
        if (playing == value) return
        playing = value
        visibility = if (value) VISIBLE else INVISIBLE
        if (value) startAnimation() else stopAnimation()
    }

    private fun startAnimation() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
            duration = 850L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = 4
        val gap = width * 0.035f
        val barWidth = (width - gap * (count - 1)) / count
        val minHeight = height * 0.28f
        for (i in 0 until count) {
            val wave = abs(sin(phase + i * 0.95f))
            val barHeight = minHeight + (height - minHeight) * wave
            val left = i * (barWidth + gap)
            canvas.drawRoundRect(left, height - barHeight, left + barWidth, height.toFloat(), barWidth * 0.22f, barWidth * 0.22f, paint)
        }
    }
}
