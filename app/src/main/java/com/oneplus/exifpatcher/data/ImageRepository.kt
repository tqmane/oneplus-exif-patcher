package com.oneplus.exifpatcher.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.oneplus.exifpatcher.util.ExifPatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            // Convert destination URI to DocumentFile
            val destinationDir = DocumentFile.fromTreeUri(context, destinationUri)
                ?: throw IllegalArgumentException("Invalid destination URI")
            
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
