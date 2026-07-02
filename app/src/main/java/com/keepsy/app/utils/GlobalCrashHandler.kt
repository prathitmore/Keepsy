package com.keepsy.app.utils

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * GlobalCrashHandler captures uncaught exceptions across all threads.
 * It ensures crashes are logged to KeepsyLogger and recorded by Crashlytics before exiting.
 */
class GlobalCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            KeepsyLogger.e("CRITICAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
            
            // Explicitly record to Crashlytics
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("thread_name", thread.name)
                recordException(throwable)
            }
        } catch (e: Exception) {
            // Avoid infinite loops
        } finally {
            // Let the default Android handler take over
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }
    }

    companion object {
        fun initialize(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(context, defaultHandler))
            KeepsyLogger.i("GlobalCrashHandler initialized")
        }
    }
}
