package com.keepsy.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.keepsy.app.database.AppDatabase
import com.keepsy.app.service.FirebaseService
import com.keepsy.app.utils.KeepsyLogger

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        KeepsyLogger.i("Worker: Starting background sync")
        
        val database = AppDatabase.getDatabase(applicationContext)
        val firestoreService = FirestoreService()
        val repository = SyncRepository(database.appDao(), firestoreService)
        
        return try {
            repository.fullSync()
            Result.success()
        } catch (e: Exception) {
            KeepsyLogger.e("Worker: Sync failed", e)
            Result.retry()
        }
    }
}
