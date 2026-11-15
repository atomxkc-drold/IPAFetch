package com.omnidex.app.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultEntryTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val entry = VaultEntry(
        id = 7,
        title = "Hugging Face token",
        brandId = "huggingface",
        type = EntryType.TOKEN.key,
        account = "ml-team",
        secret = "cipher",
        generateUrl = "https://huggingface.co/settings/tokens",
        usage = "Downloads gated models in CI",
        category = EntryCategories.AI,
        tags = listOf("ci", "models")
    )

    @Test
    fun matchesTitleAccountAndTags() {
        assertTrue(entry.matches("hugging"))
        assertTrue(entry.matches("ML-TEAM"))
        assertTrue(entry.matches("ci"))
        assertFalse(entry.matches("stripe"))
        assertTrue(entry.matches(""))
    }

    @Test
    fun expiryDetection() {
        val now = 1_000_000L
        assertTrue(entry.copy(expiresAt = now - 1).isExpired(now))
        assertFalse(entry.copy(expiresAt = now + 1000).isExpired(now))
        assertTrue(entry.copy(expiresAt = now + 1000).expiresSoon(now))
        assertFalse(entry.copy(expiresAt = now + 8L * 24 * 60 * 60 * 1000).expiresSoon(now))
        assertFalse(entry.copy(expiresAt = null).isExpired(now))
    }

    @Test
    fun serializationRoundTripKeepsEveryField() {
        val restored = json.decodeFromString<VaultEntry>(json.encodeToString(entry))
        assertEquals(entry, restored)
    }

    @Test
    fun legacyEntriesMigrate() {
        val legacyJson = """
            [{"id":3,"name":"Old key","service":"openai","category":"API",
              "username":"me","endpoint":"https://example.com",
              "notes":"note","createdAt":1700000000000,"updatedAt":1700000000000,
              "encryptedValue":"blob"}]
        """.trimIndent()

        val legacy = json.decodeFromString<List<LegacyKeyEntry>>(legacyJson)
        val migrated = legacy.map { it.toEntry() }

        assertEquals(1, migrated.size)
        assertEquals("Old key", migrated[0].title)
        assertEquals("blob", migrated[0].secret)
        assertEquals(EntryType.API.key, migrated[0].type)
        assertEquals(EntryCategories.DEV, migrated[0].category)
        assertEquals("me", migrated[0].account)
    }

    @Test
    fun typeEnumFallsBackToToken() {
        assertEquals(EntryType.TOKEN, EntryType.from("nonsense"))
        assertEquals(EntryType.JSON, EntryType.from("json"))
    }
}
