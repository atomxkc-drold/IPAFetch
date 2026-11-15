package com.omnidex.app.crypto

import com.omnidex.app.domain.EntryType
import com.omnidex.app.domain.VaultEntry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExportManagerTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val entries = listOf(
        VaultEntry(
            id = 1,
            title = "OpenAI production",
            brandId = "openai",
            type = EntryType.TOKEN.key,
            account = "team@example.com",
            secret = "aes-blob-1",
            generateUrl = "https://platform.openai.com/api-keys",
            usage = "Used by the summariser worker",
            category = "AI & ML",
            tags = listOf("prod", "billing"),
            expiresAt = 1_900_000_000_000L
        ),
        VaultEntry(
            id = 2,
            title = "Stripe test key",
            brandId = "stripe",
            type = EntryType.API.key,
            secret = "aes-blob-2",
            category = "Payments"
        )
    )

    private val vaultJson = json.encodeToString(entries)

    @Test
    fun exportImportRoundTrip() {
        val passphrase = "backup-passphrase"
        val envelope = ExportManager.export(vaultJson, passphrase)

        val decrypted = ExportManager.import(envelope, passphrase)
        assertNotNull(decrypted)

        val restored = json.decodeFromString<List<VaultEntry>>(decrypted)
        assertEquals(entries.size, restored.size)
        assertEquals(entries[0].title, restored[0].title)
        assertEquals(entries[0].secret, restored[0].secret)
        assertEquals(entries[0].generateUrl, restored[0].generateUrl)
        assertEquals(entries[0].usage, restored[0].usage)
        assertEquals(entries[0].tags, restored[0].tags)
        assertEquals(entries[0].expiresAt, restored[0].expiresAt)
    }

    @Test
    fun wrongPassphraseReturnsNull() {
        val envelope = ExportManager.export(vaultJson, "correct-passphrase")
        assertNull(ExportManager.import(envelope, "wrong-passphrase"))
    }

    @Test
    fun tamperedPayloadReturnsNull() {
        val envelope = ExportManager.export(vaultJson, "backup-passphrase")
        val tampered = envelope.replace("\"data\":\"", "\"data\":\"AAAA")
        assertNull(ExportManager.import(tampered, "backup-passphrase"))
    }

    @Test
    fun shortPassphraseIsRejected() {
        val failed = runCatching { ExportManager.export(vaultJson, "short") }.isFailure
        assertEquals(true, failed)
    }
}
