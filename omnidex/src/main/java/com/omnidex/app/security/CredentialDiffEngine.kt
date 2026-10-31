package com.omnidex.app.security

import com.omnidex.app.data.VaultRepository
import com.omnidex.app.domain.VaultEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

object CredentialDiffEngine {
    data class DiffResult(val changes: List<DiffChange>)
    data class DiffChange(val path: String, val type: DiffType, val oldValue: String?, val newValue: String?)
    enum class DiffType { ADDED, REMOVED, CHANGED }
    
    fun diff(oldEntry: VaultEntry, newEntry: VaultEntry, repo: VaultRepository): DiffResult {
        val changes = mutableListOf<DiffChange>()
        
        // Compare title
        if (oldEntry.title != newEntry.title) {
            changes.add(DiffChange("title", DiffType.CHANGED, oldEntry.title, newEntry.title))
        }
        
        // Compare category
        if (oldEntry.category != newEntry.category) {
            changes.add(DiffChange("category", DiffType.CHANGED, oldEntry.category, newEntry.category))
        }
        
        // Compare type
        if (oldEntry.type != newEntry.type) {
            changes.add(DiffChange("type", DiffType.CHANGED, oldEntry.type, newEntry.type))
        }
        
        // Try to parse extras as JSON for semantic diff
        try {
            if (oldEntry.extrasBlob.isNotBlank() && newEntry.extrasBlob.isNotBlank()) {
                val oldJson = Json.parseToJsonElement(oldEntry.extrasBlob).jsonObject
                val newJson = Json.parseToJsonElement(newEntry.extrasBlob).jsonObject
                
                val allKeys = (oldJson.keys + newJson.keys).distinct()
                for (key in allKeys) {
                    val oldVal = oldJson[key]?.toString()
                    val newVal = newJson[key]?.toString()
                    when {
                        oldVal == null && newVal != null -> changes.add(DiffChange("extras.$key", DiffType.ADDED, null, newVal))
                        oldVal != null && newVal == null -> changes.add(DiffChange("extras.$key", DiffType.REMOVED, oldVal, null))
                        oldVal != newVal -> changes.add(DiffChange("extras.$key", DiffType.CHANGED, oldVal, newVal))
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to simple comparison
        }
        
        // Note secret changes
        if (oldEntry.secret != newEntry.secret) {
            changes.add(DiffChange("secret", DiffType.CHANGED, "[REDACTED]", "[REDACTED]"))
        }
        
        return DiffResult(changes)
    }
}
