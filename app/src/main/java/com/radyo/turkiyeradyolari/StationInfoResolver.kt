package com.radyo.turkiyeradyolari

import android.net.Uri

data class StationInfo(
    val website: String,
    val content: String,
    val location: String,
    val frequency: String,
    val sourceHost: String
)

/**
 * Katalogda ayrıntı yokken bilgi panelini boş bırakmaz. Yalnızca güvenle
 * ilişkilendirilebilen resmî grup sitelerini kullanır; bilinmeyen bir adresi
 * resmî site gibi göstermeden yayın sunucusunun alan adını belirtir.
 */
object StationInfoResolver {
    fun resolve(radio: Radio): StationInfo = StationInfo(
        website = radio.website.ifBlank { knownWebsite(radio.name) },
        content = radio.content.ifBlank { inferContent(radio.name) },
        location = resolveLocation(radio),
        frequency = radio.frequency.ifBlank { inferFrequency(radio.name) },
        sourceHost = runCatching { Uri.parse(radio.url).host.orEmpty() }.getOrDefault("")
    )

    /**
     * Eski katalogdan gelen Ulusal/Yurtdışı değerleri konum değildir. Ulusal
     * kayıtlar Türkiye olarak gösterilir; Yurtdışı kayıtlarında ise ad ve alan
     * adından güvenle çıkarılabilen ülke kullanılır.
     */
    private fun resolveLocation(radio: Radio): String {
        val raw = radio.location.trim()
        return when {
            raw.isBlank() -> inferLocation(radio)
            raw.equals("Ulusal", true) ->
                StationInfoLocalizer.cityFromStationName(radio.name).ifBlank { "Türkiye" }
            raw.equals("Yurtdışı", true) || raw.equals("Çevrimiçi", true) ||
                raw.equals("İnternet Radyosu", true) -> inferLocation(radio)
            else -> raw
        }
    }

    private fun knownWebsite(name: String): String {
        val value = name.lowercase()
        return when {
            value.startsWith("trt ") || value == "trt fm" -> "https://www.trtdinle.com/radyolar"
            value.startsWith("power") -> "https://www.powerapp.com.tr/"
            value.startsWith("number1") -> "https://www.numberone.com.tr/"
            value.startsWith("kral") -> "https://www.kralmuzik.com.tr/"
            value.startsWith("kiss ") || value == "kiss fm" -> "https://kissfm.com.tr/"
            value.startsWith("joy") || value.startsWith("metro fm") ||
                value.startsWith("süper") || value.startsWith("virgin radio") ||
                value.startsWith("kisstory") -> "https://karnaval.com/"
            value.startsWith("radyo odtü") -> "https://radyodtu.com.tr/"
            value.startsWith("itü ") || value.startsWith("i̇tü ") -> "https://radyo.itu.edu.tr/"
            value.startsWith("radyo fenomen") || value.startsWith("fenomen ") -> "https://www.radyofenomen.com/"
            value.startsWith("show ") || value == "show radyo" -> "https://www.showradyo.com.tr/"
            value.startsWith("viva ") || value == "radyo viva" -> "https://www.radyoviva.com.tr/"
            value == "kafa radyo" -> "https://kafaradyo.com/"
            value == "alem fm" -> "https://www.alemfm.com/"
            value == "yön radyo" -> "https://yonradyo.com.tr/"
            value == "radyo voyage" -> "https://www.radyovoyage.com/"
            value.startsWith("radyo fenerbahçe") -> "https://www.radyofenerbahce.com/"
            value == "nts 1" -> "https://www.nts.live/"
            value == "radio paradise" -> "https://radioparadise.com/"
            value == "rinse fm" -> "https://www.rinse.fm/"
            value == "café del mar radio" -> "https://cafedelmar.com/radio"
            value == "worldwide fm" -> "https://www.worldwidefm.net/"
            value == "soho radio" -> "https://sohoradio.com/"
            value == "radyo trio" -> "https://radyotrio.com/"
            value == "cgtn türk radyo" -> "https://www.cgtnturk.com/radyo"
            value == "metal devastation radio" -> "https://metaldevastationradio.com/"
            else -> ""
        }
    }

    private fun inferContent(name: String): String {
        val value = name.lowercase()
        return when {
            "metal" in value -> "Metal müzik"
            "rock" in value -> "Rock müzik"
            "jazz" in value || "caz" in value -> "Caz müzik"
            "blues" in value -> "Blues müzik"
            "country" in value -> "Country müzik"
            "klasik" in value || "classic" in value || "musiki" in value -> "Klasik müzik"
            "türkü" in value || "turku" in value || "halk" in value -> "Türk Halk Müziği"
            "arabesk" in value || "damar" in value || "meyhane" in value -> "Arabesk ve fantezi müzik"
            "rap" in value || "hiphop" in value || "hip hop" in value -> "Rap ve hip hop"
            "dance" in value || "dans" in value || "club" in value -> "Dans ve elektronik müzik"
            "chill" in value || "lounge" in value || "relax" in value || "easy" in value -> "Chillout ve lounge müzik"
            "slow" in value || "love" in value -> "Slow ve romantik müzik"
            "nostalji" in value || "45'lik" in value || "45lik" in value ||
                "80s" in value || "90s" in value || "90lar" in value -> "Nostaljik müzik"
            "haber" in value || "news" in value || "cgtn" in value -> "Haber ve konuşma programları"
            "spor" in value || "fenerbahçe" in value -> "Spor yayını"
            "pop" in value || "hit" in value -> "Pop müzik"
            else -> "Karma müzik ve canlı radyo yayını"
        }
    }

    private fun inferLocation(radio: Radio): String {
        val namedCity = StationInfoLocalizer.cityFromStationName(radio.name)
        val value = radio.name.lowercase()
        val host = runCatching {
            Uri.parse(
                radio.website
                    .ifBlank { knownWebsite(radio.name) }
                    .ifBlank { radio.url }
            ).host.orEmpty().lowercase()
        }.getOrDefault("")
        return when {
            namedCity.isNotBlank() -> namedCity
            value.startsWith("trt ") || value.startsWith("power") ||
                value.startsWith("number1") || value.startsWith("kral") ||
                value.startsWith("kiss ") || value == "kiss fm" ||
                value.startsWith("joy") || value.startsWith("metro fm") ||
                value.startsWith("süper") || value.startsWith("virgin radio") ||
                value.startsWith("fenomen") || value.startsWith("radyo fenomen") ||
                value.startsWith("show ") || value == "show radyo" ||
                value.startsWith("viva ") || value == "radyo viva" ||
                value.startsWith("radyo odtü") || value.startsWith("itü ") ||
                value.startsWith("i̇tü ") || value == "kafa radyo" ||
                value == "alem fm" || value == "yön radyo" ||
                value == "radyo voyage" || value.startsWith("radyo fenerbahçe") -> "Türkiye"
            "hatay" in value -> "Hatay"
            "antalya" in value || "akdeniz" in value -> "Antalya"
            "ankara" in value || "odtü" in value || "ostim" in value -> "Ankara"
            "i̇tü" in value || "itu" in value || "boğaziçi" in value -> "İstanbul"
            "tunceli" in value || "munzur" in value || "piryolu" in value -> "Tunceli"
            "erzincan" in value -> "Erzincan"
            "karacadağ" in value -> "Diyarbakır"
            host.endsWith(".tr") || ".com.tr" in host -> "Türkiye"
            host.endsWith(".de") -> "Almanya"
            host.endsWith(".fr") -> "Fransa"
            host.endsWith(".it") -> "İtalya"
            host.endsWith(".es") -> "İspanya"
            host.endsWith(".nl") -> "Hollanda"
            host.endsWith(".be") -> "Belçika"
            host.endsWith(".at") -> "Avusturya"
            host.endsWith(".ch") -> "İsviçre"
            host.endsWith(".se") -> "İsveç"
            host.endsWith(".dk") -> "Danimarka"
            host.endsWith(".lt") -> "Litvanya"
            host.endsWith(".gr") -> "Yunanistan"
            host.endsWith(".rs") -> "Sırbistan"
            host.endsWith(".is") -> "İzlanda"
            host.endsWith(".sk") -> "Slovakya"
            host.endsWith(".pt") -> "Portekiz"
            host.endsWith(".ro") -> "Romanya"
            host.endsWith(".bg") -> "Bulgaristan"
            host.endsWith(".hu") -> "Macaristan"
            host.endsWith(".no") -> "Norveç"
            host.endsWith(".fi") -> "Finlandiya"
            host.endsWith(".ua") -> "Ukrayna"
            host.endsWith(".mx") -> "Meksika"
            host.endsWith(".br") -> "Brezilya"
            host.endsWith(".ar") -> "Arjantin"
            host.endsWith(".co") -> "Kolombiya"
            host.endsWith(".cl") -> "Şili"
            host.endsWith(".kz") -> "Kazakistan"
            host.endsWith(".az") -> "Azerbaycan"
            host.endsWith(".ba") -> "Bosna-Hersek"
            host.endsWith(".cy") -> "Kıbrıs"
            host.endsWith(".pl") -> "Polonya"
            host.endsWith(".au") || host.endsWith(".com.au") -> "Avustralya"
            host.endsWith(".ca") -> "Kanada"
            host.endsWith(".co.uk") || " uk" in value || value.endsWith("uk") -> "Birleşik Krallık"
            "chicago" in value || "america" in value -> "ABD"
            "rock klassiker" in value -> "İsveç"
            "rockfm heavy" in value -> "Rusya"
            "rock fm metal" in value || "maximusrock" in value -> "Almanya"
            "metal radio austria" in value -> "Avusturya"
            "viyana" in value -> "Avusturya"
            "swiss" in value -> "İsviçre"
            value.startsWith("nrk ") -> "Norveç"
            value == "abc classic 2 fm" -> "Avustralya"
            value == "radio horyzont" -> "Polonya"
            value == "radio veronika" || value == "darik radyo" -> "Bulgaristan"
            value == "literal radio" -> "Meksika"
            "bbc" in value -> "Birleşik Krallık"
            "berlin" in value -> "Almanya"
            "kktc" in value || "kıbrıs" in value -> "Kuzey Kıbrıs Türk Cumhuriyeti"
            "azerbaycan" in value -> "Azerbaycan"
            "greece" in value -> "Yunanistan"
            "k-pop" in value || "kore pop" in value -> "Güney Kore"
            "danceland turkey" in value || value.startsWith("power ") ||
                value.startsWith("nrg ") || value == "radyo voyage" ||
                value == "dijital radyo" || value == "fakir fm" ||
                value == "loops radio electronica station" ||
                value == "marmaris yacht marin fm" || value == "andromeda radio" ||
                value == "radyo maximum" || value == "türkiyem fm" ||
                value == "solo piano radyo" -> "Türkiye"
            else -> "Uluslararası internet radyosu"
        }
    }

    private fun inferFrequency(name: String): String {
        val match = Regex("(?:^|\\s)(\\d{2,3}(?:[.,]\\d)?)$").find(name.trim()) ?: return ""
        val number = match.groupValues[1].replace(',', '.')
        val numeric = number.toDoubleOrNull() ?: return ""
        return if (numeric in 87.0..108.0) "$number FM" else ""
    }
}
