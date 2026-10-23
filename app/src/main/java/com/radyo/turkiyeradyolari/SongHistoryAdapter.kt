package com.radyo.turkiyeradyolari

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.DateFormat
import java.util.Date

class SongHistoryAdapter(private val items: List<SongHistoryItem>) : RecyclerView.Adapter<SongHistoryAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val artwork: ImageView = view.findViewById(R.id.imgHistoryArtwork)
        val title: TextView = view.findViewById(R.id.txtHistoryTitle)
        val artist: TextView = view.findViewById(R.id.txtHistoryArtist)
        val time: TextView = view.findViewById(R.id.txtHistoryTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_song_history, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.artist.text = item.artist.ifBlank { item.radioName }
        holder.time.text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.time))
        val fallback = RadioRepository.byName(item.radioName)?.logoRes ?: R.drawable.radyot_logo
        Glide.with(holder.artwork).load(item.artworkUrl).placeholder(fallback).error(fallback).into(holder.artwork)
    }

    override fun getItemCount() = items.size
}
