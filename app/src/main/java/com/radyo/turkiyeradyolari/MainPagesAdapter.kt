package com.radyo.turkiyeradyolari

import android.content.Context
import android.content.res.Configuration
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

/** Ana oynatıcı ve üç radyo sayfasını aynı yatay akışta tutar. */
class MainPagesAdapter(
    private val context: Context,
    val playerPage: View,
    private val onPlay: (Radio) -> Unit,
    private val onVoiceSearch: () -> Unit
) : RecyclerView.Adapter<MainPagesAdapter.PageHolder>() {
    private val inflater = LayoutInflater.from(context)
    private val recentPage = inflater.inflate(R.layout.fragment_recent_stations, null, false)
    private val favoritesPage = inflater.inflate(R.layout.fragment_favorites, null, false)
    private val allPage = inflater.inflate(R.layout.fragment_all_stations, null, false)
    private val pages = listOf(playerPage, recentPage, favoritesPage, allPage)
    private var recentAdapter: RadioAdapter? = null
    private var favoritesAdapter: RadioAdapter? = null
    private lateinit var allAdapter: RadioAdapter
    private var searchQuery = ""
    private var activeFilter = R.id.filterAll
    private var allGridMode = false
    private var safeStart = 0
    private var safeTop = 0
    private var safeEnd = 0
    private var safeBottom = 0
    private var allAnchorName: String? = null
    private var visibleAllRadios: List<Radio> = emptyList()

    init {
        setupAllPage()
        bindSimplePage(
            recentPage, R.id.recyclerRecentStationsPage, R.id.txtEmptyRecentStationsPage,
            emptyList(), true
        )
        bindSimplePage(
            favoritesPage, R.id.recyclerFavoritesPage, R.id.txtEmptyFavoritesPage,
            emptyList(), false
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val frame = android.widget.FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return PageHolder(frame)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val page = pages[position]
        applySafeAreaToPage(page)
        (page.parent as? ViewGroup)?.removeView(page)
        holder.frame.removeAllViews()
        holder.frame.addView(page, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    override fun getItemCount(): Int = pages.size

    /** Arka plan tam ekran kalırken tüm sayfalardaki kullanılabilir içeriği sistem alanlarından korur. */
    fun applySafeArea(start: Int, top: Int, end: Int, bottom: Int) {
        safeStart = start
        safeTop = top
        safeEnd = end
        safeBottom = bottom
        pages.forEach(::applySafeAreaToPage)
    }

    private fun applySafeAreaToPage(page: View) {
        if (page === playerPage) {
            // Oynatıcı görseli ve arka planı gerçek tam ekran kalır. Köşe düğmelerinin
            // güvenli konumu MainActivity tarafından ayrı ayrı ayarlanır.
            page.setPaddingRelative(0, 0, 0, 0)
        } else {
            page.setPaddingRelative(safeStart, safeTop, safeEnd, safeBottom)
        }
    }

    fun refreshRecent() {
        val radios = AppPreferences.recentRadios(context).take(20).mapNotNull(RadioRepository::byName)
        bindSimplePage(
            recentPage, R.id.recyclerRecentStationsPage, R.id.txtEmptyRecentStationsPage, radios, true
        )
    }

    fun refreshFavorites() {
        val names = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            .getString("favorite_radios", "").orEmpty().split("|").filter(String::isNotBlank)
        bindSimplePage(
            favoritesPage, R.id.recyclerFavoritesPage, R.id.txtEmptyFavoritesPage,
            names.mapNotNull(RadioRepository::byName), false
        )
    }

    fun refreshForOrientation() {
        refreshRecent()
        refreshFavorites()
        val recycler = allPage.findViewById<RecyclerView>(R.id.recyclerAllRadios)
        configureLayout(recycler)
        val requestedGridMode = isGrid()
        if (requestedGridMode != allGridMode) {
            allGridMode = requestedGridMode
            allAdapter = RadioAdapter(visibleAllRadios, allGridMode, ::playFromAll)
            recycler.adapter = allAdapter
        }
        applyFilters()
        restoreAllAnchor(recycler)
    }

    fun refreshAll() {
        applyFilters()
        refreshRecent()
        refreshFavorites()
    }

    fun notifyPlayingChanged() {
        recentAdapter?.notifyPlayingChanged()
        favoritesAdapter?.notifyPlayingChanged()
        if (::allAdapter.isInitialized) allAdapter.notifyPlayingChanged()
    }

    fun restoreAllPosition() {
        restoreAllAnchor(allPage.findViewById(R.id.recyclerAllRadios))
    }

    private fun bindSimplePage(page: View, recyclerId: Int, emptyId: Int, radios: List<Radio>, recent: Boolean) {
        val recycler = page.findViewById<RecyclerView>(recyclerId)
        val empty = page.findViewById<TextView>(emptyId)
        empty.visibility = if (radios.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility = if (radios.isEmpty()) View.GONE else View.VISIBLE
        val grid = isGrid()
        configureLayout(recycler)
        val adapter = RadioAdapter(radios, grid, onPlay)
        recycler.adapter = adapter
        if (recent) recentAdapter = adapter else favoritesAdapter = adapter
    }

    private fun setupAllPage() {
        val recycler = allPage.findViewById<RecyclerView>(R.id.recyclerAllRadios)
        val grid = isGrid()
        allGridMode = grid
        configureLayout(recycler)
        // Büyük gömülü katalog düşük güçlü TV cihazlarında ilk ekran çizilmeden
        // başlatılmamalı. Katalog hazır olduğunda refreshAll() gerçek listeyi bağlar.
        allAdapter = RadioAdapter(emptyList(), grid, ::playFromAll)
        recycler.adapter = allAdapter
        val filterGroup = allPage.findViewById<MaterialButtonToggleGroup>(R.id.filterGroup)
        updateFilterSelection(filterGroup, activeFilter)
        filterGroup.addOnButtonCheckedListener { group, checkedId, checked ->
                if (checked) {
                    activeFilter = checkedId
                    updateFilterSelection(group, checkedId)
                    applyFilters()
                }
            }
        allPage.findViewById<TextInputLayout>(R.id.searchBox).setEndIconOnClickListener {
            onVoiceSearch()
        }
        allPage.findViewById<TextInputEditText>(R.id.inputSearch)
            .addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString().orEmpty().trim(); applyFilters()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
    }

    private fun updateFilterSelection(group: MaterialButtonToggleGroup, checkedId: Int) {
        for (index in 0 until group.childCount) {
            val button = group.getChildAt(index) as? com.google.android.material.button.MaterialButton
                ?: continue
            val selected = button.id == checkedId
            button.alpha = if (selected) 1f else 0.68f
            button.strokeWidth = if (selected) dp(3) else dp(1)
            button.setTypeface(button.typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
            button.elevation = if (selected) dp(4).toFloat() else 0f
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    fun setSearchQuery(query: String) {
        allPage.findViewById<TextInputEditText>(R.id.inputSearch).setText(query)
    }

    private fun configureLayout(recycler: RecyclerView) {
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val mode = AppPreferences.radioView(context)
        recycler.layoutManager = when (mode) {
            AppPreferences.VIEW_GRID_4 -> GridLayoutManager(context, 4)
            AppPreferences.VIEW_GRID_2 -> GridLayoutManager(context, if (landscape) 4 else 2)
            else -> LinearLayoutManager(context)
        }
    }

    private fun isGrid(): Boolean = AppPreferences.radioView(context) != AppPreferences.VIEW_LIST

    private fun applyFilters() {
        visibleAllRadios = RadioRepository.radios.filter { radio ->
            val value = listOf(radio.name, radio.content, radio.location, radio.website)
                .joinToString(" ").lowercase(TURKISH_LOCALE)
            RadioSearch.matches(radio, searchQuery) && when (activeFilter) {
                R.id.filterNew -> RadioRepository.isRecentlyAdded(radio)
                R.id.filterTurkey -> value.contains("türkiye") || value.contains("turkey") ||
                    value.contains("türkçe") || value.contains("turkish") ||
                    radio.website.lowercase(TURKISH_LOCALE).contains(".tr") ||
                    TURKISH_LOCATIONS.any(value::contains)
                R.id.filterWorld -> !(value.contains("türkiye") || value.contains("turkey") ||
                    value.contains("türkçe") || value.contains("turkish") ||
                    radio.website.lowercase(TURKISH_LOCALE).contains(".tr") ||
                    TURKISH_LOCATIONS.any(value::contains))
                R.id.filterRock -> listOf("rock", "metal", "alternative", "grunge").any(value::contains)
                R.id.filterRetro -> listOf("80s", "1980", "90s", "1990", "oldies", "nostal").any(value::contains)
                R.id.filterJazz -> listOf("jazz", "blues", "soul", "funk").any(value::contains)
                else -> true
            }
        }.sortedWith { first, second ->
            java.text.Collator.getInstance(TURKISH_LOCALE).compare(first.name, second.name)
        }
        allAdapter.submitList(visibleAllRadios)
    }

    private fun playFromAll(radio: Radio) {
        allAnchorName = radio.name
        onPlay(radio)
    }

    private fun restoreAllAnchor(recycler: RecyclerView) {
        val index = allAnchorName?.let { name -> visibleAllRadios.indexOfFirst { it.name == name } } ?: return
        if (index < 0) return
        recycler.post {
            (recycler.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
        }
    }

    class PageHolder(val frame: android.widget.FrameLayout) : RecyclerView.ViewHolder(frame)

    companion object {
        private val TURKISH_LOCALE = Locale.forLanguageTag("tr-TR")
        private val TURKISH_LOCATIONS = listOf(
            "adana", "ankara", "antalya", "balıkesir", "bursa", "çanakkale", "denizli",
            "diyarbakır", "edirne", "erzurum", "eskişehir", "gaziantep", "istanbul", "izmir",
            "kayseri", "kocaeli", "konya", "malatya", "manisa", "mersin", "muğla", "samsun",
            "sivas", "tekirdağ", "trabzon", "şanlıurfa", "uşak", "zonguldak"
        )
    }
}
