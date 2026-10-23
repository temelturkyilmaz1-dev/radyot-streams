package com.radyo.turkiyeradyolari

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

/** Son açılan radyoları Favoriler ile aynı görünüm kurallarıyla gösterir. */
class RecentStationsFragment : Fragment(), RadioPlayerManager.Listener {
    private lateinit var recycler: RecyclerView
    private lateinit var empty: TextView
    private var adapter: RadioAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View = inflater.inflate(R.layout.fragment_recent_stations, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        recycler = view.findViewById(R.id.recyclerRecentStationsPage)
        empty = view.findViewById(R.id.txtEmptyRecentStationsPage)
        refreshRecentStations()
    }

    override fun onStart() {
        super.onStart()
        RadioPlayerManager.addListener(this)
        refreshRecentStations()
    }

    override fun onStop() {
        RadioPlayerManager.removeListener(this)
        super.onStop()
    }

    private fun refreshRecentStations() {
        if (!::recycler.isInitialized) return
        val context = requireContext()
        val radios = AppPreferences.recentRadios(context).mapNotNull(RadioRepository::byName)
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
            if (RadioPlayerManager.currentRadio?.name == radio.name &&
                RadioPlayerManager.isCurrentlyPlaying()
            ) {
                RadioPlayerManager.togglePlayPause(context)
            } else {
                RadioPlayerManager.play(context, radio)
            }
        }.also { recycler.adapter = it }
    }

    override fun onPlayerChanged() {
        activity?.runOnUiThread(::refreshRecentStations)
    }

    override fun onPlayerError(message: String) = onPlayerChanged()
}
