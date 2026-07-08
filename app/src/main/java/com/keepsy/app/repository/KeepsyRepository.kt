package com.keepsy.app.repository

import android.content.Context
import android.net.Uri
import com.keepsy.app.database.AppDao
import com.keepsy.app.model.*
import com.keepsy.app.service.FirebaseService
import com.keepsy.app.utils.ActionTypes
import com.keepsy.app.utils.KeepsyLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream

class KeepsyRepository(
    private val appDao: AppDao,
    private val firebaseService: FirebaseService
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        firebaseService.addAuthStateListener { user ->
            if (user != null) {
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun refreshAuthState() {
        val user = firebaseService.getCurrentUser()
        if (user != null) {
            _authState.value = AuthState.Authenticated(user)
        }
    }

    suspend fun reloadUserVerification(): Boolean = withContext(Dispatchers.IO) {
        val isVerified = firebaseService.reloadUser()
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser)
        }
        isVerified
    }

    fun isEmailVerified(): Boolean {
        return firebaseService.isEmailVerified()
    }

    suspend fun sendEmailVerification() = withContext(Dispatchers.IO) {
        firebaseService.sendEmailVerification()
    }

    suspend fun signInWithEmail(email: String, password: String) = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            firebaseService.signInWithEmail(email, password)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Sign in failed")
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String) = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            firebaseService.signUpWithEmail(email, password, name)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Sign up failed")
        }
    }

    suspend fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential) = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            firebaseService.signInWithCredential(credential)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Google sign in failed")
        }
    }

    suspend fun signOut() {
        firebaseService.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    suspend fun sendPasswordResetEmail(email: String) = withContext(Dispatchers.IO) {
        firebaseService.sendPasswordResetEmail(email)
    }

    // Seed data
    private val defaultCategories = listOf(
        Category(name = "Documents", icon = "description", color = "#4F46E5"), // Royal Blue / Indigo
        Category(name = "Electronics", icon = "devices", color = "#0EA5E9"), // Sky Blue
        Category(name = "Keys", icon = "key", color = "#F59E0B"), // Amber
        Category(name = "Medicines", icon = "medical_services", color = "#10B981"), // Emerald Green
        Category(name = "Valuables", icon = "diamond", color = "#EC4899"), // Violet Pink
        Category(name = "Tools", icon = "build", color = "#64748B"), // Slate Grey
        Category(name = "Storage", icon = "inventory_2", color = "#8B5CF6"), // Purple
        Category(name = "Home", icon = "home", color = "#EF4444"), // Red
        Category(name = "Other", icon = "more_horiz", color = "#6B7280") // Slate
    )

    suspend fun seedDefaultCategoriesIfEmpty() {
        withContext(Dispatchers.IO) {
            val count = appDao.getCategoryCount()
            if (count == 0) {
                for (category in defaultCategories) {
                    appDao.insertCategory(category.copy(remoteId = java.util.UUID.randomUUID().toString()))
                }
            }
        }
    }

    // --- LIVE FLOWS COMBINED FOR EASY UI DISPLAY ---
    val spaces: Flow<List<Space>> = appDao.getLiveSpaces()

    val categories: Flow<List<Category>> = appDao.getLiveCategories()

    val tags: Flow<List<Tag>> = appDao.getLiveTags()

    val activityLogs: Flow<List<ActivityLog>> = appDao.getLiveActivityLogs()

    val activeItemsWithDetails: Flow<List<ItemWithDetails>> = combine(
        appDao.getLiveActiveItems(),
        appDao.getLiveSpaces(),
        appDao.getLiveCategories()
    ) { items, spacesList, categoriesList ->
        items.map { item ->
            val space = spacesList.find { it.spaceId == item.spaceId }
            val category = categoriesList.find { it.categoryId == item.categoryId }
            ItemWithDetails(item, space, category, emptyList())
        }
    }.flowOn(Dispatchers.IO)

    val trashItemsWithDetails: Flow<List<ItemWithDetails>> = combine(
        appDao.getLiveTrashItems(),
        appDao.getLiveSpaces(),
        appDao.getLiveCategories()
    ) { items, spacesList, categoriesList ->
        items.map { item ->
            val space = spacesList.find { it.spaceId == item.spaceId }
            val category = categoriesList.find { it.categoryId == item.categoryId }
            ItemWithDetails(item, space, category, emptyList())
        }
    }.flowOn(Dispatchers.IO)

    // --- CRUD OPERATIONS ---

    suspend fun getSpaceById(spaceId: Long): Space? = withContext(Dispatchers.IO) {
        appDao.getSpaceById(spaceId)
    }

    suspend fun insertSpace(space: Space): Long = withContext(Dispatchers.IO) {
        // Resolve parent remote ID
        val parent = space.parentSpaceId?.let { appDao.getSpaceById(it) }
        val finalSpace = space.copy(
            syncState = "DIRTY", 
            updatedAt = System.currentTimeMillis(),
            remoteId = space.remoteId ?: java.util.UUID.randomUUID().toString(),
            parentRemoteId = parent?.remoteId
        )
        val id = appDao.insertSpace(finalSpace)
        appDao.insertActivityLog(
            ActivityLog(
                itemId = 0L,
                itemName = space.name,
                actionType = ActionTypes.CREATED,
                details = "Created new storage Space: ${space.name}",
                syncState = "DIRTY",
                remoteId = java.util.UUID.randomUUID().toString()
            )
        )
        id
    }

    suspend fun updateSpace(space: Space) = withContext(Dispatchers.IO) {
        val parent = space.parentSpaceId?.let { appDao.getSpaceById(it) }
        appDao.updateSpace(space.copy(
            syncState = "DIRTY", 
            updatedAt = System.currentTimeMillis(),
            parentRemoteId = parent?.remoteId
        ))
    }

    suspend fun deleteSpace(spaceId: Long) = withContext(Dispatchers.IO) {
        val space = appDao.getSpaceById(spaceId)
        if (space != null) {
            // Un-nest any child spaces
            val childSpaces = appDao.getSubspaces(spaceId)
            for (child in childSpaces) {
                appDao.updateSpace(child.copy(parentSpaceId = null, parentRemoteId = null, syncState = "DIRTY", updatedAt = System.currentTimeMillis()))
            }
            
            // Mark for deletion in sync
            appDao.updateSpace(space.copy(isDeleted = true, syncState = "DIRTY", updatedAt = System.currentTimeMillis()))
            
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = 0L,
                    itemName = space.name,
                    actionType = ActionTypes.DELETED,
                    details = "Deleted storage Space: ${space.name}. Any nested subspaces were un-nested.",
                    syncState = "DIRTY",
                    remoteId = java.util.UUID.randomUUID().toString()
                )
            )
        }
    }

    @Suppress("unused")
    suspend fun getTagsForItem(itemId: Long): List<Tag> = withContext(Dispatchers.IO) {
        appDao.getTagsForItem(itemId)
    }

    suspend fun getItemById(itemId: Long): Item? = withContext(Dispatchers.IO) {
        appDao.getItemById(itemId)
    }

    suspend fun getItemWithDetails(itemId: Long): ItemWithDetails? = withContext(Dispatchers.IO) {
        val item = appDao.getItemById(itemId) ?: return@withContext null
        val space = appDao.getSpaceById(item.spaceId)
        val category = appDao.getCategoryById(item.categoryId)
        val tagsList = appDao.getTagsForItem(item.itemId)
        ItemWithDetails(item, space, category, tagsList)
    }

    suspend fun saveItem(item: Item, tagNames: List<String>): Long = withContext(Dispatchers.IO) {
        val isNew = item.itemId == 0L
        val space = appDao.getSpaceById(item.spaceId)
        val category = appDao.getCategoryById(item.categoryId)
        
        val itemToSave = item.copy(
            syncState = "DIRTY", 
            updatedAt = System.currentTimeMillis(),
            remoteId = item.remoteId ?: java.util.UUID.randomUUID().toString(),
            spaceRemoteId = space?.remoteId,
            categoryRemoteId = category?.remoteId
        )
        val savedId = appDao.saveItemWithTags(itemToSave, tagNames)
        
        val spaceText = if (space != null) "in ${space.name}" else ""
        
        val action = if (isNew) ActionTypes.CREATED else ActionTypes.UPDATED
        val desc = if (isNew) "Added physical item: ${item.name} $spaceText" else "Updated item metadata: ${item.name}"
        
        appDao.insertActivityLog(
            ActivityLog(
                itemId = savedId,
                itemName = item.name,
                actionType = action,
                details = desc,
                syncState = "DIRTY",
                remoteId = java.util.UUID.randomUUID().toString()
            )
        )
        savedId
    }

    suspend fun moveItem(itemId: Long, newSpaceId: Long, userNotes: String = "") = withContext(Dispatchers.IO) {
        val item = appDao.getItemById(itemId) ?: return@withContext
        val oldSpace = appDao.getSpaceById(item.spaceId)
        val newSpace = appDao.getSpaceById(newSpaceId) ?: return@withContext
        
        val oldName = oldSpace?.name ?: "Unknown Space"
        val newName = newSpace.name
        
        // Update item location
        val updatedItem = item.copy(
            spaceId = newSpaceId,
            spaceRemoteId = newSpace.remoteId,
            updatedAt = System.currentTimeMillis(),
            syncState = "DIRTY"
        )
        appDao.updateItem(updatedItem)
        
        // Log movement
        val extraNotes = if (userNotes.isNotEmpty()) " Reason: $userNotes" else ""
        appDao.insertActivityLog(
            ActivityLog(
                itemId = itemId,
                itemName = item.name,
                actionType = ActionTypes.MOVED,
                details = "Moved from $oldName to $newName.$extraNotes",
                syncState = "DIRTY",
                remoteId = java.util.UUID.randomUUID().toString()
            )
        )
    }

    suspend fun softDeleteItem(itemId: Long) = withContext(Dispatchers.IO) {
        val item = appDao.getItemById(itemId)
        if (item != null) {
            val updated = item.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncState = "DIRTY"
            )
            appDao.updateItem(updated)
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = itemId,
                    itemName = item.name,
                    actionType = ActionTypes.DELETED,
                    details = "Moved ${item.name} to Trash",
                    syncState = "DIRTY",
                    remoteId = java.util.UUID.randomUUID().toString()
                )
            )
        }
    }

    suspend fun restoreItem(itemId: Long) = withContext(Dispatchers.IO) {
        val item = appDao.getItemById(itemId)
        if (item != null) {
            val updated = item.copy(
                isDeleted = false,
                deletedAt = null,
                updatedAt = System.currentTimeMillis(),
                syncState = "DIRTY"
            )
            appDao.updateItem(updated)
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = itemId,
                    itemName = item.name,
                    actionType = ActionTypes.RESTORED,
                    details = "Restored ${item.name} from Trash to its space",
                    syncState = "DIRTY",
                    remoteId = java.util.UUID.randomUUID().toString()
                )
            )
        }
    }

    suspend fun permanentlyDeleteItem(itemId: Long) = withContext(Dispatchers.IO) {
        val item = appDao.getItemById(itemId)
        if (item != null) {
            // Delete image file
            item.photoPath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists() && file.absolutePath.contains("keepsy/images")) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Mark as deleted in sync
            appDao.updateItem(item.copy(isDeleted = true, syncState = "DIRTY", updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun trackItemViewed(itemId: Long) = withContext(Dispatchers.IO) {
        val item = appDao.getItemById(itemId)
        if (item != null) {
            val updated = item.copy(lastViewed = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), syncState = "DIRTY")
            appDao.updateItem(updated)
            
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = itemId,
                    itemName = item.name,
                    actionType = ActionTypes.VIEWED,
                    details = "Viewed details of ${item.name}",
                    syncState = "DIRTY",
                    remoteId = java.util.UUID.randomUUID().toString()
                )
            )
        }
    }

    fun getActivityTrailForItem(itemId: Long): Flow<List<ActivityLog>> {
        return appDao.getLiveActivityTrailForItem(itemId)
    }

    // --- LOCAL IMAGE SAVE UTILITY ---
    suspend fun copyImageToAppStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val imagesDir = File(context.filesDir, "keepsy/images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                
                val targetFile = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                targetFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
