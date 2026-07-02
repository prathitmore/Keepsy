package com.example.repository

import android.content.Context
import android.util.Base64
import com.example.database.AppDao
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BackupManager(
    private val context: Context,
    private val appDao: AppDao
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    class BackupContainer(
        val version: Int = 1,
        val timestamp: Long = System.currentTimeMillis(),
        val spaces: List<Space>,
        val items: List<Item>,
        val activityLogs: List<ActivityLog>,
        val images: List<ImageItem>
    )

    class ImageItem(
        val fileName: String,
        val base64Content: String
    )

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        // Fetch all current records from database reactive flows
        val spaces = appDao.getLiveSpaces().first()
        val activeItems = appDao.getLiveActiveItems().first()
        val trashItems = appDao.getLiveTrashItems().first()
        val allItems = activeItems + trashItems
        val logs = appDao.getLiveActivityLogs().first()

        // Gather all existing images
        val imagesList = mutableListOf<ImageItem>()
        val imagesDir = File(context.filesDir, "keepsy/images")
        
        if (imagesDir.exists()) {
            imagesDir.listFiles()?.forEach { file ->
                try {
                    val bytes = file.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    imagesList.add(ImageItem(file.name, base64))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val container = BackupContainer(
            spaces = spaces,
            items = allItems,
            activityLogs = logs,
            images = imagesList
        )

        val adapter = moshi.adapter(BackupContainer::class.java)
        adapter.toJson(container)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(BackupContainer::class.java)
            val container = adapter.fromJson(jsonString) ?: return@withContext false

            // Target directory for restored images
            val imagesDir = File(context.filesDir, "keepsy/images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            // Restore Images
            for (imageItem in container.images) {
                try {
                    val targetFile = File(imagesDir, imageItem.fileName)
                    val bytes = Base64.decode(imageItem.base64Content, Base64.NO_WRAP)
                    FileOutputStream(targetFile).use { fos ->
                        fos.write(bytes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Clean existing data
            // We wipe the DB clean of existing records to avoid conflicts and map restored elements properly
            val currentActive = appDao.getLiveActiveItems().first()
            val currentTrash = appDao.getLiveTrashItems().first()
            (currentActive + currentTrash).forEach {
                appDao.deleteCrossRefsForItem(it.itemId)
                appDao.deleteItemPermanently(it.itemId)
            }

            val currentSpaces = appDao.getLiveSpaces().first()
            currentSpaces.forEach {
                appDao.deleteSpaceById(it.spaceId)
            }

            // Map and Restore Spaces (Re-inserting them under their original IDs)
            for (space in container.spaces) {
                appDao.insertSpace(space)
            }

            // Restore Items (Correcting photo absolute paths to point to current device directories!)
            for (item in container.items) {
                val correctedPhotoPath = if (item.photoPath != null) {
                    val originalFileName = File(item.photoPath).name
                    val correctedFile = File(imagesDir, originalFileName)
                    correctedFile.absolutePath
                } else {
                    null
                }

                val restoredItem = item.copy(photoPath = correctedPhotoPath)
                appDao.insertItem(restoredItem)
            }

            // Restore Activity Logs
            for (log in container.activityLogs) {
                appDao.insertActivityLog(log)
            }

            // Log a restore action
            appDao.insertActivityLog(
                ActivityLog(
                    itemId = 0L,
                    itemName = "System",
                    actionType = "RESTORED",
                    details = "Restored full backup: ${container.spaces.size} Spaces and ${container.items.size} Items imported."
                )
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
