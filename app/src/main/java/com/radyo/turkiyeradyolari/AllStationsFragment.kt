package com.radyo.turkiyeradyolari

import android.content.res.Configuration
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class AllStationsFragment : Fragment(), RadioPlayerManager.Listener {
    private lateinit var adapter: RadioAdapter
    private lateinit var recycler: RecyclerView
    private var positionRestored = false
    private var searchQuery = ""
    private var activeFilter = R.id.filterAll
    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult
        val query = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        view?.findViewById<TextInputEditText>(R.id.inputSearch)?.setText(query)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_all_stations, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        val context = requireContext()
        recycler = view.findViewById(R.id.recyclerAllRadios)
        val viewMode = AppPreferences.radioView(context)
        val grid = viewMode != AppPreferences.VIEW_LIST
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        recycler.layoutManager = when (viewMode) {
            AppPreferences.VIEW_GRID_4 -> GridLayoutManager(context, 4)
            AppPreferences.VIEW_GRID_2 -> GridLayoutManager(context, if (landscape) 4 else 2)
            else -> LinearLayoutManager(context)
        }
        adapter = RadioAdapter(sorted(RadioRepository.radios), grid) { radio ->
            if (RadioPlayerManager.currentRadio?.name == radio.name && RadioPlayerManager.isCurrentlyPlaying()) {
                RadioPlayerManager.togglePlayPause(context)
            } else {
                RadioPlayerManager.play(context, radio)
            }
        }
        recycler.adapter = adapter
        restoreListPosition()

        val filterGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.filterGroup)
        updateFilterSelection(filterGroup, activeFilter)
        filterGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    activeFilter = checkedId
                    updateFilterSelection(group, checkedId)
                    applyFilters()
                }
            }
        view.findViewById<TextInputEditText>(R.id.inputSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty().trim()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        view.findViewById<TextInputLayout>(R.id.searchBox).setEndIconOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_search_prompt))
            }
            runCatching { voiceSearchLauncher.launch(intent) }
        }
    }

    private fun applyFilters() {
        adapter.submitList(sorted(RadioRepository.radios.filter { radio ->
            val matchesSearch = RadioSearch.matches(radio, searchQuery)
            matchesSearch && when (activeFilter) {
                R.id.filterNew -> RadioRepository.isRecentlyAdded(radio)
                R.id.filterTurkey -> radio.isFromTurkey()
                R.id.filterWorld -> !radio.isFromTurkey()
                R.id.filterRock -> radio.matchesAny("rock", "metal", "alternative", "grunge")
                R.id.filterRetro -> radio.matchesAny("80s", "80'", "80’", "1980", "90s", "90'", "90’", "1990", "oldies", "nostal")
                R.id.filterJazz -> radio.matchesAny("jazz", "blues", "soul", "funk")
                else -> true
            }
        }))
    }

    private fun sorted(radios: List<Radio>): List<Radio> = radios.sortedWith { first, second ->
        java.text.Collator.getInstance(TURKISH_LOCALE).compare(first.name, second.name)
    }

    private fun updateFilterSelection(group: MaterialButtonToggleGroup, checkedId: Int) {
        val density = resources.displayMetrics.density
        for (index in 0 until group.childCount) {
            val button = group.getChildAt(index) as? com.google.android.material.button.MaterialButton
                ?: continue
            val selected = button.id == checkedId
            button.alpha = if (selected) 1f else 0.68f
            button.strokeWidth = ((if (selected) 3 else 1) * density).toInt()
            button.setTypeface(button.typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
            button.elevation = if (selected) 4f * density else 0f
        }
    }

    private fun Radio.searchableText(): String =
        listOf(name, content, location, website).joinToString(" ").lowercase(TURKISH_LOCALE)

    private fun Radio.matchesAny(vararg terms: String): Boolean {
        val value = searchableText()
        return terms.any(value::contains)
    }

    private fun Radio.isFromTurkey(): Boolean {
        val value = searchableText()
        return value.contains("türkiye") || value.contains("turkey") ||
            value.contains("türkçe") || value.contains("turkish") ||
            website.lowercase(TURKISH_LOCALE).contains(".tr") ||
            TURKISH_LOCATIONS.any(value::contains)
    }

    override fun onStart() {
        super.onStart()
        RadioPlayerManager.addListener(this)
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }

    override fun onStop() {
        saveListPosition()
        RadioPlayerManager.removeListener(this)
        super.onStop()
    }

    override fun onPlayerChanged() {
        activity?.runOnUiThread { if (::adapter.isInitialized) adapter.notifyDataSetChanged() }
    }

    override fun onPlayerError(message: String) = onPlayerChanged()

    private fun restoreListPosition() {
        if (positionRestored) return
        val anchor = AppPreferences.allRadiosAnchor(requireContext()) ?: return
        val position = RadioRepository.radios.indexOfFirst { it.name == anchor }
        if (position < 0) return
        recycler.post {
            (recycler.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                position, AppPreferences.allRadiosOffset(requireContext())
            )
            positionRestored = true
        }
    }

    private fun saveListPosition() {
        if (!::recycler.isInitialized || !::adapter.isInitialized) return
        val manager = recycler.layoutManager as? LinearLayoutManager ?: return
        val position = manager.findFirstVisibleItemPosition()
        if (position == RecyclerView.NO_POSITION) return
        val radio = adapter.itemAt(position) ?: return
        AppPreferences.setAllRadiosPosition(
            requireContext(), radio.name, manager.findViewByPosition(position)?.top ?: 0
        )
    }

    companion object {
        private val TURKISH_LOCALE = Locale.forLanguageTag("tr-TR")
        private val TURKISH_LOCATIONS = listOf(
            "adana", "ankara", "antalya", "balıkesir", "bursa", "çanakkale",
            "denizli", "diyarbakır", "edirne", "erzurum", "eskişehir", "gaziantep",
            "gümüşhane", "hatay", "istanbul", "izmir", "kayseri", "kocaeli",
            "konya", "malatya", "manisa", "mersin", "muğla", "ordu", "sakarya",
            "samsun", "sivas", "tekirdağ", "trabzon", "şanlıurfa", "uşak", "zonguldak"
        )
    }
}
