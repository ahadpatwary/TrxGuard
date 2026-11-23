package com.txnrelay.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val endpointUrl: String = "",
    val apiKey: String = "",
    val autoSync: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Random per-install id, sent with every event so the backend can tell devices apart. */
    val deviceId: String = "",
) {
    val isConfigured: Boolean get() = endpointUrl.isNotBlank()
}

/**
 * App settings backed by EncryptedSharedPreferences (the API key is a secret).
 * Exposes a [StateFlow] for the UI and a synchronous [current] snapshot for the sync code.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)
    private val state = MutableStateFlow(load())

    val settings: StateFlow<AppSettings> = state.asStateFlow()
    val current: AppSettings get() = state.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(state.value)
        prefs.edit()
            .putString(KEY_ENDPOINT, next.endpointUrl)
            .putString(KEY_API_KEY, next.apiKey)
            .putBoolean(KEY_AUTO_SYNC, next.autoSync)
            .putString(KEY_THEME, next.themeMode.name)
            .apply()
        state.value = next
    }

    private fun load(): AppSettings {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return AppSettings(
            endpointUrl = prefs.getString(KEY_ENDPOINT, "").orEmpty(),
            apiKey = prefs.getString(KEY_API_KEY, "").orEmpty(),
            autoSync = prefs.getBoolean(KEY_AUTO_SYNC, true),
            themeMode = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null).orEmpty()) }
                .getOrDefault(ThemeMode.SYSTEM),
            deviceId = deviceId,
        )
    }

    private companion object {
        const val FILE_NAME = "txnrelay_secure_prefs"
        const val KEY_ENDPOINT = "endpoint_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_AUTO_SYNC = "auto_sync"
        const val KEY_THEME = "theme_mode"
        const val KEY_DEVICE_ID = "device_id"

        fun createPrefs(context: Context): SharedPreferences =
            try {
                openEncrypted(context)
            } catch (e: Exception) {
                // Keystore/prefs corruption (rare, seen on some OEMs after restores): start clean.
                context.deleteSharedPreferences(FILE_NAME)
                openEncrypted(context)
            }

        fun openEncrypted(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
