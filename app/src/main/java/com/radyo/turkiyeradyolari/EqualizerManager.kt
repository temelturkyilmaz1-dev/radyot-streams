package com.radyo.turkiyeradyolari

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import kotlin.math.roundToInt

/** Ekolayzır ekranı kapansa da ses efektini canlı tutar. */
object EqualizerManager {
    private var equalizer: Equalizer? = null
    private var attachedSessionId: Int = -1
    private var bassBoost: BassBoost? = null

    val presetNames = listOf("Düz", "Pop", "Rock", "Caz", "Klasik", "Bas", "Vokal", "Özel")

    @Synchronized
    fun getOrCreate(context: Context, sessionId: Int): Equalizer? {
        if (sessionId <= 0) return null
        if (equalizer == null || attachedSessionId != sessionId) {
            runCatching { equalizer?.release() }
            runCatching { bassBoost?.release() }
            equalizer = null
            bassBoost = null
            equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
            bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
            attachedSessionId = sessionId
            restore(context, equalizer)
            restoreBass(context)
        }
        equalizer?.let { eq ->
            runCatching { eq.enabled = AppPreferences.equalizerEnabled(context) }
                .onFailure {
                    runCatching { eq.release() }
                    equalizer = null
                    attachedSessionId = -1
                }
        }
        return equalizer
    }

    @Synchronized
    fun setEnabled(context: Context, enabled: Boolean) {
        AppPreferences.setEqualizerEnabled(context, enabled)
        equalizer?.let { runCatching { it.enabled = enabled } }
    }

    fun bassBoostSupported(): Boolean = bassBoost?.strengthSupported == true

    @Synchronized
    fun setBassEnabled(context: Context, enabled: Boolean): Boolean {
        AppPreferences.setBassBoostEnabled(context, enabled)
        val effect = bassBoost ?: return false
        return runCatching { effect.enabled = enabled }.isSuccess
    }

    @Synchronized
    fun setBassStrength(context: Context, strength: Short): Boolean {
        val safe = strength.coerceIn(0.toShort(), 1000.toShort())
        AppPreferences.setBassBoostStrength(context, safe)
        val effect = bassBoost ?: return false
        return runCatching { effect.setStrength(safe) }.isSuccess
    }

    private fun restoreBass(context: Context) {
        bassBoost?.let { effect ->
            runCatching {
                if (effect.strengthSupported) effect.setStrength(AppPreferences.bassBoostStrength(context))
                effect.enabled = AppPreferences.bassBoostEnabled(context)
            }
        }
    }

    @Synchronized
    fun restore(context: Context, eq: Equalizer? = equalizer) {
        eq ?: return
        val range = eq.bandLevelRange
        repeat(eq.numberOfBands.toInt()) { index ->
            val band = index.toShort()
            val saved = AppPreferences.equalizerBand(context, index, eq.getBandLevel(band))
                .coerceIn(range[0], range[1])
            runCatching { eq.setBandLevel(band, saved) }
        }
        runCatching { eq.enabled = AppPreferences.equalizerEnabled(context) }
    }

    @Synchronized
    fun setBand(context: Context, bandIndex: Int, level: Short): Boolean {
        val eq = equalizer ?: return false
        val count = runCatching { eq.numberOfBands.toInt() }.getOrDefault(0)
        if (bandIndex !in 0 until count) return false
        val range = runCatching { eq.bandLevelRange }.getOrNull() ?: return false
        val safe = level.coerceIn(range[0], range[1])
        val applied = runCatching { eq.setBandLevel(bandIndex.toShort(), safe) }.isSuccess
        if (!applied) return false
        AppPreferences.setEqualizerBand(context, bandIndex, safe)
        AppPreferences.setEqualizerPreset(context, "Özel")
        return true
    }

    @Synchronized
    fun applyPreset(context: Context, preset: String): Boolean {
        val eq = equalizer ?: return false
        val count = eq.numberOfBands.toInt()
        if (count <= 0 || preset == "Özel") return false
        val shape = when (preset) {
            "Pop" -> floatArrayOf(0.45f, 0.15f, -0.10f, 0.20f, 0.45f)
            "Rock" -> floatArrayOf(0.60f, 0.30f, -0.15f, 0.30f, 0.60f)
            "Caz" -> floatArrayOf(0.30f, 0.15f, 0.10f, 0.30f, 0.45f)
            "Klasik" -> floatArrayOf(0.40f, 0.25f, 0.00f, 0.20f, 0.40f)
            "Bas" -> floatArrayOf(0.75f, 0.55f, 0.20f, 0.00f, -0.10f)
            "Vokal" -> floatArrayOf(-0.10f, 0.00f, 0.45f, 0.60f, 0.25f)
            else -> floatArrayOf(0f, 0f, 0f, 0f, 0f)
        }
        val range = eq.bandLevelRange
        val positiveMax = range[1].toInt()
        val negativeMax = -range[0].toInt()
        repeat(count) { index ->
            val position = if (count == 1) 0f else index.toFloat() * (shape.lastIndex.toFloat() / (count - 1))
            val left = position.toInt().coerceIn(0, shape.lastIndex)
            val right = (left + 1).coerceAtMost(shape.lastIndex)
            val fraction = position - left
            val value = shape[left] + (shape[right] - shape[left]) * fraction
            val level = if (value >= 0) (value * positiveMax).roundToInt() else (value * negativeMax).roundToInt()
            val safe = level.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
            runCatching { eq.setBandLevel(index.toShort(), safe) }
            AppPreferences.setEqualizerBand(context, index, safe)
        }
        AppPreferences.setEqualizerPreset(context, preset)
        return true
    }
}
