package com.omnidex.app.security

import android.content.Context
import com.omnidex.app.domain.CanaryAccessVector
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object CanaryLogger {
    fun log(event: CanaryAccessVector, entryId: Long, context: Map<String, String> = emptyMap(), appContext: Context) {
        val detail = buildJsonObject {
            put("vector", event.name)
            put("entryId", entryId.toString())
            put("timestamp", System.currentTimeMillis().toString())
            context.forEach { (k, v) -> put(k, v) }
        }.toString()
        AuditLogger.log(appContext, "canary_trip", detail)
    }
}
