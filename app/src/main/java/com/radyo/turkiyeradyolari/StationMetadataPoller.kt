package com.radyo.turkiyeradyolari

import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * ICY metadata taşımayan fakat resmî bir şimdi-çalıyor API'si sunan
 * istasyonları destekler. Yalnızca açıkça tanımlanan istasyonlar sorgulanır;
 * diğer radyoların mevcut Media3/ICY metadata akışı değişmez.
 */
object StationMetadataPoller {
    private const val POLL_INTERVAL_MS = 30_000L
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var generation = 0
    private var activeCall: Call? = null

    fun start(radioName: String, onMetadata: (artist: String, title: String) -> Unit) {
        stop()
        val source = sourceFor(radioName) ?: return
        val token = generation
        poll(source, token, onMetadata)
    }

    fun stop() {
        generation++
        activeCall?.cancel()
        activeCall = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun poll(
        source: MetadataSource,
        token: Int,
        onMetadata: (artist: String, title: String) -> Unit
    ) {
        if (token != generation) return
        val request = Request.Builder()
            .url(source.url)
            .header("User-Agent", "RadyoT/3.2.7 (Android)")
            .build()

        activeCall = client.newCall(request).also { call ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    scheduleNext(source, token, onMetadata)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (token != generation || !it.isSuccessful) {
                            scheduleNext(source, token, onMetadata)
                            return
                        }
                        val body = it.body?.string().orEmpty()
                        if (source.followHlsVariant) {
                            pollHlsVariant(body, source, token, onMetadata)
                            return
                        }
                        val result = runCatching { source.parser(body) }.getOrNull()
                        if (result != null && token == generation) {
                            handler.post {
                                if (token == generation) onMetadata(result.first, result.second)
                            }
                        }
                    }
                    scheduleNext(source, token, onMetadata)
                }
            })
        }
    }

    /**
     * Radyo Viva ve Show alt kanalları şarkı bilgisini HLS medya listesindeki
     * EXTINF başlığına yazar. Ana liste her istekte geçici bir UserCode'lu
     * medya listesi döndürdüğü için önce onu çözüp ardından başlığı okuruz.
     */
    private fun pollHlsVariant(
        masterPlaylist: String,
        source: MetadataSource,
        token: Int,
        onMetadata: (artist: String, title: String) -> Unit
    ) {
        val variantUrl = masterPlaylist.lineSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
        if (variantUrl.isNullOrBlank() || token != generation) {
            scheduleNext(source, token, onMetadata)
            return
        }

        val request = Request.Builder()
            .url(variantUrl)
            .header("User-Agent", "RadyoT/3.2.7 (Android)")
            .build()
        activeCall = client.newCall(request).also { call ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    scheduleNext(source, token, onMetadata)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (token == generation && it.isSuccessful) {
                            val result = runCatching {
                                source.parser(it.body?.string().orEmpty())
                            }.getOrNull()
                            if (result != null) {
                                handler.post {
                                    if (token == generation) {
                                        onMetadata(result.first, result.second)
                                    }
                                }
                            }
                        }
                    }
                    scheduleNext(source, token, onMetadata)
                }
            })
        }
    }

    private fun scheduleNext(
        source: MetadataSource,
        token: Int,
        onMetadata: (artist: String, title: String) -> Unit
    ) {
        if (token != generation) return
        handler.postDelayed({ poll(source, token, onMetadata) }, POLL_INTERVAL_MS)
    }

    private val hlsMetadataUrls = mapOf(
        "Viva Efkar" to "https://listen.radyotvonline.net/hls/play/viva_efkar.m3u8",
        "Viva Pop" to "https://listen.radyotvonline.net/hls/play/viva_pop.m3u8",
        "Viva Turka" to "https://listen.radyotvonline.net/hls/play/viva_turca.m3u8",
        "Viva Club" to "https://listen.radyotvonline.net/hls/play/viva_club.m3u8",
        "Viva Türkü" to "https://listen.radyotvonline.net/hls/play/viva_turku.m3u8",
        "Show Slow" to "https://listen.radyotvonline.net/hls/play/show_slow.m3u8",
        "Show 90'lar" to "https://listen.radyotvonline.net/hls/play/show_90lar.m3u8",
        "Show Trend" to "https://listen.radyotvonline.net/hls/play/show_trend.m3u8",
        "Show Akustik" to "https://listen.radyotvonline.net/hls/play/show_akustik.m3u8",
        "Show Eller Havaya" to "https://listen.radyotvonline.net/hls/play/show_ellerhavaya.m3u8"
    )

    private fun sourceFor(radioName: String): MetadataSource? {
        hlsMetadataUrls[radioName]?.let {
            return MetadataSource(it, ::parseHlsPlaylist, followHlsVariant = true)
        }
        return when (radioName) {
        "KEXP 90.3 FM" -> MetadataSource(
            "https://api.kexp.org/v2/plays/?limit=5",
            ::parseKexp
        )
        "WWOZ 90.7 FM" -> MetadataSource(
            "https://www.wwoz.org/api/playlists",
            ::parseWwoz
        )
        "Zerguwan FM" -> MetadataSource(
            "https://ip.zerguwan.com/cp/get_info.php?p=8230",
            ::parseSimpleTitle
        )
        "Amasya FM 95.0" -> MetadataSource(
            "https://www.amasyafm.com/api/v1/now-playing.php",
            ::parseNestedTitle
        )
        "İstanbul FM" -> MetadataSource(
            "https://www.istanbulfm.com.tr/",
            ::parseIstanbulFm
        )
        "Radyo Juke 90.9 / 99.8" -> MetadataSource(
            "https://stream.netradyom.com/api/nowplaying/radyojuke",
            ::parseAzuraCast
        )
            else -> null
        }
    }

    private fun parseHlsPlaylist(raw: String): Pair<String, String>? {
        val line = raw.lineSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("#EXTINF:") }
            ?: return null
        var value = line.substringAfter(',', "").trim()
        value = value.replace(Regex(",https?://.*$"), "").trim()
        if (value.isBlank()) return null
        return splitArtistAndTitle(value)
    }

    private fun parseAzuraCast(raw: String): Pair<String, String>? {
        val song = JSONObject(raw).optJSONObject("now_playing")?.optJSONObject("song")
            ?: return null
        val artist = song.optString("artist").trim()
        val title = song.optString("title").trim()
        return if (title.isNotBlank()) artist to title else null
    }

    private fun parseIstanbulFm(raw: String): Pair<String, String>? {
        val firstSong = Regex(
            """data-index="0"[\\s\\S]*?<p class="font-semibold">(.*?)</p><p>(.*?)</p>"""
        ).find(raw) ?: return null
        val artist = decodeHtml(firstSong.groupValues[1])
        val title = decodeHtml(firstSong.groupValues[2])
        return if (artist.isNotBlank() && title.isNotBlank()) artist to title else null
    }

    @Suppress("DEPRECATION")
    private fun decodeHtml(value: String): String =
        android.text.Html.fromHtml(value).toString().trim()

    private fun parseNestedTitle(raw: String): Pair<String, String>? {
        val value = JSONObject(raw).optJSONObject("data")?.optString("title")?.trim().orEmpty()
        if (value.isBlank()) return null
        return splitArtistAndTitle(value)
    }

    private fun parseSimpleTitle(raw: String): Pair<String, String>? {
        val value = JSONObject(raw).optString("title").trim()
        if (value.isBlank()) return null
        return splitArtistAndTitle(value)
    }

    private fun splitArtistAndTitle(value: String): Pair<String, String> {
        val parts = value.split(" - ", limit = 2)
        return if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            "" to value
        }
    }

    private fun parseKexp(raw: String): Pair<String, String>? {
        val results = JSONObject(raw).optJSONArray("results") ?: return null
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val artist = item.optString("artist").trim()
            val title = item.optString("song").trim()
            if (artist.isNotBlank() && title.isNotBlank()) return artist to title
        }
        return null
    }

    private fun parseWwoz(raw: String): Pair<String, String>? {
        val items = JSONArray(raw)
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val artist = item.optString("ArtistName", item.optString("artist")).trim()
            val title = item.optString("SongName", item.optString("song")).trim()
            if (artist.isNotBlank() && title.isNotBlank()) return artist to title
        }
        return null
    }

    private data class MetadataSource(
        val url: String,
        val parser: (String) -> Pair<String, String>?,
        val followHlsVariant: Boolean = false
    )
}
