package com.ipafetch.app.crypto

import com.ipafetch.app.domain.KeyEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExportManagerTest {

    private val masterKey = "correcthorsebatterystaple".toCharArray()
    private val salt = CryptoManager.generateSalt()
    private val derived = CryptoManager.deriveMasterKey(masterKey, salt)

    private fun encrypt(value: String) = CryptoManager.encrypt(value, derived)
    private fun decrypt(blob: String) = CryptoManager.decrypt(blob, derived)

    private fun newEntry(value: String) = KeyEntry(
        name = "Test Entry",
        service = "test-service",
        category = "API",
        username = "user",
        endpoint = "https://example.com",
        notes = "some notes",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L,
        expiresAt = 1710000000000L,
        keyLength = 32,
        prefix = "sk-",
        rotatedAt = null,
        encryptedValue = encrypt(value)
    )

    @Test
    fun exportImportRoundTrip() {
        val entries = listOf(
            newEntry("sk-live_abc123"),
            newEntry("sk-test_xyz789")
        )

        val exportPassphrase = "backup-passphrase".toCharArray()
        val jsonBlob = ExportManager.export(
            entries = entries,
            exportPassphrase = exportPassphrase,
            decrypt = ::decrypt
        )

        val imported = ExportManager.import(
            blob = jsonBlob,
            exportPassphrase = exportPassphrase,
            encrypt = ::encrypt
        )

        assertEquals(entries.size, imported.size)

        assertEquals(
            listOf("sk-live_abc123", "sk-test_xyz789"),
            imported.map { decrypt(it.encryptedValue) }
        )

        // Verify all other fields round-trip
        assertEquals(entries[0].name, imported[0].name)
        assertEquals(entries[0].service, imported[0].service)
        assertEquals(entries[0].category, imported[0].category)
        assertEquals(entries[0].username, imported[0].username)
        assertEquals(entries[0].endpoint, imported[0].endpoint)
        assertEquals(entries[0].notes, imported[0].notes)
        assertEquals(entries[0].createdAt, imported[0].createdAt)
        assertEquals(entries[0].expiresAt, imported[0].expiresAt)
    }

    @Test
    fun exportImport_wrongPassphrase_fails() {
        val entries = listOf(newEntry("sk-live_abc123"))
        val jsonBlob = ExportManager.export(
            entries = entries,
            exportPassphrase = "correct".toCharArray(),
            decrypt = ::decrypt
        )

        assertFailsWith<Exception> {
            ExportManager.import(
                blob = jsonBlob,
                exportPassphrase = "wrong".toCharArray(),
                encrypt = ::encrypt
            )
        }
    }
}
