package com.keepsy.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

import androidx.core.content.FileProvider
import com.keepsy.app.BuildConfig

object ImageUtils {
    fun createTempImageUri(context: Context): Uri {
        val tempFile = File(context.cacheDir, "camera_capture.jpg")
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            tempFile
        )
    }

    fun compressImage(context: Context, uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val outputStream = ByteArrayOutputStream()
            // Compress to 70% quality
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            
            val bytes = outputStream.toByteArray()
            val compressedFile = File(context.cacheDir, "compressed_profile.jpg")
            val fos = FileOutputStream(compressedFile)
            fos.write(bytes)
            fos.close()
            
            Uri.fromFile(compressedFile)
        } catch (e: Exception) {
            KeepsyLogger.e("Image compression failed", e)
            null
        }
    }
}
