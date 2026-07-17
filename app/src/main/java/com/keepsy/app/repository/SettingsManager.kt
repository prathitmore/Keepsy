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

    private val _lastUserId = MutableStateFlow<String?>(null)
    val lastUserId: StateFlow<String?> = _lastUserId

    // Profile Cache for Instant UI
    private val _localProfileCache = MutableStateFlow<LocalProfileData>(LocalProfileData())
    val localProfileCache: StateFlow<LocalProfileData> = _localProfileCache

    data class LocalProfileData(
        val name: String? = null,
        val displayName: String? = null,
        val photoPath: String? = null
    )

    init {
        loadSettings()
    }

    private fun loadSettings() {
        try {
            _isOnboardingCompleted.value = prefs.getBoolean("onboarding_complete", false)
            _lastUserId.value = prefs.getString("last_user_id", null)
            
            _localProfileCache.value = LocalProfileData(
                name = prefs.getString("local_profile_name", null),
                displayName = prefs.getString("local_profile_display_name", null),
                photoPath = prefs.getString("local_profile_photo_path", null)
            )
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to read settings", e)
        }
    }

    fun updateLocalProfile(name: String? = null, displayName: String? = null, photoPath: String? = null) {
        try {
            val editor = prefs.edit()
            name?.let { editor.putString("local_profile_name", it) }
            displayName?.let { editor.putString("local_profile_display_name", it) }
            photoPath?.let { editor.putString("local_profile_photo_path", it) }
            editor.apply()
            
            _localProfileCache.value = _localProfileCache.value.copy(
                name = name ?: _localProfileCache.value.name,
                displayName = displayName ?: _localProfileCache.value.displayName,
                photoPath = photoPath ?: _localProfileCache.value.photoPath
            )
            KeepsyLogger.d("SettingsManager: Local profile cache updated")
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to update local profile cache", e)
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

    fun resetSettings() {
        try {
            prefs.edit().clear().apply()
            _isOnboardingCompleted.value = false
            _localProfileCache.value = LocalProfileData()
            KeepsyLogger.i("SettingsManager: Secure preferences wiped")
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to reset settings", e)
        }
    }
}
