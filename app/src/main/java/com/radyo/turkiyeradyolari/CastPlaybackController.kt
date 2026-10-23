package com.radyo.turkiyeradyolari

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class CastPlaybackController(context: Context) : RadioPlayerManager.Listener {
    private val appContext = context.applicationContext
    private val castContext = CastContext.getSharedInstance(context)
    private var listening = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastLoadedStream = ""
    private var lastMetadataKey = ""
    private var latestArtworkUrl = ""
    private var latestStationName = ""
    private var latestTrackKey = ""
    private var showAlbumArtwork = true
    private val artworkRotation = object : Runnable {
        override fun run() {
            val radio = RadioPlayerManager.currentRadio ?: return
            if (latestArtworkUrl.isBlank() || stationArtworkUrl(radio).isBlank()) return
            showAlbumArtwork = !showAlbumArtwork
            updateCastMetadata(radio, RadioPlayerManager.currentStreamUrl() ?: radio.url)
            scheduleArtworkRotation()
        }
    }

    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            syncCurrentRadio(force = true)
            RadioPlayerManager.pauseLocalForRemotePlayback()
        }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            syncCurrentRadio(force = true)
            RadioPlayerManager.pauseLocalForRemotePlayback()
        }
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionStartFailed(session: CastSession, error: Int) = Unit
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionEnded(session: CastSession, error: Int) {
            resetCastState()
            RadioPlayerManager.pauseForExternalDisconnect()
        }
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumeFailed(session: CastSession, error: Int) = Unit
        override fun onSessionSuspended(session: CastSession, reason: Int) {
            resetCastState()
            RadioPlayerManager.pauseForExternalDisconnect()
        }
    }

    fun attach(button: MediaRouteButton) {
        CastButtonFactory.setUpMediaRouteButton(appContext, button)
    }

    fun start() {
        if (listening) return
        castContext.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        RadioPlayerManager.addListener(this)
        listening = true
        syncCurrentRadio()
    }

    fun stop() {
        if (!listening) return
        castContext.sessionManager.removeSessionManagerListener(listener, CastSession::class.java)
        RadioPlayerManager.removeListener(this)
        handler.removeCallbacks(artworkRotation)
        listening = false
    }

    fun isConnected(): Boolean =
        castContext.sessionManager.currentCastSession?.isConnected == true

    fun isRemotePlaying(): Boolean =
        castContext.sessionManager.currentCastSession?.remoteMediaClient?.isPlaying == true

    /** Cast bağlıyken oynat/duraklat komutunu TV'ye gönderir. */
    fun toggleRemotePlayback(): Boolean {
        val session = castContext.sessionManager.currentCastSession ?: return false
        if (!session.isConnected) return false
        val remote = session.remoteMediaClient ?: return false
        if (remote.isPlaying) remote.pause() else remote.play()
        return true
    }

    /** Uygulamadan çıkışta TV yayınını da durdurur ve Cast oturumunu kapatır. */
    fun stopRemotePlayback(endSession: Boolean = true) {
        handler.removeCallbacks(artworkRotation)
        castContext.sessionManager.currentCastSession?.remoteMediaClient?.stop()
        resetCastState()
        if (endSession) castContext.sessionManager.endCurrentSession(true)
    }

    override fun onPlayerChanged() = syncCurrentRadio()

    override fun onMetadataChanged(artist: String, title: String) = syncCurrentRadio()

    fun syncCurrentRadio(artworkUrl: String? = null, force: Boolean = false) {
        val radio = RadioPlayerManager.currentRadio ?: return
        if (latestStationName != radio.name) {
            latestStationName = radio.name
            latestArtworkUrl = ""
            latestTrackKey = ""
            showAlbumArtwork = true
        }
        val trackKey = "${RadioPlayerManager.currentArtist}|${RadioPlayerManager.currentTitle}"
        if (trackKey != latestTrackKey) {
            latestTrackKey = trackKey
            latestArtworkUrl = ""
            showAlbumArtwork = true
        }
        artworkUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?.let { latestArtworkUrl = it }
        val session = castContext.sessionManager.currentCastSession ?: return
        if (!session.isConnected) return
        // Kanal değişiminde RadioPlayerManager önce yeni akışı hazırlar. Cast
        // bağlıysa yerel AudioTrack hemen susturulmalı; ses yalnızca TV'den gelir.
        RadioPlayerManager.pauseLocalForRemotePlayback()
        val streamUrl = RadioPlayerManager.currentStreamUrl() ?: radio.url
        val streamChanged = streamUrl != lastLoadedStream
        if (force || streamChanged || session.remoteMediaClient?.hasMediaSession() != true) {
            loadCastStream(radio, streamUrl)
        } else {
            updateCastMetadata(radio, streamUrl)
        }
        scheduleArtworkRotation()
    }

    private fun loadCastStream(radio: Radio, streamUrl: String) {
        val remote = castContext.sessionManager.currentCastSession?.remoteMediaClient ?: return
        val mediaInfo = buildMediaInfo(radio, streamUrl)
        remote.load(
            MediaLoadRequestData.Builder().setMediaInfo(mediaInfo).setAutoplay(true).build()
        ).setResultCallback { result ->
            if (result.status.isSuccess) {
                lastLoadedStream = streamUrl
                lastMetadataKey = metadataKey(radio, streamUrl)
            } else {
                Log.w(TAG, "Chromecast stream load failed: ${result.status.statusCode}")
                lastLoadedStream = ""
            }
        }
    }

    /** Updates the current Cast queue item so artwork changes do not restart live audio. */
    private fun updateCastMetadata(radio: Radio, streamUrl: String) {
        val remote = castContext.sessionManager.currentCastSession?.remoteMediaClient ?: return
        val key = metadataKey(radio, streamUrl)
        if (key == lastMetadataKey) return
        val currentItem = remote.currentItem ?: run {
            loadCastStream(radio, streamUrl)
            return
        }
        val updatedItem = MediaQueueItem.Builder(buildMediaInfo(radio, streamUrl))
            .setItemId(currentItem.itemId)
            .build()
        remote.queueUpdateItems(arrayOf(updatedItem), null).setResultCallback { result ->
            if (result.status.isSuccess) {
                lastMetadataKey = key
            } else {
                Log.w(TAG, "Chromecast metadata update failed: ${result.status.statusCode}")
            }
        }
    }

    private fun buildMediaInfo(radio: Radio, streamUrl: String): MediaInfo {
        val title = RadioPlayerManager.currentTitle
        val artist = RadioPlayerManager.currentArtist
        val stationArtwork = stationArtworkUrl(radio)
        val firstArtwork = if (showAlbumArtwork) latestArtworkUrl.ifBlank { stationArtwork }
            else stationArtwork.ifBlank { latestArtworkUrl }
        val secondArtwork = if (firstArtwork == latestArtworkUrl) stationArtwork else latestArtworkUrl

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, title.ifBlank { radio.name })
            putString(MediaMetadata.KEY_ALBUM_TITLE, radio.name)
            artist.takeIf(String::isNotBlank)?.let {
                putString(MediaMetadata.KEY_ARTIST, it)
            }
            firstArtwork.takeIf(String::isNotBlank)?.let {
                addImage(WebImage(Uri.parse(it)))
            }
            secondArtwork.takeIf { it.isNotBlank() && it != firstArtwork }?.let {
                addImage(WebImage(Uri.parse(it)))
            }
        }
        return MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(contentType(streamUrl))
            .setMetadata(metadata)
            .build()
    }

    private fun contentType(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u") -> "application/x-mpegURL"
            lower.contains("aac") -> "audio/aac"
            lower.contains(".ogg") || lower.contains("opus") -> "audio/ogg"
            else -> "audio/mpeg"
        }
    }

    private fun stationArtworkUrl(radio: Radio): String {
        StationLogoOverrides.remoteLogoFor(radio).takeIf(::isRemoteUrl)?.let { return it }
        radio.logoPath.takeIf(::isRemoteUrl)?.let { return it }
        val website = radio.website.takeIf(::isRemoteUrl) ?: return ""
        val encoded = URLEncoder.encode(website, StandardCharsets.UTF_8.name())
        return "https://www.google.com/s2/favicons?sz=256&domain_url=$encoded"
    }

    private fun metadataKey(radio: Radio, streamUrl: String) = listOf(
        radio.name, streamUrl, RadioPlayerManager.currentArtist, RadioPlayerManager.currentTitle,
        latestArtworkUrl, stationArtworkUrl(radio), showAlbumArtwork.toString()
    ).joinToString("|")

    private fun scheduleArtworkRotation() {
        handler.removeCallbacks(artworkRotation)
        val radio = RadioPlayerManager.currentRadio ?: return
        if (latestArtworkUrl.isNotBlank() && stationArtworkUrl(radio).isNotBlank()) {
            handler.postDelayed(artworkRotation, ARTWORK_ROTATION_MS)
        }
    }

    private fun resetCastState() {
        handler.removeCallbacks(artworkRotation)
        lastLoadedStream = ""
        lastMetadataKey = ""
    }

    private fun isRemoteUrl(value: String) =
        value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true)

    companion object {
        private const val TAG = "CastPlayback"
        private const val ARTWORK_ROTATION_MS = 15_000L
    }
}
