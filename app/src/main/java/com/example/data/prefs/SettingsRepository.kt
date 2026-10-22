package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

val Context.dataStore by preferencesDataStore(name = "lens_settings")

data class AppSettings(
    val language: String = if (Locale.getDefault().language.startsWith("ar")) "ar" else "en",
    val designLook: String = "ios", // "ios" or "md"
    val appearance: String = "auto", // "auto", "light", "dark"
    val photoQuality: String = "std", // "std" (85%) or "high" (95%)
    val defaultCamera: String = "back", // "back" or "front"
    val shutterSound: Boolean = true,
    val gridEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false
)

class SettingsRepository(private val context: Context) {
    companion object {
        private val KEY_LANG = stringPreferencesKey("lang")
        private val KEY_DESIGN = stringPreferencesKey("design")
        private val KEY_APPEARANCE = stringPreferencesKey("appearance")
        private val KEY_QUALITY = stringPreferencesKey("quality")
        private val KEY_DEFAULT_CAM = stringPreferencesKey("default_cam")
        private val KEY_SHUTTER_SOUND = booleanPreferencesKey("shutter_sound")
        private val KEY_GRID = booleanPreferencesKey("grid")
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaultLang = if (Locale.getDefault().language.startsWith("ar")) "ar" else "en"
        AppSettings(
            language = prefs[KEY_LANG] ?: defaultLang,
            designLook = prefs[KEY_DESIGN] ?: "ios",
            appearance = prefs[KEY_APPEARANCE] ?: "auto",
            photoQuality = prefs[KEY_QUALITY] ?: "std",
            defaultCamera = prefs[KEY_DEFAULT_CAM] ?: "back",
            shutterSound = prefs[KEY_SHUTTER_SOUND] ?: true,
            gridEnabled = prefs[KEY_GRID] ?: false,
            onboardingCompleted = prefs[KEY_ONBOARDED] ?: false
        )
    }

    suspend fun updateLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANG] = lang }
    }

    suspend fun updateDesign(design: String) {
        context.dataStore.edit { it[KEY_DESIGN] = design }
    }

    suspend fun updateAppearance(appearance: String) {
        context.dataStore.edit { it[KEY_APPEARANCE] = appearance }
    }

    suspend fun updateQuality(quality: String) {
        context.dataStore.edit { it[KEY_QUALITY] = quality }
    }

    suspend fun updateDefaultCamera(camera: String) {
        context.dataStore.edit { it[KEY_DEFAULT_CAM] = camera }
    }

    suspend fun updateShutterSound(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHUTTER_SOUND] = enabled }
    }

    suspend fun updateGrid(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GRID] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDED] = completed }
    }
}
