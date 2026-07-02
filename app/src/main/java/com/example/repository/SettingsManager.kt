package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("keepsy_settings", Context.MODE_PRIVATE)

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_complete", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted

    private val _darkModePreference = MutableStateFlow(
        if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else null
    )
    // Returns: null for System default, true for Force Dark, false for Force Light
    val darkModePreference: StateFlow<Boolean?> = _darkModePreference

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_complete", completed).apply()
        _isOnboardingCompleted.value = completed
    }

    fun setDarkModePreference(dark: Boolean?) {
        val editor = prefs.edit()
        if (dark == null) {
            editor.remove("dark_mode")
            _darkModePreference.value = null
        } else {
            editor.putBoolean("dark_mode", dark)
            _darkModePreference.value = dark
        }
        editor.apply()
    }

    fun resetSettings() {
        prefs.edit().clear().apply()
        _isOnboardingCompleted.value = false
        _darkModePreference.value = null
    }
}
