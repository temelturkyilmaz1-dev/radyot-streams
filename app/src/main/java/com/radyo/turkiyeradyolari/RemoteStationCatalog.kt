package com.radyo.turkiyeradyolari

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Cloudflare kataloğunu güvenli biçimde önbelleğe alır; başarısızlık oynatmayı etkilemez. */
object RemoteStationCatalog {
    private const val CATALOG_URL =
        "https://autumn-sun-8d69.temel-turkyilmaz1.workers.dev/stations.json"
    private const val PREFS = "remote_station_catalog"
    private const val KEY_JSON = "validated_json"
    private const val KEY_UPDATED_AT = "updated_at"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cacheLock = Any()
    private val refreshLock = Any()
    @Volatile private var cacheLoaded = false
    @Volatile private var refreshInFlight = false

    fun loadCached(context: Context) {
        synchronized(cacheLock) {
            if (cacheLoaded) return
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val cached = prefs.getString(KEY_JSON, null)
            // Keep the last validated catalog available offline until a newer
            // Cloudflare version replaces it.
            if (!cached.isNullOrBlank()) {
                RadioRepository.applyRemoteCatalog(cached)
            }
            cacheLoaded = true
        }
    }

    fun refresh(context: Context, onApplied: (() -> Unit)? = null) {
        synchronized(refreshLock) {
            if (refreshInFlight) return
            refreshInFlight = true
        }
        val appContext = context.applicationContext
        val request = Request.Builder()
            .url(CATALOG_URL)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                refreshInFlight = false
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        refreshInFlight = false
                        return
                    }
                    val raw = it.body?.string()?.takeIf(String::isNotBlank) ?: run {
                        refreshInFlight = false
                        return
                    }
                    val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    if (prefs.getString(KEY_JSON, null) == raw) {
                        prefs.edit().putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply()
                        refreshInFlight = false
                        return
                    }
                    mainHandler.post {
                        try {
                            if (!RadioRepository.applyRemoteCatalog(raw)) return@post
                            prefs.edit()
                                .putString(KEY_JSON, raw)
                                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                                .apply()
                            onApplied?.invoke()
                        } finally {
                            refreshInFlight = false
                        }
                    }
                }
            }
        })
    }
}
