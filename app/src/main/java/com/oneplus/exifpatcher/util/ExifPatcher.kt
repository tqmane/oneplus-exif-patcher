package com.oneplus.exifpatcher.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

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
     * @param destinationFile DocumentFile where the patched image will be saved
     * @param fileName Name for the destination file
     * @return true if processing was successful, false otherwise
     */
    fun patchImage(
        context: Context,
        sourceUri: Uri,
        destinationFile: DocumentFile,
        fileName: String
    ): Boolean {
        return try {
            // Create a new file in the destination directory
            val newFile = destinationFile.createFile("image/jpeg", fileName)
                ?: return false
            
            // Use cache directory for temporary file to apply EXIF changes
            val tempFile = File(context.cacheDir, "temp_$fileName")
            
            // Copy source to temp file
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Apply EXIF patches to the temp file
            val exif = ExifInterface(tempFile.absolutePath)
            
            // Set OnePlus-specific EXIF tags
            exif.setAttribute(ExifInterface.TAG_MODEL, DEVICE_MODEL)
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, USER_COMMENT)
            exif.setAttribute(ExifInterface.TAG_MAKE, MAKE)
            
            // Save the modified EXIF data
            exif.saveAttributes()
            
            // Copy the patched file to the destination
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Clean up temp file
            tempFile.delete()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Batch process multiple images
     * 
     * @param context Application context
     * @param sourceUris List of source image URIs
     * @param destinationDir Destination directory (as DocumentFile) for patched images
     * @param onProgress Callback for progress updates (current index, total count)
     * @return Number of successfully processed images
     */
    suspend fun patchImages(
        context: Context,
        sourceUris: List<Uri>,
        destinationDir: DocumentFile,
        onProgress: (Int, Int) -> Unit
    ): Int {
        var successCount = 0
        
        sourceUris.forEachIndexed { index, uri ->
            onProgress(index, sourceUris.size)
            
            // Generate unique filename
            val fileName = "patched_${System.currentTimeMillis()}_$index.jpg"
            
            if (patchImage(context, uri, destinationDir, fileName)) {
                successCount++
            }
        }
        
        onProgress(sourceUris.size, sourceUris.size)
        return successCount
    }
}
