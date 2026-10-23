package com.radyo.turkiyeradyolari

import java.text.Normalizer
import java.util.Locale

/** Telefon, TV ve Android Auto için tek ve tutarlı radyo arama kuralı. */
object RadioSearch {
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val combiningMarks = Regex("\\p{M}+")
    private val separators = Regex("[^a-z0-9]+")

    fun normalize(value: String): String = Normalizer
        .normalize(value.lowercase(turkishLocale).replace('ı', 'i'), Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .replace(separators, " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    fun matches(radio: Radio, query: String): Boolean {
        val words = normalize(query).split(' ').filter(String::isNotBlank)
        if (words.isEmpty()) return true

        val searchable = normalize(
            listOf(radio.name, radio.frequency, radio.location, radio.content, radio.website)
                .joinToString(" ")
        )
        return words.all(searchable::contains)
    }
}
