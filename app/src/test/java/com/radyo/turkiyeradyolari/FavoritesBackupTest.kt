package com.radyo.turkiyeradyolari

import org.junit.Assert.*
import org.junit.Test

class FavoritesBackupTest {
    private val a = Radio("A", "https://radio.example/a", 0)
    private val b = Radio("Yeni B", "https://radio.example/b", 0)

    @Test fun roundTripPreservesOrder() {
        val decoded = FavoritesBackup.decode(FavoritesBackup.encode(listOf(b.name, a.name), listOf(a, b)))
        assertEquals(listOf(b.name, a.name), decoded.map { it.name })
        assertEquals(listOf(b.url), decoded.first().urls)
    }

    @Test fun mergesWithoutDeletingOrDuplicatingAndMatchesRenamedStation() {
        val entries = listOf(
            FavoritesBackup.Entry("Eski B", listOf(b.url)),
            FavoritesBackup.Entry("A", listOf(a.url)),
            FavoritesBackup.Entry("Kayıp", listOf("https://radio.example/missing"))
        )
        val merged = FavoritesBackup.merge(listOf("A"), entries, listOf(a, b))
        assertEquals(listOf("A", "Yeni B"), merged.names)
        assertEquals(1, merged.added)
        assertEquals(listOf("Kayıp"), merged.missing)
    }

    @Test fun ambiguousStreamDoesNotPickArbitraryStation() {
        val merged = FavoritesBackup.merge(emptyList(), listOf(
            FavoritesBackup.Entry("Eski", listOf(a.url))
        ), listOf(a, a.copy(name = "Başka")))
        assertTrue(merged.names.isEmpty())
        assertEquals(listOf("Eski"), merged.missing)
    }

    @Test fun rejectsUnrelatedAndFutureBackups() {
        for (text in listOf(
            """{"type":"other","version":1,"favorites":[]}""",
            """{"type":"radionline_favorites","version":2,"favorites":[]}""",
            """{"type":"radionline_favorites","version":1,"favorites":[{"name":"A"}]}"""
        )) {
            assertTrue(runCatching { FavoritesBackup.decode(text) }.isFailure)
        }
    }
}