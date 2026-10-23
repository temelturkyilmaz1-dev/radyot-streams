package com.radyo.turkiyeradyolari

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoriteAdapter(
    private val favorites: List<Radio>,
    private val onRadioClick: (Radio) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {


    class FavoriteViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imgLogo =
            itemView.findViewById<ImageView>(
                R.id.imgFavoriteLogo
            )

        val txtName =
            itemView.findViewById<TextView>(
                R.id.txtFavoriteName
            )
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavoriteViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_favorite_radio,
                    parent,
                    false
                )

        return FavoriteViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: FavoriteViewHolder,
        position: Int
    ) {

        val radio = favorites[position]

        RadioLogoLoader.load(holder.imgLogo, radio)

        holder.txtName.text =
            radio.name


        holder.itemView.setOnClickListener {

            onRadioClick(radio)

        }
    }


    override fun getItemCount(): Int {
        return favorites.size
    }
}
