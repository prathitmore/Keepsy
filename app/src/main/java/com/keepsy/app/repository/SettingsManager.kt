package com.keepsy.app.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.keepsy.app.utils.KeepsyLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "keepsy_secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        KeepsyLogger.e("Failed to initialize EncryptedSharedPreferences, falling back to standard", e)
        context.getSharedPreferences("keepsy_fallback_settings", Context.MODE_PRIVATE)
    }

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted

    private val _isTutorialCompleted = MutableStateFlow(false)
    val isTutorialCompleted: StateFlow<Boolean> = _isTutorialCompleted

    private val _tutorialStep = MutableStateFlow(0)
    val tutorialStep: StateFlow<Int> = _tutorialStep

    private val _darkModePreference = MutableStateFlow<Boolean?>(null)
    val darkModePreference: StateFlow<Boolean?> = _darkModePreference

    init {
        try {
            _isOnboardingCompleted.value = prefs.getBoolean("onboarding_complete", false)
            _isTutorialCompleted.value = prefs.getBoolean("tutorial_complete", false)
            _tutorialStep.value = prefs.getInt("tutorial_step", 0)
            _darkModePreference.value = if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else null
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to read settings in init", e)
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

    fun setTutorialCompleted(completed: Boolean) {
        try {
            prefs.edit().putBoolean("tutorial_complete", completed).apply()
            _isTutorialCompleted.value = completed
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to set tutorial status", e)
        }
    }

    fun setTutorialStep(step: Int) {
        try {
            prefs.edit().putInt("tutorial_step", step).apply()
            _tutorialStep.value = step
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to set tutorial step", e)
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
