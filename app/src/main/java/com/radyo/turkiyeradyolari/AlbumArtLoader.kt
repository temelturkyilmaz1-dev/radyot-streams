package com.radyo.turkiyeradyolari

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

object AlbumArtLoader {
    private val client = OkHttpClient()
    private val main = Handler(Looper.getMainLooper())

    fun search(artist: String, title: String, callback: (String?) -> Unit) {
        if (title.isBlank()) { callback(null); return }
        thread {
            val result = runCatching {
                val q = URLEncoder.encode("$artist $title", StandardCharsets.UTF_8.toString())
                val request = Request.Builder().url("https://itunes.apple.com/search?term=$q&entity=song&limit=1").build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    val array = JSONObject(body).optJSONArray("results")
                    if (array != null && array.length() > 0) {
                        array.getJSONObject(0).optString("artworkUrl100").replace("100x100", "600x600")
                    } else null
                }
            }.getOrNull()
            main.post { callback(result) }
        }
    }
}
