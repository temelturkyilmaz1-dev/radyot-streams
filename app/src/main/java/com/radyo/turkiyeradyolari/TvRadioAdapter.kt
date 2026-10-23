package com.radyo.turkiyeradyolari

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Android TV icin yalniz gorunen kartlari baglayan hafif istasyon listesi. */
class TvRadioAdapter(
    private var radios: List<Radio>,
    private val onClick: (Radio) -> Unit,
    private val onLongClick: (Radio) -> Boolean,
    private val isFavorite: (Radio) -> Boolean
) : RecyclerView.Adapter<TvRadioAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView = view.findViewById(R.id.tvRadioLogo)
        val name: TextView = view.findViewById(R.id.tvRadioName)
        val favoriteBadge: ImageView = view.findViewById(R.id.tvFavoriteBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_tv_radio, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val radio = radios[position]
        holder.name.text = radio.name
        RadioLogoLoader.load(holder.logo, radio)
        val favorite = isFavorite(radio)
        holder.favoriteBadge.visibility = if (favorite) View.VISIBLE else View.GONE
        holder.itemView.contentDescription = if (favorite) "${radio.name}, favorilerde" else radio.name
        holder.itemView.setOnClickListener { onClick(radio) }
        holder.itemView.setOnLongClickListener {
            onLongClick(radio)
            true
        }
        holder.itemView.nextFocusUpId = R.id.tvPlayPause
        holder.itemView.setOnKeyListener { view, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN ||
                keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT
            ) return@setOnKeyListener false

            val current = holder.bindingAdapterPosition
            if (current == RecyclerView.NO_POSITION) return@setOnKeyListener true
            val direction = if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
            val target = current + direction
            if (target !in radios.indices) return@setOnKeyListener true

            val recycler = view.parent as? RecyclerView ?: return@setOnKeyListener true
            recycler.scrollToPosition(target)
            recycler.post {
                recycler.findViewHolderForAdapterPosition(target)?.itemView?.requestFocus()
            }
            true
        }
        holder.itemView.setOnFocusChangeListener { view, focused ->
            view.isSelected = focused
            holder.name.isSelected = focused
        }
    }

    override fun getItemCount(): Int = radios.size

    fun submitList(newRadios: List<Radio>) {
        radios = newRadios
        notifyDataSetChanged()
    }

    fun itemAt(position: Int): Radio? = radios.getOrNull(position)

    fun notifyFavoriteChanged(radioName: String) {
        val position = radios.indexOfFirst { it.name == radioName }
        if (position >= 0) notifyItemChanged(position)
    }

}
