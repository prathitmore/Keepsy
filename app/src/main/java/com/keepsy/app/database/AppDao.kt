package com.keepsy.app.database

import androidx.room.*
import com.keepsy.app.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- SPACES ---
    @Query("SELECT * FROM spaces WHERE isDeleted = 0 ORDER BY name ASC")
    fun getLiveSpaces(): Flow<List<Space>>

    @Query("SELECT * FROM spaces WHERE spaceId = :spaceId")
    suspend fun getSpaceById(spaceId: Long): Space?

    @Query("SELECT * FROM spaces WHERE remoteId = :remoteId")
    suspend fun getSpaceByRemoteId(remoteId: String): Space?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: Space): Long

    @Update
    suspend fun updateSpace(space: Space)

    @Query("DELETE FROM spaces WHERE spaceId = :spaceId")
    suspend fun deleteSpaceById(spaceId: Long)

    @Query("SELECT * FROM spaces WHERE parentSpaceId = :parentId AND isDeleted = 0 ORDER BY name ASC")
    suspend fun getSubspaces(parentId: Long): List<Space>

    @Query("SELECT * FROM spaces WHERE syncState != 'SYNCED'")
    suspend fun getDirtySpaces(): List<Space>


    // --- ITEMS ---
    @Query("SELECT * FROM items WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getLiveActiveItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getLiveTrashItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE itemId = :itemId")
    suspend fun getItemById(itemId: Long): Item?

    @Query("SELECT * FROM items WHERE remoteId = :remoteId")
    suspend fun getItemByRemoteId(remoteId: String): Item?

    @Query("SELECT * FROM items WHERE spaceId = :spaceId AND isDeleted = 0")
    suspend fun getItemsInSpace(spaceId: Long): List<Item>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Query("DELETE FROM items WHERE itemId = :itemId")
    suspend fun deleteItemPermanently(itemId: Long)

    @Query("SELECT * FROM items WHERE syncState != 'SYNCED'")
    suspend fun getDirtyItems(): List<Item>


    // --- CATEGORIES ---
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun getLiveCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?

    @Query("SELECT * FROM categories WHERE remoteId = :remoteId")
    suspend fun getCategoryByRemoteId(remoteId: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Query("DELETE FROM categories WHERE categoryId = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Query("SELECT COUNT(*) FROM categories WHERE isDeleted = 0")
    suspend fun getCategoryCount(): Int

    @Query("SELECT * FROM categories WHERE syncState != 'SYNCED'")
    suspend fun getDirtyCategories(): List<Category>


    // --- TAGS ---
    @Query("SELECT * FROM tags WHERE isDeleted = 0 ORDER BY name ASC")
    fun getLiveTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): Tag?

    @Query("SELECT * FROM tags WHERE remoteId = :remoteId")
    suspend fun getTagByRemoteId(remoteId: String): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTagCrossRef(crossRef: ItemTagCrossRef)

    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId")
    suspend fun deleteCrossRefsForItem(itemId: Long)

    @Query("""
        SELECT t.* FROM tags t 
        INNER JOIN item_tag_cross_ref r ON t.tagId = r.tagId 
        WHERE r.itemId = :itemId AND t.isDeleted = 0
    """)
    suspend fun getTagsForItem(itemId: Long): List<Tag>

    @Query("""
        SELECT t.* FROM tags t 
        INNER JOIN item_tag_cross_ref r ON t.tagId = r.tagId 
        WHERE r.itemId = :itemId AND t.isDeleted = 0
    """)
    fun getLiveTagsForItem(itemId: Long): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE syncState != 'SYNCED'")
    suspend fun getDirtyTags(): List<Tag>


    // --- ACTIVITY LOGS ---
    @Query("SELECT * FROM activity_logs WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getLiveActivityLogs(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE itemId = :itemId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getLiveActivityTrailForItem(itemId: Long): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog): Long

    @Update
    suspend fun updateActivityLog(log: ActivityLog)

    @Query("SELECT * FROM activity_logs WHERE remoteId = :remoteId")
    suspend fun getActivityLogByRemoteId(remoteId: String): ActivityLog?

    @Query("SELECT * FROM activity_logs WHERE syncState != 'SYNCED'")
    suspend fun getDirtyActivityLogs(): List<ActivityLog>


    // --- SYNC STATUS UPDATES ---
    @Transaction
    suspend fun updateSyncStatus(table: String, localId: Long, remoteId: String, syncState: String, lastSynced: Long) {
        when (table) {
            "spaces" -> {
                val entity = getSpaceById(localId)
                if (entity != null) updateSpace(entity.copy(remoteId = remoteId, syncState = syncState, lastSynced = lastSynced))
            }
            "items" -> {
                val entity = getItemById(localId)
                if (entity != null) updateItem(entity.copy(remoteId = remoteId, syncState = syncState, lastSynced = lastSynced))
            }
            "categories" -> {
                val entity = getCategoryById(localId)
                if (entity != null) updateCategory(entity.copy(remoteId = remoteId, syncState = syncState, lastSynced = lastSynced))
            }
            "tags" -> {
                val entity = getTagByRemoteId(remoteId) // Tags might not have localId yet if from cloud, but here we assume local exists
                // Re-fetch by ID for accuracy if possible
                // Let's use a more direct approach for tags
            }
        }
    }
    
    // Direct ID based updates are safer
    @Query("UPDATE spaces SET remoteId = :remoteId, syncState = :state, lastSynced = :ts WHERE spaceId = :id")
    suspend fun markSpaceSynced(id: Long, remoteId: String, state: String, ts: Long)

    @Query("UPDATE items SET remoteId = :remoteId, syncState = :state, lastSynced = :ts WHERE itemId = :id")
    suspend fun markItemSynced(id: Long, remoteId: String, state: String, ts: Long)

    @Query("UPDATE categories SET remoteId = :remoteId, syncState = :state, lastSynced = :ts WHERE categoryId = :id")
    suspend fun markCategorySynced(id: Long, remoteId: String, state: String, ts: Long)

    @Query("UPDATE tags SET remoteId = :remoteId, syncState = :state, lastSynced = :ts WHERE tagId = :id")
    suspend fun markTagSynced(id: Long, remoteId: String, state: String, ts: Long)

    @Query("UPDATE activity_logs SET remoteId = :remoteId, syncState = :state, lastSynced = :ts WHERE activityId = :id")
    suspend fun markActivityLogSynced(id: Long, remoteId: String, state: String, ts: Long)


    // --- TRANSACTIONS FOR ATOMIC ITEM SAVING ---
    @Transaction
    suspend fun saveItemWithTags(item: Item, tagNames: List<String>): Long {
        val itemId = insertItem(item)
        val finalItemId = if (item.itemId == 0L) itemId else item.itemId

        // Clear existing tags
        deleteCrossRefsForItem(finalItemId)

        // Insert new tags & map relations
        for (name in tagNames) {
            val lowercaseName = name.trim().lowercase()
            if (lowercaseName.isNotEmpty()) {
                var tag = getTagByName(lowercaseName)
                val tagId = if (tag == null) {
                    val newTagId = insertTag(Tag(name = lowercaseName))
                    newTagId
                } else {
                    tag.tagId
                }
                insertItemTagCrossRef(ItemTagCrossRef(itemId = finalItemId, tagId = tagId))
            }
        }
        return finalItemId
    }
}
