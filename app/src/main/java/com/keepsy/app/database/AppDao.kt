package com.keepsy.app.database

import androidx.room.*
import com.keepsy.app.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface AppDao {
    // --- SPACES ---
    @Query("SELECT * FROM spaces WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getLiveSpaces(): Flow<List<Space>>

    @Query("SELECT * FROM spaces WHERE spaceId = :id")
    suspend fun getSpaceById(id: Long): Space?

    @Query("SELECT * FROM spaces WHERE remoteId = :remoteId")
    suspend fun getSpaceByRemoteId(remoteId: String): Space?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: Space): Long

    @Update
    suspend fun updateSpace(space: Space)

    @Query("DELETE FROM spaces WHERE spaceId = :id")
    suspend fun deleteSpaceById(id: Long)

    @Query("SELECT * FROM spaces WHERE parentSpaceId = :parentId AND isDeleted = 0")
    suspend fun getSubspaces(parentId: Long): List<Space>

    @Query("SELECT * FROM spaces WHERE syncState != 'SYNCED'")
    suspend fun getDirtySpaces(): List<Space>

    @Query("SELECT COUNT(*) FROM spaces WHERE isDeleted = 0")
    suspend fun getSpaceCount(): Int

    // --- ITEMS ---
    @Query("SELECT * FROM items WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getLiveActiveItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getLiveTrashItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE itemId = :id")
    suspend fun getItemById(id: Long): Item?

    @Query("SELECT * FROM items WHERE remoteId = :remoteId")
    suspend fun getItemByRemoteId(remoteId: String): Item?

    @Query("SELECT * FROM items WHERE spaceId = :spaceId AND isDeleted = 0")
    suspend fun getItemsInSpace(spaceId: Long): List<Item>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Query("DELETE FROM items WHERE itemId = :id")
    suspend fun deleteItemPermanently(id: Long)

    @Query("SELECT * FROM items WHERE syncState != 'SYNCED'")
    suspend fun getDirtyItems(): List<Item>

    @Query("SELECT COUNT(*) FROM items WHERE isDeleted = 0")
    suspend fun getItemCount(): Int

    // --- CATEGORIES ---
    @Query("SELECT * FROM categories WHERE isDeleted = 0")
    fun getLiveCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE categoryId = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE remoteId = :remoteId")
    suspend fun getCategoryByRemoteId(remoteId: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Query("DELETE FROM categories WHERE categoryId = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("SELECT COUNT(*) FROM categories WHERE isDeleted = 0")
    suspend fun getCategoryCount(): Int

    @Query("SELECT * FROM categories WHERE syncState != 'SYNCED'")
    suspend fun getDirtyCategories(): List<Category>

    // --- TAGS ---
    @Query("SELECT * FROM tags WHERE isDeleted = 0")
    fun getLiveTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): Tag?

    @Query("SELECT * FROM tags WHERE remoteId = :remoteId")
    suspend fun getTagByRemoteId(remoteId: String): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemTagCrossRef(crossRef: ItemTagCrossRef)

    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId")
    suspend fun deleteCrossRefsForItem(itemId: Long)

    @Query("""
        SELECT tags.* FROM tags 
        INNER JOIN item_tag_cross_ref ON tags.tagId = item_tag_cross_ref.tagId 
        WHERE item_tag_cross_ref.itemId = :itemId
    """)
    suspend fun getTagsForItem(itemId: Long): List<Tag>

    @Query("""
        SELECT tags.* FROM tags 
        INNER JOIN item_tag_cross_ref ON tags.tagId = item_tag_cross_ref.tagId 
        WHERE item_tag_cross_ref.itemId = :itemId
    """)
    fun getLiveTagsForItem(itemId: Long): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE syncState != 'SYNCED'")
    suspend fun getDirtyTags(): List<Tag>

    // --- ACTIVITY LOGS ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getLiveActivityLogs(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getLiveActivityTrailForItem(itemId: Long): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog): Long

    @Update
    suspend fun updateActivityLog(log: ActivityLog)

    @Query("SELECT * FROM activity_logs WHERE remoteId = :remoteId")
    suspend fun getActivityLogByRemoteId(remoteId: String): ActivityLog?

    @Query("SELECT * FROM activity_logs WHERE syncState != 'SYNCED'")
    suspend fun getDirtyActivityLogs(): List<ActivityLog>

    @Transaction
    suspend fun markSpaceSynced(localId: Long, remoteId: String, syncState: String, lastSynced: Long) {
        val entity = getSpaceById(localId)
        if (entity != null) updateSpace(entity.copy(remoteId = remoteId, syncState = syncState, lastSynced = lastSynced))
    }

    @Transaction
    suspend fun markItemSynced(localId: Long, remoteId: String, syncState: String, lastSynced: Long) {
        val entity = getItemById(localId)
        if (entity != null) updateItem(entity.copy(remoteId = remoteId, syncState = syncState, lastSynced = lastSynced))
    }

    @Transaction
    suspend fun markCategorySynced(localId: Long, remoteId: String, syncState: String, lastSynced: Long) {
        val entity = getCategoryById(localId)
        if (entity != null) updateCategory(entity.copy(remoteId = remoteId, syncState = syncState, lastSynced = lastSynced))
    }

    @Transaction
    suspend fun markTagSynced(localId: Long, remoteId: String, syncState: String, lastSynced: Long) {
        val entity = appDaoGetTagById(localId)
        if (entity != null) updateTag(entity.copy(remoteId = remoteId, syncState = syncState, lastSynced = lastSynced))
    }

    @Query("SELECT * FROM tags WHERE tagId = :id")
    suspend fun appDaoGetTagById(id: Long): Tag?

    @Transaction
    suspend fun markActivityLogSynced(localId: Long, remoteId: String, syncState: String, lastSynced: Long) {
        val entity = getActivityLogByRemoteId(remoteId)
        if (entity != null) updateActivityLog(entity.copy(syncState = syncState, lastSynced = lastSynced))
    }

    @Transaction
    suspend fun saveItemWithTags(item: Item, tags: List<String>): Long {
        val itemId = if (item.itemId == 0L) insertItem(item) else { updateItem(item); item.itemId }
        deleteCrossRefsForItem(itemId)
        for (tagName in tags) {
            val tag = getTagByName(tagName) ?: Tag(tagId = 0, name = tagName, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), version = 1, syncState = "DIRTY", isDeleted = false, lastSynced = null, remoteId = null).let { t -> 
                val id = insertTag(t); t.copy(tagId = id) 
            }
            insertItemTagCrossRef(ItemTagCrossRef(itemId, tag.tagId))
        }
        return itemId
    }
}
