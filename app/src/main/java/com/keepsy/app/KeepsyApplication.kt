package com.keepsy.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.keepsy.app.utils.KeepsyLogger
import com.keepsy.app.utils.GlobalCrashHandler

class KeepsyApplication : Application() {

    companion object {
        lateinit var instance: KeepsyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Global Crash Handler
        GlobalCrashHandler.initialize(this)
        
        // Firebase is typically auto-initialized by FirebaseInitProvider.
        // We log status to verify.
        try {
            val apps = FirebaseApp.getApps(this)
            if (apps.isEmpty()) {
                FirebaseApp.initializeApp(this)
                KeepsyLogger.i("Firebase initialized manually in Application")
            } else {
                KeepsyLogger.i("Firebase initialized by Provider")
            }
        } catch (e: Exception) {
            // Non-fatal logging
            android.util.Log.e("Keepsy", "Firebase status check failed", e)
        }
    }
}
