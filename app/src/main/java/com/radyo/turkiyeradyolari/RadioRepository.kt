package com.radyo.turkiyeradyolari

import org.json.JSONObject
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Runtime station catalog. Station records and logos are delivered by Cloudflare;
 * this APK contains only the generic placeholder and the catalog client.
 */
object RadioRepository {
    val radios = mutableListOf<Radio>()
    private val recentlyAddedStationKeys = listOf(
        "Cadena 100", "Loca Urban", "Catalunya Música", "Rock FM İspanya",
        "Sunshine Live CH", "Radio TOP İsviçre", "Radio Argovia", "Radio RaBe",
        "R101", "DJ International", "Radio Bari 88.8", "P12 Hitmix",
        "Kral FM 100 İskeçe", "Rás 2", "Kiss FM İzlanda", "FlashBack",
        "80's FlashBack", "Bylgjan", "K100", "Retro", "FM 957", "X-ið",
        "90's FlashBack", "Kiss FM Xtra", "Gullbylgjan", "Léttbylgjan",
        "70's FlashBack", "60's FlashBack", "Country Bylgjan", "Íslenska Bylgjan",
        "FM Trölli", "Suðurland FM", "80s Bylgjan", "Rondó", "Apparatið",
        "Turkuaz FM", "Münih Radyo", "Birmingham Mountain Radio 107.3",
        "Oyente Stereo", "Classic Country 104.9", "Radio Woodstock 100.1 WDST",
        "GDRadio.net", "Radio Zvezda 95.6", "Bassdrive", "Rock The Bones Radio",
        "Radyo Eylül 99.1", "İşleyen FM 96.6", "Radyo FEF 101.5", "Size Radio"
    ).mapTo(hashSetOf(), ::canonicalName)

    @Synchronized
    fun applyRemoteCatalog(rawJson: String): Boolean {
        val parsed = runCatching {
            val root = JSONObject(rawJson)
            if (root.optInt("schemaVersion", -1) != 1) return@runCatching null
            val stations = root.optJSONArray("stations") ?: return@runCatching null
            // Reject a truncated or unrelated JSON response without replacing a
            // previously cached working catalog.
            if (stations.length() < 50 || stations.length() > 10_000) return@runCatching null

            val byName = linkedMapOf<String, Radio>()
            for (index in 0 until stations.length()) {
                val item = stations.optJSONObject(index) ?: continue
                if (!item.optBoolean("enabled", true)) continue
                val name = item.optString("name").trim()
                if (name.isBlank()) continue

                val urlsJson = item.optJSONArray("urls") ?: continue
                val urls = (0 until urlsJson.length())
                    .mapNotNull { urlsJson.optString(it).trim().takeIf(::isHttpUrl) }
                    .distinct()
                if (urls.isEmpty()) continue

                val key = canonicalName(name)
                if (key.isBlank() || key in byName) continue
                val logo = item.optString("logo").trim().takeIf(::isHttpUrl).orEmpty()
                byName[key] = Radio(
                    name = name,
                    url = urls.first(),
                    logoRes = R.drawable.radyot_logo,
                    backupUrls = urls.drop(1),
                    website = item.optString("website").trim().takeIf(::isHttpUrl).orEmpty(),
                    content = item.optString("content").trim(),
                    location = item.optString("location").trim(),
                    frequency = item.optString("frequency").trim(),
                    logoPath = logo
                )
            }
            if (byName.size < 50) return@runCatching null

            val trCollator = Collator.getInstance(Locale.forLanguageTag("tr-TR"))
            byName.values.sortedWith { a, b -> trCollator.compare(a.name, b.name) }
        }.getOrNull() ?: return false

        radios.clear()
        radios.addAll(parsed)
        return true
    }

    @Synchronized
    fun restoreBundledCatalog() {
        // There is intentionally no embedded station catalog. The last validated
        // Cloudflare response is loaded from the app's local cache on startup.
        radios.clear()
    }

    fun isRecentlyAdded(radio: Radio): Boolean = canonicalName(radio.name) in recentlyAddedStationKeys

    @Synchronized
    fun byName(name: String): Radio? {
        val wanted = canonicalName(name)
        return radios.firstOrNull { canonicalName(it.name) == wanted }
    }

    private fun isHttpUrl(value: String): Boolean =
        value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true)

    private fun canonicalName(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.ROOT).replace('ı', 'i'), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), "")
}
