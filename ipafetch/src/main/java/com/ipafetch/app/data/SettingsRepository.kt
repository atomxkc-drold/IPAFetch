package com.ipafetch.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Non-secret user preferences (themes, layout, interaction).
 * Stored as a single JSON string; the vault's encrypted store is untouched.
 */
class SettingsRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val current: AppSettings get() = _settings.value

    private fun load(): AppSettings {
        val raw = prefs.getString(KEY, null) ?: return AppSettings()
        return runCatching { json.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        _settings.value = next
        prefs.edit().putString(KEY, json.encodeToString(next)).apply()
    }

    fun set(next: AppSettings) = update { next }

    companion object {
        private const val PREFS = "ipafetch_settings"
        private const val KEY = "settings_json"

        @Volatile
        private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
    }
}
