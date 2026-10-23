package com.radyo.turkiyeradyolari

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import java.util.concurrent.CopyOnWriteArraySet
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

object RadioPlayerManager {
    enum class ConnectionQuality { IDLE, CONNECTING, GOOD, FAIR, POOR }

    private fun mediaAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    interface Listener {
        fun onPlayerChanged() {}
        fun onMetadataChanged(artist: String, title: String) {}
        fun onPlayerError(message: String) {}
    }

    private var player: ExoPlayer? = null
    private var applicationContext: Context? = null
    private val listeners = CopyOnWriteArraySet<Listener>()
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private val history = mutableListOf<Radio>()
    private var historyIndex = -1
    private var reconnectAttempts = 0
    private var streamIndex = 0
    private var hasBeenReady = false
    private var connectionInterruptions = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var noisyReceiverRegistered = false
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pauseForExternalDisconnect()
            }
        }
    }
    private const val MAX_RECONNECT_ATTEMPTS = 3

    var currentRadio: Radio? = null
        private set
    var currentArtist: String = ""
        private set
    var currentTitle: String = ""
        private set

    fun getPlayer(context: Context): ExoPlayer {
        if (player == null) {
            val appContext = context.applicationContext
            applicationContext = appContext
            registerNetworkPolicy(appContext)
            registerNoisyReceiver(appContext)

            // Bazı HLS radyo sunucuları ana listeden sonra segment listesi için
            // aynı oturum çerezini bekler (Kent FM altyapısı gibi).
            if (CookieHandler.getDefault() == null) {
                CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
            }

            // Birçok Icecast/Shoutcast sunucusu metadata'yı yalnızca bu başlık
            // açıkça istendiğinde gönderir. Redirect desteği de eski/yönlenen
            // radyo adreslerinin daha güvenilir açılmasını sağlar.
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("RadyoT/2.2 (Android; Media3)")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(12_000)
                .setReadTimeoutMs(20_000)
                .setDefaultRequestProperties(
                    mapOf(
                        "Icy-MetaData" to "1",
                        "Accept" to "*/*",
                        "Connection" to "keep-alive"
                    )
                )

            val dataSourceFactory = DefaultDataSource.Factory(appContext, httpFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            val (minBuffer, maxBuffer, playBuffer, rebuffer) = when (AppPreferences.bufferProfile(appContext)) {
                AppPreferences.BUFFER_FAST -> intArrayOf(2_000, 12_000, 600, 1_200)
                AppPreferences.BUFFER_STABLE -> intArrayOf(8_000, 45_000, 2_000, 4_000)
                else -> intArrayOf(4_000, 20_000, 1_000, 2_000)
            }
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    minBuffer,
                    maxBuffer,
                    playBuffer,
                    rebuffer
                )
                .build()

            player = ExoPlayer.Builder(appContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build()
                .also { exo ->
                    exo.setAudioAttributes(
                        mediaAudioAttributes(),
                        !AppPreferences.continuousAudio(appContext)
                    )
                    exo.setHandleAudioBecomingNoisy(true)
                    exo.setWakeMode(C.WAKE_MODE_NETWORK)
                    exo.playWhenReady = true
                    exo.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_BUFFERING && hasBeenReady) {
                                connectionInterruptions++
                            }
                            if (playbackState == Player.STATE_READY) {
                                reconnectAttempts = 0
                                hasBeenReady = true
                            }
                            notifyChanged()
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) = notifyChanged()

                        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                            if (!playWhenReady && (
                                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY ||
                                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS
                                )) {
                                AppPreferences.setPlaybackRequested(appContext, false)
                            }
                            notifyChanged()
                        }

                        override fun onAudioSessionIdChanged(audioSessionId: Int) {
                            EqualizerManager.getOrCreate(appContext, audioSessionId)
                        }

                        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                            parseMetadata(appContext, mediaMetadata)
                        }

                        override fun onMetadata(metadata: Metadata) {
                            parseTimedMetadata(appContext, metadata)
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            reconnect(appContext)
                        }
                    })
                }
        }
        return player!!
    }


    fun applyAudioFocusPreference(context: Context) {
        val exo = getPlayer(context)
        exo.setAudioAttributes(
            mediaAudioAttributes(),
            !AppPreferences.continuousAudio(context.applicationContext)
        )
    }

    /**
     * Android Auto yerel oynatmayı araç ses kanalına ancak aktif bir media
     * audio-focus isteğinden sonra yönlendirebiliyor. Telefonda isteğe bağlı
     * olan "kesintisiz ses" davranışı Auto komutlarında uygulanmaz.
     */
    fun playFromAutomotive(context: Context, radio: Radio, addToHistory: Boolean = true) {
        getPlayer(context).setAudioAttributes(mediaAudioAttributes(), true)
        play(context, radio, addToHistory)
    }

    /**
     * Auto bağlandığında oynatıcı zaten PLAYING durumundaysa sadece play()
     * çağrısı ses rotasını yenilemiyor. Kısa pause/play geçişi audio focus'u
     * tekrar alır ve AudioTrack'i araç çıkışına bağlar; stream yeniden açılmaz.
     */
    fun resumeFromAutomotive(context: Context) {
        val appContext = context.applicationContext
        val exo = getPlayer(appContext)
        exo.setAudioAttributes(mediaAudioAttributes(), true)
        val radio = currentRadio ?: return
        if (exo.mediaItemCount == 0) startActiveStream(appContext, radio)
        if (exo.playbackState == Player.STATE_IDLE || exo.playbackState == Player.STATE_ENDED) {
            exo.prepare()
        }
        if (exo.isPlaying || exo.playWhenReady) {
            exo.pause()
        }
        exo.playWhenReady = true
        exo.play()
        AppPreferences.setPlaybackRequested(appContext, true)
        notifyChanged()
    }

    fun addListener(listener: Listener) { listeners.add(listener) }
    fun removeListener(listener: Listener) { listeners.remove(listener) }

    fun currentAudioQuality(): String {
        val format = player?.audioFormat ?: return ""
        val codec = when (format.sampleMimeType.orEmpty().lowercase()) {
            "audio/aac", "audio/mp4a-latm" -> "AAC"
            "audio/mpeg" -> "MP3"
            "audio/flac" -> "FLAC"
            "audio/ogg", "audio/opus" -> "OPUS"
            else -> format.codecs.orEmpty().uppercase()
        }
        val bitrate = format.bitrate.takeIf { it > 0 }?.let { "${it / 1000} kbps" }.orEmpty()
        val sampleRate = format.sampleRate.takeIf { it > 0 }?.let {
            val khz = if (it % 1000 == 0) "${it / 1000}" else "%.1f".format(it / 1000f)
            "$khz kHz"
        }.orEmpty()
        val channels = when (format.channelCount) {
            1 -> "Mono"
            2 -> "Stereo"
            else -> ""
        }
        return listOf(codec, bitrate, sampleRate, channels)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" • ")
    }

    fun currentConnectionQuality(): ConnectionQuality {
        val exo = player ?: return ConnectionQuality.IDLE
        if (currentRadio == null || (!exo.playWhenReady && !exo.isPlaying)) {
            return ConnectionQuality.IDLE
        }
        return when (exo.playbackState) {
            Player.STATE_BUFFERING -> if (hasBeenReady) ConnectionQuality.POOR else ConnectionQuality.CONNECTING
            Player.STATE_READY -> when {
                connectionInterruptions >= 2 -> ConnectionQuality.POOR
                connectionInterruptions == 1 -> ConnectionQuality.FAIR
                else -> ConnectionQuality.GOOD
            }
            else -> ConnectionQuality.CONNECTING
        }
    }

    fun play(context: Context, radio: Radio, addToHistory: Boolean = true) {
        applicationContext = context.applicationContext
        registerNetworkPolicy(context.applicationContext)
        if (!networkAllowed(context.applicationContext)) {
            listeners.forEach { it.onPlayerError("Yalnızca Wi-Fi açık. Yayını başlatmak için Wi-Fi bağlantısı gerekli.") }
            return
        }
        if (addToHistory) addHistory(radio)
        currentRadio = radio
        AppPreferences.setLastRadio(context.applicationContext, radio.name)
        AppPreferences.addRecentRadio(context.applicationContext, radio.name)
        AppPreferences.setPlaybackRequested(context.applicationContext, true)
        reconnectAttempts = 0
        streamIndex = 0
        hasBeenReady = false
        connectionInterruptions = 0
        currentArtist = ""
        currentTitle = ""
        reconnectHandler.removeCallbacksAndMessages(null)
        StationMetadataPoller.start(radio.name) { artist, title ->
            if (currentRadio?.name == radio.name) {
                publishMetadata(context.applicationContext, artist, title)
            }
        }
        startActiveStream(context.applicationContext, radio)
        ContextCompat.startForegroundService(
            context.applicationContext,
            Intent(context.applicationContext, RadyoTMediaBrowserService::class.java)
        )
        notifyChanged()
    }

    private fun addHistory(radio: Radio) {
        if (history.getOrNull(historyIndex)?.name == radio.name) return
        if (historyIndex < history.lastIndex) history.subList(historyIndex + 1, history.size).clear()
        history.add(radio)
        historyIndex = history.lastIndex
    }

    fun previous(context: Context): Boolean {
        if (historyIndex <= 0) return false
        historyIndex--
        play(context, history[historyIndex], false)
        return true
    }

    fun next(context: Context): Boolean {
        if (historyIndex < 0 || historyIndex >= history.lastIndex) return false
        historyIndex++
        play(context, history[historyIndex], false)
        return true
    }

    fun canGoPrevious() = historyIndex > 0
    fun canGoNext() = historyIndex in 0 until history.lastIndex

    fun togglePlayPause(context: Context) {
        val p = getPlayer(context)
        if (p.isPlaying) {
            p.pause()
            AppPreferences.setPlaybackRequested(context.applicationContext, false)
        } else {
            currentRadio?.let { radio ->
                if (p.mediaItemCount == 0) startActiveStream(context.applicationContext, radio)
            }
            if (p.playbackState == Player.STATE_IDLE || p.playbackState == Player.STATE_ENDED) {
                p.prepare()
            }
            p.playWhenReady = true
            p.play()
            AppPreferences.setPlaybackRequested(context.applicationContext, true)
        }
        notifyChanged()
    }

    fun isPlaying(context: Context) = getPlayer(context).isPlaying
    fun isCurrentlyPlaying(): Boolean = player?.isPlaying == true
    fun audioSessionId(context: Context) = getPlayer(context).audioSessionId

    /**
     * Chromecast / Android Auto / harici ses çıkışı kapandığında sesin telefona
     * otomatik dönmesini engeller. Kullanıcı yeniden Play'e basmadan yerel
     * oynatma başlamaz.
     */
    fun pauseForExternalDisconnect() {
        reconnectHandler.removeCallbacksAndMessages(null)
        player?.apply {
            playWhenReady = false
            pause()
        }
        applicationContext?.let { AppPreferences.setPlaybackRequested(it, false) }
        notifyChanged()
    }

    /** Cast başlarken yerel hoparlörü susturur; remote stream ayrı çalışır. */
    fun pauseLocalForRemotePlayback() {
        val localPlayer = player ?: return
        val changed = localPlayer.isPlaying || localPlayer.playWhenReady
        localPlayer.playWhenReady = false
        localPlayer.pause()
        if (changed) notifyChanged()
    }

    fun stop() {
        reconnectHandler.removeCallbacksAndMessages(null)
        StationMetadataPoller.stop()
        applicationContext?.let {
            AppPreferences.setPlaybackRequested(it, false)
            if (RadioAlarmScheduler.isActive(it)) RadioAlarmScheduler.stopActive(it)
        }
        player?.stop()
        player?.clearMediaItems()
        notifyChanged()
    }

    fun enforceNetworkPreference(context: Context) {
        registerNetworkPolicy(context.applicationContext)
        if (!networkAllowed(context.applicationContext) && player?.isPlaying == true) {
            stop()
            listeners.forEach { it.onPlayerError("Wi-Fi bağlantısı kesildiği için yayın durduruldu.") }
        }
    }

    private fun networkAllowed(context: Context): Boolean {
        if (!AppPreferences.wifiOnly(context)) return true
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.allNetworks.any { network ->
            val capabilities = manager.getNetworkCapabilities(network) ?: return@any false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        }
    }

    private fun registerNoisyReceiver(context: Context) {
        if (noisyReceiverRegistered) return
        runCatching {
            ContextCompat.registerReceiver(
                context,
                noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            noisyReceiverRegistered = true
        }
    }

    private fun registerNetworkPolicy(context: Context) {
        if (networkCallback != null) return
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = enforceNetworkPreference(context)
            override fun onLost(network: Network) = enforceNetworkPreference(context)
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
                enforceNetworkPreference(context)
        }
        runCatching {
            manager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
            networkCallback = callback
        }
    }

    private fun startActiveStream(context: Context, radio: Radio) {
        val streamUrl = activeStreamUrl(radio)
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(radio.name)

        // Radyo İLEF'in resmi yayını uzantısız /stream adresinden MP3 döndürüyor.
        // Web oynatıcısı da aynı nedenle formatı açıkça MP3 olarak belirtiyor.
        // Bazı Android cihazlarda yalnızca URL'den tür kestirilemediği için
        // Media3'e bu istasyonda doğru MIME türünü doğrudan bildiriyoruz.
        if (radio.name == "Radyo İLEF" && streamUrl.contains("radyo.ilef.net/stream")) {
            mediaItemBuilder.setMimeType(MimeTypes.AUDIO_MPEG)
        }

        val mediaItem = mediaItemBuilder.build()
        getPlayer(context).apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            play()
        }
    }

    private fun parseMetadata(context: Context, metadata: MediaMetadata) {
        if (player?.currentMediaItem?.mediaId != currentRadio?.name) return
        publishMetadata(
            context,
            metadata.artist?.toString().orEmpty(),
            metadata.title?.toString().orEmpty()
        )
    }

    private fun parseTimedMetadata(context: Context, metadata: Metadata) {
        var artist = ""
        var title = ""

        for (index in 0 until metadata.length()) {
            when (val entry = metadata[index]) {
                is IcyInfo -> {
                    val icyTitle = entry.title.orEmpty()
                    if (icyTitle.isNotBlank()) publishMetadata(context, "", icyTitle)
                }

                is TextInformationFrame -> {
                    val values = entry.values
                        .map { it.cleanMetadata() }
                        .filter { it.isNotBlank() }
                    val value = values.firstOrNull().orEmpty()
                    when (entry.id.uppercase()) {
                        "TPE1", "TPE2" -> if (artist.isBlank()) artist = value
                        "TIT2" -> title = value
                        // Bazı HLS yayınları tüm 'Artist - Title' bilgisini
                        // TXXX/özel metin alanında taşır. Tek değer yerine tüm
                        // değerleri taramak Kafa Radyo gibi yayınlarda daha
                        // toleranslı davranır.
                        "TXXX" -> if (title.isBlank()) {
                            title = values.firstOrNull { looksLikeTrackText(it) }.orEmpty()
                        }
                    }
                }

                else -> {
                    // Bazı HLS zincirleri zamanlanmış metadata'yı Media3'ün
                    // özel entry türlerinde taşır. UI'ye dokunmadan, entry'nin
                    // metinsel temsilinden yalnızca şarkı biçimine benzeyen
                    // güvenli bir aday ayıklanır.
                    if (title.isBlank()) {
                        title = extractTrackCandidate(entry.toString())
                    }
                }
            }
        }

        if (artist.isNotBlank() || title.isNotBlank()) {
            publishMetadata(context, artist, title)
        }
    }

    private fun publishMetadata(context: Context, rawArtist: String, rawTitle: String) {
        var artist = rawArtist.cleanMetadata()
        var title = rawTitle.cleanMetadata()

        if (artist.isBlank()) {
            val separator = listOf(" - ", " – ", " — ", " | ")
                .firstOrNull { title.contains(it) }
            if (separator != null) {
                val parts = title.split(separator, limit = 2)
                artist = parts[0].trim()
                title = parts[1].trim()
            }
        }

        if (title.isBlank() || isNonSongMetadata(title)) return
        if (artist == currentArtist && title == currentTitle) return
        currentArtist = artist
        currentTitle = title
        currentRadio?.let { SongHistoryStore.add(context, it.name, artist, title) }
        listeners.forEach { it.onMetadataChanged(artist, title) }
        notifyChanged()
    }

    private fun String.cleanMetadata(): String =
        replace(Regex("\\s+"), " ").trim().trim('-', '–', '—', '|')

    private fun looksLikeTrackText(value: String): Boolean {
        val clean = value.cleanMetadata()
        return clean.length in 5..180 &&
            listOf(" - ", " – ", " — ", " | ").any(clean::contains) &&
            !isNonSongMetadata(clean)
    }

    private fun extractTrackCandidate(raw: String): String {
        if (raw.isBlank()) return ""

        // JSON-benzeri alanlar: title=..., value=..., text=..., messageData=...
        val decoded = raw
            .replace("\\u0026", "&")
            .replace("\\u002d", "-")
            .replace("\\n", " ")
            .replace("\\r", " ")

        val quotedCandidates = Regex("(?:title|value|text|description|messageData)\\s*[=:]\\s*[\"']([^\"']{3,220})[\"']", RegexOption.IGNORE_CASE)
            .findAll(decoded)
            .map { it.groupValues[1].cleanMetadata() }
            .toList()

        val broadCandidates = quotedCandidates + decoded
            .split(',', ';', '[', ']', '{', '}', '(', ')')
            .map { fragment ->
                fragment.substringAfter('=', fragment)
                    .substringAfter(':', fragment)
                    .trim().trim('\"', '\'', ' ')
                    .cleanMetadata()
            }

        return broadCandidates.firstOrNull(::looksLikeTrackText).orEmpty()
    }

    private fun isNonSongMetadata(value: String): Boolean {
        val normalized = value.lowercase()
        return normalized in setOf(
            "unknown", "null", "stream", "live", "canlı yayın", "yayın yok",
            "trt dinle", "trt radyo"
        ) || normalized.startsWith("http://") || normalized.startsWith("https://") ||
            normalized.contains("türkiye'nin nostalji radyosu") ||
            normalized.contains("turkiyenin nostalji radyosu")
    }

    private fun reconnect(context: Context) {
        val radio = currentRadio ?: return
        reconnectHandler.removeCallbacksAndMessages(null)

        if (streamIndex < radio.streamUrls.lastIndex) {
            streamIndex++
            reconnectAttempts = 0
        } else if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            streamIndex = 0
        } else {
            listeners.forEach { it.onPlayerError("Yayın Yok") }
            return
        }

        val delayMs = (1_500L * (reconnectAttempts + 1)).coerceAtMost(6_000L)
        reconnectHandler.postDelayed({
            if (currentRadio?.name == radio.name) {
                startActiveStream(context, radio)
                notifyChanged()
            }
        }, delayMs)
    }

    private fun activeStreamUrl(radio: Radio): String =
        radio.streamUrls.getOrElse(streamIndex) { radio.url }

    /** The stream that is actually active, including an automatically selected backup. */
    fun currentStreamUrl(): String? = currentRadio?.let(::activeStreamUrl)

    fun isUsingBackupStream(): Boolean {
        val radio = currentRadio ?: return false
        return streamIndex > 0 && activeStreamUrl(radio) != radio.url
    }

    private fun notifyChanged() = listeners.forEach { it.onPlayerChanged() }
}
