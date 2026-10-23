package com.radyo.turkiyeradyolari

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesFragment : Fragment(), RadioPlayerManager.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var recycler: RecyclerView
    private lateinit var empty: TextView
    private lateinit var prefs: SharedPreferences
    private var adapter: RadioAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_favorites, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        recycler = view.findViewById(R.id.recyclerFavoritesPage)
        empty = view.findViewById(R.id.txtEmptyFavoritesPage)
        prefs = requireContext().getSharedPreferences("favorites", Context.MODE_PRIVATE)
        refreshFavorites()
    }

    override fun onStart() {
        super.onStart()
        prefs.registerOnSharedPreferenceChangeListener(this)
        RadioPlayerManager.addListener(this)
        refreshFavorites()
    }

    override fun onStop() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        RadioPlayerManager.removeListener(this)
        super.onStop()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "favorite_radios") activity?.runOnUiThread(::refreshFavorites)
    }

    private fun refreshFavorites() {
        if (!::recycler.isInitialized) return
        val context = requireContext()
        val names = prefs.getString("favorite_radios", "").orEmpty()
            .split("|").filter(String::isNotBlank)
        val radios = names.mapNotNull(RadioRepository::byName)
        empty.visibility = if (radios.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility = if (radios.isEmpty()) View.GONE else View.VISIBLE

        val viewMode = AppPreferences.radioView(context)
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val columns = when (viewMode) {
            AppPreferences.VIEW_GRID_4 -> 4
            AppPreferences.VIEW_GRID_2 -> if (landscape) 4 else 2
            else -> 1
        }
        recycler.layoutManager = if (columns == 1) LinearLayoutManager(context)
            else GridLayoutManager(context, columns)
        adapter = RadioAdapter(radios, columns > 1) { radio ->
            if (RadioPlayerManager.currentRadio?.name == radio.name && RadioPlayerManager.isCurrentlyPlaying()) {
                RadioPlayerManager.togglePlayPause(context)
            } else {
                RadioPlayerManager.play(context, radio)
            }
        }.also { recycler.adapter = it }
    }

    override fun onPlayerChanged() {
        activity?.runOnUiThread { adapter?.notifyDataSetChanged() }
    }

    override fun onPlayerError(message: String) = onPlayerChanged()
}
