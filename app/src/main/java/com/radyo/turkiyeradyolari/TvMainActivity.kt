package com.radyo.turkiyeradyolari

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Locale

/**
 * Android TV/box icin telefondaki dort sayfali arayuzden tamamen ayrilmis,
 * Chromecast ve genisletilmis internet kataloglarini baslatmayan hafif ekran.
 */
class TvMainActivity : AppCompatActivity(), RadioPlayerManager.Listener {
    private lateinit var list: RecyclerView
    private lateinit var adapter: TvRadioAdapter
    private lateinit var artwork: ImageView
    private lateinit var backdrop: ImageView
    private lateinit var stationName: TextView
    private lateinit var countryBlock: View
    private lateinit var countryFlag: ImageView
    private lateinit var stationCity: TextView
    private lateinit var metadata: TextView
    private lateinit var status: TextView
    private lateinit var connectionQuality: TextView
    private lateinit var playPause: ImageButton
    private lateinit var allButton: View
    private lateinit var favoritesButton: View
    private lateinit var searchButton: View
    private lateinit var voiceSearchButton: View
    private lateinit var root: View
    private lateinit var ambient: View
    private lateinit var ambientLogo: ImageView
    private lateinit var ambientName: TextView
    private lateinit var ambientTrack: TextView
    private lateinit var ambientDetails: TextView
    private var allStations: List<Radio> = emptyList()
    private var stations: List<Radio> = emptyList()
    private var selectedIndex = 0
    private var renderedRadioName: String? = null
    private var renderedTrackKey = ""
    private var albumUrl: String? = null
    private var showingAlbumArt = false
    private var liveBlink: ObjectAnimator? = null
    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val alternatives = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            .orEmpty()
        applyVoiceSearchResults(alternatives)
    }
    private val artworkHandler = Handler(Looper.getMainLooper())
    private val ambientHandler = Handler(Looper.getMainLooper())
    private val showAmbient = Runnable {
        if (AppPreferences.tvAmbient(this) && RadioPlayerManager.currentRadio != null &&
            RadioPlayerManager.isCurrentlyPlaying()) setAmbientVisible(true)
    }
    private val artworkSwitcher = object : Runnable {
        override fun run() {
            if (!canRotateArtwork() || isFinishing || isDestroyed) return
            showingAlbumArt = !showingAlbumArt
            showCurrentArtworkFace()
            artworkHandler.postDelayed(this, ARTWORK_ROTATION_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_main)
        root = findViewById(R.id.tvRoot)
        ambient = findViewById(R.id.tvAmbientOverlay)
        ambientLogo = findViewById(R.id.tvAmbientLogo)
        ambientName = findViewById(R.id.tvAmbientName)
        ambientTrack = findViewById(R.id.tvAmbientTrack)
        ambientDetails = findViewById(R.id.tvAmbientDetails)

        artwork = findViewById(R.id.tvArtwork)
        backdrop = findViewById(R.id.tvBackdrop)
        stationName = findViewById(R.id.tvStationName)
        countryBlock = findViewById(R.id.tvCountryBlock)
        countryFlag = findViewById(R.id.tvCountryFlag)
        stationCity = findViewById(R.id.tvStationCity)
        metadata = findViewById(R.id.tvMetadata)
        status = findViewById(R.id.tvStatus)
        connectionQuality = findViewById(R.id.tvConnectionQuality)
        playPause = findViewById(R.id.tvPlayPause)
        list = findViewById(R.id.tvRadioList)
        adapter = TvRadioAdapter(emptyList(), ::playRadio, ::toggleFavorite, ::isFavorite)
        list.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        list.adapter = adapter
        list.setHasFixedSize(true)
        list.itemAnimator = null

        findViewById<View>(R.id.tvPrevious).setOnClickListener { playAdjacent(-1) }
        findViewById<View>(R.id.tvNext).setOnClickListener { playAdjacent(1) }
        playPause.setOnClickListener { togglePlayback() }
        allButton = findViewById(R.id.tvAll)
        favoritesButton = findViewById(R.id.tvFavorites)
        val settingsButton = findViewById<View>(R.id.tvSettings)
        searchButton = findViewById(R.id.tvSearch)
        voiceSearchButton = findViewById(R.id.tvVoiceSearch)
        allButton.setOnClickListener { showAll() }
        favoritesButton.setOnClickListener { showFavorites() }
        settingsButton.setOnClickListener { showTvSettings() }
        searchButton.setOnClickListener { showTextSearch() }
        voiceSearchButton.setOnClickListener { startVoiceSearch() }
        listOf(
            searchButton,
            voiceSearchButton,
            allButton,
            favoritesButton,
            settingsButton
        ).forEach(::installTopMenuFocusEffect)
        updateSectionSelection(all = true)

        RadioPlayerManager.addListener(this)
        loadCatalog()
        scheduleAmbient()
    }

    private fun loadCatalog() {
        status.text = "Radyolar hazirlaniyor…"
        Thread({
            // Önce son doğrulanmış Cloudflare kataloğunu önbellekten göster.
            runCatching { RemoteStationCatalog.loadCached(applicationContext) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                applyCatalogSnapshot()
                RemoteStationCatalog.refresh(this) { applyCatalogSnapshot() }
            }
        }, "RadiOnline-TV-Catalog").start()
    }

    private fun applyCatalogSnapshot() {
        if (isFinishing || isDestroyed) return
        val snapshot = RadioRepository.radios.distinctBy { it.name.lowercase() }
            .sortedWith { first, second ->
                java.text.Collator.getInstance(java.util.Locale.forLanguageTag("tr-TR"))
                    .compare(first.name, second.name)
            }
        allStations = snapshot
        val favorites = favoriteStations()
        stations = favorites.ifEmpty { snapshot }
        adapter.submitList(stations)
        updateSectionSelection(
            all = favorites.isEmpty(),
            favorites = favorites.isNotEmpty()
        )
        val remembered = RadioPlayerManager.currentRadio?.name
            ?: AppPreferences.lastRadio(this)
        selectedIndex = stations.indexOfFirst { it.name == remembered }.coerceAtLeast(0)
        showSelectedWithoutStarting()
        list.post {
            (list.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(selectedIndex, 0)
            list.findViewHolderForAdapterPosition(selectedIndex)?.itemView?.requestFocus()
                ?: list.requestFocus()
        }
    }

    private fun showSelectedWithoutStarting() {
        val radio = RadioPlayerManager.currentRadio ?: stations.getOrNull(selectedIndex) ?: return
        stationName.text = radio.name
        showCountryFlag(radio)
        metadata.text = listOf(radio.frequency, radio.location, radio.content)
            .filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Yayin icin OK tusuna basin" }
        RadioLogoLoader.load(artwork, radio)
        refreshPlayerState()
    }

    private fun playRadio(radio: Radio) {
        selectedIndex = stations.indexOfFirst { it.name == radio.name }.coerceAtLeast(0)
        RadioPlayerManager.play(this, radio)
        refreshPlayerState()
    }

    private fun togglePlayback() {
        val current = RadioPlayerManager.currentRadio
        if (current == null) stations.getOrNull(selectedIndex)?.let(::playRadio)
        else RadioPlayerManager.togglePlayPause(this)
    }

    private fun playAdjacent(direction: Int) {
        if (stations.isEmpty()) return
        val currentName = RadioPlayerManager.currentRadio?.name
        val base = stations.indexOfFirst { it.name == currentName }
            .takeIf { it >= 0 } ?: selectedIndex
        selectedIndex = (base + direction + stations.size) % stations.size
        playRadio(stations[selectedIndex])
        list.scrollToPosition(selectedIndex)
    }

    private fun showAll() {
        stations = allStations
        selectedIndex = stations.indexOfFirst {
            it.name == RadioPlayerManager.currentRadio?.name
        }.coerceAtLeast(0)
        adapter.submitList(stations)
        refreshPlayerState()
        updateSectionSelection(all = true)
        focusFirstResult()
    }

    private fun showFavorites() {
        val favorites = favoriteStations()
        if (favorites.isEmpty()) {
            Toast.makeText(this, "Favori radyo bulunamadi", Toast.LENGTH_SHORT).show()
            return
        }
        stations = favorites
        selectedIndex = 0
        adapter.submitList(stations)
        refreshPlayerState()
        updateSectionSelection(favorites = true)
        focusFirstResult()
    }

    private fun showTextSearch() {
        val input = EditText(this).apply {
            hint = "Radyo adını yazın"
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setSelectAllOnFocus(true)
        }
        val density = resources.displayMetrics.density.toInt().coerceAtLeast(1)
        val container = LinearLayout(this).apply {
            setPadding(24 * density, 4 * density, 24 * density, 0)
            addView(input, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Radyo ara")
            .setView(container)
            .setPositiveButton("Ara") { _, _ -> filterStations(input.text.toString()) }
            .setNeutralButton("Temizle") { _, _ -> showAll() }
            .setNegativeButton("Kapat", null)
            .create()
        dialog.setOnShowListener {
            input.requestFocus()
            dialog.window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            )
        }
        dialog.show()
    }

    private fun startVoiceSearch() {
        updateSectionSelection(voice = true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Radyo adını söyleyin")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            voiceSearchLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Bu cihazda sesli arama bulunamadı", Toast.LENGTH_SHORT).show()
            showTextSearch()
        }
    }

    private fun filterStations(query: String) {
        val normalized = RadioSearch.normalize(query)
        if (normalized.isBlank()) {
            showAll()
            return
        }
        val matches = allStations.filter { radio -> RadioSearch.matches(radio, query) }
        if (matches.isEmpty()) {
            Toast.makeText(this, "\"$query\" için radyo bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        stations = matches
        selectedIndex = 0
        adapter.submitList(stations)
        updateSectionSelection(search = true)
        showSelectedWithoutStarting()
        focusFirstResult()
    }

    /**
     * TV ses tanıma motorunun tek bir tahminine bağlı kalmaz. Telefon aramasındaki
     * gibi bütün anlamlı sonuçları katalog alanlarıyla eşleştirir ve en güçlü
     * eşleşmeleri önce gösterir. Böylece kumandayla ikinci bir seçim ekranı gerekmez.
     */
    private fun applyVoiceSearchResults(alternatives: List<String>) {
        val spoken = alternatives.map(String::trim).filter(String::isNotBlank).distinct()
        if (spoken.isEmpty()) {
            Toast.makeText(this, "Ses anlaşılamadı, tekrar deneyin", Toast.LENGTH_SHORT).show()
            updateSectionSelection(all = stations === allStations)
            return
        }
        val queries = spoken.map(RadioSearch::normalize).filter(String::isNotBlank)
        val ranked = allStations.mapNotNull { radio ->
            val name = RadioSearch.normalize(radio.name)
            val searchable = listOf(radio.name, radio.frequency, radio.location, radio.content)
                .joinToString(" ").let(RadioSearch::normalize)
            val score = queries.maxOfOrNull { query ->
                when {
                    name == query -> 100
                    name.startsWith(query) || query.startsWith(name) -> 80
                    name.contains(query) || query.contains(name) -> 60
                    RadioSearch.matches(radio, query) -> 30
                    else -> 0
                }
            } ?: 0
            radio.takeIf { score > 0 }?.let { it to score }
        }.sortedWith(compareByDescending<Pair<Radio, Int>> { it.second }.thenBy { it.first.name })

        if (ranked.isEmpty()) {
            Toast.makeText(this, "\"${spoken.first()}\" için radyo bulunamadı", Toast.LENGTH_LONG).show()
            updateSectionSelection(all = stations === allStations)
            return
        }
        stations = ranked.map { it.first }
        selectedIndex = 0
        adapter.submitList(stations)
        updateSectionSelection(voice = true)
        showSelectedWithoutStarting()
        focusFirstResult()
        Toast.makeText(this, "Aranan: ${spoken.first()}", Toast.LENGTH_SHORT).show()
    }

    private fun updateSectionSelection(
        all: Boolean = false,
        favorites: Boolean = false,
        search: Boolean = false,
        voice: Boolean = false
    ) {
        if (!::allButton.isInitialized) return
        allButton.isSelected = all
        favoritesButton.isSelected = favorites
        searchButton.isSelected = search
        voiceSearchButton.isSelected = voice
    }

    private fun focusFirstResult() {
        list.post {
            (list.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
            list.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                ?: list.requestFocus()
        }
    }

    private fun favoriteNames(): MutableList<String> = getSharedPreferences("favorites", MODE_PRIVATE)
        .getString("favorite_radios", "").orEmpty().split('|')
        .filter(String::isNotBlank).toMutableList()

    private fun favoriteStations(): List<Radio> {
        val names = favoriteNames().toSet()
        return allStations.filter { it.name in names }
    }

    private fun selectedRadio(): Radio? = RadioPlayerManager.currentRadio
        ?: stations.getOrNull(selectedIndex)

    private fun isFavorite(radio: Radio): Boolean = radio.name in favoriteNames()

    private fun toggleFavorite(radio: Radio): Boolean {
        val names = favoriteNames()
        val added = if (names.remove(radio.name)) false else {
            names.add(radio.name)
            true
        }
        getSharedPreferences("favorites", MODE_PRIVATE).edit()
            .putString("favorite_radios", names.distinct().joinToString("|"))
            .apply()
        adapter.notifyFavoriteChanged(radio.name)

        showFavoriteMessage(radio, added)
        return added
    }

    private fun showFavoriteMessage(radio: Radio, added: Boolean) {
        val density = resources.displayMetrics.density
        val message = TextView(this).apply {
            text = if (added) "FAVORİLERE EKLENDİ" else "FAVORİLERDEN ÇIKARILDI"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
        }
        val logo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                (44 * density).toInt(),
                (44 * density).toInt()
            ).apply { marginEnd = (14 * density).toInt() }
        }
        RadioLogoLoader.load(logo, radio)
        val notification = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                (22 * density).toInt(),
                (12 * density).toInt(),
                (26 * density).toInt(),
                (12 * density).toInt()
            )
            addView(logo)
            addView(message)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18 * density
                setColor(if (added) 0xFF178A51.toInt() else 0xFFC62828.toInt())
                setStroke((2 * density).toInt(), Color.WHITE)
            }
            elevation = 10 * density
        }
        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, (92 * density).toInt())
            view = notification
            show()
        }
    }

    private fun showTvSettings() {
        val values = arrayOf(
            AppPreferences.BUFFER_FAST,
            AppPreferences.BUFFER_BALANCED,
            AppPreferences.BUFFER_STABLE
        )
        val labels = arrayOf("Hizli", "Dengeli", "Kararli")
        val current = values.indexOf(AppPreferences.bufferProfile(this)).coerceAtLeast(1)
        AlertDialog.Builder(this)
            .setTitle("Yayin tamponu")
            .setSingleChoiceItems(labels, current) { dialog, which ->
                AppPreferences.setBufferProfile(this, values[which])
                Toast.makeText(this, "Ayar kaydedildi", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun refreshPlayerState() {
        val radio = RadioPlayerManager.currentRadio ?: stations.getOrNull(selectedIndex) ?: return
        val stationChanged = renderedRadioName != radio.name
        if (stationChanged) {
            renderedRadioName = radio.name
            renderedTrackKey = ""
            albumUrl = null
            showingAlbumArt = false
            artworkHandler.removeCallbacks(artworkSwitcher)
        }
        stationName.text = radio.name
        showCountryFlag(radio)
        val artist = RadioPlayerManager.currentArtist
        val title = RadioPlayerManager.currentTitle
        val track = listOf(artist, title)
            .filter(String::isNotBlank).joinToString(" — ")
        metadata.text = track.ifBlank {
            listOf(radio.frequency, radio.location, radio.content)
                .filter { it.isNotBlank() }.joinToString(" • ")
        }
        val trackKey = "$artist|$title"
        if (title.isNotBlank() && renderedTrackKey != trackKey) {
            renderedTrackKey = trackKey
            albumUrl = SongHistoryStore.get(this, radio.name)
                .firstOrNull { it.artist == artist && it.title == title }
                ?.artworkUrl
            showingAlbumArt = !albumUrl.isNullOrBlank()
            if (albumUrl.isNullOrBlank()) requestAlbumArtwork(radio.name, artist, title)
            restartArtworkRotation()
        } else {
            showCurrentArtworkFace()
        }
        val playing = RadioPlayerManager.isCurrentlyPlaying()
        playPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        status.text = when {
            playing && RadioPlayerManager.isUsingBackupStream() -> "CANLI • YEDEK YAYIN"
            playing -> "CANLI • ANA YAYIN"
            else -> "DURAKLATILDI"
        }
        status.setTextColor(if (playing) 0xFF42E695.toInt() else 0xFFB8C3D1.toInt())
        updateConnectionQuality()
        updateLiveBlink(playing)
        updateAmbientContent(radio, track)
        if (AppPreferences.logoColors(this)) root.setBackgroundColor(StationVisualColors.forRadio(this, radio))
    }

    private fun updateConnectionQuality() {
        val quality = RadioPlayerManager.currentConnectionQuality()
        val qualityText = when (quality) {
            RadioPlayerManager.ConnectionQuality.IDLE -> ""
            RadioPlayerManager.ConnectionQuality.CONNECTING -> "Bağlantı: Bağlanıyor…"
            RadioPlayerManager.ConnectionQuality.GOOD -> "Bağlantı: İyi"
            RadioPlayerManager.ConnectionQuality.FAIR -> "Bağlantı: Orta"
            RadioPlayerManager.ConnectionQuality.POOR -> "Bağlantı: Zayıf"
        }
        val audioQuality = RadioPlayerManager.currentAudioQuality()
        connectionQuality.text = listOf(qualityText, audioQuality)
            .filter(String::isNotBlank)
            .joinToString(" • ")
        connectionQuality.setTextColor(
            when (quality) {
                RadioPlayerManager.ConnectionQuality.GOOD -> 0xFF42E695.toInt()
                RadioPlayerManager.ConnectionQuality.FAIR -> 0xFFFFC857.toInt()
                RadioPlayerManager.ConnectionQuality.POOR -> 0xFFFF6B6B.toInt()
                else -> 0xFFB8C3D1.toInt()
            }
        )
    }

    private fun scheduleAmbient() {
        ambientHandler.removeCallbacks(showAmbient)
        if (AppPreferences.tvAmbient(this)) ambientHandler.postDelayed(showAmbient, 90_000L)
    }

    private fun setAmbientVisible(visible: Boolean) {
        ambient.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) scheduleAmbient()
    }

    private fun updateAmbientContent(radio: Radio, track: String) {
        ambientName.text = radio.name
        ambientTrack.text = track.ifBlank { StationInfoResolver.resolve(radio).location }
        val favorite = if (isFavorite(radio)) "♥ FAVORİ" else "CANLI"
        val source = if (RadioPlayerManager.isUsingBackupStream()) " • YEDEK YAYIN" else ""
        ambientDetails.text = favorite + source
        RadioLogoLoader.load(ambientLogo, radio)
        if (AppPreferences.logoColors(this)) {
            val color = StationVisualColors.forRadio(this, radio)
            ambient.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(color, 0xFF070B11.toInt(), color)
            )
        }
    }

    private fun installTopMenuFocusEffect(view: View) {
        view.setOnFocusChangeListener { target, focused ->
            target.animate()
                .scaleX(if (focused) 1.09f else 1f)
                .scaleY(if (focused) 1.09f else 1f)
                .translationZ(if (focused) 14f else 0f)
                .setDuration(140L)
                .start()
        }
    }

    private fun showCountryFlag(radio: Radio) {
        val resolvedLocation = StationInfoResolver.resolve(radio).location
        val code = StationInfoLocalizer.countryCode(radio, resolvedLocation)
            .substringBefore('-')
            .lowercase()
        if (code.length != 2 || code.any { it !in 'a'..'z' }) {
            Glide.with(this).clear(countryFlag)
            countryBlock.visibility = View.GONE
            return
        }
        countryBlock.visibility = View.VISIBLE
        val place = StationInfoLocalizer.placeLabel(radio, resolvedLocation)
        stationCity.text = place
        stationCity.visibility = if (place.isBlank()) View.GONE else View.VISIBLE
        Glide.with(this)
            .load("https://flagcdn.com/w80/$code.png")
            .centerCrop()
            .into(countryFlag)
    }

    private fun updateLiveBlink(playing: Boolean) {
        if (!playing) {
            liveBlink?.cancel()
            liveBlink = null
            status.alpha = 1f
            return
        }
        if (liveBlink?.isRunning == true) return
        liveBlink = ObjectAnimator.ofFloat(status, View.ALPHA, 1f, .58f, 1f).apply {
            duration = 3_200L
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun requestAlbumArtwork(radioName: String, artist: String, title: String) {
        AlbumArtLoader.search(artist, title) { url ->
            val sameRadio = RadioPlayerManager.currentRadio?.name == radioName
            val sameTrack = RadioPlayerManager.currentArtist == artist &&
                RadioPlayerManager.currentTitle == title
            if (!sameRadio || !sameTrack || url.isNullOrBlank()) return@search
            albumUrl = url
            showingAlbumArt = true
            SongHistoryStore.updateArtwork(this, radioName, artist, title, url)
            restartArtworkRotation()
        }
    }

    private fun canRotateArtwork(): Boolean =
        AppPreferences.showAlbumArt(this) && !albumUrl.isNullOrBlank() &&
            AppPreferences.showStationLogo(this)

    private fun restartArtworkRotation() {
        artworkHandler.removeCallbacks(artworkSwitcher)
        showCurrentArtworkFace()
        if (canRotateArtwork()) artworkHandler.postDelayed(artworkSwitcher, ARTWORK_ROTATION_MS)
    }

    private fun showCurrentArtworkFace() {
        val radio = selectedRadio() ?: return
        val albumEnabled = AppPreferences.showAlbumArt(this) && !albumUrl.isNullOrBlank()
        val logoEnabled = AppPreferences.showStationLogo(this)
        if ((showingAlbumArt && albumEnabled) || (albumEnabled && !logoEnabled)) {
            artwork.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(this)
                .load(albumUrl)
                .centerCrop()
                .placeholder(radio.logoRes)
                .error(radio.logoRes)
                .into(artwork)
            Glide.with(this)
                .load(albumUrl)
                .centerCrop()
                .placeholder(radio.logoRes)
                .error(radio.logoRes)
                .into(backdrop)
        } else {
            artwork.scaleType = ImageView.ScaleType.FIT_CENTER
            RadioLogoLoader.load(artwork, radio)
            backdrop.scaleType = ImageView.ScaleType.CENTER_CROP
            RadioLogoLoader.load(backdrop, radio)
        }
    }

    override fun onPlayerChanged() = runOnUiThread { refreshPlayerState() }

    override fun onMetadataChanged(artist: String, title: String) = runOnUiThread {
        refreshPlayerState()
    }

    override fun onPlayerError(message: String) {
        runOnUiThread {
            status.text = "Yayin hatasi"
            updateConnectionQuality()
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (::ambient.isInitialized && ambient.visibility == View.VISIBLE) {
                setAmbientVisible(false)
                return true
            }
            scheduleAmbient()
            when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    playAdjacent(1); return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    playAdjacent(-1); return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                    togglePlayback(); return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        ambientHandler.removeCallbacksAndMessages(null)
        artworkHandler.removeCallbacksAndMessages(null)
        liveBlink?.cancel()
        liveBlink = null
        Glide.with(applicationContext).clear(artwork)
        Glide.with(applicationContext).clear(backdrop)
        Glide.with(applicationContext).clear(countryFlag)
        if (::ambientLogo.isInitialized) Glide.with(applicationContext).clear(ambientLogo)
        RadioPlayerManager.removeListener(this)
        super.onDestroy()
    }

    companion object {
        private const val ARTWORK_ROTATION_MS = 15_000L
    }
}
