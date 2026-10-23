package com.radyo.turkiyeradyolari

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class RadioAdapter(
    private var radios: List<Radio>,
    private val grid: Boolean,
    private val onRadioClick: (Radio) -> Unit
) : RecyclerView.Adapter<RadioAdapter.Holder>() {

    private val favoriteNames = linkedSetOf<String>()
    private var lastPlayingName: String? = RadioPlayerManager.currentRadio?.name

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.radioCard)
        val logo: ImageView = view.findViewById(R.id.imgLogo)
        val name: TextView = view.findViewById(R.id.txtRadioName)
        val favorite: ImageButton = view.findViewById(R.id.btnFavorite)
        val playingEqualizer: PlayingEqualizerView = view.findViewById(R.id.playingEqualizer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        if (favoriteNames.isEmpty()) favoriteNames.addAll(readFavorites(parent.context))
        val layout = if (grid) R.layout.item_radio_grid else R.layout.item_radio
        return Holder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val radio = radios[position]
        val context = holder.itemView.context
        holder.name.text = radio.name
        holder.name.tag = radio.name
        RadioLogoLoader.load(holder.logo, radio)
        val isCurrentPlaying = RadioPlayerManager.currentRadio?.name == radio.name && RadioPlayerManager.isCurrentlyPlaying()
        holder.name.isSelected = isCurrentPlaying
        holder.itemView.isFocusable = true
        holder.itemView.setOnFocusChangeListener { _, focused ->
            holder.name.isSelected = focused ||
                (RadioPlayerManager.currentRadio?.name == radio.name && RadioPlayerManager.isCurrentlyPlaying())
        }
        holder.itemView.setOnHoverListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER -> holder.name.isSelected = true
                MotionEvent.ACTION_HOVER_EXIT -> holder.name.isSelected =
                    RadioPlayerManager.currentRadio?.name == radio.name && RadioPlayerManager.isCurrentlyPlaying()
            }
            false
        }
        holder.itemView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> holder.name.isSelected = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> holder.name.postDelayed({
                    holder.name.isSelected = holder.name.tag == radio.name &&
                        holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
                        RadioPlayerManager.currentRadio?.name == radio.name &&
                        RadioPlayerManager.isCurrentlyPlaying()
                }, 2_500L)
            }
            false
        }
        holder.card.strokeColor = AppPreferences.accent(context)
        holder.card.strokeWidth = if (isCurrentPlaying) 3 else 1
        holder.playingEqualizer.setAccentColor(AppPreferences.accent(context))
        holder.playingEqualizer.setPlaying(isCurrentPlaying)
        holder.itemView.setOnClickListener { onRadioClick(radio) }
        bindFavoriteIcon(holder, radio, context)

        holder.favorite.setOnClickListener {
            if (favoriteNames.remove(radio.name).not()) {
                favoriteNames.add(radio.name)
            }
            saveFavorites(context)
            bindFavoriteIcon(holder, radio, context)
        }
    }

    private fun bindFavoriteIcon(holder: Holder, radio: Radio, context: Context) {
        val selected = favoriteNames.contains(radio.name)
        holder.favorite.setImageResource(
            if (selected) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        )
        holder.favorite.imageTintList = ColorStateList.valueOf(0xFFE53935.toInt())
        holder.favorite.contentDescription = if (selected) {
            "${radio.name} favorilerden çıkar"
        } else {
            "${radio.name} favorilere ekle"
        }
        holder.favorite.alpha = if (selected) 1f else 0.72f
    }

    private fun saveFavorites(context: Context) {
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            .edit()
            .putString("favorite_radios", favoriteNames.joinToString("|"))
            .apply()
    }

    override fun getItemCount() = radios.size

    fun submitList(value: List<Radio>) {
        radios = value
        notifyDataSetChanged()
    }

    /** Metadata değişiminde binlerce kartı ve logoyu yeniden bağlamak yerine
     * yalnızca çalma işareti değişebilen eski/yeni istasyonları günceller. */
    fun notifyPlayingChanged() {
        val current = RadioPlayerManager.currentRadio?.name
        val oldIndex = lastPlayingName?.let { old -> radios.indexOfFirst { it.name == old } } ?: -1
        val newIndex = current?.let { name -> radios.indexOfFirst { it.name == name } } ?: -1
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0 && newIndex != oldIndex) notifyItemChanged(newIndex)
        lastPlayingName = current
    }

    fun itemAt(position: Int): Radio? = radios.getOrNull(position)

    private fun readFavorites(context: Context): List<String> =
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            .getString("favorite_radios", "")
            .orEmpty()
            .split("|")
            .filter { it.isNotBlank() }
}
