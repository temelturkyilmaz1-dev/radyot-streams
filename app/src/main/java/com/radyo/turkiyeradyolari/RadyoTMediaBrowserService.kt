package com.radyo.turkiyeradyolari

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

/**
 * Android Auto, RadyoT içeriğini bu servis üzerinden keşfeder.
 * Araç ekranının görünümü Android Auto tarafından oluşturulur; RadyoT burada
 * favorileri, tüm radyoları ve oynatma komutlarını sağlar.
 */
class RadyoTMediaBrowserService : MediaBrowserServiceCompat(), RadioPlayerManager.Listener {

    private lateinit var mediaSession: MediaSessionCompat
    private val artworkHandler = Handler(Looper.getMainLooper())
    private var showAlbumArtwork = false
    private var publishedRadioName: String? = null
    private var publishedNotificationKey: String? = null
    private var autoArtwork: android.graphics.Bitmap? = null
    private var artworkRotationScheduled = false
    private val stationLogoCache = object : LruCache<Int, Bitmap>(8) {}
    private val artworkClient = OkHttpClient()
    private var playbackSource = ALL_RADIOS_ID
    private val artworkRotation = object : Runnable {
        override fun run() {
            artworkRotationScheduled = false
            if (!canRotateAutoArtwork()) {
                publishState()
                return
            }
            showAlbumArtwork = !showAlbumArtwork
            publishState()
        }
    }
    private val statePublish = Runnable { publishState() }

    private fun requestStatePublish() {
        artworkHandler.removeCallbacks(statePublish)
        artworkHandler.postDelayed(statePublish, STATE_PUBLISH_DEBOUNCE_MS)
    }

    override fun onCreate() {
        super.onCreate()
        RemoteStationCatalog.loadCached(this)
        RemoteStationCatalog.refresh(this)
        StreamHealthMonitor.schedule(this)
        mediaSession = MediaSessionCompat(this, "RadyoTAutoSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setPlaybackToLocal(AudioManager.STREAM_MUSIC)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    val radio = RadioPlayerManager.currentRadio
                        ?: AppPreferences.lastRadio(this@RadyoTMediaBrowserService)?.let(RadioRepository::byName)
                        ?: RadioRepository.radios.firstOrNull()
                    if (radio != null) {
                        if (RadioPlayerManager.currentRadio?.name != radio.name) {
                            RadioPlayerManager.playFromAutomotive(this@RadyoTMediaBrowserService, radio)
                        } else {
                            RadioPlayerManager.resumeFromAutomotive(this@RadyoTMediaBrowserService)
                        }
                    }
                    publishState()
                }

                override fun onPause() {
                    if (RadioPlayerManager.isPlaying(this@RadyoTMediaBrowserService)) {
                        RadioPlayerManager.togglePlayPause(this@RadyoTMediaBrowserService)
                    }
                    publishState()
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    val radioName = when {
                        mediaId?.startsWith(HISTORY_PREFIX) == true -> {
                            playbackSource = ALL_RADIOS_ID
                            mediaId.removePrefix(HISTORY_PREFIX).substringBefore("|")
                        }
                        mediaId?.startsWith(RADIO_PREFIX) == true && mediaId.contains(SOURCE_SEPARATOR) -> {
                            val payload = mediaId.removePrefix(RADIO_PREFIX)
                            playbackSource = payload.substringBefore(SOURCE_SEPARATOR)
                            payload.substringAfter(SOURCE_SEPARATOR)
                        }
                        else -> {
                            playbackSource = ALL_RADIOS_ID
                            mediaId?.removePrefix(RADIO_PREFIX)
                        }
                    }
                    val radio = radioName?.let(RadioRepository::byName) ?: return
                    RadioPlayerManager.playFromAutomotive(this@RadyoTMediaBrowserService, radio)
                    publishState()
                }

                override fun onPlayFromSearch(query: String?, extras: Bundle?) {
                    val radio = searchRadios(query.orEmpty()).firstOrNull() ?: return
                    playbackSource = ALL_RADIOS_ID
                    RadioPlayerManager.playFromAutomotive(this@RadyoTMediaBrowserService, radio)
                    publishState()
                }

                override fun onSkipToNext() {
                    playAdjacent(+1)
                }

                override fun onSkipToPrevious() {
                    playAdjacent(-1)
                }

                override fun onSkipToQueueItem(id: Long) {
                    val radio = RadioRepository.radios.firstOrNull { queueId(it) == id } ?: return
                    playbackSource = ALL_RADIOS_ID
                    RadioPlayerManager.playFromAutomotive(this@RadyoTMediaBrowserService, radio)
                    publishState()
                }

                override fun onStop() {
                    if (RadioAlarmScheduler.isActive(this@RadyoTMediaBrowserService)) {
                        stopRadioAlarm()
                        return
                    }
                    RadioPlayerManager.stop()
                    publishState()
                }

                override fun onCustomAction(action: String?, extras: Bundle?) {
                    when (action) {
                        ACTION_TOGGLE_FAVORITE -> toggleCurrentFavorite()
                        ACTION_EXIT -> {
                            shutdownAndStop()
                            return
                        }
                    }
                    publishState()
                }
            })
            setActive(true)
        }
        sessionToken = mediaSession.sessionToken
        // Keep Android Auto transport controls independent from its queue/list UI.
        // Publishing a queue here makes some expanded Auto layouts navigate back
        // to that list after a next/previous command.
        mediaSession.setQueue(null)
        RadioPlayerManager.addListener(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        publishState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> startRadioAlarm(intent.getStringExtra(AlarmReceiver.EXTRA_RADIO))
            ACTION_STOP_ALARM -> {
                stopRadioAlarm()
                return START_NOT_STICKY
            }
            else -> intent?.let { MediaButtonReceiver.handleIntent(mediaSession, it) }
        }
        if (intent == null && RadioPlayerManager.currentRadio == null && AppPreferences.playbackRequested(this)) {
            val restored = AppPreferences.lastRadio(this)?.let(RadioRepository::byName)
            if (restored != null) RadioPlayerManager.play(this, restored)
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot = BrowserRoot(
        ROOT_ID,
        Bundle().apply {
            putBoolean(SEARCH_SUPPORTED_KEY, true)
            putInt(CONTENT_STYLE_BROWSABLE_KEY, CONTENT_STYLE_CATEGORY_GRID)
            putInt(CONTENT_STYLE_PLAYABLE_KEY, CONTENT_STYLE_LIST)
        }
    )

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val items = when (parentId) {
            ROOT_ID -> mutableListOf<MediaBrowserCompat.MediaItem>().apply {
                val resumeRadio = RadioPlayerManager.currentRadio
                    ?: AppPreferences.lastRadio(this@RadyoTMediaBrowserService)?.let(RadioRepository::byName)
                if (resumeRadio != null) {
                    add(
                        playable(
                            resumeRadio,
                            RECENT_ID,
                            getString(R.string.auto_now_playing),
                            stationDetails(resumeRadio)
                        )
                    )
                }
                val recent = recentRadios()
                if (recent.isNotEmpty()) {
                    add(
                        browsable(
                            RECENT_ID,
                            getString(R.string.recent_stations),
                            getString(R.string.recent_stations_summary),
                            stationLogoRes(recent.first())
                        )
                    )
                }
                add(browsable(FAVORITES_ID, getString(R.string.favorites), getString(R.string.favorite_stations), R.drawable.ic_favorite_filled))
                add(browsable(ALL_RADIOS_ID, getString(R.string.all_radios), getString(R.string.auto_alphabetical_browse), R.drawable.ic_radio))
                add(browsable(SONG_HISTORY_ID, getString(R.string.song_history), getString(R.string.recent_songs), R.drawable.ic_history))
            }
            FAVORITES_ID -> favoriteRadios().map { playable(it, FAVORITES_ID) }.toMutableList().apply {
                if (isEmpty()) {
                    add(
                        browsable(
                            ALL_RADIOS_ID,
                            getString(R.string.auto_no_favorites),
                            getString(R.string.auto_choose_favorites),
                            R.drawable.ic_radio
                        )
                    )
                }
            }
            ALL_RADIOS_ID -> alphabetGroups().map { group ->
                browsable(
                    ALPHABET_PREFIX + group.id,
                    group.title,
                    resources.getQuantityString(R.plurals.auto_station_count, group.radios.size, group.radios.size),
                    group.radios.firstOrNull()?.let(::stationLogoRes) ?: R.drawable.radyot_logo
                )
            }.toMutableList()
            RECENT_ID -> recentRadios().map { playable(it, RECENT_ID) }.toMutableList()
            SONG_HISTORY_ID -> songHistoryItems()
            else -> if (parentId.startsWith(ALPHABET_PREFIX)) {
                val groupId = parentId.removePrefix(ALPHABET_PREFIX)
                alphabetGroups().firstOrNull { it.id == groupId }
                    ?.radios.orEmpty().map { playable(it, ALL_RADIOS_ID) }.toMutableList()
            } else mutableListOf()
        }
        result.sendResult(items)
    }

    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(searchRadios(query).map { playable(it, ALL_RADIOS_ID) }.toMutableList())
    }

    private fun browsable(
        id: String,
        title: String,
        subtitle: String,
        iconRes: Int = R.drawable.radyot_logo
    ): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIconUri(resourceUri(iconRes))
            .setExtras(Bundle().apply {
                putInt(CONTENT_STYLE_BROWSABLE_KEY, CONTENT_STYLE_CATEGORY_GRID)
                putInt(CONTENT_STYLE_PLAYABLE_KEY, CONTENT_STYLE_LIST)
            })
            .build()
        return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    private fun playable(
        radio: Radio,
        source: String,
        title: String = radio.name,
        subtitle: String = stationDetails(radio)
    ): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(RADIO_PREFIX + source + SOURCE_SEPARATOR + radio.name)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIconUri(stationIconUri(radio))
            .setExtras(Bundle().apply {
                putInt(CONTENT_STYLE_PLAYABLE_KEY, CONTENT_STYLE_LIST)
            })
            .build()
        return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    private fun favoriteRadios(): List<Radio> {
        val names = getSharedPreferences("favorites", MODE_PRIVATE)
            .getString("favorite_radios", "").orEmpty()
            .split("|").filter(String::isNotBlank)
        return names.mapNotNull(RadioRepository::byName)
    }

    private fun recentRadios(): List<Radio> {
        val stored = AppPreferences.recentRadios(this).mapNotNull(RadioRepository::byName)
        val current = RadioPlayerManager.currentRadio
        return (listOfNotNull(current) + stored).distinctBy { it.name }.take(20)
    }

    private fun searchRadios(query: String): List<Radio> {
        if (RadioSearch.normalize(query).isBlank()) return RadioRepository.radios
        return RadioRepository.radios.filter { radio -> RadioSearch.matches(radio, query) }
    }

    private data class AlphabetGroup(val id: String, val title: String, val radios: List<Radio>)

    private fun alphabetGroups(): List<AlphabetGroup> {
        val definitions = listOf(
            "a_e" to "A - E",
            "f_k" to "F - K",
            "l_q" to "L - Q",
            "r_z" to "R - Z",
            "other" to "0 - 9 / Diğer"
        )
        return definitions.mapNotNull { (id, title) ->
            val radios = RadioRepository.radios
                .filter { alphabetGroupId(it.name) == id }
                .sortedBy { stationSortKey(it.name) }
            AlphabetGroup(id, title, radios).takeIf { radios.isNotEmpty() }
        }
    }

    private fun alphabetGroupId(name: String): String {
        val first = stationSortKey(name).firstOrNull() ?: return "other"
        return when (first) {
            in 'a'..'e' -> "a_e"
            in 'f'..'k' -> "f_k"
            in 'l'..'q' -> "l_q"
            in 'r'..'z' -> "r_z"
            else -> "other"
        }
    }

    private fun stationSortKey(name: String): String {
        val normalized = RadioSearch.normalize(name).replace(" ", "")
        val withoutGenericPrefix = when {
            normalized.startsWith("radyo") && normalized.length > 5 -> normalized.removePrefix("radyo")
            normalized.startsWith("radio") && normalized.length > 5 -> normalized.removePrefix("radio")
            else -> normalized
        }
        return withoutGenericPrefix.ifBlank { normalized }
    }

    private fun stationDetails(radio: Radio): String {
        val content = radio.content.substringBefore(',').trim()
        return listOf(content, radio.location, radio.frequency)
            .filter(String::isNotBlank)
            .joinToString(" • ")
            .ifBlank { getString(R.string.live_radio) }
    }

    private fun songHistoryItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val radio = RadioPlayerManager.currentRadio
            ?: AppPreferences.lastRadio(this)?.let(RadioRepository::byName)
            ?: return mutableListOf()
        return SongHistoryStore.get(this, radio.name).take(20).mapIndexed { index, item ->
            val description = MediaDescriptionCompat.Builder()
                .setMediaId("$HISTORY_PREFIX${radio.name}|$index")
                .setTitle(item.title)
                .setSubtitle(item.artist.ifBlank { radio.name })
                .setIconUri(item.artworkUrl?.let(Uri::parse) ?: stationIconUri(radio))
                .build()
            MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        }.toMutableList()
    }

    private fun toggleCurrentFavorite() {
        val radio = RadioPlayerManager.currentRadio ?: return
        val preferences = getSharedPreferences("favorites", MODE_PRIVATE)
        val names = preferences.getString("favorite_radios", "").orEmpty()
            .split("|").filter(String::isNotBlank).toMutableSet()
        if (!names.add(radio.name)) names.remove(radio.name)
        preferences.edit().putString("favorite_radios", names.joinToString("|")).apply()
        notifyChildrenChanged(FAVORITES_ID)
    }

    private fun isCurrentFavorite(): Boolean {
        val name = RadioPlayerManager.currentRadio?.name ?: return false
        return getSharedPreferences("favorites", MODE_PRIVATE)
            .getString("favorite_radios", "").orEmpty()
            .split("|").any { it == name }
    }

    private fun playAdjacent(direction: Int) {
        val radios = when (playbackSource) {
            FAVORITES_ID -> favoriteRadios()
            else -> RadioRepository.radios
        }
        if (radios.isEmpty()) return
        val selectedName = RadioPlayerManager.currentRadio?.name
            ?: AppPreferences.lastRadio(this)
        val currentIndex = radios.indexOfFirst { it.name == selectedName }
        val nextIndex = if (currentIndex >= 0) {
            (currentIndex + direction + radios.size) % radios.size
        } else if (direction > 0) {
            0
        } else {
            radios.lastIndex
        }
        RadioPlayerManager.playFromAutomotive(this, radios[nextIndex])
        publishState()
    }

    private fun queueId(radio: Radio): Long = radio.name.hashCode().toLong() and 0x7fffffffL

    private fun publishQueue() {
        val radios = RadioRepository.radios
        mediaSession.setQueue(radios.map { radio ->
            MediaSessionCompat.QueueItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(RADIO_PREFIX + ALL_RADIOS_ID + SOURCE_SEPARATOR + radio.name)
                    .setTitle(radio.name)
                    .setSubtitle(getString(R.string.live_radio))
                    .setIconUri(stationIconUri(radio))
                    .build(),
                queueId(radio)
            )
        })
        mediaSession.setQueueTitle(getString(R.string.app_name))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackChannel = NotificationChannel(
                CHANNEL_ID,
                "RadyoT arka plan oynatma",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kesintisiz radyo oynatımı için sessiz sistem bildirimi"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                getString(R.string.radio_alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.alarm_lockscreen_summary)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(playbackChannel)
                createNotificationChannel(alarmChannel)
            }
        }
    }

    private fun buildNotification(): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val radio = RadioPlayerManager.currentRadio
        val playing = RadioPlayerManager.isCurrentlyPlaying()
        val alarmActive = RadioAlarmScheduler.isActive(this)
        val previousIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
        val playPauseIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this,
            if (playing) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
        )
        val nextIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
        val builder = NotificationCompat.Builder(this, if (alarmActive) ALARM_CHANNEL_ID else CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radio)
            .setContentTitle(
                if (alarmActive) getString(R.string.alarm_notification_title, radio?.name ?: "RadyoT")
                else radio?.name ?: "RadyoT"
            )
            .setContentText(
                RadioPlayerManager.currentTitle.ifBlank { getString(R.string.live_radio) }
            )
            .setContentIntent(pendingIntent)
            .setOngoing(playing || alarmActive)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(if (alarmActive) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(if (alarmActive) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (alarmActive) {
            val stopAlarm = PendingIntent.getService(
                this,
                1210,
                Intent(this, RadyoTMediaBrowserService::class.java).setAction(ACTION_STOP_ALARM),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder
                .addAction(R.drawable.ic_exit, getString(R.string.stop_radio_alarm), stopAlarm)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0)
                )
        } else {
            builder
                .addAction(R.drawable.ic_previous, getString(R.string.previous_station), previousIntent)
                .addAction(
                    if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                    getString(R.string.play_pause),
                    playPauseIntent
                )
                .addAction(R.drawable.ic_next, getString(R.string.next_station), nextIntent)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )
        }
        val notificationArtwork = autoArtwork
            .takeIf { showAlbumArtwork && AppPreferences.showAlbumArt(this) }
            ?: radio?.let { stationLogoBitmap(stationLogoRes(it)) }
        notificationArtwork?.let(builder::setLargeIcon)
        return builder.build()
    }

    private fun publishState() {
        RadyoTWidgetProvider.updateAll(this, autoArtwork)
        val radio = RadioPlayerManager.currentRadio
        if (publishedRadioName != radio?.name) {
            publishedRadioName = radio?.name
            showAlbumArtwork = false
            autoArtwork = null
            artworkHandler.removeCallbacks(artworkRotation)
            artworkRotationScheduled = false
            // Metadata'yı burada boşaltma. Android Auto, setMetadata(null)
            // çağrısını oynatma ekranının kapandığı sinyali sayıp ileri/geri
            // komutundan sonra kullanıcıyı Tüm Radyolar listesine döndürebiliyor.
            // Yeni kanalın adı ve logosu aşağıda tek, atomik metadata paketiyle
            // eski paketin üzerine yazılır.
        }
        val title = RadioPlayerManager.currentTitle
        val artist = RadioPlayerManager.currentArtist
        val displayTitle = title.ifBlank { radio?.name.orEmpty() }
        val displayArtist = artist.ifBlank { if (title.isBlank()) "Canlı Radyo" else radio?.name.orEmpty() }
        val latestArtwork = radio?.let { SongHistoryStore.get(this, it.name).firstOrNull()?.artworkUrl }
        val albumEnabled = AppPreferences.showAlbumArt(this) && !latestArtwork.isNullOrBlank()
        val logoEnabled = AppPreferences.showStationLogo(this)
        val rotationEnabled = albumEnabled && logoEnabled
        if (!rotationEnabled) {
            showAlbumArtwork = albumEnabled && !logoEnabled
        }
        updateArtworkRotation(rotationEnabled)
        val artworkUri = latestArtwork.takeIf { showAlbumArtwork && albumEnabled }

        val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    radio?.let { RADIO_PREFIX + playbackSource + SOURCE_SEPARATOR + it.name }.orEmpty()
                )
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, displayTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, displayArtist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, radio?.name.orEmpty())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, displayTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, displayArtist)
        if (!artworkUri.isNullOrBlank()) {
            metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artworkUri)
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artworkUri)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, artworkUri)
        }
        if (!showAlbumArtwork && radio != null) {
            val resolvedLogoUri = stationIconUri(radio)
            val logoUri = resolvedLogoUri.toString()
            metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, logoUri)
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, logoUri)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, logoUri)
            // Uzak logo seçilmişken ayrıca yerel varsayılan bitmap göndermek bazı
            // Android Auto ünitelerinde URI'deki doğru logoyu eziyordu. Bitmap'i
            // yalnızca çözümlenen kaynak gerçekten yerel olduğunda ekle.
            if (resolvedLogoUri.scheme.equals("android.resource", ignoreCase = true)) {
                stationLogoBitmap(stationLogoRes(radio))?.let { logo ->
                    metadataBuilder
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, logo)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, logo)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, logo)
                }
            }
        } else if (showAlbumArtwork) {
            autoArtwork?.let { cover ->
                metadataBuilder
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, cover)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, cover)
            }
        }
        mediaSession.setMetadata(metadataBuilder.build())

        val state = when {
            radio == null -> PlaybackStateCompat.STATE_NONE
            RadioPlayerManager.isPlaying(this) -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        // Aynı bildirim kimliğini sessizce güncelleyerek kilit ekranı/medya
        // kartındaki şarkı ve kapağı canlı tut. setOnlyAlertOnce + setSilent
        // sayesinde her metadata değişiminde yeni bir bildirim uyarısı oluşmaz.
        val notificationKey = buildString {
            append(radio?.name.orEmpty()).append('|')
            append(state).append('|')
            append(RadioAlarmScheduler.isActive(this@RadyoTMediaBrowserService)).append('|')
            append(artist).append('|').append(title).append('|')
            append(artworkUri.orEmpty()).append('|').append(showAlbumArtwork)
        }
        if (publishedNotificationKey != notificationKey) {
            publishedNotificationKey = notificationKey
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification())
        }

        var actions =
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        if (RadioRepository.radios.size > 1) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }
        val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .addCustomAction(
                    ACTION_TOGGLE_FAVORITE,
                    if (isCurrentFavorite()) getString(R.string.remove_from_favorites) else getString(R.string.add_to_favorites),
                    if (isCurrentFavorite()) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
                )
                .addCustomAction(ACTION_EXIT, getString(R.string.exit), R.drawable.ic_exit)
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun stationLogoBitmap(resId: Int): Bitmap? {
        stationLogoCache.get(resId)?.let { return it }
        val decoded = BitmapFactory.decodeResource(resources, resId) ?: return null
        val largestEdge = maxOf(decoded.width, decoded.height)
        val optimized = if (largestEdge > AUTO_LOGO_MAX_PX) {
            val scale = AUTO_LOGO_MAX_PX.toFloat() / largestEdge
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }
        stationLogoCache.put(resId, optimized)
        return optimized
    }

    private fun stationLogoRes(radio: Radio): Int =
        StationLogoOverrides.localLogoFor(radio)
            ?: CatalogLogoFallbacks.localLogoFor(radio)
            ?: radio.logoRes

    private fun stationIconUri(radio: Radio): Uri {
        (StationLogoOverrides.localLogoFor(radio) ?: CatalogLogoFallbacks.localLogoFor(radio))
            ?.let { return resourceUri(it) }
        return StationLogoOverrides.remoteLogoFor(radio).ifBlank { radio.logoPath }
            .takeIf { it.startsWith("https://", true) || it.startsWith("http://", true) }
            ?.let(Uri::parse) ?: resourceUri(radio.logoRes)
    }

    private fun resourceUri(resId: Int): Uri {
        // Android Auto kapak görsellerini URI ile önbelleğe alır. Sayısal kaynak
        // kimlikleri yeni drawable'lar eklendiğinde değişebildiğinden eski kimlik başka
        // bir istasyonun logosuna dönüşebiliyordu. Kaynak adı sabit ve benzersizdir.
        val type = resources.getResourceTypeName(resId)
        val name = resources.getResourceEntryName(resId)
        return Uri.parse("android.resource://$packageName/$type/$name")
    }

    override fun onPlayerChanged() = requestStatePublish()
    override fun onMetadataChanged(artist: String, title: String) {
        val requestedRadio = RadioPlayerManager.currentRadio?.name ?: return publishState()
        AlbumArtLoader.search(artist, title) { url ->
            if (RadioPlayerManager.currentRadio?.name != requestedRadio ||
                RadioPlayerManager.currentArtist != artist ||
                RadioPlayerManager.currentTitle != title) return@search
            SongHistoryStore.updateArtwork(this, requestedRadio, artist, title, url)
            if (url.isNullOrBlank()) {
                autoArtwork = null
                showAlbumArtwork = false
                publishState()
            } else {
                loadArtworkForAuto(url, requestedRadio, artist, title)
            }
        }
        publishState()
    }

    private fun loadArtworkForAuto(url: String, radioName: String, artist: String, title: String) {
        thread(name = "RadyoT-AutoArtwork") {
            val bitmap = runCatching {
                artworkClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.bytes()?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            }.getOrNull()
            artworkHandler.post {
                if (RadioPlayerManager.currentRadio?.name == radioName &&
                    RadioPlayerManager.currentArtist == artist &&
                    RadioPlayerManager.currentTitle == title
                ) {
                    autoArtwork = bitmap
                    showAlbumArtwork = bitmap != null
                    publishState()
                }
            }
        }
    }
    override fun onPlayerError(message: String) = publishState()

    private fun startRadioAlarm(requestedRadio: String?) {
        RemoteStationCatalog.loadCached(this)
        val radio = requestedRadio?.let(RadioRepository::byName)
            ?: RadioAlarmScheduler.selectedRadio(this).let(RadioRepository::byName)
            ?: RadioRepository.radios.firstOrNull()
        if (radio == null) return
        RadioAlarmScheduler.applyAlarmVolume(this)
        RadioPlayerManager.play(this, radio)
        publishState()
    }

    private fun isTelevisionDevice(): Boolean =
        (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) ||
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEVISION)

    private fun stopRadioAlarm() {
        RadioAlarmScheduler.stopActive(this)
        shutdownAndStop()
    }

    private fun canRotateAutoArtwork(): Boolean {
        val radio = RadioPlayerManager.currentRadio ?: return false
        val latestArtwork = SongHistoryStore.get(this, radio.name).firstOrNull()?.artworkUrl
        return AppPreferences.showAlbumArt(this) &&
            AppPreferences.showStationLogo(this) &&
            !latestArtwork.isNullOrBlank()
    }

    private fun updateArtworkRotation(enabled: Boolean) {
        if (!enabled) {
            artworkHandler.removeCallbacks(artworkRotation)
            artworkRotationScheduled = false
        } else if (!artworkRotationScheduled) {
            artworkRotationScheduled = true
            artworkHandler.postDelayed(artworkRotation, ARTWORK_ROTATION_MS)
        }
    }

    private fun shutdownAndStop() {
        RadioAlarmScheduler.stopActive(this)
        AppPreferences.setPlaybackRequested(this, false)
        RadioPlayerManager.stop()
        artworkHandler.removeCallbacksAndMessages(null)
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f)
                .build()
        )
        mediaSession.setActive(false)
        @Suppress("DEPRECATION")
        stopForeground(true)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        artworkHandler.removeCallbacks(artworkRotation)
        artworkHandler.removeCallbacks(statePublish)
        RadioPlayerManager.removeListener(this)
        mediaSession.setActive(false)
        mediaSession.release()
        @Suppress("DEPRECATION")
        stopForeground(true)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    companion object {
        fun stopCompletely(context: Context) {
            RadioAlarmScheduler.stopActive(context)
            AppPreferences.setPlaybackRequested(context, false)
            RadioPlayerManager.stop()
            context.stopService(Intent(context, RadyoTMediaBrowserService::class.java))
            context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        }

        // Android Auto medya ağacını uzun süre önbellekte tutabiliyor. Katalogdan
        // kaldırılan istasyonların (Radyo Burada gibi) eski listede kalmaması için
        // katalog yapısı değiştiğinde bu kimliğin sürümü artırılır.
        private const val ROOT_ID = "radyot_root_v2"
        private const val FAVORITES_ID = "radyot_favorites"
        private const val ALL_RADIOS_ID = "radyot_all"
        private const val RECENT_ID = "radyot_recent"
        private const val SONG_HISTORY_ID = "radyot_song_history"
        private const val ALPHABET_PREFIX = "radyot_alphabet:"
        private const val RADIO_PREFIX = "radio:"
        private const val SOURCE_SEPARATOR = "|"
        private const val HISTORY_PREFIX = "history:"
        private const val ACTION_TOGGLE_FAVORITE = "radyot.action.TOGGLE_FAVORITE"
        private const val ACTION_EXIT = "radyot.action.EXIT"
        const val ACTION_START_ALARM = "radyot.action.START_ALARM"
        const val ACTION_STOP_ALARM = "radyot.action.STOP_ALARM"
        private const val SEARCH_SUPPORTED_KEY = "android.media.browse.SEARCH_SUPPORTED"
        private const val CONTENT_STYLE_BROWSABLE_KEY = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val CONTENT_STYLE_PLAYABLE_KEY = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val CONTENT_STYLE_LIST = 1
        private const val CONTENT_STYLE_CATEGORY_GRID = 4
        private const val ARTWORK_ROTATION_MS = 15_000L
        private const val AUTO_LOGO_MAX_PX = 512
        private const val STATE_PUBLISH_DEBOUNCE_MS = 80L
        private const val CHANNEL_ID = "radyot_playback_silent_v2"
        private const val ALARM_CHANNEL_ID = "radyot_radio_alarm_v1"
        private const val NOTIFICATION_ID = 1207
    }
}
