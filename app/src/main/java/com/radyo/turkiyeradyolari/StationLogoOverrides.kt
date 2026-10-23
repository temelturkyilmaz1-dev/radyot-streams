package com.radyo.turkiyeradyolari

import java.text.Normalizer
import java.util.Locale

/** Presentation-only spacing rules; station logo files live on Cloudflare. */
object StationLogoOverrides {
    private fun key(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.ROOT).replace('ı', 'i'), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), "")

    private val paddedLogoNames = setOf(
        "General FM", "Radyo Fon Malatya", "Radyo Energy", "Radyo Diyalog",
        "Radyo Akademi", "Radio Veronika", "Radyo 72", "Radyo Ahenk",
        "Radyo Düet", "Dümensiz Radyo", "İmparator FM Sakarya", "Kolaylı Radyo",
        "Kral Rizeli FM", "KU Radyo", "Mavi Nokta Fm", "Mega Radyo İskenderun",
        "Merkez FM Samsun 88.4", "Moda FM Ünye", "Nevşehir Radyo Mega", "Nurs FM",
        "Örnek Radyo Muğla", "Radyo Çetin", "Radio Soft Denmark",
        "Radio Soft Classic", "Radio Soft Modern"
    ).map(::key).toSet()

    // No station-specific logo resources are bundled in the APK.
    fun localLogoFor(radio: Radio): Int? = null

    // The catalog's Cloudflare logo URL is the single source of truth.
    fun remoteLogoFor(radio: Radio): String = radio.logoPath.trim()

    fun logoPaddingDpFor(radio: Radio): Int = when {
        key(radio.name) == key("TRT Memleketim") || key(radio.name) == key("TRT Memleketim FM") -> 16
        key(radio.name) == key("TRT TSR") -> 18
        key(radio.name).startsWith("trt") -> 10
        key(radio.name) in paddedLogoNames -> 8
        else -> 0
    }

    fun needsLightLogoBackground(radio: Radio): Boolean = key(radio.name) in paddedLogoNames
}
