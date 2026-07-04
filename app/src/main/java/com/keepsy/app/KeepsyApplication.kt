package com.keepsy.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.keepsy.app.utils.KeepsyLogger
import com.keepsy.app.utils.GlobalCrashHandler

class KeepsyApplication : Application() {

    companion object {
        private var _instance: KeepsyApplication? = null
        val instance: KeepsyApplication 
            get() = _instance ?: throw IllegalStateException("Application not yet initialized")
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
        
        // Initialize Global Crash Handler
        GlobalCrashHandler.initialize(this)
        
        // Firebase initialization check
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.e("Keepsy", "Firebase check failed", e)
        }
    }
}
