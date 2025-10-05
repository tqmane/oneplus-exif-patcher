package com.oneplus.exifpatcher.utilpackage com.oneplus.exi    /**

     * Process an image by applying OnePlus EXIF patches

import android.content.Context     * 

import android.graphics.Bitmap     * @param context Application context

import android.graphics.BitmapFactory     * @param sourceUri URI of the source image

import android.net.Uri     * @param destinationFile DocumentFile where the patched image will be saved

import androidx.documentfile.provider.DocumentFile     * @param fileName Name for the destination file

import androidx.exifinterface.media.ExifInterface     * @param customModelName Optional custom model name to set (null to preserve original)

import com.oneplus.exifpatcher.watermark.HasselbladWatermarkManager     * @param watermarkStyleId Optional watermark style ID to apply (null for no watermark)

import java.io.File     * @return True if successful, false otherwise

import java.io.FileOutputStream     */

    fun patchImage(

/**        context: Context,

 * Utility class for EXIF metadata manipulation        sourceUri: Uri,

 * Applies OnePlus-specific EXIF patches to images        destinationFile: DocumentFile,

 */        fileName: String,

object ExifPatcher {        customModelName: String? = null,

            watermarkStyleId: String? = null

    // OnePlus-specific EXIF values    ): Boolean {import android.content.Context

    private const val MAKE = "OnePlus"import android.graphics.Bitmap

    private const val USER_COMMENT = "oplus_1048864"import android.graphics.BitmapFactory

    private const val DEVICE_VALUE = "0xcdcc8c3fff"  // XMP Device tagimport android.net.Uri

    import androidx.documentfile.provider.DocumentFile

    /**import androidx.exifinterface.media.ExifInterface

     * Process an image by applying OnePlus EXIF patchesimport com.oneplus.exifpatcher.watermark.HasselbladWatermarkManager

     * import java.io.File

     * @param context Application contextimport java.io.FileOutputStream

     * @param sourceUri URI of the source image

     * @param destinationFile DocumentFile where the patched image will be saved/**

     * @param fileName Name for the destination file * Utility class for EXIF metadata manipulation

     * @param customModelName Optional custom model name to set (null to preserve original) * Applies OnePlus-specific EXIF patches to images

     * @param watermarkStyleId Optional watermark style ID to apply (null for no watermark) */

     * @return True if successful, false otherwiseobject ExifPatcher {

     */    

    fun patchImage(    // OnePlus-specific EXIF values

        context: Context,    private const val MAKE = "OnePlus"

        sourceUri: Uri,    private const val USER_COMMENT = "oplus_1048864"

        destinationFile: DocumentFile,    private const val DEVICE_VALUE = "0xcdcc8c3fff"  // XMP Device tag

        fileName: String,    

        customModelName: String? = null,    /**

        watermarkStyleId: String? = null     * Process an image by applying OnePlus EXIF patches

    ): Boolean {     * 

        return try {     * @param context Application context

            // Create a new file in the destination directory     * @param sourceUri URI of the source image

            val newFile = destinationFile.createFile("image/jpeg", fileName)     * @param destinationFile DocumentFile where the patched image will be saved

                ?: return false     * @param fileName Name for the destination file

                 * @param customModelName Optional custom model name to set (null to preserve original)

            // Use cache directory for temporary file to apply EXIF changes     * @return true if processing was successful, false otherwise

            val tempFile = File(context.cacheDir, "temp_$fileName")     */

                fun patchImage(

            // Copy source to temp file        context: Context,

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->        sourceUri: Uri,

                FileOutputStream(tempFile).use { outputStream ->        destinationFile: DocumentFile,

                    inputStream.copyTo(outputStream)        fileName: String,

                }        customModelName: String? = null

            }    ): Boolean {

                    return try {

            // Apply EXIF patches to the temp file            // Create a new file in the destination directory

            val exif = ExifInterface(tempFile.absolutePath)            val newFile = destinationFile.createFile("image/jpeg", fileName)

                            ?: return false

            // Set OnePlus-specific EXIF tags            

            exif.setAttribute(ExifInterface.TAG_MAKE, MAKE)  // Set Make to "OnePlus"            // Use cache directory for temporary file to apply EXIF changes

                        val tempFile = File(context.cacheDir, "temp_$fileName")

            // Set Model if custom name is provided            

            customModelName?.let { modelName ->            // Copy source to temp file

                if (modelName.isNotEmpty()) {            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->

                    exif.setAttribute(ExifInterface.TAG_MODEL, modelName)                FileOutputStream(tempFile).use { outputStream ->

                }                    inputStream.copyTo(outputStream)

            }                }

            // Note: If customModelName is null or empty, we preserve the original Model metadata            }

                        

            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, USER_COMMENT)  // Set UserComment to "oplus_1048864"            // Apply EXIF patches to the temp file

                        val exif = ExifInterface(tempFile.absolutePath)

            // Try to set XMP Device tag            

            // Note: Android's ExifInterface has limited XMP write support            // Set OnePlus-specific EXIF tags

            // The device value should be set in XMP metadata            exif.setAttribute(ExifInterface.TAG_MAKE, MAKE)  // Set Make to "OnePlus"

            try {            

                // Get existing XMP data or create new            // Set Model if custom name is provided

                val xmpData = exif.getAttribute(ExifInterface.TAG_XMP) ?: ""            customModelName?.let { modelName ->

                                if (modelName.isNotEmpty()) {

                // Check if Device tag already exists in XMP                    exif.setAttribute(ExifInterface.TAG_MODEL, modelName)

                val updatedXmp = if (xmpData.contains("Device")) {                }

                    // Replace existing Device value            }

                    xmpData.replace(Regex("Device[^>]*>[^<]*"), "Device>$DEVICE_VALUE")            // Note: If customModelName is null or empty, we preserve the original Model metadata

                } else if (xmpData.isNotEmpty()) {            

                    // Add Device tag to existing XMP            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, USER_COMMENT)  // Set UserComment to "oplus_1048864"

                    xmpData.replace("</rdf:Description>",             

                        "  <Device>$DEVICE_VALUE</Device>\n</rdf:Description>")            // Try to set XMP Device tag

                } else {            // Note: Android's ExifInterface has limited XMP write support

                    // Create minimal XMP with Device tag            // The device value should be set in XMP metadata

                    """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>            try {

<x:xmpmeta xmlns:x="adobe:ns:meta/">                // Get existing XMP data or create new

  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">                val xmpData = exif.getAttribute(ExifInterface.TAG_XMP) ?: ""

    <rdf:Description rdf:about="">                

      <Device>$DEVICE_VALUE</Device>                // Check if Device tag already exists in XMP

    </rdf:Description>                val updatedXmp = if (xmpData.contains("Device")) {

  </rdf:RDF>                    // Replace existing Device value

</x:xmpmeta>                    xmpData.replace(Regex("Device[^>]*>[^<]*"), "Device>$DEVICE_VALUE")

<?xpacket end="w"?>"""                } else if (xmpData.isNotEmpty()) {

                }                    // Add Device tag to existing XMP

                                    xmpData.replace("</rdf:Description>", 

                exif.setAttribute(ExifInterface.TAG_XMP, updatedXmp)                        "  <Device>$DEVICE_VALUE</Device>\n</rdf:Description>")

            } catch (e: Exception) {                } else {

                // XMP setting failed, continue with other tags                    // Create minimal XMP with Device tag

                e.printStackTrace()                    """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>

            }<x:xmpmeta xmlns:x="adobe:ns:meta/">

              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

            // Save the modified EXIF data    <rdf:Description rdf:about="">

            exif.saveAttributes()      <Device>$DEVICE_VALUE</Device>

                </rdf:Description>

            // Apply watermark if specified  </rdf:RDF>

            val finalFile = if (watermarkStyleId != null) {</x:xmpmeta>

                try {<?xpacket end="w"?>"""

                    // Load the image with EXIF data                }

                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)                

                                    exif.setAttribute(ExifInterface.TAG_XMP, updatedXmp)

                    // Apply watermark            } catch (e: Exception) {

                    val watermarkManager = HasselbladWatermarkManager(context)                // XMP setting failed, continue with other tags

                    val watermarkedBitmap = watermarkManager.addWatermark(                e.printStackTrace()

                        source = bitmap,            }

                        styleId = watermarkStyleId,            

                        exifInterface = exif            // Save the modified EXIF data

                    )            exif.saveAttributes()

                                

                    // Save watermarked image to a new temp file            // Apply watermark if specified

                    val watermarkedFile = File(context.cacheDir, "watermarked_${System.currentTimeMillis()}.jpg")            val finalFile = if (watermarkStyleId != null) {

                    FileOutputStream(watermarkedFile).use { fos ->                try {

                        watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)                    // Load the image with EXIF data

                    }                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)

                                        

                    // Update EXIF on watermarked file                    // Apply watermark

                    val watermarkedExif = ExifInterface(watermarkedFile.absolutePath)                    val watermarkManager = HasselbladWatermarkManager(context)

                    exif.getAttribute(ExifInterface.TAG_MAKE)?.let {                     val watermarkedBitmap = watermarkManager.addWatermark(

                        watermarkedExif.setAttribute(ExifInterface.TAG_MAKE, it)                         source = bitmap,

                    }                        styleId = watermarkStyleId,

                    exif.getAttribute(ExifInterface.TAG_MODEL)?.let {                         exifInterface = exif

                        watermarkedExif.setAttribute(ExifInterface.TAG_MODEL, it)                     )

                    }                    

                    exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.let {                     // Save watermarked image to a new temp file

                        watermarkedExif.setAttribute(ExifInterface.TAG_USER_COMMENT, it)                     val watermarkedFile = File(context.cacheDir, "watermarked_${System.currentTimeMillis()}.jpg")

                    }                    FileOutputStream(watermarkedFile).use { fos ->

                    exif.getAttribute(ExifInterface.TAG_XMP)?.let {                         watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)

                        watermarkedExif.setAttribute(ExifInterface.TAG_XMP, it)                     }

                    }                    

                    watermarkedExif.saveAttributes()                    // Update EXIF on watermarked file

                                        val watermarkedExif = ExifInterface(watermarkedFile.absolutePath)

                    // Clean up original temp file                    exif.getAttribute(ExifInterface.TAG_MAKE)?.let { 

                    tempFile.delete()                        watermarkedExif.setAttribute(ExifInterface.TAG_MAKE, it) 

                    bitmap.recycle()                    }

                    watermarkedBitmap.recycle()                    exif.getAttribute(ExifInterface.TAG_MODEL)?.let { 

                                            watermarkedExif.setAttribute(ExifInterface.TAG_MODEL, it) 

                    watermarkedFile                    }

                } catch (e: Exception) {                    exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.let { 

                    e.printStackTrace()                        watermarkedExif.setAttribute(ExifInterface.TAG_USER_COMMENT, it) 

                    // If watermarking fails, use original temp file                    }

                    tempFile                    exif.getAttribute(ExifInterface.TAG_XMP)?.let { 

                }                        watermarkedExif.setAttribute(ExifInterface.TAG_XMP, it) 

            } else {                    }

                tempFile                    watermarkedExif.saveAttributes()

            }                    

                                // Clean up original temp file

            // Copy the patched file to the destination                    tempFile.delete()

            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->                    bitmap.recycle()

                finalFile.inputStream().use { inputStream ->                    watermarkedBitmap.recycle()

                    inputStream.copyTo(outputStream)                    

                }                    watermarkedFile

            }                } catch (e: Exception) {

                                e.printStackTrace()

            // Clean up temp file                    // If watermarking fails, use original temp file

            finalFile.delete()                    tempFile

                            }

            true            } else {

        } catch (e: Exception) {                tempFile

            e.printStackTrace()            }

            false            

        }            // Copy the patched file to the destination

    }            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->

                    finalFile.inputStream().use { inputStream ->

    /**                    inputStream.copyTo(outputStream)

     * Batch process multiple images                }

     *             }

     * @param context Application context            

     * @param sourceUris List of source image URIs            // Clean up temp file

     * @param destinationDir Destination directory (as DocumentFile) for patched images            finalFile.delete()

     * @param customModelName Optional custom model name to set (null to preserve original)            

     * @param watermarkStyleId Optional watermark style ID to apply (null for no watermark)            true

     * @param onProgress Callback for progress updates (current index, total count)        } catch (e: Exception) {

     * @return Number of successfully processed images            e.printStackTrace()

     */            false

    suspend fun patchImages(        }

        context: Context,    }

        sourceUris: List<Uri>,    

        destinationDir: DocumentFile,    /**

        customModelName: String? = null,     * Batch process multiple images

        watermarkStyleId: String? = null,     * 

        onProgress: (Int, Int) -> Unit     * @param context Application context

    ): Int {     * @param sourceUris List of source image URIs

        var successCount = 0     * @param destinationDir Destination directory (as DocumentFile) for patched images

             * @param customModelName Optional custom model name to set (null to preserve original)

        sourceUris.forEachIndexed { index, uri ->     * @param watermarkStyleId Optional watermark style ID to apply (null for no watermark)

            onProgress(index, sourceUris.size)     * @param onProgress Callback for progress updates (current index, total count)

                 * @return Number of successfully processed images

            // Generate unique filename     */

            val fileName = "patched_${System.currentTimeMillis()}_$index.jpg"    suspend fun patchImages(

                    context: Context,

            if (patchImage(context, uri, destinationDir, fileName, customModelName, watermarkStyleId)) {        sourceUris: List<Uri>,

                successCount++        destinationDir: DocumentFile,

            }        customModelName: String? = null,

        }        watermarkStyleId: String? = null,

                onProgress: (Int, Int) -> Unit

        onProgress(sourceUris.size, sourceUris.size)    ): Int {

        return successCount        var successCount = 0

    }        

}        sourceUris.forEachIndexed { index, uri ->

            onProgress(index, sourceUris.size)
            
            // Generate unique filename
            val fileName = "patched_${System.currentTimeMillis()}_$index.jpg"
            
            if (patchImage(context, uri, destinationDir, fileName, customModelName, watermarkStyleId)) {
                successCount++
            }
        }
        
        onProgress(sourceUris.size, sourceUris.size)
        return successCount
    }
}
