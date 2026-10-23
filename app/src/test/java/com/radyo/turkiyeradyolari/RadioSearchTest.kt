package com.radyo.turkiyeradyolari

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioSearchTest {
    private val radio = Radio(
        name = "Adıyaman Güney Radyo",
        url = "https://example.test/live",
        logoRes = 0,
        location = "Adıyaman, Türkiye",
        frequency = "99.2 FM",
        content = "Türkçe müzik"
    )

    @Test
    fun matchesWordsAnywhereInStationData() {
        assertTrue(RadioSearch.matches(radio, "güney"))
        assertTrue(RadioSearch.matches(radio, "adıyaman radyo"))
        assertTrue(RadioSearch.matches(radio, "radyo 99.2"))
        assertTrue(RadioSearch.matches(radio, "guney muzik"))
    }

    @Test
    fun requiresEveryEnteredWord() {
        assertFalse(RadioSearch.matches(radio, "güney istanbul"))
    }
}
