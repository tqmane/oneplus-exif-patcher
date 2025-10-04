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
    private const val MAKE = "OnePlus"
    private const val USER_COMMENT = "oplus_1048864"
    private const val DEVICE_VALUE = "0xcdcc8c3fff"  // XMP Device tag
    
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
            exif.setAttribute(ExifInterface.TAG_MAKE, MAKE)  // Set Make to "OnePlus"
            // Note: We preserve the original Model metadata
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, USER_COMMENT)  // Set UserComment to "oplus_1048864"
            
            // Try to set XMP Device tag
            // Note: Android's ExifInterface has limited XMP write support
            // The device value should be set in XMP metadata
            try {
                // Get existing XMP data or create new
                val xmpData = exif.getAttribute(ExifInterface.TAG_XMP) ?: ""
                
                // Check if Device tag already exists in XMP
                val updatedXmp = if (xmpData.contains("Device")) {
                    // Replace existing Device value
                    xmpData.replace(Regex("Device[^>]*>[^<]*"), "Device>$DEVICE_VALUE")
                } else if (xmpData.isNotEmpty()) {
                    // Add Device tag to existing XMP
                    xmpData.replace("</rdf:Description>", 
                        "  <Device>$DEVICE_VALUE</Device>\n</rdf:Description>")
                } else {
                    // Create minimal XMP with Device tag
                    """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/">
  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <rdf:Description rdf:about="">
      <Device>$DEVICE_VALUE</Device>
    </rdf:Description>
  </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>"""
                }
                
                exif.setAttribute(ExifInterface.TAG_XMP, updatedXmp)
            } catch (e: Exception) {
                // XMP setting failed, continue with other tags
                e.printStackTrace()
            }
            
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
