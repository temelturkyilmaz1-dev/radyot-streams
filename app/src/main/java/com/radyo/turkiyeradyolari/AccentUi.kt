package com.radyo.turkiyeradyolari

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

/** Applies the saved accent without tinting station artwork or semantic actions. */
object AccentUi {
    fun apply(root: View, accent: Int) {
        val tint = ColorStateList.valueOf(accent)
        val contrasting = if (ColorUtils.calculateLuminance(accent) > .55) Color.BLACK else Color.WHITE
        applyRecursive(root, tint, contrasting)
    }

    private fun applyRecursive(view: View, tint: ColorStateList, contrasting: Int) {
        when (view) {
            is MaterialButton -> {
                view.backgroundTintList = tint
                view.setTextColor(contrasting)
                view.iconTint = ColorStateList.valueOf(contrasting)
            }
            is SwitchMaterial -> {
                view.setTextColor(tint.defaultColor)
                view.thumbTintList = tint
            }
            is CompoundButton -> {
                view.setTextColor(tint.defaultColor)
                view.buttonTintList = tint
            }
            is ImageButton -> {
                when {
                    view.id == R.id.btnPlayPause -> {
                        // Primary transport action: keep icon readable against the selected accent.
                        // Previously the generic accent pass tinted both the button and its icon
                        // to the same tone, which could make the play/pause glyph disappear.
                        view.backgroundTintList = tint
                        view.imageTintList = ColorStateList.valueOf(contrasting)
                    }
                    !isSemanticRedAction(view.id) -> view.imageTintList = tint
                }
            }
            is SeekBar -> {
                view.progressTintList = tint
                view.thumbTintList = tint
            }
            is TextView -> view.setTextColor(tint.defaultColor)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyRecursive(view.getChildAt(index), tint, contrasting)
        }
    }

    private fun isSemanticRedAction(id: Int): Boolean =
        id == R.id.btnPlayerFavorite || id == R.id.btnExit || id == R.id.btnFavorite
}
