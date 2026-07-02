package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.database.AppDao
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class KeepsyRepository(private val appDao: AppDao) {

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
                    appDao.insertCategory(category)
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
            // Fetch tags synchronously for each mapping is not optimal, but we can load tags for details,
            // or pass preloaded cross-refs. Since Room doesn't let us load tags synchronously in the flow mapper easily
            // unless we do a DB query, we will fetch tags when displaying item details or searching.
            // Let's also resolve tags in the search engine of the ViewModel or via helper repository queries!
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
        val id = appDao.insertSpace(space)
        appDao.insertActivityLog(
            ActivityLog(
                itemId = 0L,
                itemName = space.name,
                actionType = "CREATED",
                details = "Created new storage Space: ${space.name}"
            )
        )
        id
    }

    suspend fun updateSpace(space: Space) = withContext(Dispatchers.IO) {
        appDao.updateSpace(space)
    }

    suspend fun deleteSpace(spaceId: Long) = withContext(Dispatchers.IO) {
        val space = appDao.getSpaceById(spaceId)
        if (space != null) {
            // Un-nest any child spaces
            val childSpaces = appDao.getSubspaces(spaceId)
            for (child in childSpaces) {
                appDao.updateSpace(child.copy(parentSpaceId = null))
            }

            // Move any items in this deleted space to some other location or set space as null (if supported),
            // but since items must belong to a space, we can either reassign them to parent space or standard Home
            // or we flag them as unassigned. Let's find first available space or leave their spaceId unchanged if they delete it,
            // or reassign items of deleted space.
            val itemsInSpace = appDao.getItemsInSpace(spaceId)
            val spacesList = appDao.getLiveSpaces() // retrieve some placeholder if exists
            
            appDao.deleteSpaceById(spaceId)
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = 0L,
                    itemName = space.name,
                    actionType = "DELETED",
                    details = "Deleted storage Space: ${space.name}. Any nested subspaces were un-nested."
                )
            )
        }
    }

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
        val savedId = appDao.saveItemWithTags(item, tagNames)
        
        val space = appDao.getSpaceById(item.spaceId)
        val spaceText = if (space != null) "in ${space.name}" else ""
        
        val action = if (isNew) "CREATED" else "UPDATED"
        val desc = if (isNew) "Added physical item: ${item.name} $spaceText" else "Updated item metadata: ${item.name}"
        
        appDao.insertActivityLog(
            ActivityLog(
                itemId = savedId,
                itemName = item.name,
                actionType = action,
                details = desc
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
            updatedAt = System.currentTimeMillis()
        )
        appDao.updateItem(updatedItem)
        
        // Log movement for MEMORY TRAIL!
        val extraNotes = if (userNotes.isNotEmpty()) " Reason: $userNotes" else ""
        appDao.insertActivityLog(
            ActivityLog(
                itemId = itemId,
                itemName = item.name,
                actionType = "MOVED",
                details = "Moved from $oldName to $newName.$extraNotes"
            )
        )
    }

    suspend fun softDeleteItem(itemId: Long) = withContext(Dispatchers.IO) {
        val item = appDao.getItemById(itemId)
        if (item != null) {
            val updated = item.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis()
            )
            appDao.updateItem(updated)
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = itemId,
                    itemName = item.name,
                    actionType = "DELETED",
                    details = "Moved ${item.name} to Trash"
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
                updatedAt = System.currentTimeMillis()
            )
            appDao.updateItem(updated)
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = itemId,
                    itemName = item.name,
                    actionType = "RESTORED",
                    details = "Restored ${item.name} from Trash to its space"
                )
            )
        }
    }

    suspend fun permanentlyDeleteItem(itemId: Long) = withContext(Dispatchers.IO) {
        val item = appDao.getItemById(itemId)
        if (item != null) {
            // Delete image file if exists in Keepsy/Images directory to free up memory
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
            appDao.deleteCrossRefsForItem(itemId)
            appDao.deleteItemPermanently(itemId)
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = 0L, // Item doesn't exist anymore
                    itemName = item.name,
                    actionType = "PURGED",
                    details = "Permanently deleted ${item.name} and removed image files."
                )
            )
        }
    }

    suspend fun trackItemViewed(itemId: Long) = withContext(Dispatchers.IO) {
        val item = appDao.getItemById(itemId)
        if (item != null) {
            val updated = item.copy(lastViewed = System.currentTimeMillis())
            appDao.updateItem(updated)
            
            // Add a VIEWED activity log entry
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = itemId,
                    itemName = item.name,
                    actionType = "VIEWED",
                    details = "Viewed details of ${item.name}"
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
