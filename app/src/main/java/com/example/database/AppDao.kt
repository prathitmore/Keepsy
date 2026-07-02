package com.example.database

import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- SPACES ---
    @Query("SELECT * FROM spaces ORDER BY name ASC")
    fun getLiveSpaces(): Flow<List<Space>>

    @Query("SELECT * FROM spaces WHERE spaceId = :spaceId")
    suspend fun getSpaceById(spaceId: Long): Space?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: Space): Long

    @Update
    suspend fun updateSpace(space: Space)

    @Query("DELETE FROM spaces WHERE spaceId = :spaceId")
    suspend fun deleteSpaceById(spaceId: Long)

    @Query("SELECT * FROM spaces WHERE parentSpaceId = :parentId ORDER BY name ASC")
    suspend fun getSubspaces(parentId: Long): List<Space>


    // --- ITEMS ---
    @Query("SELECT * FROM items WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getLiveActiveItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getLiveTrashItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE itemId = :itemId")
    suspend fun getItemById(itemId: Long): Item?

    @Query("SELECT * FROM items WHERE spaceId = :spaceId AND isDeleted = 0")
    suspend fun getItemsInSpace(spaceId: Long): List<Item>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Query("DELETE FROM items WHERE itemId = :itemId")
    suspend fun deleteItemPermanently(itemId: Long)


    // --- CATEGORIES ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getLiveCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE categoryId = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Query("DELETE FROM categories WHERE categoryId = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int


    // --- TAGS ---
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getLiveTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTagCrossRef(crossRef: ItemTagCrossRef)

    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId")
    suspend fun deleteCrossRefsForItem(itemId: Long)

    @Query("""
        SELECT t.* FROM tags t 
        INNER JOIN item_tag_cross_ref r ON t.tagId = r.tagId 
        WHERE r.itemId = :itemId
    """)
    suspend fun getTagsForItem(itemId: Long): List<Tag>

    @Query("""
        SELECT t.* FROM tags t 
        INNER JOIN item_tag_cross_ref r ON t.tagId = r.tagId 
        WHERE r.itemId = :itemId
    """)
    fun getLiveTagsForItem(itemId: Long): Flow<List<Tag>>


    // --- ACTIVITY LOGS ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getLiveActivityLogs(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getLiveActivityTrailForItem(itemId: Long): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog): Long


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
