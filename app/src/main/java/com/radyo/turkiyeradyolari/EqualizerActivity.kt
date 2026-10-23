package com.radyo.turkiyeradyolari

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial

class EqualizerActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var status: TextView
    private lateinit var presetGroup: ChipGroup
    private var session = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizer)
        applySystemBarInsets()
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        container = findViewById(R.id.bandContainer)
        status = findViewById(R.id.txtEqStatus)
        presetGroup = findViewById(R.id.presetChipGroup)
        session = RadioPlayerManager.audioSessionId(this)
        if (session > 0) EqualizerManager.getOrCreate(this, session)

        val toggle = findViewById<SwitchMaterial>(R.id.switchEqualizer)
        toggle.isChecked = AppPreferences.equalizerEnabled(this)
        toggle.setOnCheckedChangeListener { _, enabled ->
            EqualizerManager.setEnabled(this, enabled)
            updateChildrenEnabled(enabled)
            updateStatus(enabled)
        }
        buildPresetChips(toggle.isChecked && session > 0)
        setupBassControls()
        updateStatus(toggle.isChecked)
        if (session > 0) buildBands()
    }

    private fun buildPresetChips(enabled: Boolean) {
        presetGroup.removeAllViews()
        EqualizerManager.presetNames.filterNot { it == "Özel" }.forEach { preset ->
            presetGroup.addView(Chip(this).apply {
                id = android.view.View.generateViewId()
                text = preset
                isCheckable = true
                isChecked = preset == AppPreferences.equalizerPreset(this@EqualizerActivity)
                isEnabled = enabled
                setOnClickListener {
                    if (EqualizerManager.applyPreset(this@EqualizerActivity, preset)) buildBands()
                }
            })
        }
    }

    private fun setupBassControls() {
        val bassSwitch = findViewById<SwitchMaterial>(R.id.switchBassBoost)
        val bassSeek = findViewById<SeekBar>(R.id.seekBassBoost)
        val bassValue = findViewById<TextView>(R.id.txtBassValue)
        val supported = session > 0 && EqualizerManager.bassBoostSupported()
        bassSwitch.isChecked = AppPreferences.bassBoostEnabled(this)
        bassSeek.progress = AppPreferences.bassBoostStrength(this).toInt()
        bassValue.text = "Güç: %${bassSeek.progress / 10}"
        bassSwitch.isEnabled = supported
        bassSeek.isEnabled = supported && bassSwitch.isChecked
        if (!supported) bassValue.text = if (session <= 0) "Önce bir radyo çalın" else "Bu cihaz bas güçlendirmeyi desteklemiyor"
        bassSwitch.setOnCheckedChangeListener { _, enabled ->
            EqualizerManager.setBassEnabled(this, enabled)
            bassSeek.isEnabled = supported && enabled
        }
        bassSeek.progressTintList = ColorStateList.valueOf(AppPreferences.accent(this))
        bassSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                bassValue.text = "Güç: %${progress / 10}"
                if (fromUser) EqualizerManager.setBassStrength(this@EqualizerActivity, progress.toShort())
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
    }

    private fun updateStatus(enabled: Boolean) {
        status.text = when {
            session <= 0 -> "Ekolayzırı kullanmak için önce bir radyo çalın."
            !enabled -> "Ekolayzır kapalı"
            else -> "Ekolayzır açık • değişiklikler otomatik kaydedilir"
        }
    }

    private fun buildBands() {
        container.removeAllViews()
        val eq = EqualizerManager.getOrCreate(this, session) ?: return showUnsupported()
        val range = runCatching { eq.bandLevelRange }.getOrNull() ?: return showUnsupported()
        val bandCount = runCatching { eq.numberOfBands.toInt() }.getOrDefault(0)
        if (bandCount <= 0) return showUnsupported()
        repeat(bandCount) { index ->
            val band = index.toShort()
            val current = runCatching { eq.getBandLevel(band) }.getOrDefault(0)
            val saved = AppPreferences.equalizerBand(this, index, current).coerceIn(range[0], range[1])
            runCatching { eq.setBandLevel(band, saved) }
            val frequency = runCatching { eq.getCenterFreq(band) / 1000 }.getOrDefault(0)
            val value = TextView(this).apply {
                text = dbLabel(saved)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            val seek = VerticalSeekBar(this).apply {
                max = (range[1] - range[0]).toInt()
                progress = (saved - range[0]).toInt()
                isEnabled = AppPreferences.equalizerEnabled(this@EqualizerActivity)
                progressTintList = ColorStateList.valueOf(AppPreferences.accent(this@EqualizerActivity))
                thumbTintList = ColorStateList.valueOf(AppPreferences.accent(this@EqualizerActivity))
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                        val level = (progress + range[0]).toShort()
                        value.text = dbLabel(level)
                        if (fromUser && !EqualizerManager.setBand(this@EqualizerActivity, index, level)) {
                            bar?.isEnabled = false
                            status.text = "Ekolayzır bağlantısı kesildi. Radyoyu yeniden başlatın."
                        }
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(bar: SeekBar?) = Unit
                })
            }
            val frequencyLabel = TextView(this).apply {
                text = frequencyLabel(frequency)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            container.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(value, LinearLayout.LayoutParams(dp(54), dp(22)))
                addView(seek, LinearLayout.LayoutParams(dp(44), dp(132)))
                addView(frequencyLabel, LinearLayout.LayoutParams(dp(54), dp(24)))
            })
        }
    }

    private fun dbLabel(level: Short): String {
        val db = level / 100f
        return if (db > 0) "+%.1f".format(db) else "%.1f".format(db)
    }

    private fun frequencyLabel(hz: Int) = if (hz >= 1000) "${hz / 1000}k" else hz.toString()

    private fun showUnsupported() {
        status.text = "Bu cihaz ekolayzırı desteklemiyor."
        container.removeAllViews()
    }

    private fun updateChildrenEnabled(enabled: Boolean) {
        for (i in 0 until container.childCount) {
            val column = container.getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until column.childCount) column.getChildAt(j).isEnabled = enabled
        }
        for (i in 0 until presetGroup.childCount) presetGroup.getChildAt(i).isEnabled = enabled && session > 0
    }

    private fun applySystemBarInsets() {
        val root = findViewById<NestedScrollView>(R.id.equalizerRoot)
        val initialStart = root.paddingStart
        val initialTop = root.paddingTop
        val initialEnd = root.paddingEnd
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.mandatorySystemGestures()
            )
            view.setPaddingRelative(
                initialStart + safe.left,
                initialTop + safe.top,
                initialEnd + safe.right,
                initialBottom + safe.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
