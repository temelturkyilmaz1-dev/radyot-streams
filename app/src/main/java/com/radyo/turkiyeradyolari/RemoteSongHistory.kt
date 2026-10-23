package com.radyo.turkiyeradyolari

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.thread

/**
 * Yalnızca yayıncının herkese açık geçmiş uç noktası varsa geçmişi geri getirir.
 * Canlı ses akışından geçmiş uydurulmaz; desteklenmeyen istasyonlar yerel kayıtla devam eder.
 */
object RemoteSongHistory {
    private const val PREFS = "remote_song_history"
    private const val REFRESH_MS = 15 * 60 * 1000L
    private val client = OkHttpClient()
    private val main = Handler(Looper.getMainLooper())

    fun refresh(context: Context, radio: Radio, done: () -> Unit) {
        val stationId = radio.streamUrls.asSequence()
            .mapNotNull { Regex("/(s[0-9a-f]{9})/", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
            .firstOrNull()
        if (stationId == null) return

        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "last_${radio.name}"
        if (System.currentTimeMillis() - prefs.getLong(key, 0L) < REFRESH_MS) return
        prefs.edit().putLong(key, System.currentTimeMillis()).apply()

        thread(name = "RadyoT-RemoteHistory") {
            val items = runCatching {
                val request = Request.Builder()
                    .url("https://public.radio.co/stations/$stationId/status")
                    .header("User-Agent", "RadyoT/3.1 Android")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    val history = JSONObject(response.body?.string().orEmpty()).optJSONArray("history")
                        ?: return@use emptyList()
                    (0 until history.length()).mapNotNull { index ->
                        val item = history.optJSONObject(index) ?: return@mapNotNull null
                        val raw = item.optString("title").trim()
                        if (raw.isBlank()) return@mapNotNull null
                        val separator = listOf(" - ", " – ", " — ").firstOrNull(raw::contains)
                        val artist = separator?.let { raw.substringBefore(it).trim() }.orEmpty()
                        val title = separator?.let { raw.substringAfter(it).trim() } ?: raw
                        SongHistoryItem(
                            radio.name, artist, title,
                            parseTime(item.optString("start_time")),
                            item.optString("artwork_url").takeIf(String::isNotBlank)
                        )
                    }
                }
            }.getOrDefault(emptyList())
            if (items.isNotEmpty()) SongHistoryStore.merge(app, radio.name, items)
            main.post(done)
        }
    }

    private fun parseTime(value: String): Long {
        if (value.isBlank()) return System.currentTimeMillis()
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return runCatching { formatter.parse(value)?.time }.getOrNull() ?: System.currentTimeMillis()
    }
}
