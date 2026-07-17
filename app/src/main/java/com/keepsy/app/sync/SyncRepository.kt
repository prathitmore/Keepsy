package com.keepsy.app.sync

import com.google.firebase.auth.FirebaseAuth
import com.keepsy.app.database.AppDao
import com.keepsy.app.model.*
import com.keepsy.app.service.FirebaseService
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import com.keepsy.app.utils.KeepsyLogger
import java.io.File

class SyncRepository(
    private val appDao: AppDao,
    private val firestoreService: FirestoreService,
    private val firebaseService: FirebaseService
) {
    private val TAG = "KeepsySync"
    private val auth = FirebaseAuth.getInstance()

    suspend fun fullSync() = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) {
            KeepsyLogger.w("Sync aborted: No authenticated user")
            return@withContext
        }
        
        KeepsyLogger.i("Starting full sync...")
        try {
            uploadDirtyData()
            downloadAndMergeData()
            resolveRelationships()
            KeepsyLogger.i("Full sync completed successfully")
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
        val uid = auth.currentUser?.uid
        if (uid == null) {
            KeepsyLogger.w("CloudCheck: No UID found")
            return@withContext false
        }
        
        KeepsyLogger.i("CloudCheck: Scanning account for $uid")
        try {
            // 1. Check Profile
            val profile = firestoreService.getProfile()
            if (profile != null) {
                KeepsyLogger.d("CloudCheck: Profile found")
                if (profile["onboardingCompleted"] as? Boolean == true) {
                    KeepsyLogger.i("CloudCheck: Onboarding flag found in profile")
                    return@withContext true
                }
            }
            
            // 2. Deep scan collections for existence (limit 1 for speed)
            val collections = listOf("spaces", "items", "activityLogs")
            for (coll in collections) {
                if (firestoreService.checkIfCollectionHasData(coll)) {
                    KeepsyLogger.i("CloudCheck: Data found in $coll")
                    return@withContext true
                }
            }
            
            KeepsyLogger.i("CloudCheck: No data found in any collection")
            false
        } catch (e: Exception) {
            KeepsyLogger.e("CloudCheck: Error during scanning", e)
            false
        }
    }

    private suspend fun uploadDirtyData() {
        KeepsyLogger.d("Uploading dirty data...")
        
        // Upload Spaces
        try {
            val dirtySpaces = appDao.getDirtySpaces()
            for (space in dirtySpaces) {
                var currentSpace = space
                val rId = space.remoteId ?: java.util.UUID.randomUUID().toString()
                
                // Upload image if exists locally but not on cloud
                if (space.photoPath != null && space.photoUrl == null) {
                    val file = File(space.photoPath)
                    if (file.exists()) {
                        try {
                            val url = firebaseService.uploadEntityImage(Uri.fromFile(file), "spaces", "space_${space.name}")
                            currentSpace = space.copy(photoUrl = url)
                        } catch (e: Exception) {
                            KeepsyLogger.e("Image upload failed for space ${space.name}", e)
                        }
                    }
                }
                
                firestoreService.uploadEntity("spaces", rId, currentSpace.toMap())
                
                // Save the synced state including any new photoUrl
                appDao.updateSpace(currentSpace.copy(
                    remoteId = rId,
                    syncState = "SYNCED",
                    lastSynced = System.currentTimeMillis()
                ))
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error uploading spaces", e)
        }

        // Upload Items
        try {
            val dirtyItems = appDao.getDirtyItems()
            for (item in dirtyItems) {
                var currentItem = item
                val rId = item.remoteId ?: java.util.UUID.randomUUID().toString()

                // Upload image if exists locally but not on cloud
                if (item.photoPath != null && item.photoUrl == null) {
                    val file = File(item.photoPath)
                    if (file.exists()) {
                        try {
                            val url = firebaseService.uploadEntityImage(Uri.fromFile(file), "items", "item_${item.name}")
                            currentItem = item.copy(photoUrl = url)
                        } catch (e: Exception) {
                            KeepsyLogger.e("Image upload failed for item ${item.name}", e)
                        }
                    }
                }

                firestoreService.uploadEntity("items", rId, currentItem.toMap())
                
                // Save the synced state including any new photoUrl
                appDao.updateItem(currentItem.copy(
                    remoteId = rId,
                    syncState = "SYNCED",
                    lastSynced = System.currentTimeMillis()
                ))
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error uploading items", e)
        }

        // Upload Categories
        try {
            val dirtyCategories = appDao.getDirtyCategories()
            for (category in dirtyCategories) {
                val rId = category.remoteId ?: java.util.UUID.randomUUID().toString()
                firestoreService.uploadEntity("categories", rId, category.toMap())
                appDao.markCategorySynced(category.categoryId, rId, "SYNCED", System.currentTimeMillis())
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error uploading categories", e)
        }

        // Upload Tags
        try {
            val dirtyTags = appDao.getDirtyTags()
            for (tag in dirtyTags) {
                val rId = tag.remoteId ?: java.util.UUID.randomUUID().toString()
                firestoreService.uploadEntity("tags", rId, tag.toMap())
                appDao.markTagSynced(tag.tagId, rId, "SYNCED", System.currentTimeMillis())
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error uploading tags", e)
        }

        // Upload Logs
        try {
            val dirtyLogs = appDao.getDirtyActivityLogs()
            for (log in dirtyLogs) {
                val rId = log.remoteId ?: java.util.UUID.randomUUID().toString()
                firestoreService.uploadEntity("activityLogs", rId, log.toMap())
                appDao.markActivityLogSynced(log.activityId, rId, "SYNCED", System.currentTimeMillis())
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error uploading activity logs", e)
        }
    }

    private suspend fun downloadAndMergeData() {
        KeepsyLogger.i("Downloading remote data (Pass 1: Upsert)...")
        
        // Spaces
        try {
            val remoteSpaces = firestoreService.getAllEntities("spaces")
            for (data in remoteSpaces) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toSpace(remoteId)
                val localEntity = appDao.getSpaceByRemoteId(remoteId)
                
                if (localEntity == null) {
                    appDao.insertSpace(remoteEntity.copy(parentSpaceId = null))
                } else if (remoteEntity.updatedAt > localEntity.updatedAt) {
                    // Update metadata but preserve local image path if cloud doesn't have a newer one
                    appDao.updateSpace(remoteEntity.copy(
                        spaceId = localEntity.spaceId,
                        parentSpaceId = localEntity.parentSpaceId,
                        photoPath = localEntity.photoPath 
                    ))
                } else if (localEntity.photoUrl == null && remoteEntity.photoUrl != null) {
                    // Specific case: local is newer but missing cloud link, pull link only
                    appDao.updateSpace(localEntity.copy(photoUrl = remoteEntity.photoUrl))
                }
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error downloading spaces", e)
        }

        // Items
        try {
            val remoteItems = firestoreService.getAllEntities("items")
            for (data in remoteItems) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toItem(remoteId)
                val localEntity = appDao.getItemByRemoteId(remoteId)
                
                if (localEntity == null) {
                    appDao.insertItem(remoteEntity.copy(spaceId = 0L, categoryId = 0L))
                } else if (remoteEntity.updatedAt > localEntity.updatedAt) {
                    appDao.updateItem(remoteEntity.copy(
                        itemId = localEntity.itemId,
                        spaceId = localEntity.spaceId,
                        categoryId = localEntity.categoryId,
                        photoPath = localEntity.photoPath
                    ))
                } else if (localEntity.photoUrl == null && remoteEntity.photoUrl != null) {
                    appDao.updateItem(localEntity.copy(photoUrl = remoteEntity.photoUrl))
                }
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error downloading items", e)
        }

        // Categories
        try {
            val remoteCategories = firestoreService.getAllEntities("categories")
            for (data in remoteCategories) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toCategory(remoteId)
                val localEntity = appDao.getCategoryByRemoteId(remoteId)
                
                if (localEntity == null) {
                    appDao.insertCategory(remoteEntity)
                } else if (remoteEntity.updatedAt > localEntity.updatedAt) {
                    appDao.updateCategory(remoteEntity.copy(categoryId = localEntity.categoryId))
                }
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error downloading categories", e)
        }

        // Tags
        try {
            val remoteTags = firestoreService.getAllEntities("tags")
            for (data in remoteTags) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toTag(remoteId)
                val localEntity = appDao.getTagByRemoteId(remoteId)
                
                if (localEntity == null) {
                    appDao.insertTag(remoteEntity)
                } else if (remoteEntity.updatedAt > localEntity.updatedAt) {
                    appDao.updateTag(remoteEntity.copy(tagId = localEntity.tagId))
                }
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error downloading tags", e)
        }

        // Activity Logs
        try {
            val remoteLogs = firestoreService.getAllEntities("activityLogs")
            for (data in remoteLogs) {
                val remoteId = data["remoteId"] as? String ?: continue
                val remoteEntity = data.toActivityLog(remoteId)
                val localEntity = appDao.getActivityLogByRemoteId(remoteId)
                
                if (localEntity == null) {
                    appDao.insertActivityLog(remoteEntity)
                } else if (remoteEntity.updatedAt > localEntity.updatedAt) {
                    appDao.updateActivityLog(remoteEntity.copy(activityId = localEntity.activityId))
                }
            }
        } catch (e: Exception) {
            KeepsyLogger.e("Error downloading activity logs", e)
        }
    }

    private suspend fun resolveRelationships() {
        KeepsyLogger.i("Resolving relationships (Pass 2)...")
        
        // Resolve Space parents
        val spacesList = (appDao.getDirtySpaces() + appDao.getLiveSpaces().first()).distinctBy { it.spaceId }
        for (space in spacesList) {
            if (space.parentRemoteId != null) {
                val parent = appDao.getSpaceByRemoteId(space.parentRemoteId)
                if (parent != null && space.parentSpaceId != parent.spaceId) {
                    appDao.updateSpace(space.copy(parentSpaceId = parent.spaceId))
                }
            } else if (space.parentSpaceId != null) {
                appDao.updateSpace(space.copy(parentSpaceId = null))
            }
        }

        // Resolve Item spaces and categories
        val itemsList = (appDao.getDirtyItems() + appDao.getLiveActiveItems().first() + appDao.getLiveTrashItems().first()).distinctBy { it.itemId }
        for (item in itemsList) {
            var updatedItem = item
            var changed = false
            
            if (item.spaceRemoteId != null) {
                val space = appDao.getSpaceByRemoteId(item.spaceRemoteId)
                if (space != null && item.spaceId != space.spaceId) {
                    updatedItem = updatedItem.copy(spaceId = space.spaceId)
                    changed = true
                }
            }
            
            if (item.categoryRemoteId != null) {
                val category = appDao.getCategoryByRemoteId(item.categoryRemoteId)
                if (category != null && item.categoryId != category.categoryId) {
                    updatedItem = updatedItem.copy(categoryId = category.categoryId)
                    changed = true
                }
            }
            
            if (changed) {
                appDao.updateItem(updatedItem)
            }
        }
    }
}
