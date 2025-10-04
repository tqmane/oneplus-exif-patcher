package com.oneplus.exifpatcher.data

import android.content.Context
import android.net.Uri
import com.oneplus.exifpatcher.util.ExifPatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository class for handling image processing operations
 */
class ImageRepository(private val context: Context) {
    
    /**
     * Process multiple images with EXIF patches
     * 
     * @param imageUris List of image URIs to process
     * @param destinationUri URI of the destination directory
     * @param onProgress Callback for progress updates
     * @return Result containing success count or error
     */
    suspend fun processImages(
        imageUris: List<Uri>,
        destinationUri: Uri,
        onProgress: (Int, Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Convert destination URI to File
            val destinationPath = destinationUri.path ?: throw IllegalArgumentException("Invalid destination path")
            val destinationDir = File(destinationPath)
            
            // Ensure destination directory exists
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }
            
            if (!destinationDir.isDirectory) {
                throw IllegalArgumentException("Destination is not a directory")
            }
            
            // Process images using ExifPatcher
            val successCount = ExifPatcher.patchImages(
                context = context,
                sourceUris = imageUris,
                destinationDir = destinationDir,
                onProgress = onProgress
            )
            
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
