package com.oneplus.exifpatcher.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utility class for EXIF metadata manipulation
 * Applies OnePlus-specific EXIF patches to images
 */
object ExifPatcher {
    
    // OnePlus-specific EXIF values
    private const val DEVICE_MODEL = "0xcdcc8c3fff"
    private const val USER_COMMENT = "oplus_1048864"
    private const val MAKE = "OnePlus"
    
    /**
     * Process an image by applying OnePlus EXIF patches
     * 
     * @param context Application context
     * @param sourceUri URI of the source image
     * @param destinationFile Destination file where the patched image will be saved
     * @return true if processing was successful, false otherwise
     */
    fun patchImage(
        context: Context,
        sourceUri: Uri,
        destinationFile: File
    ): Boolean {
        return try {
            // Copy the source file to destination
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Apply EXIF patches to the copied file
            val exif = ExifInterface(destinationFile.absolutePath)
            
            // Set OnePlus-specific EXIF tags
            exif.setAttribute(ExifInterface.TAG_MODEL, DEVICE_MODEL)
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, USER_COMMENT)
            exif.setAttribute(ExifInterface.TAG_MAKE, MAKE)
            
            // Save the modified EXIF data
            exif.saveAttributes()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Clean up the destination file if processing failed
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            false
        }
    }
    
    /**
     * Batch process multiple images
     * 
     * @param context Application context
     * @param sourceUris List of source image URIs
     * @param destinationDir Destination directory for patched images
     * @param onProgress Callback for progress updates (current index, total count)
     * @return Number of successfully processed images
     */
    suspend fun patchImages(
        context: Context,
        sourceUris: List<Uri>,
        destinationDir: File,
        onProgress: (Int, Int) -> Unit
    ): Int {
        var successCount = 0
        
        sourceUris.forEachIndexed { index, uri ->
            onProgress(index, sourceUris.size)
            
            // Generate unique filename
            val fileName = "patched_${System.currentTimeMillis()}_$index.jpg"
            val destinationFile = File(destinationDir, fileName)
            
            if (patchImage(context, uri, destinationFile)) {
                successCount++
            }
        }
        
        onProgress(sourceUris.size, sourceUris.size)
        return successCount
    }
}
