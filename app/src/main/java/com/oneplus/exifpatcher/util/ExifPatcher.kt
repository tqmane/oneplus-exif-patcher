package com.oneplus.exifpatcher.utilpackage com.oneplus.exifpatcher.utilpackage com.oneplus.exi    /**



import android.content.Context     * Process an image by applying OnePlus EXIF patches

import android.graphics.Bitmap

import android.graphics.BitmapFactoryimport android.content.Context     * 

import android.net.Uri

import androidx.documentfile.provider.DocumentFileimport android.graphics.Bitmap     * @param context Application context

import androidx.exifinterface.media.ExifInterface

import com.oneplus.exifpatcher.watermark.HasselbladWatermarkManagerimport android.graphics.BitmapFactory     * @param sourceUri URI of the source image

import java.io.File

import java.io.FileOutputStreamimport android.net.Uri     * @param destinationFile DocumentFile where the patched image will be saved



/**import androidx.documentfile.provider.DocumentFile     * @param fileName Name for the destination file

 * Utility class for EXIF metadata manipulation

 * Applies OnePlus-specific EXIF patches to imagesimport androidx.exifinterface.media.ExifInterface     * @param customModelName Optional custom model name to set (null to preserve original)

 */

object ExifPatcher {import com.oneplus.exifpatcher.watermark.HasselbladWatermarkManager     * @param watermarkStyleId Optional watermark style ID to apply (null for no watermark)

    

    // OnePlus-specific EXIF valuesimport java.io.File     * @return True if successful, false otherwise

    private const val MAKE = "OnePlus"

    private const val USER_COMMENT = "oplus_1048864"import java.io.FileOutputStream     */

    private const val DEVICE_VALUE = "0xcdcc8c3fff"  // XMP Device tag

        fun patchImage(

    /**

     * Process an image by applying OnePlus EXIF patches/**        context: Context,

     * 

     * @param context Application context * Utility class for EXIF metadata manipulation        sourceUri: Uri,

     * @param sourceUri URI of the source image

     * @param destinationFile DocumentFile where the patched image will be saved * Applies OnePlus-specific EXIF patches to images        destinationFile: DocumentFile,

     * @param fileName Name for the destination file

     * @param customModelName Optional custom model name to set (null to preserve original) */        fileName: String,

     * @param watermarkStyleId Optional watermark style ID to apply (null for no watermark)

     * @return True if successful, false otherwiseobject ExifPatcher {        customModelName: String? = null,

     */

    fun patchImage(            watermarkStyleId: String? = null

        context: Context,

        sourceUri: Uri,    // OnePlus-specific EXIF values    ): Boolean {import android.content.Context

        destinationFile: DocumentFile,

        fileName: String,    private const val MAKE = "OnePlus"import android.graphics.Bitmap

        customModelName: String? = null,

        watermarkStyleId: String? = null    private const val USER_COMMENT = "oplus_1048864"import android.graphics.BitmapFactory

    ): Boolean {

        return try {    private const val DEVICE_VALUE = "0xcdcc8c3fff"  // XMP Device tagimport android.net.Uri

            // Create a new file in the destination directory

            val newFile = destinationFile.createFile("image/jpeg", fileName)    import androidx.documentfile.provider.DocumentFile

                ?: return false

                /**import androidx.exifinterface.media.ExifInterface

            // Use cache directory for temporary file to apply EXIF changes

            val tempFile = File(context.cacheDir, "temp_$fileName")     * Process an image by applying OnePlus EXIF patchesimport com.oneplus.exifpatcher.watermark.HasselbladWatermarkManager

            

            // Copy source to temp file     * import java.io.File

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->

                FileOutputStream(tempFile).use { outputStream ->     * @param context Application contextimport java.io.FileOutputStream

                    inputStream.copyTo(outputStream)

                }     * @param sourceUri URI of the source image

            }

                 * @param destinationFile DocumentFile where the patched image will be saved/**

            // Apply EXIF patches to the temp file

            val exif = ExifInterface(tempFile.absolutePath)     * @param fileName Name for the destination file * Utility class for EXIF metadata manipulation

            

            // Set OnePlus-specific EXIF tags     * @param customModelName Optional custom model name to set (null to preserve original) * Applies OnePlus-specific EXIF patches to images

            exif.setAttribute(ExifInterface.TAG_MAKE, MAKE)  // Set Make to "OnePlus"

                 * @param watermarkStyleId Optional watermark style ID to apply (null for no watermark) */

            // Set Model if custom name is provided

            customModelName?.let { modelName ->     * @return True if successful, false otherwiseobject ExifPatcher {

                if (modelName.isNotEmpty()) {

                    exif.setAttribute(ExifInterface.TAG_MODEL, modelName)     */    

                }

            }    fun patchImage(    // OnePlus-specific EXIF values

            // Note: If customModelName is null or empty, we preserve the original Model metadata

                    context: Context,    private const val MAKE = "OnePlus"

            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, USER_COMMENT)  // Set UserComment to "oplus_1048864"

                    sourceUri: Uri,    private const val USER_COMMENT = "oplus_1048864"

            // Try to set XMP Device tag

            try {        destinationFile: DocumentFile,    private const val DEVICE_VALUE = "0xcdcc8c3fff"  // XMP Device tag

                val xmpData = exif.getAttribute(ExifInterface.TAG_XMP) ?: ""

                        fileName: String,    

                val updatedXmp = if (xmpData.contains("Device")) {

                    xmpData.replace(Regex("Device[^>]*>[^<]*"), "Device>$DEVICE_VALUE")        customModelName: String? = null,    /**

                } else if (xmpData.isNotEmpty()) {

                    xmpData.replace("</rdf:Description>",         watermarkStyleId: String? = null     * Process an image by applying OnePlus EXIF patches

                        "  <Device>$DEVICE_VALUE</Device>\n</rdf:Description>")

                } else {    ): Boolean {     * 

                    """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>

<x:xmpmeta xmlns:x="adobe:ns:meta/">        return try {     * @param context Application context

  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

    <rdf:Description rdf:about="">            // Create a new file in the destination directory     * @param sourceUri URI of the source image

      <Device>$DEVICE_VALUE</Device>

    </rdf:Description>            val newFile = destinationFile.createFile("image/jpeg", fileName)     * @param destinationFile DocumentFile where the patched image will be saved

  </rdf:RDF>

</x:xmpmeta>                ?: return false     * @param fileName Name for the destination file

<?xpacket end="w"?>"""

                }                 * @param customModelName Optional custom model name to set (null to preserve original)

                

                exif.setAttribute(ExifInterface.TAG_XMP, updatedXmp)            // Use cache directory for temporary file to apply EXIF changes     * @return true if processing was successful, false otherwise

            } catch (e: Exception) {

                e.printStackTrace()            val tempFile = File(context.cacheDir, "temp_$fileName")     */

            }

                            fun patchImage(

            // Save the modified EXIF data

            exif.saveAttributes()            // Copy source to temp file        context: Context,

            

            // Apply watermark if specified            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->        sourceUri: Uri,

            val finalFile = if (watermarkStyleId != null) {

                try {                FileOutputStream(tempFile).use { outputStream ->        destinationFile: DocumentFile,

                    // Load the image

                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)                    inputStream.copyTo(outputStream)        fileName: String,

                    

                    // Apply watermark                }        customModelName: String? = null

                    val watermarkManager = HasselbladWatermarkManager(context)

                    val watermarkedBitmap = watermarkManager.addWatermark(            }    ): Boolean {

                        source = bitmap,

                        styleId = watermarkStyleId,                    return try {

                        exifInterface = exif

                    )            // Apply EXIF patches to the temp file            // Create a new file in the destination directory

                    

                    // Save watermarked image to a new temp file            val exif = ExifInterface(tempFile.absolutePath)            val newFile = destinationFile.createFile("image/jpeg", fileName)

                    val watermarkedFile = File(context.cacheDir, "watermarked_${System.currentTimeMillis()}.jpg")

                    FileOutputStream(watermarkedFile).use { fos ->                            ?: return false

                        watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)

                    }            // Set OnePlus-specific EXIF tags            

                    

                    // Update EXIF on watermarked file            exif.setAttribute(ExifInterface.TAG_MAKE, MAKE)  // Set Make to "OnePlus"            // Use cache directory for temporary file to apply EXIF changes

                    val watermarkedExif = ExifInterface(watermarkedFile.absolutePath)

                    exif.getAttribute(ExifInterface.TAG_MAKE)?.let {                         val tempFile = File(context.cacheDir, "temp_$fileName")

                        watermarkedExif.setAttribute(ExifInterface.TAG_MAKE, it) 

                    }            // Set Model if custom name is provided            

                    exif.getAttribute(ExifInterface.TAG_MODEL)?.let { 

                        watermarkedExif.setAttribute(ExifInterface.TAG_MODEL, it)             customModelName?.let { modelName ->            // Copy source to temp file

                    }

                    exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.let {                 if (modelName.isNotEmpty()) {            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->

                        watermarkedExif.setAttribute(ExifInterface.TAG_USER_COMMENT, it) 

                    }                    exif.setAttribute(ExifInterface.TAG_MODEL, modelName)                FileOutputStream(tempFile).use { outputStream ->

                    exif.getAttribute(ExifInterface.TAG_XMP)?.let { 

                        watermarkedExif.setAttribute(ExifInterface.TAG_XMP, it)                 }                    inputStream.copyTo(outputStream)

                    }

                    watermarkedExif.saveAttributes()            }                }

                    

                    // Clean up            // Note: If customModelName is null or empty, we preserve the original Model metadata            }

                    tempFile.delete()

                    bitmap.recycle()                        

                    watermarkedBitmap.recycle()

                                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, USER_COMMENT)  // Set UserComment to "oplus_1048864"            // Apply EXIF patches to the temp file

                    watermarkedFile

                } catch (e: Exception) {                        val exif = ExifInterface(tempFile.absolutePath)

                    e.printStackTrace()

                    tempFile            // Try to set XMP Device tag            

                }

            } else {            // Note: Android's ExifInterface has limited XMP write support            // Set OnePlus-specific EXIF tags

                tempFile

            }            // The device value should be set in XMP metadata            exif.setAttribute(ExifInterface.TAG_MAKE, MAKE)  // Set Make to "OnePlus"

            

            // Copy the patched file to the destination            try {            

            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->

                finalFile.inputStream().use { inputStream ->                // Get existing XMP data or create new            // Set Model if custom name is provided

                    inputStream.copyTo(outputStream)

                }                val xmpData = exif.getAttribute(ExifInterface.TAG_XMP) ?: ""            customModelName?.let { modelName ->

            }

                                            if (modelName.isNotEmpty()) {

            // Clean up temp file

            finalFile.delete()                // Check if Device tag already exists in XMP                    exif.setAttribute(ExifInterface.TAG_MODEL, modelName)

            

            true                val updatedXmp = if (xmpData.contains("Device")) {                }

        } catch (e: Exception) {

            e.printStackTrace()                    // Replace existing Device value            }

            false

        }                    xmpData.replace(Regex("Device[^>]*>[^<]*"), "Device>$DEVICE_VALUE")            // Note: If customModelName is null or empty, we preserve the original Model metadata

    }

                    } else if (xmpData.isNotEmpty()) {            

    /**

     * Batch process multiple images                    // Add Device tag to existing XMP            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, USER_COMMENT)  // Set UserComment to "oplus_1048864"

     */

    suspend fun patchImages(                    xmpData.replace("</rdf:Description>",             

        context: Context,

        sourceUris: List<Uri>,                        "  <Device>$DEVICE_VALUE</Device>\n</rdf:Description>")            // Try to set XMP Device tag

        destinationDir: DocumentFile,

        customModelName: String? = null,                } else {            // Note: Android's ExifInterface has limited XMP write support

        watermarkStyleId: String? = null,

        onProgress: (Int, Int) -> Unit                    // Create minimal XMP with Device tag            // The device value should be set in XMP metadata

    ): Int {

        var successCount = 0                    """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>            try {

        

        sourceUris.forEachIndexed { index, uri -><x:xmpmeta xmlns:x="adobe:ns:meta/">                // Get existing XMP data or create new

            onProgress(index, sourceUris.size)

              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">                val xmpData = exif.getAttribute(ExifInterface.TAG_XMP) ?: ""

            val fileName = "patched_${System.currentTimeMillis()}_$index.jpg"

                <rdf:Description rdf:about="">                

            if (patchImage(context, uri, destinationDir, fileName, customModelName, watermarkStyleId)) {

                successCount++      <Device>$DEVICE_VALUE</Device>                // Check if Device tag already exists in XMP

            }

        }    </rdf:Description>                val updatedXmp = if (xmpData.contains("Device")) {

        

        onProgress(sourceUris.size, sourceUris.size)  </rdf:RDF>                    // Replace existing Device value

        return successCount

    }</x:xmpmeta>                    xmpData.replace(Regex("Device[^>]*>[^<]*"), "Device>$DEVICE_VALUE")

}

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
