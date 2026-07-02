package com.keepsy.app.sync

import android.content.Context
import androidx.work.*
import com.keepsy.app.model.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import com.keepsy.app.utils.KeepsyLogger
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context, private val repository: SyncRepository) {
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val networkObserver = NetworkObserver(context)

    fun schedulePeriodicSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "KeepsyPeriodicSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            KeepsyLogger.i("Periodic sync scheduled")
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to schedule periodic sync", e)
        }
    }

    fun triggerOneTimeSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "KeepsyOneTimeSync",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
            KeepsyLogger.i("One-time sync triggered")
        } catch (e: Exception) {
            KeepsyLogger.e("Failed to trigger one-time sync", e)
        }
    }

    suspend fun startNetworkObservation(scope: kotlinx.coroutines.CoroutineScope) {
        networkObserver.isConnected.collectLatest { isConnected ->
            if (isConnected) {
                performSync()
            }
        }
    }

    suspend fun performSync() {
        if (_syncState.value == SyncState.SYNCING || _syncState.value == SyncState.UPLOADING || _syncState.value == SyncState.DOWNLOADING) return
        
        _syncState.value = SyncState.SYNCING
        try {
            _syncState.value = SyncState.UPLOADING
            repository.fullSync()
            _syncState.value = SyncState.COMPLETED
            kotlinx.coroutines.delay(1500)
            _syncState.value = SyncState.IDLE
        } catch (e: Exception) {
            _syncState.value = SyncState.FAILED
            kotlinx.coroutines.delay(3000)
            _syncState.value = SyncState.IDLE
        }
    }
}
