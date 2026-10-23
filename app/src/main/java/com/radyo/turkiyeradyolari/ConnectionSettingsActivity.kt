package com.radyo.turkiyeradyolari

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.switchmaterial.SwitchMaterial

class ConnectionSettingsActivity : AppCompatActivity() {
    private lateinit var bufferSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(36))
            addView(header())
            addView(optionCard(
                getString(R.string.wifi_only),
                getString(R.string.wifi_only_summary)
            ) { row ->
                row.addView(SwitchMaterial(this@ConnectionSettingsActivity).apply {
                    text = ""
                    isChecked = AppPreferences.wifiOnly(this@ConnectionSettingsActivity)
                    setOnCheckedChangeListener { _, enabled ->
                        AppPreferences.setWifiOnly(this@ConnectionSettingsActivity, enabled)
                        RadioPlayerManager.enforceNetworkPreference(this@ConnectionSettingsActivity)
                    }
                })
            }, cardParams())

            addView(actionCard(getString(R.string.buffer_profile)) {
                bufferSummary = summaryText(bufferProfileLabel())
                addView(bufferSummary)
                addView(summaryText(getString(R.string.buffer_restart_hint)).apply { alpha = .58f })
                setOnClickListener { chooseBufferProfile() }
            }, cardParams())

            addView(actionCard(getString(R.string.clear_cache)) {
                addView(summaryText(getString(R.string.clear_cache_summary)))
                setOnClickListener {
                    cacheDir.listFiles()?.forEach { file -> runCatching { file.deleteRecursively() } }
                    Toast.makeText(this@ConnectionSettingsActivity, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
                }
            }, cardParams())
        }

        val scroll = NestedScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.mandatorySystemGestures())
            view.updatePadding(top = safe.top, left = safe.left, right = safe.right, bottom = safe.bottom)
            insets
        }
        setContentView(scroll)
        ViewCompat.requestApplyInsets(scroll)
    }

    private fun chooseBufferProfile() {
        val profiles = arrayOf(AppPreferences.BUFFER_FAST, AppPreferences.BUFFER_BALANCED, AppPreferences.BUFFER_STABLE)
        val labels = arrayOf(
            getString(R.string.buffer_fast),
            getString(R.string.buffer_balanced),
            getString(R.string.buffer_stable)
        )
        val selected = profiles.indexOf(AppPreferences.bufferProfile(this)).coerceAtLeast(1)
        AlertDialog.Builder(this)
            .setTitle(R.string.buffer_profile)
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                AppPreferences.setBufferProfile(this, profiles[which])
                bufferSummary.text = labels[which]
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun bufferProfileLabel(): String = when (AppPreferences.bufferProfile(this)) {
        AppPreferences.BUFFER_FAST -> getString(R.string.buffer_fast)
        AppPreferences.BUFFER_STABLE -> getString(R.string.buffer_stable)
        else -> getString(R.string.buffer_balanced)
    }

    private fun header() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageButton(this@ConnectionSettingsActivity).apply {
            setImageResource(R.drawable.ic_back)
            contentDescription = getString(R.string.back)
            background = null
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        addView(TextView(this@ConnectionSettingsActivity).apply {
            text = getString(R.string.connection_storage)
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
        })
    }

    private fun optionCard(title: String, summary: String, trailing: (LinearLayout) -> Unit) = styledCard().apply {
        val row = LinearLayout(this@ConnectionSettingsActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(12), dp(15))
            addView(labels(title, summary), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        trailing(row)
        addView(row)
    }

    private fun actionCard(title: String, content: LinearLayout.() -> Unit) = styledCard().apply {
        isClickable = true
        isFocusable = true
        foreground = getDrawable(android.R.drawable.list_selector_background)
        addView(LinearLayout(this@ConnectionSettingsActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(15))
            addView(TextView(this@ConnectionSettingsActivity).apply {
                text = title
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
            })
            content()
        })
    }

    private fun labels(title: String, summary: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(this@ConnectionSettingsActivity).apply {
            text = title
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(summaryText(summary))
    }

    private fun summaryText(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        alpha = .7f
        setPadding(0, dp(3), 0, 0)
    }

    private fun styledCard() = MaterialCardView(this).apply {
        radius = dp(18).toFloat()
        cardElevation = 0f
        strokeWidth = dp(1)
        strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)
        setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer))
    }

    private fun cardParams() = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(6), 0, dp(8)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
