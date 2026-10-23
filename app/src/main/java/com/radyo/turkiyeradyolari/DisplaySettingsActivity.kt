package com.radyo.turkiyeradyolari

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.switchmaterial.SwitchMaterial

class DisplaySettingsActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(40))
            addView(header())
        }

        addCollapsibleSection(getString(R.string.theme), getString(R.string.theme)) {
            addView(buildTheme())
        }
        addCollapsibleSection(getString(R.string.language), currentLanguageLabel()) {
            addView(buildLanguage())
        }
        addCollapsibleSection(getString(R.string.accent_color), getString(R.string.accent_color)) {
            addView(buildAccentColors())
        }
        addCollapsibleSection(getString(R.string.player_background), getString(R.string.background_effect)) {
            addView(buildBackgroundSettings())
        }
        addCollapsibleSection(getString(R.string.player_elements), getString(R.string.player_elements)) {
            addView(buildPlayerElements())
        }
        addCollapsibleSection(getString(R.string.radio_view), getString(R.string.radio_view)) {
            addView(buildRadioView())
        }

        val scroll = NestedScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.mandatorySystemGestures())
            view.updatePadding(top = safe.top, left = safe.left, right = safe.right, bottom = safe.bottom + dp(36))
            insets
        }
        setContentView(scroll)
        ViewCompat.requestApplyInsets(scroll)
    }

    private fun header() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageButton(this@DisplaySettingsActivity).apply {
            setImageResource(R.drawable.ic_back)
            contentDescription = getString(R.string.back)
            background = null
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        addView(TextView(this@DisplaySettingsActivity).apply {
            text = getString(R.string.display_settings)
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
        })
    }

    private fun addCollapsibleSection(title: String, summary: String, buildBody: LinearLayout.() -> Unit) {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, dp(18), dp(16))
            visibility = View.GONE
            buildBody()
        }
        val arrow = TextView(this).apply {
            text = "›"
            textSize = 28f
            gravity = Gravity.CENTER
            alpha = .7f
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(12), dp(14))
            addView(LinearLayout(this@DisplaySettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@DisplaySettingsActivity).apply {
                    text = title
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(this@DisplaySettingsActivity).apply {
                    text = summary
                    textSize = 13f
                    alpha = .66f
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(arrow, LinearLayout.LayoutParams(dp(36), dp(44)))
        }
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer))
            addView(LinearLayout(this@DisplaySettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(header)
                addView(body)
            })
        }
        header.setOnClickListener {
            val opening = body.visibility != View.VISIBLE
            body.visibility = if (opening) View.VISIBLE else View.GONE
            arrow.text = if (opening) "⌄" else "›"
        }
        root.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(6), 0, dp(6))
        })
    }

    private fun buildTheme() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val group = RadioGroup(this@DisplaySettingsActivity)
        listOf(
            getString(R.string.theme_system) to AppPreferences.THEME_SYSTEM,
            getString(R.string.theme_light) to AppPreferences.THEME_LIGHT,
            getString(R.string.theme_dark) to AppPreferences.THEME_DARK,
            getString(R.string.theme_metallic_gold) to AppPreferences.THEME_METALLIC_GOLD,
            getString(R.string.theme_rose_gold) to AppPreferences.THEME_ROSE_GOLD,
            getString(R.string.theme_titanium) to AppPreferences.THEME_TITANIUM
        ).forEach { (label, value) ->
            group.addView(RadioButton(this@DisplaySettingsActivity).apply {
                text = label
                isChecked = AppPreferences.theme(this@DisplaySettingsActivity) == value
                setOnClickListener {
                    if (AppPreferences.theme(this@DisplaySettingsActivity) != value) {
                        AppPreferences.setTheme(this@DisplaySettingsActivity, value)
                        recreate()
                    }
                }
            })
        }
        addView(group)
    }

    private fun buildLanguage() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(this@DisplaySettingsActivity).apply {
            text = currentLanguageLabel()
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { showLanguageDialog() }
        })
    }

    private fun showLanguageDialog() {
        val labels = arrayOf(getString(R.string.system_language), "Türkçe", "English", "Deutsch")
        val tags = arrayOf("", "tr", "en", "de")
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
        val checked = tags.indexOf(currentTag).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.language))
            .setSingleChoiceItems(labels, checked) { dialog, index ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[index]))
                dialog.dismiss()
            }.show()
    }

    private fun currentLanguageLabel(): String = when (AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')) {
        "tr" -> "Türkçe"
        "en" -> "English"
        "de" -> "Deutsch"
        else -> getString(R.string.system_language)
    }

    private fun buildAccentColors() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val selected = AppPreferences.accent(this@DisplaySettingsActivity)
        val colors = listOf(
            0xFF5F6368, 0xFF1565C0, 0xFFE65100, 0xFFC62828, 0xFF6A1B9A, 0xFF2E7D32,
            0xFF5FAF9F, 0xFF7C9BCB, 0xFFD29A69, 0xFFCE8795, 0xFF9A8BC4, 0xFF87A879
        )
        colors.chunked(6).forEach { rowColors ->
            addView(LinearLayout(this@DisplaySettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                rowColors.forEach { raw ->
                    val color = raw.toInt()
                    addView(View(this@DisplaySettingsActivity).apply {
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(color)
                            if (selected == color) setStroke(dp(3), Color.WHITE)
                        }
                        alpha = if (selected == color) 1f else .72f
                        contentDescription = getString(R.string.accent_color)
                        setOnClickListener {
                            AppPreferences.setAccent(this@DisplaySettingsActivity, color)
                            recreate()
                        }
                    }, LinearLayout.LayoutParams(dp(if (selected == color) 44 else 38), dp(if (selected == color) 44 else 38)).apply {
                        setMargins(dp(7), dp(7), dp(7), dp(7))
                    })
                }
            })
        }
    }

    private fun buildBackgroundSettings() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(SwitchMaterial(this@DisplaySettingsActivity).apply {
            text = "Renkleri radyo logosundan al"
            isChecked = AppPreferences.logoColors(this@DisplaySettingsActivity)
            setOnCheckedChangeListener { _, enabled -> AppPreferences.setLogoColors(this@DisplaySettingsActivity, enabled) }
        })
        addView(SwitchMaterial(this@DisplaySettingsActivity).apply {
            text = "TV Box tam ekran radyo modu"
            isChecked = AppPreferences.tvAmbient(this@DisplaySettingsActivity)
            setOnCheckedChangeListener { _, enabled -> AppPreferences.setTvAmbient(this@DisplaySettingsActivity, enabled) }
        })
        addView(SwitchMaterial(this@DisplaySettingsActivity).apply {
            text = getString(R.string.show_blurred_logo)
            isChecked = AppPreferences.blurLogoEnabled(this@DisplaySettingsActivity)
            setOnCheckedChangeListener { _, enabled -> AppPreferences.setBlurLogoEnabled(this@DisplaySettingsActivity, enabled) }
        })
        addView(steppedSeek(getString(R.string.background_effect), AppPreferences.blurLogoIntensity(this@DisplaySettingsActivity)) {
            AppPreferences.setBlurLogoIntensity(this@DisplaySettingsActivity, it)
        })
        addView(steppedSeek(getString(R.string.history_panel_transparency), AppPreferences.historyPanelTransparency(this@DisplaySettingsActivity)) {
            AppPreferences.setHistoryPanelTransparency(this@DisplaySettingsActivity, it)
        })
    }

    private fun buildPlayerElements() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(check(getString(R.string.show_album_art), AppPreferences.showAlbumArt(this@DisplaySettingsActivity)) { AppPreferences.setShowAlbumArt(this@DisplaySettingsActivity, it) })
        addView(check(getString(R.string.show_station_logo), AppPreferences.showStationLogo(this@DisplaySettingsActivity)) { AppPreferences.setShowStationLogo(this@DisplaySettingsActivity, it) })
        addView(check(getString(R.string.show_station_name), AppPreferences.showStationName(this@DisplaySettingsActivity)) { AppPreferences.setShowStationName(this@DisplaySettingsActivity, it) })
    }

    private fun buildRadioView(): RadioGroup {
        val group = RadioGroup(this)
        val values = mutableMapOf<Int, String>()
        listOf(
            getString(R.string.list_view) to AppPreferences.VIEW_LIST,
            getString(R.string.grid_view_2) to AppPreferences.VIEW_GRID_2,
            getString(R.string.grid_view_4) to AppPreferences.VIEW_GRID_4
        ).forEach { (label, value) ->
            val id = View.generateViewId()
            values[id] = value
            group.addView(RadioButton(this).apply { this.id = id; text = label })
            if (AppPreferences.radioView(this) == value) group.check(id)
        }
        group.setOnCheckedChangeListener { _, id -> values[id]?.let { AppPreferences.setRadioView(this, it) } }
        return group
    }

    private fun steppedSeek(label: String, initial: Int, save: (Int) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(8), 0, dp(4))
        val value = TextView(this@DisplaySettingsActivity).apply { text = "$label  ${initial / 20 * 20}%"; textSize = 15f }
        addView(value)
        addView(SeekBar(this@DisplaySettingsActivity).apply {
            max = 5
            progress = initial / 20
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    value.text = "$label  ${progress * 20}%"
                    if (fromUser) save(progress * 20)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        })
    }

    private fun check(label: String, checked: Boolean, save: (Boolean) -> Unit) = CheckBox(this).apply {
        text = label
        isChecked = checked
        setOnCheckedChangeListener { _, value -> save(value) }
    }

    private fun sectionLabel(label: String) = TextView(this).apply {
        text = label
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(12), 0, dp(5))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
