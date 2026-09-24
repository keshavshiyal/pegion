package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pegion_preferences")

data class UserPreferences(
    val wifiOnly: Boolean = false,
    val chargingOnly: Boolean = false,
    val maxConcurrent: Int = 3,
    val speedLimitKbps: Long = 0L, // 0 = unlimited
    val themeMode: String = "SYSTEM",
    val downloadFolder: String = "Pegion",
    val clipboardDetection: Boolean = true,
    val notificationsEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CHARGING_ONLY = booleanPreferencesKey("charging_only")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        val SPEED_LIMIT_KBPS = longPreferencesKey("speed_limit_kbps")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DOWNLOAD_FOLDER = stringPreferencesKey("download_folder")
        val CLIPBOARD_DETECTION = booleanPreferencesKey("clipboard_detection")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            wifiOnly = preferences[PreferencesKeys.WIFI_ONLY] ?: false,
            chargingOnly = preferences[PreferencesKeys.CHARGING_ONLY] ?: false,
            maxConcurrent = preferences[PreferencesKeys.MAX_CONCURRENT] ?: 3,
            speedLimitKbps = preferences[PreferencesKeys.SPEED_LIMIT_KBPS] ?: 0L,
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            downloadFolder = preferences[PreferencesKeys.DOWNLOAD_FOLDER] ?: "Pegion",
            clipboardDetection = preferences[PreferencesKeys.CLIPBOARD_DETECTION] ?: true,
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        )
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_ONLY] = enabled
        }
    }

    suspend fun setChargingOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHARGING_ONLY] = enabled
        }
    }

    suspend fun setMaxConcurrent(count: Int) {
        val clamped = count.coerceIn(1, 5)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_CONCURRENT] = clamped
        }
    }

    suspend fun setSpeedLimitKbps(kbps: Long) {
        val safe = if (kbps < 0) 0L else kbps
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEED_LIMIT_KBPS] = safe
        }
    }

    suspend fun setThemeMode(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = theme
        }
    }

    suspend fun setDownloadFolder(folder: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_FOLDER] = folder
        }
    }

    suspend fun setClipboardDetection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLIPBOARD_DETECTION] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun exportJson(prefs: UserPreferences): String {
        val json = JSONObject()
        json.put("wifiOnly", prefs.wifiOnly)
        json.put("chargingOnly", prefs.chargingOnly)
        json.put("maxConcurrent", prefs.maxConcurrent)
        json.put("speedLimitKbps", prefs.speedLimitKbps)
        json.put("themeMode", prefs.themeMode)
        json.put("downloadFolder", prefs.downloadFolder)
        json.put("clipboardDetection", prefs.clipboardDetection)
        json.put("notificationsEnabled", prefs.notificationsEnabled)
        return json.toString(2)
    }

    suspend fun importJson(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            context.dataStore.edit { preferences ->
                if (json.has("wifiOnly")) preferences[PreferencesKeys.WIFI_ONLY] = json.getBoolean("wifiOnly")
                if (json.has("chargingOnly")) preferences[PreferencesKeys.CHARGING_ONLY] = json.getBoolean("chargingOnly")
                if (json.has("maxConcurrent")) preferences[PreferencesKeys.MAX_CONCURRENT] = json.getInt("maxConcurrent").coerceIn(1, 5)
                if (json.has("speedLimitKbps")) preferences[PreferencesKeys.SPEED_LIMIT_KBPS] = json.getLong("speedLimitKbps").coerceAtLeast(0L)
                if (json.has("themeMode")) preferences[PreferencesKeys.THEME_MODE] = json.getString("themeMode")
                if (json.has("downloadFolder")) preferences[PreferencesKeys.DOWNLOAD_FOLDER] = json.getString("downloadFolder")
                if (json.has("clipboardDetection")) preferences[PreferencesKeys.CLIPBOARD_DETECTION] = json.getBoolean("clipboardDetection")
                if (json.has("notificationsEnabled")) preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = json.getBoolean("notificationsEnabled")
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
