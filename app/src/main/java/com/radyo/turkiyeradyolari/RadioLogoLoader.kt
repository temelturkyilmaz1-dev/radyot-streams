package com.radyo.turkiyeradyolari

import android.graphics.Color
import android.widget.ImageView
import com.bumptech.glide.Glide

object RadioLogoLoader {
    fun load(view: ImageView, radio: Radio) {
        val padding = (StationLogoOverrides.logoPaddingDpFor(radio) *
            view.resources.displayMetrics.density).toInt()
        view.setPadding(padding, padding, padding, padding)
        view.setBackgroundColor(
            if (StationLogoOverrides.needsLightLogoBackground(radio)) Color.WHITE else Color.TRANSPARENT
        )

        val logoUrl = radio.logoPath.trim()
        if (logoUrl.startsWith("https://", true) || logoUrl.startsWith("http://", true)) {
            Glide.with(view)
                .load(logoUrl)
                .fitCenter()
                .placeholder(R.drawable.radyot_logo)
                .error(R.drawable.radyot_logo)
                .into(view)
        } else {
            view.setImageResource(R.drawable.radyot_logo)
        }
    }
}
