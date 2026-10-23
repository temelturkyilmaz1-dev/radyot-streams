package com.radyo.turkiyeradyolari

import android.content.Context
import java.util.Locale

object StationInfoLocalizer {
    private data class Country(val en: String, val de: String, val code: String)
    private val countries = linkedMapOf(
        "Türkiye" to Country("Turkey", "Türkei", "tr"), "ABD" to Country("USA", "USA", "us"),
        "Almanya" to Country("Germany", "Deutschland", "de"), "Fransa" to Country("France", "Frankreich", "fr"),
        "İngiltere" to Country("United Kingdom", "Vereinigtes Königreich", "gb"),
        "Birleşik Krallık" to Country("United Kingdom", "Vereinigtes Königreich", "gb"),
        "Avustralya" to Country("Australia", "Australien", "au"), "Kanada" to Country("Canada", "Kanada", "ca"),
        "İspanya" to Country("Spain", "Spanien", "es"), "İtalya" to Country("Italy", "Italien", "it"),
        "Hollanda" to Country("Netherlands", "Niederlande", "nl"), "Belçika" to Country("Belgium", "Belgien", "be"),
        "İsviçre" to Country("Switzerland", "Schweiz", "ch"), "Avusturya" to Country("Austria", "Österreich", "at"),
        "Brezilya" to Country("Brazil", "Brasilien", "br"), "Arjantin" to Country("Argentina", "Argentinien", "ar"),
        "Yunanistan" to Country("Greece", "Griechenland", "gr"),
        "İrlanda" to Country("Ireland", "Irland", "ie"), "Polonya" to Country("Poland", "Polen", "pl"),
        "Japonya" to Country("Japan", "Japan", "jp"), "Yeni Zelanda" to Country("New Zealand", "Neuseeland", "nz"),
        "Portekiz" to Country("Portugal", "Portugal", "pt"), "Danimarka" to Country("Denmark", "Dänemark", "dk"),
        "Romanya" to Country("Romania", "Rumänien", "ro"), "Filistin" to Country("Palestine", "Palästina", "ps"),
        "Birleşik Arap Emirlikleri" to Country("United Arab Emirates", "Vereinigte Arabische Emirate", "ae"),
        "İskoçya" to Country("Scotland", "Schottland", "gb-sct"), "Güney Afrika" to Country("South Africa", "Südafrika", "za"),
        "Vietnam" to Country("Vietnam", "Vietnam", "vn"), "Kolombiya" to Country("Colombia", "Kolumbien", "co"),
        "Malezya" to Country("Malaysia", "Malaysia", "my"), "Litvanya" to Country("Lithuania", "Litauen", "lt"),
        "Macaristan" to Country("Hungary", "Ungarn", "hu"), "Meksika" to Country("Mexico", "Mexiko", "mx"),
        "Norveç" to Country("Norway", "Norwegen", "no"), "Peru" to Country("Peru", "Peru", "pe"),
        "Çekya" to Country("Czechia", "Tschechien", "cz"), "Letonya" to Country("Latvia", "Lettland", "lv"),
        "Rusya" to Country("Russia", "Russland", "ru"), "Bulgaristan" to Country("Bulgaria", "Bulgarien", "bg"),
        "Finlandiya" to Country("Finland", "Finnland", "fi"), "Ukrayna" to Country("Ukraine", "Ukraine", "ua"),
        "Venezuela" to Country("Venezuela", "Venezuela", "ve"), "Hırvatistan" to Country("Croatia", "Kroatien", "hr"),
        "İsveç" to Country("Sweden", "Schweden", "se"), "Kıbrıs" to Country("Cyprus", "Zypern", "cy"),
        "Lihtenştayn" to Country("Liechtenstein", "Liechtenstein", "li"),
        "Sırbistan" to Country("Serbia", "Serbien", "rs"),
        "İzlanda" to Country("Iceland", "Island", "is"),
        "Slovakya" to Country("Slovakia", "Slowakei", "sk"),
        "Kazakistan" to Country("Kazakhstan", "Kasachstan", "kz"),
        "Bosna-Hersek" to Country("Bosnia and Herzegovina", "Bosnien und Herzegowina", "ba"),
        "Trinidad ve Tobago" to Country("Trinidad and Tobago", "Trinidad und Tobago", "tt"),
        "Şili" to Country("Chile", "Chile", "cl"),
        "Dominik Cumhuriyeti" to Country("Dominican Republic", "Dominikanische Republik", "do"),
        "Küba" to Country("Cuba", "Kuba", "cu"),
        "Malta" to Country("Malta", "Malta", "mt"),
        "Azerbaycan" to Country("Azerbaijan", "Aserbaidschan", "az"),
        "Güney Kore" to Country("South Korea", "Südkorea", "kr"),
        "Fas" to Country("Morocco", "Marokko", "ma"),
        "Kuzey Kıbrıs Türk Cumhuriyeti" to Country("Northern Cyprus", "Nordzypern", "cy")
    )
    private val turkishCities = listOf(
        "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Aksaray", "Amasya", "Ankara", "Antalya",
        "Ardahan", "Artvin", "Aydın", "Balıkesir", "Bartın", "Batman", "Bayburt", "Bilecik",
        "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa", "Çanakkale", "Çankırı", "Çorum",
        "Denizli", "Diyarbakır", "Düzce", "Edirne", "Elazığ", "Erzincan", "Erzurum", "Eskişehir",
        "Gaziantep", "Giresun", "Gümüşhane", "Hakkari", "Hatay", "Iğdır", "Isparta", "İstanbul",
        "İzmir", "Kahramanmaraş", "Karabük", "Karaman", "Kars", "Kastamonu", "Kayseri", "Kilis",
        "Kırıkkale", "Kırklareli", "Kırşehir", "Kocaeli", "Konya", "Kütahya", "Malatya", "Manisa",
        "Mardin", "Mersin", "Muğla", "Muş", "Nevşehir", "Niğde", "Ordu", "Osmaniye", "Rize",
        "Sakarya", "Samsun", "Siirt", "Sinop", "Sivas", "Şanlıurfa", "Şırnak", "Tekirdağ",
        "Tokat", "Trabzon", "Tunceli", "Uşak", "Van", "Yalova", "Yozgat", "Zonguldak"
    )

    fun location(context: Context, radio: Radio, raw: String): String {
        if (raw.isBlank()) return ""
        val language = language(context)
        var translated = raw
        countries.forEach { (tr, values) ->
            if (listOf(tr, values.en, values.de).any { raw.contains(it, true) }) {
                val localized = if (language == "en") values.en else if (language == "de") values.de else tr
                listOf(tr, values.en, values.de).forEach { translated = translated.replace(it, localized, true) }
            }
        }
        return translated.trim()
    }

    fun countryCode(radio: Radio, raw: String): String {
        countries.forEach { (tr, values) ->
            if (listOf(tr, values.en, values.de).any { raw.contains(it, true) }) return values.code
        }
        return if (turkishCities.any { raw.contains(it, true) } ||
            cityFromStationName(radio.name).isNotBlank() || radio.website.contains(".tr", true)
        ) "tr" else ""
    }

    fun city(raw: String): String {
        val first = raw.split(',', '•', '/').map(String::trim).firstOrNull(String::isNotBlank).orEmpty()
        if (first.isBlank() || first.equals("Uluslararası internet radyosu", true)) return ""
        val isCountry = countries.any { (tr, values) ->
            listOf(tr, values.en, values.de).any { first.equals(it, true) }
        }
        return first.takeUnless { isCountry }.orEmpty()
    }

    fun city(radio: Radio, raw: String): String = city(raw).ifBlank {
        cityFromStationName(radio.name)
    }

    /** Bayrağın altında şehir yoksa ülke adını gösterir; alan boş kalmaz. */
    fun placeLabel(radio: Radio, raw: String): String = city(radio, raw).ifBlank {
        countries.entries.sortedByDescending { it.key.length }.firstNotNullOfOrNull { (tr, values) ->
            if (listOf(tr, values.en, values.de).any { raw.contains(it, true) }) tr else null
        }.orEmpty()
    }

    fun cityFromStationName(name: String): String {
        val words = " ${fold(name)} "
        val compact = fold(name).replace(" ", "")
        return turkishCities.firstOrNull { candidate ->
            val city = fold(candidate)
            val cityCompact = city.replace(" ", "")
            words.contains(" $city ") || compact.endsWith(cityCompact) ||
                compact.contains("radyo$cityCompact") || compact.contains("$cityCompact" + "fm")
        }.orEmpty()
    }

    private fun fold(value: String): String = java.text.Normalizer
        .normalize(value.lowercase(Locale.forLanguageTag("tr-TR")).replace('ı', 'i'), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    fun content(context: Context, raw: String): String {
        val language = language(context)
        if (language == "tr" || raw.isBlank()) return raw
        val value = raw.lowercase(Locale.forLanguageTag("tr-TR"))
        val key = when {
            "metal" in value -> "metal"
            "rock" in value -> "rock"
            "caz" in value || "jazz" in value -> "jazz"
            "blues" in value -> "blues"
            "haber" in value || "konuşma" in value -> "news"
            "spor" in value -> "sports"
            "klasik" in value -> "classical"
            "türk halk" in value || "türkü" in value -> "folk"
            "arabesk" in value || "fantezi" in value -> "arabesque"
            "hip-hop" in value || "hip hop" in value || "rap" in value -> "hiphop"
            "elektronik" in value || "dans" in value -> "dance"
            "chill" in value || "lounge" in value -> "chill"
            "slow" in value || "romantik" in value -> "slow"
            "nostal" in value || "oldies" in value -> "oldies"
            "pop" in value || "hit" in value -> "pop"
            else -> "mixed"
        }
        val en = mapOf("metal" to "Metal music", "rock" to "Rock music", "jazz" to "Jazz music", "blues" to "Blues music", "news" to "News and talk", "sports" to "Sports radio", "classical" to "Classical music", "folk" to "Turkish folk music", "arabesque" to "Arabesque and fantasy music", "hiphop" to "Rap, hip-hop and R&B", "dance" to "Dance and electronic music", "chill" to "Chillout and lounge music", "slow" to "Slow and romantic music", "oldies" to "Oldies and nostalgic music", "pop" to "Pop music and current hits", "mixed" to "Mixed music and live radio")
        val de = mapOf("metal" to "Metal-Musik", "rock" to "Rockmusik", "jazz" to "Jazzmusik", "blues" to "Bluesmusik", "news" to "Nachrichten und Gespräche", "sports" to "Sportradio", "classical" to "Klassische Musik", "folk" to "Türkische Volksmusik", "arabesque" to "Arabeske und Fantasiemusik", "hiphop" to "Rap, Hip-Hop und R&B", "dance" to "Dance und elektronische Musik", "chill" to "Chillout- und Lounge-Musik", "slow" to "Ruhige und romantische Musik", "oldies" to "Oldies und nostalgische Musik", "pop" to "Popmusik und aktuelle Hits", "mixed" to "Gemischte Musik und Live-Radio")
        return (if (language == "de") de else en).getValue(key)
    }

    private fun language(context: Context): String =
        context.resources.configuration.locales[0].language.ifBlank { "tr" }
}
