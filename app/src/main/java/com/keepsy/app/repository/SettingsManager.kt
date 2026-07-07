package com.keepsy.app.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.keepsy.app.utils.KeepsyLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private var prefs: SharedPreferences = try {
        createSecurePrefs()
    } catch (e: Exception) {
        KeepsyLogger.e("SettingsManager: Secure storage corruption, performing recovery", e)
        // Recovery: Clear the underlying file if possible
        context.getSharedPreferences("keepsy_secure_settings", Context.MODE_PRIVATE).edit().clear().apply()
        try {
            createSecurePrefs()
        } catch (e2: Exception) {
            KeepsyLogger.e("SettingsManager: Fallback to standard prefs", e2)
            context.getSharedPreferences("keepsy_standard_settings", Context.MODE_PRIVATE)
        }
    }

    private fun createSecurePrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "keepsy_secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted

    private val _darkModePreference = MutableStateFlow<Boolean?>(null)
    val darkModePreference: StateFlow<Boolean?> = _darkModePreference

    private val _lastUserId = MutableStateFlow<String?>(null)
    val lastUserId: StateFlow<String?> = _lastUserId

    init {
        loadSettings()
    }

    private fun loadSettings() {
        try {
            _isOnboardingCompleted.value = prefs.getBoolean("onboarding_complete", false)
            _darkModePreference.value = if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else null
            _lastUserId.value = prefs.getString("last_user_id", null)
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to read settings", e)
        }
    }

    fun setLastUserId(uid: String?) {
        try {
            prefs.edit().putString("last_user_id", uid).apply()
            _lastUserId.value = uid
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to set last user id", e)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        try {
            prefs.edit().putBoolean("onboarding_complete", completed).apply()
            _isOnboardingCompleted.value = completed
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to set onboarding status", e)
        }
    }

    fun setDarkModePreference(dark: Boolean?) {
        try {
            val editor = prefs.edit()
            if (dark == null) {
                editor.remove("dark_mode")
                _darkModePreference.value = null
            } else {
                editor.putBoolean("dark_mode", dark)
                _darkModePreference.value = dark
            }
            editor.apply()
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to set dark mode preference", e)
        }
    }

    fun resetSettings() {
        try {
            prefs.edit().clear().apply()
            _isOnboardingCompleted.value = false
            _darkModePreference.value = null
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to reset settings", e)
        }
    }
}
