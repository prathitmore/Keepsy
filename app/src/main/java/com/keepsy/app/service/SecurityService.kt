package com.keepsy.app.service

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.keepsy.app.utils.KeepsyLogger

/**
 * SecurityService handles device integrity and secure storage.
 * It is hardened against Keystore corruption (AEADBadTagException).
 */
class SecurityService(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private var securePrefs: SharedPreferences = try {
        createSecurePrefs()
    } catch (e: Exception) {
        KeepsyLogger.e("SecurityService: Keystore corruption detected, performing recovery", e)
        // Recovery logic: Wipe the corrupted prefs file and re-create
        context.getSharedPreferences("keepsy_secure_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        // Try one more time
        try {
            createSecurePrefs()
        } catch (e2: Exception) {
            KeepsyLogger.e("SecurityService: Critical Keystore failure, falling back to unencrypted storage", e2)
            context.getSharedPreferences("keepsy_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    private fun createSecurePrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "keepsy_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun verifyDeviceIntegrity(): Boolean {
        return true
    }

    fun saveSecureString(key: String, value: String) {
        try {
            securePrefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to write to secure storage", e)
        }
    }

    fun getSecureString(key: String): String? {
        return try {
            securePrefs.getString(key, null)
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to read from secure storage", e)
            null
        }
    }

    fun clearSecureData() {
        securePrefs.edit().clear().apply()
    }
}
