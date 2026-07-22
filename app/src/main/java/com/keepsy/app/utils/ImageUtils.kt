package com.keepsy.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import com.keepsy.app.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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
        var bitmap: Bitmap? = null
        return try {
            // 1. Get Orientation from EXIF using system ExifInterface
            var orientation = 1 
            val exifInput = context.contentResolver.openInputStream(uri)
            if (exifInput != null) {
                try {
                    val exif = android.media.ExifInterface(exifInput)
                    orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
                } catch (e: Exception) {
                    KeepsyLogger.w("EXIF read error: " + e.message)
                } finally {
                    try { exifInput.close() } catch (e: Exception) {}
                }
            }

            // 2. Decode Bitmap dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val decodeInput1 = context.contentResolver.openInputStream(uri)
            if (decodeInput1 != null) {
                BitmapFactory.decodeStream(decodeInput1, null, options)
                decodeInput1.close()
            }

            // Downsample if needed (max 1200px)
            var inSampleSize = 1
            val maxDim = 1200
            if (options.outHeight > maxDim || options.outWidth > maxDim) {
                val hh = options.outHeight / 2
                val hw = options.outWidth / 2
                while (hh / inSampleSize >= maxDim && hw / inSampleSize >= maxDim) { inSampleSize *= 2 }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize

            // 3. Decode actual Bitmap
            val decodeInput2 = context.contentResolver.openInputStream(uri)
            if (decodeInput2 != null) {
                bitmap = BitmapFactory.decodeStream(decodeInput2, null, options)
                decodeInput2.close()
            }
            
            if (bitmap == null) return null

            // 4. Rotate Bitmap based on EXIF
            val matrix = Matrix()
            when (orientation) {
                6 -> matrix.postRotate(90f)
                3 -> matrix.postRotate(180f)
                8 -> matrix.postRotate(270f)
            }

            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }

            // 5. Compress and Save to temp file
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            val bytes = out.toByteArray()
            
            val file = File(context.cacheDir, "compressed_profile.jpg")
            val fos = FileOutputStream(file)
            fos.write(bytes)
            fos.close()
            
            bitmap.recycle()
            Uri.fromFile(file)
        } catch (e: Exception) {
            KeepsyLogger.e("Image rotation correction failed", e)
            bitmap?.recycle()
            null
        }
    }
}
