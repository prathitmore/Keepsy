package com.keepsy.app.sync

import com.google.firebase.auth.FirebaseAuth
import com.keepsy.app.database.AppDao
import com.keepsy.app.model.*
import com.keepsy.app.service.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import com.keepsy.app.utils.KeepsyLogger

class SyncRepository(
    private val appDao: AppDao,
    private val firestoreService: FirestoreService,
    private val firebaseService: FirebaseService
) {
    private val auth = FirebaseAuth.getInstance()

    suspend fun fullSync() = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext
        KeepsyLogger.i("Starting full sync...")
        try {
            uploadDirtyData()
            downloadAndMergeData()
            resolveRelationships()
            KeepsyLogger.i("Full sync completed")
        } catch (e: Exception) {
            KeepsyLogger.e("Sync failed", e)
            throw e
        }
    }

    suspend fun syncOnLogin() = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext
        KeepsyLogger.i("First sync on login...")
        try {
            downloadAndMergeData()
            resolveRelationships()
            uploadDirtyData()
        } catch (e: Exception) {
            KeepsyLogger.e("Sync on login failed", e)
        }
    }

    suspend fun isUserAlreadyExistsOnCloud(): Boolean = withContext(Dispatchers.IO) {
        try {
            val profile = firestoreService.getProfile()
            if (profile != null && profile["onboardingCompleted"] as? Boolean == true) return@withContext true
            val collections = listOf("spaces", "items", "activityLogs")
            for (coll in collections) {
                if (firestoreService.checkIfCollectionHasData(coll)) return@withContext true
            }
            false
        } catch (e: Exception) { false }
    }

    private suspend fun uploadDirtyData() {
        try {
            val dirtySpaces = appDao.getDirtySpaces()
            for (space in dirtySpaces) {
                val rId = space.remoteId ?: java.util.UUID.randomUUID().toString()
                firestoreService.uploadEntity("spaces", rId, space.toMap())
                appDao.updateSpace(space.copy(remoteId = rId, syncState = "SYNCED", lastSynced = System.currentTimeMillis()))
            }
        } catch (e: Exception) { }

        try {
            val dirtyItems = appDao.getDirtyItems()
            for (item in dirtyItems) {
                val rId = item.remoteId ?: java.util.UUID.randomUUID().toString()
                firestoreService.uploadEntity("items", rId, item.toMap())
                appDao.updateItem(item.copy(remoteId = rId, syncState = "SYNCED", lastSynced = System.currentTimeMillis()))
            }
        } catch (e: Exception) { }

        try {
            val dirtyCategories = appDao.getDirtyCategories()
            for (category in dirtyCategories) {
                val rId = category.remoteId ?: java.util.UUID.randomUUID().toString()
                firestoreService.uploadEntity("categories", rId, category.toMap())
                appDao.markCategorySynced(category.categoryId, rId, "SYNCED", System.currentTimeMillis())
            }
        } catch (e: Exception) { }

        try {
            val dirtyTags = appDao.getDirtyTags()
            for (tag in dirtyTags) {
                val rId = tag.remoteId ?: java.util.UUID.randomUUID().toString()
                firestoreService.uploadEntity("tags", rId, tag.toMap())
                appDao.markTagSynced(tag.tagId, rId, "SYNCED", System.currentTimeMillis())
            }
        } catch (e: Exception) { }

        try {
            val dirtyLogs = appDao.getDirtyActivityLogs()
            for (log in dirtyLogs) {
                val rId = log.remoteId ?: java.util.UUID.randomUUID().toString()
                firestoreService.uploadEntity("activityLogs", rId, log.toMap())
                appDao.markActivityLogSynced(log.activityId, rId, "SYNCED", System.currentTimeMillis())
            }
        } catch (e: Exception) { }
    }

    private suspend fun downloadAndMergeData() {
        try {
            val remoteSpaces = firestoreService.getAllEntities("spaces")
            for (data in remoteSpaces) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toSpace(remoteId)
                val localEntity = appDao.getSpaceByRemoteId(remoteId)
                if (localEntity == null) appDao.insertSpace(remoteEntity.copy(parentSpaceId = null))
                else if (remoteEntity.updatedAt > localEntity.updatedAt) appDao.updateSpace(remoteEntity.copy(spaceId = localEntity.spaceId, parentSpaceId = localEntity.parentSpaceId))
            }
        } catch (e: Exception) { }

        try {
            val remoteItems = firestoreService.getAllEntities("items")
            for (data in remoteItems) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toItem(remoteId)
                val localEntity = appDao.getItemByRemoteId(remoteId)
                if (localEntity == null) appDao.insertItem(remoteEntity.copy(spaceId = 0L, categoryId = 0L))
                else if (remoteEntity.updatedAt > localEntity.updatedAt) appDao.updateItem(remoteEntity.copy(itemId = localEntity.itemId, spaceId = localEntity.spaceId, categoryId = localEntity.categoryId))
            }
        } catch (e: Exception) { }

        try {
            val remoteCategories = firestoreService.getAllEntities("categories")
            for (data in remoteCategories) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toCategory(remoteId)
                val localEntity = appDao.getCategoryByRemoteId(remoteId)
                if (localEntity == null) appDao.insertCategory(remoteEntity)
                else if (remoteEntity.updatedAt > localEntity.updatedAt) appDao.updateCategory(remoteEntity.copy(categoryId = localEntity.categoryId))
            }
        } catch (e: Exception) { }

        try {
            val remoteTags = firestoreService.getAllEntities("tags")
            for (data in remoteTags) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toTag(remoteId)
                val localEntity = appDao.getTagByRemoteId(remoteId)
                if (localEntity == null) appDao.insertTag(remoteEntity)
                else if (remoteEntity.updatedAt > localEntity.updatedAt) appDao.updateTag(remoteEntity.copy(tagId = localEntity.tagId))
            }
        } catch (e: Exception) { }

        try {
            val remoteLogs = firestoreService.getAllEntities("activityLogs")
            for (data in remoteLogs) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toActivityLog(remoteId)
                val localEntity = appDao.getActivityLogByRemoteId(remoteId)
                if (localEntity == null) appDao.insertActivityLog(remoteEntity)
                else if (remoteEntity.updatedAt > localEntity.updatedAt) appDao.updateActivityLog(remoteEntity.copy(activityId = localEntity.activityId))
            }
        } catch (e: Exception) { }
    }

    private suspend fun resolveRelationships() {
        val spacesList = appDao.getLiveSpaces().first()
        for (space in spacesList) {
            if (space.parentRemoteId != null) {
                val parent = appDao.getSpaceByRemoteId(space.parentRemoteId!!)
                if (parent != null && space.parentSpaceId != parent.spaceId) appDao.updateSpace(space.copy(parentSpaceId = parent.spaceId))
            } else if (space.parentSpaceId != null) appDao.updateSpace(space.copy(parentSpaceId = null))
        }

        val itemsList = appDao.getLiveActiveItems().first()
        for (item in itemsList) {
            var updatedItem = item; var changed = false
            if (item.spaceRemoteId != null) {
                val space = appDao.getSpaceByRemoteId(item.spaceRemoteId!!)
                if (space != null && item.spaceId != space.spaceId) { updatedItem = updatedItem.copy(spaceId = space.spaceId); changed = true }
            }
            if (item.categoryRemoteId != null) {
                val category = appDao.getCategoryByRemoteId(item.categoryRemoteId!!)
                if (category != null && item.categoryId != category.categoryId) { updatedItem = updatedItem.copy(categoryId = category.categoryId); changed = true }
            }
            if (changed) appDao.updateItem(updatedItem)
        }
    }
}
