package com.keepsy.app.service

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.keepsy.app.utils.KeepsyLogger

/**
 * SecurityService handles device integrity and secure storage.
 * Prepared for future Play Integrity API integration.
 */
class SecurityService(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "keepsy_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Placeholder for Play Integrity API verification.
     * Future implementation will request a token and verify it with a backend.
     */
    suspend fun verifyDeviceIntegrity(): Boolean {
        KeepsyLogger.d("Verifying device integrity (Placeholder)")
        // Simulation of a successful integrity check
        return true
    }

    fun saveSecureString(key: String, value: String) {
        securePrefs.edit().putString(key, value).apply()
    }

    fun getSecureString(key: String): String? {
        return securePrefs.getString(key, null)
    }

    fun clearSecureData() {
        securePrefs.edit().clear().apply()
    }
}
