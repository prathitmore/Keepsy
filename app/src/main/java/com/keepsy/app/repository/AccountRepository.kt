package com.keepsy.app.repository

import android.net.Uri
import com.keepsy.app.service.FirebaseService
import com.keepsy.app.model.UserProfile
import com.keepsy.app.database.AppDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.keepsy.app.utils.KeepsyLogger

class AccountRepository(
    private val firebaseService: FirebaseService,
    private val appDao: AppDao
) {
    suspend fun uploadProfilePhoto(uri: Uri): String {
        return firebaseService.uploadProfilePicture(uri)
    }

    suspend fun deleteProfilePhoto() {
        firebaseService.deleteProfilePicture()
    }

    suspend fun updateProfile(name: String?, photoUrl: String?) {
        firebaseService.updateProfile(name, photoUrl)
    }

    suspend fun changePassword(current: String, new: String) {
        firebaseService.changePassword(current, new)
    }

    suspend fun getStorageUsage(): Map<String, Long> {
        // Mock implementation for now as requested breakdown
        // Real implementation would calculate file sizes in app storage
        return mapOf(
            "images" to 800000L,
            "database" to 400000L,
            "backup" to 20000L
        )
    }
}
