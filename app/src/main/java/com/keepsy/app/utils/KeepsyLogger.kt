package com.keepsy.app.utils

import android.util.Log
import com.keepsy.app.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * KeepsyLogger provides a centralized logging mechanism.
 * Logs are also sent to Firebase Crashlytics for remote debugging.
 */
object KeepsyLogger {
    private const val TAG = "Keepsy"
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
        crashlytics.log("DEBUG: $message")
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        crashlytics.log("ERROR: $message")
        throwable?.let { crashlytics.recordException(it) }
    }

    fun i(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message)
        }
        crashlytics.log("INFO: $message")
    }

    fun w(message: String) {
        Log.w(TAG, message)
        crashlytics.log("WARN: $message")
    }
}
