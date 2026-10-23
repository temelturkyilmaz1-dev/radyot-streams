package com.radyo.turkiyeradyolari

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Bakım ekranı: logoları aynı çerçevede göstererek küçük/karanlık/yanlış olanları buldurur. */
class LogoAuditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        val list = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@LogoAuditActivity, if (resources.configuration.smallestScreenWidthDp >= 600) 5 else 3)
            adapter = LogoAdapter(RadioRepository.radios.distinctBy { it.name }.sortedBy { it.name })
            setPadding(dp(8), dp(8), dp(8), dp(24))
            clipToPadding = false
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(this@LogoAuditActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageButton(this@LogoAuditActivity).apply {
                    setImageResource(R.drawable.ic_back); background = null
                    contentDescription = getString(R.string.back); setOnClickListener { finish() }
                }, LinearLayout.LayoutParams(dp(52), dp(52)))
                addView(TextView(this@LogoAuditActivity).apply {
                    text = "Logo Denetimi • ${RadioRepository.radios.size} radyo"
                    textSize = 21f; typeface = Typeface.DEFAULT_BOLD
                })
            })
            addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.mandatorySystemGestures()
            )
            view.updatePadding(top = safe.top, left = safe.left, right = safe.right)
            insets
        }
        setContentView(content)
        ViewCompat.requestApplyInsets(content)
    }

    private inner class LogoAdapter(private val radios: List<Radio>) : RecyclerView.Adapter<LogoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, type: Int): LogoHolder {
            val box = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(5), dp(7), dp(5), dp(9))
            }
            val image = ImageView(parent.context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            val name = TextView(parent.context).apply { gravity = Gravity.CENTER; maxLines = 2; textSize = 11f }
            box.addView(image, LinearLayout.LayoutParams(-1, dp(92)))
            box.addView(name, LinearLayout.LayoutParams(-1, dp(38)))
            return LogoHolder(box, image, name)
        }
        override fun getItemCount() = radios.size
        override fun onBindViewHolder(holder: LogoHolder, position: Int) {
            val radio = radios[position]
            holder.name.text = radio.name
            RadioLogoLoader.load(holder.image, radio)
        }
    }
    private class LogoHolder(view: LinearLayout, val image: ImageView, val name: TextView) : RecyclerView.ViewHolder(view)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
