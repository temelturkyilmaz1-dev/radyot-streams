package com.radyo.turkiyeradyolari

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
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

class SettingsActivity : AppCompatActivity() {
    private val createBackup = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) backupWork {
            val json = FavoritesBackup.encode(FavoritesBackup.names(this), RadioRepository.radios.toList())
            val output = contentResolver.openOutputStream(uri, "wt")
                ?: error("Dosya açılamadı.")
            output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
            "Favori yedeği kaydedildi."
        }
    }

    private val restoreBackup = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) backupWork {
            val input = contentResolver.openInputStream(uri) ?: error("Dosya açılamadı.")
            val bytes = input.use { stream ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (out.size() <= 2 * 1024 * 1024) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    out.write(buffer, 0, count)
                }
                out.toByteArray()
            }
            require(bytes.size <= 2 * 1024 * 1024) { "Yedek dosyası çok büyük." }
            val entries = FavoritesBackup.decode(bytes.toString(Charsets.UTF_8))
            // Apply on main thread so UI favorite changes cannot race the merge.
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    val merged = FavoritesBackup.merge(
                        FavoritesBackup.names(this), entries, RadioRepository.radios.toList()
                    )
                    getSharedPreferences("favorites", MODE_PRIVATE).edit()
                        .putString("favorite_radios", merged.names.joinToString("|")).apply()
                    val message = "${merged.added} favori eklendi." +
                        if (merged.missing.isEmpty()) "" else
                            "\n\nKatalogda bulunamayanlar:\n" + merged.missing.joinToString("\n")
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Geri yükleme tamamlandı").setMessage(message)
                        .setPositiveButton("Tamam", null).show()
                }
            }
            null
        }
    }

    private fun backupWork(work: () -> String?) {
        Thread {
            try {
                val message = work()
                if (message != null) runOnUiThread {
                    if (!isFinishing && !isDestroyed)
                        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed)
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("İşlem tamamlanamadı")
                            .setMessage(error.message ?: "Dosya okunamadı veya yazılamadı.")
                            .setPositiveButton("Tamam", null).show()
                }
            }
        }.start()
    }

    private fun showFavoritesBackup() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Favori Yedekleme")
            .setItems(arrayOf("Yedekle", "Geri Yükle")) { _, selected ->
                if (selected == 0) {
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
                        .format(java.util.Date())
                    createBackup.launch("RadiOnline_Favoriler_${date}.json")
                } else restoreBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(36))
            addView(header())
        }
        content.addMenu(getString(R.string.display_settings), getString(R.string.display_settings_summary)) {
            startActivity(Intent(this, DisplaySettingsActivity::class.java))
        }
        content.addMenu(getString(R.string.player_settings), getString(R.string.player_settings_summary)) {
            startActivity(Intent(this, PlayerSettingsActivity::class.java))
        }
        content.addMenu(getString(R.string.connection_storage), getString(R.string.connection_storage_summary)) {
            startActivity(Intent(this, ConnectionSettingsActivity::class.java))
        }
        content.addMenu(getString(R.string.timer), getString(R.string.timer_summary)) {
            startActivity(Intent(this, TimerSettingsActivity::class.java))
        }
        content.addMenu("Favori Yedekleme", "Favorilerini ve sıralamalarını yedekle veya geri yükle") {
            showFavoritesBackup()
        }
        content.addMenu("Logo Denetimi", "Tüm radyo logolarını eşit boyutta kontrol et") {
            startActivity(Intent(this, LogoAuditActivity::class.java))
        }
        content.addMenu(getString(R.string.about), getString(R.string.about_summary)) {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        val scroll = NestedScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content)
        }
        val baseBottom = dp(28)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.mandatorySystemGestures()
            )
            view.updatePadding(
                top = safe.top,
                left = safe.left,
                right = safe.right,
                bottom = baseBottom + safe.bottom
            )
            insets
        }
        setContentView(scroll)
        ViewCompat.requestApplyInsets(scroll)
    }

    private fun header() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, 0, 0, dp(8))
        addView(ImageButton(this@SettingsActivity).apply {
            setImageResource(R.drawable.ic_back)
            contentDescription = getString(R.string.back)
            background = null
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
    }

    private fun LinearLayout.addMenu(title: String, detail: String, action: () -> Unit) {
        val card = MaterialCardView(this@SettingsActivity).apply {
            radius = dp(18).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer))
            isClickable = true
            isFocusable = true
            foreground = getDrawable(android.R.drawable.list_selector_background)
            setOnClickListener { action() }
        }
        val row = LinearLayout(this@SettingsActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(18), dp(14), dp(14), dp(14))
        }
        val labels = LinearLayout(this@SettingsActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@SettingsActivity).apply {
                text = title
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(TextView(this@SettingsActivity).apply {
                text = detail
                textSize = 14f
                alpha = .72f
                setPadding(0, dp(3), 0, 0)
            })
        }
        row.addView(labels, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(this@SettingsActivity).apply {
            text = "›"
            textSize = 30f
            gravity = Gravity.CENTER
            alpha = .65f
        }, LinearLayout.LayoutParams(dp(34), dp(48)))
        card.addView(row)
        addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, dp(6), 0, dp(6)) })
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
