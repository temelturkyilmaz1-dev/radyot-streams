package com.radyo.turkiyeradyolari

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.switchmaterial.SwitchMaterial

class PlayerSettingsActivity : AppCompatActivity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(36))
            addView(header())
            addView(optionCard(
                getString(R.string.continuous_audio),
                "Kısa bağlantı kesintilerinde yayının sürmesini sağlar"
            ) { row ->
                row.addView(SwitchMaterial(this@PlayerSettingsActivity).apply {
                    text = ""
                    isChecked = AppPreferences.continuousAudio(this@PlayerSettingsActivity)
                    setOnCheckedChangeListener { _, value ->
                        AppPreferences.setContinuousAudio(this@PlayerSettingsActivity, value)
                        RadioPlayerManager.applyAudioFocusPreference(this@PlayerSettingsActivity)
                    }
                })
            }, cardParams())

            val tiles = LinearLayout(this@PlayerSettingsActivity).apply { orientation = LinearLayout.HORIZONTAL }
            tiles.addView(actionTile(getString(R.string.equalizer), "Ses profili ve bas ayarı") {
                startActivity(Intent(this@PlayerSettingsActivity, EqualizerActivity::class.java))
            }, tileParams(true))
            tiles.addView(actionTile("Pil ayarı", "Arka planda kesintisiz çalma") { openBatterySettings() }, tileParams(false))
            addView(tiles)
            status = TextView(this@PlayerSettingsActivity).apply {
                textSize = 14f
                alpha = .72f
                setPadding(dp(4), dp(10), dp(4), 0)
            }
            addView(status)
        }
        val scroll = NestedScrollView(this).apply { isFillViewport = true; addView(content) }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.mandatorySystemGestures())
            view.updatePadding(top = safe.top, bottom = safe.bottom)
            insets
        }
        setContentView(scroll)
        ViewCompat.requestApplyInsets(scroll)
    }

    override fun onResume() {
        super.onResume()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        status.text = if (pm.isIgnoringBatteryOptimizations(packageName))
            getString(R.string.battery_optimization_disabled_short)
        else getString(R.string.battery_optimization_enabled_short)
    }

    private fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }.onFailure {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun header() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageButton(this@PlayerSettingsActivity).apply {
            setImageResource(R.drawable.ic_back); contentDescription = getString(R.string.back); background = null
            setPadding(dp(10), dp(10), dp(10), dp(10)); setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        addView(TextView(this@PlayerSettingsActivity).apply {
            text = getString(R.string.player_settings); textSize = 25f; typeface = Typeface.DEFAULT_BOLD
        })
    }

    private fun optionCard(title: String, summary: String, trailing: (LinearLayout) -> Unit) = styledCard().apply {
        val row = LinearLayout(this@PlayerSettingsActivity).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(15), dp(12), dp(15))
            addView(labels(title, summary), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        trailing(row); addView(row)
    }

    private fun actionTile(title: String, summary: String, action: () -> Unit) = styledCard().apply {
        isClickable = true; isFocusable = true; foreground = getDrawable(android.R.drawable.list_selector_background)
        setOnClickListener { action() }
        addView(LinearLayout(this@PlayerSettingsActivity).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(14), dp(16))
            addView(labels(title, summary))
            addView(TextView(this@PlayerSettingsActivity).apply { text = "›"; textSize = 27f; alpha = .65f })
        })
    }

    private fun labels(title: String, summary: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(this@PlayerSettingsActivity).apply { text = title; textSize = 17f; typeface = Typeface.DEFAULT_BOLD })
        addView(TextView(this@PlayerSettingsActivity).apply { text = summary; textSize = 13f; alpha = .7f; setPadding(0, dp(3), 0, 0) })
    }

    private fun styledCard() = MaterialCardView(this).apply {
        radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
        strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)
        setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer))
    }
    private fun cardParams() = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(6), 0, dp(8)) }
    private fun tileParams(first: Boolean) = LinearLayout.LayoutParams(0, dp(142), 1f).apply {
        if (first) setMargins(0, dp(6), dp(5), dp(6)) else setMargins(dp(5), dp(6), 0, dp(6))
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
