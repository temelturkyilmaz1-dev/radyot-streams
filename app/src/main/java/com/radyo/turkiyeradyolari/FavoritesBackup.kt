package com.radyo.turkiyeradyolari

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/** Versioned portable backup. Parsing completes before any preferences are changed. */
object FavoritesBackup {
    data class Entry(val name: String, val urls: List<String>)
    data class Merge(val names: List<String>, val added: Int, val missing: List<String>)

    fun encode(names: List<String>, catalog: List<Radio>): String {
        val entries = JSONArray()
        names.distinct().forEach { name ->
            val radio = catalog.firstOrNull { it.name == name }
            entries.put(JSONObject().put("name", name)
                .put("urls", JSONArray(radio?.streamUrls ?: emptyList<String>())))
        }
        return JSONObject().put("type", "radionline_favorites").put("version", 1)
            .put("favorites", entries).toString(2)
    }

    fun decode(text: String): List<Entry> {
        val root = JSONObject(text)
        require(root.get("type") == "radionline_favorites") { "Bu dosya RadiOnline favori yedeği değil." }
        require(root.get("version") is Number && root.get("version").toString() == "1") {
            "Bu yedek sürümü desteklenmiyor."
        }
        val items = root.getJSONArray("favorites")
        require(items.length() <= 10000) { "Yedekte çok fazla kayıt var." }
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val name = item.get("name") as? String
            require(!name.isNullOrBlank() && name.length <= 500 && !name.contains('|')) {
                "Yedekte geçersiz radyo adı var."
            }
            val urls = item.getJSONArray("urls")
            require(urls.length() <= 100) { "Geçersiz yayın listesi." }
            Entry(name, (0 until urls.length()).map {
                val url = urls.get(it) as? String
                require(url != null && url.length <= 4096 &&
                    (url.startsWith("http://") || url.startsWith("https://"))) {
                    "Yedekte geçersiz yayın adresi var."
                }
                url
            })
        }
    }

    private fun streamKey(value: String): String = try {
        val uri = URI(value.trim())
        // Keep path, query and port: different mounts may be different stations.
        "${uri.host?.lowercase()}:${uri.port}${uri.rawPath.orEmpty().trimEnd('/')}" +
            (uri.rawQuery?.let { "?$it" } ?: "")
    } catch (_: Exception) { value.trim() }

    fun merge(existing: List<String>, entries: List<Entry>, catalog: List<Radio>): Merge {
        val names = existing.distinct().toMutableList()
        val missing = mutableListOf<String>()
        var added = 0
        entries.forEach { entry ->
            val exact = catalog.firstOrNull { it.name == entry.name }
            val urlKeys = entry.urls.map(::streamKey).toSet()
            val matches = if (exact == null && urlKeys.isNotEmpty()) catalog.filter { radio ->
                radio.streamUrls.any { streamKey(it) in urlKeys }
            } else emptyList()
            val radio = exact ?: matches.singleOrNull()
            if (radio == null) missing.add(entry.name)
            else if (radio.name !in names) { names.add(radio.name); added++ }
        }
        return Merge(names, added, missing.distinct())
    }

    fun names(context: Context): List<String> =
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
            .getString("favorite_radios", "").orEmpty().split('|').filter(String::isNotBlank)
}