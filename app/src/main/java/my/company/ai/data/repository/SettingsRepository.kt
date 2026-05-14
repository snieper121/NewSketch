package my.company.ai.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.company.ai.BuildConfig

private val Context.dataStore by preferencesDataStore(name = "ai_settings")

/**
 * Настройки приложения: выбор AI-провайдера, API-ключ, модель, base URL
 * (ADR-006).  Всё async через DataStore — `Flow` читается напрямую UI.
 */
class SettingsRepository(private val context: Context) {

    data class Snapshot(
        val apiKey: String,
        val baseUrl: String,
        val model: String,
    )

    private object Keys {
        val API_KEY = stringPreferencesKey("ai_api_key")
        val BASE_URL = stringPreferencesKey("ai_base_url")
        val MODEL = stringPreferencesKey("ai_model")
    }

    val snapshot: Flow<Snapshot> = context.dataStore.data.map { prefs ->
        Snapshot(
            apiKey = prefs[Keys.API_KEY].orEmpty(),
            baseUrl = prefs[Keys.BASE_URL]?.takeIf { it.isNotBlank() }
                ?: BuildConfig.DEFAULT_AI_BASE_URL,
            model = prefs[Keys.MODEL]?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL,
        )
    }

    suspend fun setApiKey(value: String) = context.dataStore.edit { it[Keys.API_KEY] = value }

    suspend fun setBaseUrl(value: String) = context.dataStore.edit { it[Keys.BASE_URL] = value }

    suspend fun setModel(value: String) = context.dataStore.edit { it[Keys.MODEL] = value }

    private companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
    }
}
