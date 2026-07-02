package com.keepsy.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.keepsy.app.utils.KeepsyLogger
import com.keepsy.app.utils.GlobalCrashHandler

class KeepsyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Global Crash Handler first
        GlobalCrashHandler.initialize(this)
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            KeepsyLogger.i("Firebase initialized successfully")
        } catch (e: Exception) {
            KeepsyLogger.e("Firebase initialization failed", e)
        }
    }
}
