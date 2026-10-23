package com.radyo.turkiyeradyolari

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.ColorUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

class MainActivity : AppCompatActivity(), RadioPlayerManager.Listener {
    companion object {
        private const val STREAM_HEALTH_PERMISSION_REQUEST = 1405
        private const val ARTWORK_ROTATION_MS = 15_000L
    }

    private lateinit var artwork: ImageView
    private lateinit var stationLogo: ImageView
    private lateinit var logoBackground: ImageView
    private lateinit var backgroundShade: View
    private lateinit var radioName: TextView
    private lateinit var song: TextView
    private lateinit var playerEqualizer: PlayingEqualizerView
    private lateinit var streamQuality: TextView
    private lateinit var playPause: ImageButton
    private lateinit var previous: ImageButton
    private lateinit var next: ImageButton
    private lateinit var volume: SeekBar
    private lateinit var volumePercent: TextView
    private lateinit var playerLayout: LinearLayout
    private lateinit var artworkContainer: FrameLayout
    private lateinit var playerControlsColumn: LinearLayout
    private lateinit var playerFavorite: ImageButton
    private lateinit var stationInfo: ImageButton
    private lateinit var stationInfoPanel: MaterialCardView
    private lateinit var infoStationLogo: ImageView
    private lateinit var infoStationName: TextView
    private lateinit var infoContent: TextView
    private lateinit var infoLocationRow: View
    private lateinit var infoLocation: TextView
    private lateinit var infoCountryFlag: ImageView
    private lateinit var infoDetails: TextView
    private lateinit var infoWebsite: TextView
    private lateinit var infoWebsiteButton: MaterialButton
    private lateinit var infoShareButton: MaterialButton
    private lateinit var mainRoot: ConstraintLayout
    private val portraitConstraints = ConstraintSet()
    private lateinit var historyToggle: MaterialButton
    private lateinit var historyOverlay: MaterialCardView
    private lateinit var historyTitle: TextView
    private lateinit var historyEmpty: TextView
    private lateinit var historyRecycler: RecyclerView
    private lateinit var historyAll: MaterialButton
    private lateinit var castController: CastPlaybackController
    private lateinit var mainPager: ViewPager2
    private lateinit var pagesAdapter: MainPagesAdapter
    @Volatile private var catalogReady = false
    private lateinit var playerPage: View
    private var volumeTouchActive = false

    private var albumUrl: String? = null
    private var renderedRadioName: String? = null
    private var historyOpen = false
    private var stationInfoOpen = false
    private val artworkHandler = Handler(Looper.getMainLooper())
    private val playerRefresh = Runnable { refreshPlayer() }
    private var showingAlbumArt = true
    private val artworkSwitcher = object : Runnable {
        override fun run() {
            switchArtworkFace()
            artworkHandler.postDelayed(this, ARTWORK_ROTATION_MS)
        }
    }

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val query = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
            .trim()
        if (query.isNotBlank() && ::pagesAdapter.isInitialized) {
            pagesAdapter.setSearchQuery(query)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContentView(R.layout.activity_main_pages)
        playerPage = layoutInflater.inflate(R.layout.activity_main, null, false)
        mainPager = super.findViewById(R.id.mainPager)
        pagesAdapter = MainPagesAdapter(
            context = this,
            playerPage = playerPage,
            onPlay = ::playSelectedRadio,
            onVoiceSearch = ::launchVoiceSearch
        )
        mainPager.adapter = pagesAdapter
        // Dört sayfayı ve büyük radyo listesini aynı anda hazırlamak bazı Android
        // TV cihazlarında ilk ekranı kilitliyordu. Yalnız komşu sayfa tutulur.
        mainPager.offscreenPageLimit = 1
        setupPageDots()
        bindViews()
        val castButton = playerView<androidx.mediarouter.app.MediaRouteButton>(R.id.btnCast)
        val isTelevision = (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION
        if (isTelevision) {
            castButton.visibility = View.GONE
        } else {
            runCatching {
                CastPlaybackController(this).also { it.attach(castButton) }
            }.onSuccess {
                castController = it
            }.onFailure {
                // Cast modülü olmayan cihazda uygulama çalışmaya devam eder.
                castButton.visibility = View.GONE
            }
        }
        moveTopActionsToPlayerOverlay()
        hideHistoryOverlayImmediately()
        portraitConstraints.clone(mainRoot)
        applyOrientationLayout(resources.configuration.orientation)
        artwork.cameraDistance = resources.displayMetrics.density * 8000f
        applySystemBarInsets()
        applyAccent()
        applyBackgroundLogoSettings()
        applyPlayerElementSettings()
        applyHistoryPanelAppearance()
        setupActions()
        setupVolume()
        // İlk kare ve oynatıcı hemen çizilir. Son Cloudflare kataloğu önbellekten
        // açılır; yeni katalog ağdan ayrı iş parçacığında yenilenir.
        val appContext = applicationContext
        Thread({
            runCatching {
                RemoteStationCatalog.loadCached(appContext)
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                catalogReady = true
                pagesAdapter.refreshAll()
                refreshFavorites()
                restoreLastRadio()
                refreshPlayer()
                RemoteStationCatalog.refresh(this) {
                    refreshFavorites()
                    refreshPlayer()
                    pagesAdapter.refreshAll()
                }
                StreamHealthMonitor.schedule(this)
                requestStreamHealthNotificationPermission()
            }
        }, "RadiOnline-CatalogInit").start()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (stationInfoOpen) closeStationInfo()
                else if (historyOpen) closeHistoryOverlay()
                else if (mainPager.currentItem != 0) mainPager.setCurrentItem(0, true)
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun <T : View> playerView(id: Int): T = playerPage.findViewById(id)

    private fun setupPageDots() {
        val dots = super.findViewById<LinearLayout>(R.id.mainPageDots)
        val density = resources.displayMetrics.density
        repeat(4) {
            dots.addView(View(this).apply {
                isClickable = false
                isFocusable = false
            }, LinearLayout.LayoutParams((7 * density).toInt(), (7 * density).toInt()).apply {
                marginStart = (4 * density).toInt(); marginEnd = (4 * density).toInt()
            })
        }
        fun render(selected: Int) {
            val accent = AppPreferences.accent(this)
            repeat(dots.childCount) { index ->
                dots.getChildAt(index).background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (index == selected) accent else 0x66999999)
                }
                dots.getChildAt(index).alpha = if (index == selected) 1f else .72f
            }
        }
        render(0)
        mainPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                render(position)
                if (position == 1) pagesAdapter.refreshRecent()
                if (position == 2) pagesAdapter.refreshFavorites()
                if (position == 3) pagesAdapter.restoreAllPosition()
                pagesAdapter.notifyPlayingChanged()
            }
        })
    }

    private fun updatePagerInputEnabled() {
        mainPager.isUserInputEnabled = !stationInfoOpen && !historyOpen && !volumeTouchActive
    }

    private fun requestStreamHealthNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                STREAM_HEALTH_PERMISSION_REQUEST
            )
        } else {
            StreamHealthMonitor.check(this) {}
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STREAM_HEALTH_PERMISSION_REQUEST &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            StreamHealthMonitor.schedule(this)
            StreamHealthMonitor.check(this) {}
        }
    }

    private fun bindViews() {
        artwork = playerView(R.id.imgArtwork)
        stationLogo = playerView(R.id.imgStationLogo)
        logoBackground = playerView(R.id.imgLogoBackground)
        backgroundShade = playerView(R.id.viewBackgroundShade)
        radioName = playerView(R.id.txtRadioName)
        song = playerView(R.id.txtSong)
        playerEqualizer = playerView(R.id.playerEqualizer)
        streamQuality = playerView(R.id.txtStreamQuality)
        streamQuality.isSelected = true
        playPause = playerView(R.id.btnPlayPause)
        previous = playerView(R.id.btnPrevious)
        next = playerView(R.id.btnNext)
        volume = playerView(R.id.seekVolume)
        volumePercent = playerView(R.id.txtVolumePercent)
        playerLayout = playerView(R.id.playerLayout)
        artworkContainer = playerView(R.id.artworkContainer)
        playerControlsColumn = playerView(R.id.playerControlsColumn)
        playerFavorite = playerView(R.id.btnPlayerFavorite)
        stationInfo = playerView(R.id.btnStationInfo)
        stationInfoPanel = playerView(R.id.stationInfoPanel)
        infoStationLogo = playerView(R.id.imgInfoStationLogo)
        infoStationName = playerView(R.id.txtInfoStationName)
        infoContent = playerView(R.id.txtInfoContent)
        infoLocationRow = playerView(R.id.infoLocationRow)
        infoLocation = playerView(R.id.txtInfoLocation)
        infoCountryFlag = playerView(R.id.imgInfoCountryFlag)
        infoDetails = playerView(R.id.txtInfoDetails)
        infoWebsite = playerView(R.id.txtInfoWebsite)
        infoWebsiteButton = playerView(R.id.btnInfoWebsite)
        infoShareButton = playerView(R.id.btnInfoShare)
        mainRoot = playerView(R.id.mainRoot)
        historyToggle = playerView(R.id.btnHistoryToggle)
        historyOverlay = playerView(R.id.historyOverlayCard)
        historyTitle = playerView(R.id.txtHistoryOverlayTitle)
        historyEmpty = playerView(R.id.txtHistoryOverlayEmpty)
        historyRecycler = playerView(R.id.recyclerHistoryOverlay)
        historyAll = playerView(R.id.btnHistoryOverlayAll)
    }

    private fun applyOrientationLayout(orientation: Int) {
        portraitConstraints.applyTo(mainRoot)
        val set = ConstraintSet().apply { clone(mainRoot) }
        fun connect(id: Int, side: Int, target: Int, targetSide: Int, margin: Int = 0) {
            set.connect(id, side, target, targetSide, margin)
        }
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp4 = (4 * resources.displayMetrics.density).toInt()
        val dp48 = (48 * resources.displayMetrics.density).toInt()

        set.clear(R.id.playerCard)
        connect(R.id.playerCard, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(R.id.playerCard, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(R.id.playerCard, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(R.id.playerCard, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        set.clear(R.id.btnHistoryToggle)
        set.setVisibility(R.id.btnHistoryToggle, View.GONE)
        set.constrainWidth(R.id.btnHistoryToggle, 0)
        set.constrainHeight(R.id.btnHistoryToggle, dp48)
        set.setHorizontalWeight(R.id.btnHistoryToggle, 1f)
        connect(R.id.btnHistoryToggle, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp8)
        connect(R.id.btnHistoryToggle, ConstraintSet.END, R.id.btnFavorites, ConstraintSet.START, dp4)
        connect(R.id.btnHistoryToggle, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dp8)

        set.clear(R.id.btnFavorites)
        set.setVisibility(R.id.btnFavorites, View.GONE)
        set.constrainWidth(R.id.btnFavorites, 0)
        set.constrainHeight(R.id.btnFavorites, dp48)
        set.setHorizontalWeight(R.id.btnFavorites, 1f)
        connect(R.id.btnFavorites, ConstraintSet.START, R.id.btnHistoryToggle, ConstraintSet.END, dp4)
        connect(R.id.btnFavorites, ConstraintSet.END, R.id.btnAllRadios, ConstraintSet.START, dp4)
        connect(R.id.btnFavorites, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dp8)

        set.clear(R.id.btnAllRadios)
        set.setVisibility(R.id.btnAllRadios, View.GONE)
        set.constrainWidth(R.id.btnAllRadios, 0)
        set.constrainHeight(R.id.btnAllRadios, dp48)
        set.setHorizontalWeight(R.id.btnAllRadios, 1f)
        connect(R.id.btnAllRadios, ConstraintSet.START, R.id.btnFavorites, ConstraintSet.END, dp4)
        connect(R.id.btnAllRadios, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dp8)
        connect(R.id.btnAllRadios, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dp8)

        set.setVisibility(R.id.txtRecentStationsTitle, View.GONE)
        set.setVisibility(R.id.recyclerRecentStations, View.GONE)

        set.clear(R.id.historyOverlayCard)
        connect(R.id.historyOverlayCard, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp8)
        connect(R.id.historyOverlayCard, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dp8)
        connect(R.id.historyOverlayCard, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dp8)
        // Şarkı geçmişi oynatıcıyı tamamen kapatmasın. Panel ekranın yalnızca
        // üst yarısında kalır; alt yarıda oynatıcı görünmeye devam eder.
        set.constrainHeight(R.id.historyOverlayCard, 0)
        set.constrainPercentHeight(R.id.historyOverlayCard, 0.50f)
        set.setVisibility(
            R.id.historyOverlayCard,
            if (historyOpen) View.VISIBLE else View.GONE
        )
        set.applyTo(mainRoot)
        applyPlayerContentOrientation(orientation == Configuration.ORIENTATION_LANDSCAPE)

        listOf(R.id.btnHistoryToggle, R.id.btnFavorites, R.id.btnAllRadios).forEach { id ->
            playerView<MaterialButton>(id).apply {
                maxLines = 1
                setPadding(dp4, paddingTop, dp4, paddingBottom)
            }
        }
    }

    private fun applyPlayerContentOrientation(landscape: Boolean) {
        val density = resources.displayMetrics.density
        val dp8 = (8 * density).toInt()
        val dp12 = (12 * density).toInt()
        val dp20 = (20 * density).toInt()
        val dp24 = (24 * density).toInt()
        val dp36 = (36 * density).toInt()
        val dp64 = (64 * density).toInt()
        val dp72 = (72 * density).toInt()
        val dp180 = (180 * density).toInt()
        artwork.animate().cancel()
        artwork.rotationY = 0f
        if (landscape) {
            playerLayout.orientation = LinearLayout.HORIZONTAL
            playerLayout.gravity = android.view.Gravity.CENTER_VERTICAL
            playerLayout.setPadding(dp20, dp12, dp20, dp12)
            artworkContainer.layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, .90f
            ).apply { marginEnd = dp24 }
            playerControlsColumn.layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1.10f
            )
            playerControlsColumn.setPadding(dp8, 0, dp8, 0)
        } else {
            playerLayout.orientation = LinearLayout.VERTICAL
            // Tam ekran oynatıcıdaki sabit içerik grubunu üstte yığmak yerine
            // kullanılabilir yüksekliğin ortasına yayar. Arka plan uçtan uca kalır.
            playerLayout.gravity = android.view.Gravity.CENTER
            playerLayout.setPadding(dp24, dp36, dp24, dp20)
            artworkContainer.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            playerControlsColumn.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            playerControlsColumn.setPadding(0, 0, 0, 0)
        }
        playerLayout.post {
            val square = if (landscape) {
                minOf(
                    (playerLayout.height - dp72).coerceAtLeast(dp180),
                    (playerLayout.width * .36f).toInt()
                )
            } else {
                minOf(
                    (playerLayout.width - dp64).coerceAtLeast(dp180),
                    (playerLayout.height * .36f).toInt()
                )
            }.coerceAtLeast(dp180)
            artworkContainer.layoutParams = if (landscape) {
                LinearLayout.LayoutParams(square, square).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    marginEnd = dp24
                }
            } else {
                LinearLayout.LayoutParams(square, square).apply {
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                }
            }
            playerControlsColumn.layoutParams = if (landscape) {
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            } else {
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            updateStationInfoPanelLayout(landscape)
            playerLayout.requestLayout()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideHistoryOverlayImmediately()
        applyOrientationLayout(newConfig.orientation)
        pagesAdapter.refreshForOrientation()
        ViewCompat.requestApplyInsets(super.findViewById(R.id.mainPagesRoot))
    }

    private fun applySystemBarInsets() {
        val root = super.findViewById<View>(R.id.mainPagesRoot)
        val dots = super.findViewById<View>(R.id.mainPageDots)
        val dotStripHeight = (20 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigation = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val bottomSafeArea = maxOf(navigation.bottom, gestures.bottom)
            val safeTop = maxOf(status.top, cutout.top)
            val safeStart = maxOf(navigation.left, cutout.left)
            val safeEnd = maxOf(navigation.right, cutout.right)

            (dots.layoutParams as ConstraintLayout.LayoutParams).apply {
                // Noktalar içerik alanını aşağı itmeden, durum çubuğunun hemen
                // altında üst katman olarak görünür.
                topMargin = safeTop
                dots.layoutParams = this
            }
            applyPlayerTopActionInsets(safeTop, safeStart, safeEnd)
            pagesAdapter.applySafeArea(
                safeStart,
                safeTop + dotStripHeight,
                safeEnd,
                bottomSafeArea
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun applyPlayerTopActionInsets(safeTop: Int, safeStart: Int, safeEnd: Int) {
        val density = resources.displayMetrics.density
        val baseMargin = (14 * density).toInt()
        val info = playerView<View>(R.id.btnStationInfo)
        (info.layoutParams as? FrameLayout.LayoutParams)?.apply {
            setMargins(baseMargin + safeStart, baseMargin + safeTop, baseMargin, baseMargin)
            info.layoutParams = this
        }
        val settings = playerView<View>(R.id.btnSettings)
        (settings.layoutParams as? FrameLayout.LayoutParams)?.apply {
            setMargins(baseMargin, baseMargin + safeTop, baseMargin + safeEnd, baseMargin)
            settings.layoutParams = this
        }
    }

    private fun applyAccent() {
        val accent = AppPreferences.accent(this)
        playerView<MaterialCardView>(R.id.playerCard).strokeColor = accent
        val labelColor = if (ColorUtils.calculateLuminance(accent) > .55) {
            0xFF000000.toInt()
        } else {
            0xFFFFFFFF.toInt()
        }
        listOf(R.id.btnHistoryToggle, R.id.btnFavorites, R.id.btnAllRadios).forEach { id ->
            playerView<MaterialButton>(id).apply {
                setBackgroundColor(accent)
                setTextColor(labelColor)
            }
        }
        historyOverlay.strokeColor = accent
        volume.progressTintList = android.content.res.ColorStateList.valueOf(accent)
        volume.thumbTintList = android.content.res.ColorStateList.valueOf(accent)
        playerEqualizer.setAccentColor(accent)
        AccentUi.apply(super.findViewById(android.R.id.content), accent)
    }

    private fun applyBackgroundLogoSettings() {
        val enabled = AppPreferences.blurLogoEnabled(this)
        logoBackground.visibility = if (enabled) View.VISIBLE else View.GONE
        backgroundShade.visibility = if (enabled) View.VISIBLE else View.GONE
        if (!enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) logoBackground.setRenderEffect(null)
            return
        }

        val intensity = AppPreferences.blurLogoIntensity(this).coerceIn(0, 100)
        val fraction = intensity / 100f
        logoBackground.alpha = 0.28f + (fraction * 0.62f)
        backgroundShade.alpha = fraction * 0.82f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (intensity == 0) {
                logoBackground.setRenderEffect(null)
            } else {
                val radius = 2f + fraction * 78f
                logoBackground.setRenderEffect(
                    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                )
            }
        }
    }

    private fun applyPlayerElementSettings() {
        val showName = AppPreferences.showStationName(this)
        stationLogo.visibility = View.GONE
        radioName.visibility = if (showName) View.VISIBLE else View.GONE
        updateArtwork()
        restartArtworkRotation()
    }

    private fun applyHistoryPanelAppearance() {
        val transparency = AppPreferences.historyPanelTransparency(this).coerceIn(0, 100)
        val surface = MaterialColors.getColor(historyOverlay, com.google.android.material.R.attr.colorSurface)
        // Son çalanlar kartında 20'lik adımların gerçekten fark edilmesi için
        // tam ayar aralığını kullan; metin ve düğmeler opak kalır.
        val alpha = ((100 - transparency) * 255 / 100).coerceIn(36, 255)
        historyOverlay.setCardBackgroundColor(ColorUtils.setAlphaComponent(surface, alpha))
    }

    private fun setupActions() {
        playerView<ImageButton>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        playerView<ImageButton>(R.id.btnExit).setOnClickListener { confirmExit() }
        playerView<ImageButton>(R.id.btnPlayerTimer).setOnClickListener {
            startActivity(Intent(this, TimerSettingsActivity::class.java))
        }
        playerView<ImageButton>(R.id.btnPlayerShare).setOnClickListener {
            shareCurrentStation()
        }
        playerView<ImageButton>(R.id.btnPlayerHistory).setOnClickListener {
            if (historyOpen) closeHistoryOverlay() else openHistoryOverlay()
        }
        playerFavorite.setOnClickListener { toggleCurrentFavorite() }
        stationInfo.setOnClickListener {
            if (stationInfoOpen) closeStationInfo() else showStationInfo()
        }
        infoWebsiteButton.setOnClickListener {
            val website = infoWebsiteButton.tag as? String ?: ""
            if (website.isNotBlank()) {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(website))) }
            }
        }
        infoShareButton.setOnClickListener { shareCurrentStation() }
        stationInfoPanel.setOnClickListener { closeStationInfo() }
        playPause.setOnClickListener {
            val handledByCast = ::castController.isInitialized && castController.toggleRemotePlayback()
            if (!handledByCast) {
                RadioPlayerManager.togglePlayPause(this)
            }
            refreshPlayer()
            if (handledByCast) artworkHandler.postDelayed(playerRefresh, 350L)
        }
        previous.setOnClickListener {
            playRadioAdjacent(-1)
        }
        next.setOnClickListener {
            playRadioAdjacent(+1)
        }
        playerView<ImageButton>(R.id.btnMute).setOnClickListener {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
            syncVolume()
        }
        historyToggle.setOnClickListener {
            if (historyOpen) closeHistoryOverlay() else openHistoryOverlay()
        }
        historyAll.setOnClickListener {
            val radio = RadioPlayerManager.currentRadio ?: return@setOnClickListener
            closeHistoryOverlay()
            startActivity(Intent(this, SongHistoryActivity::class.java).putExtra("RADIO_NAME", radio.name))
        }
    }

    private fun playRadioAdjacent(direction: Int) {
        val radios = RadioRepository.radios
        if (radios.isEmpty()) return
        val selectedName = RadioPlayerManager.currentRadio?.name
            ?: AppPreferences.lastRadio(this)
        val currentIndex = radios.indexOfFirst {
            it.name == selectedName
        }
        val targetIndex = if (currentIndex >= 0) {
            (currentIndex + direction + radios.size) % radios.size
        } else if (direction > 0) {
            0
        } else {
            radios.lastIndex
        }
        RadioPlayerManager.play(this, radios[targetIndex])
        refreshPlayer()
    }

    private fun playSelectedRadio(radio: Radio) {
        val sameStation = RadioPlayerManager.currentRadio?.name == radio.name
        if (::castController.isInitialized && castController.isConnected()) {
            if (sameStation) {
                castController.toggleRemotePlayback()
                artworkHandler.postDelayed(playerRefresh, 350L)
            } else RadioPlayerManager.play(this, radio)
        } else if (sameStation && RadioPlayerManager.isCurrentlyPlaying()) {
            RadioPlayerManager.togglePlayPause(this)
        } else {
            RadioPlayerManager.play(this, radio)
        }
    }

    private fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_search_prompt))
        }
        runCatching { voiceSearchLauncher.launch(intent) }
            .onFailure {
                Toast.makeText(this, R.string.voice_search_unavailable, Toast.LENGTH_SHORT).show()
            }
    }

    private fun openHistoryOverlay() {
        val radio = RadioPlayerManager.currentRadio
        if (radio == null) {
            Toast.makeText(this, R.string.select_radio_first, Toast.LENGTH_SHORT).show()
            return
        }
        RemoteSongHistory.refresh(this, radio) {
            if (historyOpen && RadioPlayerManager.currentRadio?.name == radio.name) {
                val refreshed = SongHistoryStore.get(this, radio.name)
                historyEmpty.visibility = if (refreshed.isEmpty()) View.VISIBLE else View.GONE
                historyRecycler.visibility = if (refreshed.isEmpty()) View.GONE else View.VISIBLE
                historyRecycler.adapter = SongHistoryAdapter(refreshed)
            }
        }
        val list = SongHistoryStore.get(this, radio.name)
        historyTitle.text = getString(R.string.recent_tracks_for, radio.name)
        historyEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        historyRecycler.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        historyRecycler.layoutManager = LinearLayoutManager(this)
        historyRecycler.adapter = SongHistoryAdapter(list)
        applyHistoryPanelAppearance()

        historyOpen = true
        historyToggle.setText(R.string.hide_recent_tracks)
        historyOverlay.visibility = View.VISIBLE
        historyOverlay.pivotY = 0f
        historyOverlay.alpha = 0f
        historyOverlay.scaleY = 0.72f
        historyOverlay.translationY = -18f
        historyOverlay.animate()
            .alpha(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(280L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        updatePagerInputEnabled()
    }

    private fun closeHistoryOverlay() {
        if (!historyOpen) return
        historyOpen = false
        updatePagerInputEnabled()
        historyToggle.setText(R.string.show_recent_tracks)
        historyOverlay.animate()
            .alpha(0f)
            .scaleY(0.72f)
            .translationY(-18f)
            .setDuration(220L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                historyOverlay.visibility = View.GONE
                historyOverlay.alpha = 1f
                historyOverlay.scaleY = 1f
                historyOverlay.translationY = 0f
            }
            .start()
    }

    private fun hideHistoryOverlayImmediately() {
        historyOverlay.animate().cancel()
        historyOpen = false
        updatePagerInputEnabled()
        historyToggle.setText(R.string.show_recent_tracks)
        historyOverlay.visibility = View.GONE
        historyOverlay.alpha = 1f
        historyOverlay.scaleY = 1f
        historyOverlay.translationY = 0f
    }

    private fun setupVolume() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volume.max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        syncVolume()
        volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                volumePercent.text = "${(progress * 100 / volume.max.coerceAtLeast(1))}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        volume.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> volumeTouchActive = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> volumeTouchActive = false
            }
            updatePagerInputEnabled()
            false
        }
    }

    private fun syncVolume() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        volume.progress = current
        volumePercent.text = "${(current * 100 / volume.max.coerceAtLeast(1))}%"
    }

    private fun restoreLastRadio() {
        if (RadioPlayerManager.currentRadio != null) return
        val last = AppPreferences.lastRadio(this)?.let(RadioRepository::byName)
            ?: RadioRepository.radios.firstOrNull()
        last?.let { RadioPlayerManager.play(this, it) }
    }

    private fun refreshFavorites() {
        updatePlayerFavorite()
        if (::pagesAdapter.isInitialized) pagesAdapter.refreshFavorites()
    }

    private fun moveTopActionsToPlayerOverlay() {
        val playerContent = playerLayout.parent as? FrameLayout ?: return
        val density = resources.displayMetrics.density
        val margin = (14 * density).toInt()
        val size = (40 * density).toInt()

        (stationInfoPanel.parent as? ViewGroup)?.removeView(stationInfoPanel)
        (stationInfo.parent as? ViewGroup)?.removeView(stationInfo)

        playerContent.addView(stationInfoPanel)
        playerContent.addView(stationInfo, FrameLayout.LayoutParams(size, size).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            setMargins(margin, margin, margin, margin)
        })
    }

    private fun updateStationInfoPanelLayout(landscape: Boolean) {
        val parent = stationInfoPanel.parent as? FrameLayout ?: return
        parent.post {
            if (parent.width <= 0 || parent.height <= 0) return@post
            val density = resources.displayMetrics.density
            val margin = (14 * density).toInt()
            val topMargin = (68 * density).toInt()
            val availableWidth = (parent.width - margin * 2).coerceAtLeast(1)
            val availableHeight = (parent.height - topMargin - margin).coerceAtLeast(1)
            val width = (parent.width * if (landscape) .42f else .90f).toInt()
                .coerceAtMost(availableWidth)
            val height = (parent.height * if (landscape) .74f else .38f).toInt()
                .coerceAtMost(availableHeight)
            stationInfoPanel.layoutParams = FrameLayout.LayoutParams(width, height).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                setMargins(margin, topMargin, margin, margin)
            }
        }
    }

    private fun favoriteNames(): MutableList<String> = getSharedPreferences("favorites", MODE_PRIVATE)
        .getString("favorite_radios", "").orEmpty().split("|")
        .filter(String::isNotBlank).toMutableList()

    private fun toggleCurrentFavorite() {
        val radio = RadioPlayerManager.currentRadio ?: return
        val names = favoriteNames()
        if (!names.remove(radio.name)) names.add(radio.name)
        getSharedPreferences("favorites", MODE_PRIVATE).edit()
            .putString("favorite_radios", names.distinct().joinToString("|")).apply()
        refreshFavorites()
    }

    private fun updatePlayerFavorite() {
        val radio = RadioPlayerManager.currentRadio
        val selected = radio != null && favoriteNames().contains(radio.name)
        playerFavorite.isEnabled = radio != null
        playerFavorite.setImageResource(if (selected) R.drawable.ic_favorite_filled else R.drawable.ic_favorite)
        playerFavorite.imageTintList = android.content.res.ColorStateList.valueOf(0xFFE53935.toInt())
        playerFavorite.contentDescription = getString(if (selected) R.string.remove_from_favorites else R.string.add_to_favorites)
        playerFavorite.alpha = if (radio == null) .35f else if (selected) 1f else .82f
    }

    private fun updateStationInfoButton() {
        val available = RadioPlayerManager.currentRadio != null
        stationInfo.isEnabled = available
        stationInfo.alpha = if (available) .86f else .35f
        stationInfo.imageTintList = android.content.res.ColorStateList.valueOf(AppPreferences.accent(this))
    }

    private fun showStationInfo() {
        val radio = RadioPlayerManager.currentRadio ?: return
        val resolved = StationInfoResolver.resolve(radio)
        RadioLogoLoader.load(infoStationLogo, radio)
        infoStationName.text = radio.name
        infoContent.text = getString(
            R.string.station_content_value,
            StationInfoLocalizer.content(this, resolved.content)
        )
        val location = StationInfoLocalizer.location(this, radio, resolved.location)
        infoLocation.text = location.takeIf(String::isNotBlank)
            ?.let { getString(R.string.station_location_value, it) }.orEmpty()
        infoLocationRow.visibility = if (location.isBlank()) View.GONE else View.VISIBLE
        val countryCode = StationInfoLocalizer.countryCode(radio, resolved.location)
        Glide.with(this).clear(infoCountryFlag)
        if (countryCode.isBlank()) {
            infoCountryFlag.visibility = View.GONE
        } else {
            infoCountryFlag.visibility = View.VISIBLE
            Glide.with(this)
                .load("https://flagcdn.com/w40/$countryCode.png")
                .into(infoCountryFlag)
        }
        infoDetails.text = resolved.frequency.takeIf(String::isNotBlank)
            ?.let { getString(R.string.station_frequency_value, it) }.orEmpty()
        infoDetails.visibility = if (infoDetails.text.isBlank()) View.GONE else View.VISIBLE
        val websiteText = resolved.website.ifBlank {
            resolved.sourceHost.takeIf(String::isNotBlank)
                ?.let { getString(R.string.broadcast_source, it) }
                ?: getString(R.string.website_not_available)
        }
        infoWebsite.text = getString(R.string.station_website_value, websiteText)
        infoWebsiteButton.visibility = if (resolved.website.isBlank()) View.GONE else View.VISIBLE
        infoWebsiteButton.tag = resolved.website
        stationInfoPanel.animate().cancel()
        stationInfoPanel.visibility = View.VISIBLE
        stationInfoPanel.post {
            stationInfoPanel.translationX = -stationInfoPanel.width.toFloat()
            stationInfoPanel.alpha = .7f
            stationInfoPanel.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(240L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
        stationInfo.setImageResource(R.drawable.ic_close)
        stationInfoOpen = true
        updatePagerInputEnabled()
    }

    private fun closeStationInfo() {
        if (!stationInfoOpen) return
        stationInfoOpen = false
        updatePagerInputEnabled()
        stationInfo.setImageResource(R.drawable.ic_info)
        stationInfoPanel.animate().cancel()
        stationInfoPanel.animate()
            .translationX(-stationInfoPanel.width.toFloat())
            .alpha(.7f)
            .setDuration(200L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                stationInfoPanel.visibility = View.GONE
                stationInfoPanel.translationX = 0f
                stationInfoPanel.alpha = 1f
            }.start()
    }

    private fun shareCurrentStation() {
        val radio = RadioPlayerManager.currentRadio ?: return
        val address = StationInfoResolver.resolve(radio).website
            .takeIf(String::isNotBlank)?.let { "\n$it" }.orEmpty()
        val shareText = getString(R.string.share_station_text, radio.name, address)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, radio.name)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_station)))
    }

    private fun refreshPlayer() {
        val radio = RadioPlayerManager.currentRadio
        val stationChanged = renderedRadioName != radio?.name
        if (stationChanged) {
            if (stationInfoOpen) closeStationInfo()
            renderedRadioName = radio?.name
            albumUrl = null
            radio?.let {
                RadioLogoLoader.load(stationLogo, it)
                RadioLogoLoader.load(logoBackground, it)
                showingAlbumArt = false
            }
        }
        radioName.text = radio?.name ?: getString(R.string.no_radio_selected)
        val title = RadioPlayerManager.currentTitle
        val artist = RadioPlayerManager.currentArtist
        song.text = when {
            title.isBlank() -> getString(R.string.waiting_song_info)
            artist.isBlank() -> title
            else -> "$artist — $title"
        }
        val playing = radio != null && if (::castController.isInitialized && castController.isConnected()) {
            castController.isRemotePlaying()
        } else {
            RadioPlayerManager.isPlaying(this)
        }
        playerEqualizer.setPlaying(playing)
        val quality = if (playing) RadioPlayerManager.currentAudioQuality() else ""
        val backup = if (RadioPlayerManager.isUsingBackupStream()) "YEDEK YAYIN" else ""
        streamQuality.text = listOf(quality, backup).filter(String::isNotBlank).joinToString(" • ")
        streamQuality.visibility = if (streamQuality.text.isBlank()) View.GONE else View.VISIBLE
        playPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        // These buttons navigate the complete station list, not playback history.
        // Keeping their enabled state tied to history made the forward button inert
        // immediately after selecting a station.
        val canBrowseStations = catalogReady && RadioRepository.radios.size > 1
        previous.isEnabled = canBrowseStations
        next.isEnabled = canBrowseStations
        updateArtwork()
        updatePlayerFavorite()
        updateStationInfoButton()
        if (::castController.isInitialized) castController.syncCurrentRadio(albumUrl)
        if (catalogReady && ::pagesAdapter.isInitialized) {
            if (stationChanged) pagesAdapter.refreshRecent()
            pagesAdapter.notifyPlayingChanged()
        }
    }

    /**
     * Ana ekran arka plandayken metadata dinlemeye servis devam eder. Activity
     * yeniden görünür olduğunda listener geriye dönük olay almadığı için, o an
     * çalan parçanın kapağını kalıcı şarkı geçmişinden yeniden eşitle.
     */
    private fun syncCurrentTrackArtwork() {
        val radio = RadioPlayerManager.currentRadio
        val artist = RadioPlayerManager.currentArtist
        val title = RadioPlayerManager.currentTitle

        albumUrl = if (radio != null && title.isNotBlank()) {
            SongHistoryStore.get(this, radio.name)
                .firstOrNull { it.artist == artist && it.title == title }
                ?.artworkUrl
        } else {
            null
        }

        // Servisteki kapak sorgusu henüz tamamlanmadıysa Activity de aynı
        // güvenli şarkı/radyo doğrulamasıyla sonucu isteyebilir.
        if (radio != null && title.isNotBlank() && albumUrl.isNullOrBlank()) {
            onMetadataChanged(artist, title)
        }
    }

    private fun updateArtwork() {
        val radio = RadioPlayerManager.currentRadio
        stationLogo.visibility = View.GONE
        radioName.visibility = if (AppPreferences.showStationName(this)) View.VISIBLE else View.GONE
        // Albüm kapağı yoksa, dönüşüm ayarı kapalı olsa bile istasyonun kendi
        // logosu oynatıcıdaki güvenli yedek görseldir.
        artwork.visibility = View.VISIBLE

        if (radio == null) {
            backgroundShade.setBackgroundColor(Color.BLACK)
            artwork.setImageResource(R.drawable.radyot_logo)
            logoBackground.setImageResource(R.drawable.radyot_logo)
            return
        }
        backgroundShade.setBackgroundColor(
            if (AppPreferences.logoColors(this)) StationVisualColors.forRadio(this, radio)
            else Color.BLACK
        )
        RadioLogoLoader.load(logoBackground, radio)
        showCurrentArtworkFace()
    }

    private fun restartArtworkRotation() {
        artworkHandler.removeCallbacks(artworkSwitcher)
        artwork.animate().cancel()
        artwork.rotationY = 0f
        showingAlbumArt = AppPreferences.showAlbumArt(this) && !albumUrl.isNullOrBlank()
        showCurrentArtworkFace()
        if (canRotateArtwork()) artworkHandler.postDelayed(artworkSwitcher, ARTWORK_ROTATION_MS)
    }

    private fun canRotateArtwork(): Boolean =
        AppPreferences.showAlbumArt(this) && !albumUrl.isNullOrBlank() && AppPreferences.showStationLogo(this)

    private fun switchArtworkFace() {
        if (!canRotateArtwork() || isFinishing || isDestroyed) return
        artwork.animate().cancel()
        artwork.animate()
            .rotationY(90f)
            .setDuration(320L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                showingAlbumArt = !showingAlbumArt
                showCurrentArtworkFace()
                artwork.rotationY = -90f
                artwork.animate()
                    .rotationY(0f)
                    .setDuration(320L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }.start()
    }

    private fun showCurrentArtworkFace() {
        val radio = RadioPlayerManager.currentRadio ?: return
        val albumEnabled = AppPreferences.showAlbumArt(this) && !albumUrl.isNullOrBlank()
        val logoEnabled = AppPreferences.showStationLogo(this)
        when {
            showingAlbumArt && albumEnabled && logoEnabled -> {
                artwork.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(this).load(albumUrl).centerCrop().placeholder(radio.logoRes).error(radio.logoRes).into(artwork)
            }
            albumEnabled && !logoEnabled -> {
                artwork.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(this)
                    .load(albumUrl)
                    .centerCrop()
                    .placeholder(radio.logoRes)
                    .error(radio.logoRes)
                    .into(artwork)
            }
            else -> {
                artwork.scaleType = ImageView.ScaleType.FIT_CENTER
                RadioLogoLoader.load(artwork, radio)
            }
        }
    }

    private fun confirmExit() {
        AlertDialog.Builder(this)
            .setTitle(R.string.exit_app)
            .setMessage(R.string.exit_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.exit) { _, _ ->
                if (::castController.isInitialized) castController.stopRemotePlayback(endSession = true)
                RadyoTMediaBrowserService.stopCompletely(this)
                finishAffinity()
            }
            .show()
    }

    override fun onMetadataChanged(artist: String, title: String) {
        val requestedRadio = RadioPlayerManager.currentRadio?.name ?: return
        val requestedArtist = artist
        val requestedTitle = title
        AlbumArtLoader.search(artist, title) { url ->
            val sameRadio = RadioPlayerManager.currentRadio?.name == requestedRadio
            val sameTrack = RadioPlayerManager.currentArtist == requestedArtist && RadioPlayerManager.currentTitle == requestedTitle
            if (!sameRadio || !sameTrack) return@search
            // Kapak yoksa istasyonun kendi logosuna dön; kapak varsa görünüm
            // ayarına göre sabit göster veya istasyon logosuyla dönüşümlü kullan.
            albumUrl = url
            SongHistoryStore.updateArtwork(this, requestedRadio, artist, title, url)
            refreshPlayer()
            restartArtworkRotation()
            if (historyOpen) openHistoryOverlay()
        }
    }

    override fun onPlayerChanged() {
        artworkHandler.removeCallbacks(playerRefresh)
        artworkHandler.postDelayed(playerRefresh, 80L)
        if (::castController.isInitialized) castController.syncCurrentRadio(albumUrl)
    }
    override fun onPlayerError(message: String) { runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() } }

    override fun onStart() {
        super.onStart()
        if (::castController.isInitialized) castController.start()
    }

    override fun onStop() {
        // Cast metadata ve logo/album döngüsü telefon ekranı kapanınca da
        // sürmeli; denetleyici yalnızca Activity gerçekten yok edilirken ayrılır.
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        RadioPlayerManager.addListener(this)
        applyAccent()
        applyBackgroundLogoSettings()
        applyPlayerElementSettings()
        applyHistoryPanelAppearance()
        if (catalogReady && ::pagesAdapter.isInitialized) {
            pagesAdapter.refreshForOrientation()
            refreshFavorites()
        }
        syncCurrentTrackArtwork()
        refreshPlayer()
        syncVolume()
        restartArtworkRotation()
    }

    override fun onPause() {
        artworkHandler.removeCallbacks(artworkSwitcher)
        artworkHandler.removeCallbacks(playerRefresh)
        artwork.animate().cancel()
        artwork.rotationY = 0f
        RadioPlayerManager.removeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        if (::castController.isInitialized) castController.stop()
        super.onDestroy()
    }
}
