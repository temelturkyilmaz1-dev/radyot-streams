package com.radyo.turkiyeradyolari

import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

class RecentRadioAdapter(
    private val radios: List<Radio>,
    private val onClick: (Radio) -> Unit
) : RecyclerView.Adapter<RecentRadioAdapter.Holder>() {
    class Holder(val card: MaterialCardView, val logo: ImageView, val title: TextView) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val context = parent.context
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
        val logo = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setPadding(dp(1), dp(1), dp(1), dp(1)) }
        val title = TextView(context).apply {
            textSize = 12f; typeface = Typeface.DEFAULT_BOLD; maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END; gravity = Gravity.CENTER_VERTICAL
        }
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(7), dp(4), dp(9), dp(4))
            addView(logo, LinearLayout.LayoutParams(dp(48), dp(48)))
            addView(title, LinearLayout.LayoutParams(dp(82), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(5) })
        }
        val card = MaterialCardView(context).apply {
            radius = dp(16).toFloat(); cardElevation = 0f; strokeWidth = dp(1)
            strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant)
            setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer))
            isClickable = true; isFocusable = true; addView(row)
            layoutParams = ViewGroup.MarginLayoutParams(dp(146), dp(54)).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
        }
        return Holder(card, logo, title)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val radio = radios[position]
        RadioLogoLoader.load(holder.logo, radio)
        holder.title.text = radio.name
        holder.card.strokeColor = if (RadioPlayerManager.currentRadio?.name == radio.name)
            AppPreferences.accent(holder.card.context)
        else MaterialColors.getColor(holder.card, com.google.android.material.R.attr.colorOutlineVariant)
        holder.card.setOnClickListener { onClick(radio) }
    }

    override fun getItemCount() = radios.size
}
