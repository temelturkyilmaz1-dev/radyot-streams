package com.radyo.turkiyeradyolari

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SongHistoryItem(
    val radioName: String,
    val artist: String,
    val title: String,
    val time: Long,
    val artworkUrl: String? = null
)

object SongHistoryStore {
    private const val PREFS = "song_history"
    private const val MAX_ITEMS = 50

    fun add(context: Context, radioName: String, artist: String, title: String, artworkUrl: String? = null) {
        if (radioName.isBlank() || title.isBlank()) return
        val list = get(context, radioName).toMutableList()
        val first = list.firstOrNull()
        if (first?.artist == artist && first.title == title) {
            if (!artworkUrl.isNullOrBlank() && first.artworkUrl != artworkUrl) {
                list[0] = first.copy(artworkUrl = artworkUrl)
                save(context, radioName, list)
            }
            return
        }
        list.add(0, SongHistoryItem(radioName, artist, title, System.currentTimeMillis(), artworkUrl))
        while (list.size > MAX_ITEMS) list.removeAt(list.lastIndex)
        save(context, radioName, list)
    }

    fun updateArtwork(context: Context, radioName: String, artist: String, title: String, artworkUrl: String?) {
        if (artworkUrl.isNullOrBlank()) return
        val list = get(context, radioName).toMutableList()
        val index = list.indexOfFirst { it.artist == artist && it.title == title }
        if (index < 0) return
        list[index] = list[index].copy(artworkUrl = artworkUrl)
        save(context, radioName, list)
    }

    /** Resmî istasyon geçmişinden gelen eski kayıtları mevcut yerel geçmişle birleştirir. */
    fun merge(context: Context, radioName: String, incoming: List<SongHistoryItem>) {
        if (incoming.isEmpty()) return
        val merged = (get(context, radioName) + incoming)
            .filter { it.title.isNotBlank() }
            .distinctBy { "${it.artist.lowercase()}|${it.title.lowercase()}" }
            .sortedByDescending(SongHistoryItem::time)
            .take(MAX_ITEMS)
        save(context, radioName, merged)
    }

    fun get(context: Context, radioName: String): List<SongHistoryItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(radioName, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                SongHistoryItem(
                    radioName = o.optString("radioName", radioName),
                    artist = o.optString("artist"),
                    title = o.optString("title"),
                    time = o.optLong("time"),
                    artworkUrl = o.optString("artworkUrl").takeIf { it.isNotBlank() }
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, radioName: String, list: List<SongHistoryItem>) {
        val array = JSONArray()
        list.forEach { item ->
            array.put(JSONObject().apply {
                put("radioName", item.radioName)
                put("artist", item.artist)
                put("title", item.title)
                put("time", item.time)
                put("artworkUrl", item.artworkUrl ?: "")
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(radioName, array.toString()).apply()
    }

    fun clear(context: Context, radioName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(radioName).apply()
    }
}
