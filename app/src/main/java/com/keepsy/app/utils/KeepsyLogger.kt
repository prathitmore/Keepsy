package com.keepsy.app.utils

import android.util.Log
import com.keepsy.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * KeepsyLogger provides a centralized logging mechanism.
 * Logs are also sent to Firebase Crashlytics for remote debugging.
 */
object KeepsyLogger {
    private const val TAG = "Keepsy"

    private fun getCrashlytics(): FirebaseCrashlytics? {
        return try {
            // Safely check if the application instance exists before using it
            val context = try { com.keepsy.app.KeepsyApplication.instance } catch (e: Exception) { null }
            if (context != null && FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseCrashlytics.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
        getCrashlytics()?.log("DEBUG: $message")
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        getCrashlytics()?.apply {
            log("ERROR: $message")
            throwable?.let { recordException(it) }
        }
    }

    fun i(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message)
        }
        getCrashlytics()?.log("INFO: $message")
    }

    fun w(message: String) {
        Log.w(TAG, message)
        getCrashlytics()?.log("WARN: $message")
    }
}
