package com.example.eat.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

object ImageUtils {
    // LRU Cache for thumbnails (max 50 images, ~10MB if each is 200KB)
    private val thumbnailCache = object : android.util.LruCache<String, Bitmap>(50) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return 1 // Count by number of images
        }
    }
    
    fun loadRotatedBitmap(path: String, maxSize: Int? = null): Bitmap? {
        // Check cache first if loading thumbnail
        if (maxSize != null) {
            val cacheKey = "$path-$maxSize"
            thumbnailCache.get(cacheKey)?.let { return it }
        }
        
        val options = BitmapFactory.Options()
        
        // If maxSize is specified, calculate sample size for downsampling
        if (maxSize != null) {
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight
            var sampleSize = 1
            
            while (imageWidth / sampleSize > maxSize || imageHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }
            
            options.inSampleSize = sampleSize
            options.inJustDecodeBounds = false
        }
        
        val originalBitmap = BitmapFactory.decodeFile(path, options) ?: return null
        
        return try {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val rotatedBitmap = if (rotationDegrees != 0f) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees)
                Bitmap.createBitmap(
                    originalBitmap,
                    0,
                    0,
                    originalBitmap.width,
                    originalBitmap.height,
                    matrix,
                    true
                )
            } else {
                originalBitmap
            }
            
            // Cache thumbnail if maxSize was specified
            if (maxSize != null) {
                val cacheKey = "$path-$maxSize"
                thumbnailCache.put(cacheKey, rotatedBitmap)
            }
            
            rotatedBitmap
        } catch (e: Exception) {
            originalBitmap
        }
    }
}
