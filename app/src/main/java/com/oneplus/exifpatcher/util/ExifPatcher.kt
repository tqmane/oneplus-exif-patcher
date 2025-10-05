package com.oneplus.exifpatcher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import com.oneplus.exifpatcher.watermark.HasselbladWatermarkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Utility object for applying OnePlus-specific EXIF patches to images.
 */
object ExifPatcher {

    private const val JPEG_MIME_TYPE = "image/jpeg"
    private const val MAKE = "OnePlus"
    private const val USER_COMMENT = "oplus_1048864"
    private const val DEVICE_VALUE = "0xcdcc8c3fff"

    /**
     * Apply EXIF patches (and optional watermark) to a single image.
     *
     * @param context Application context
     * @param sourceUri URI of the source image
     * @param destinationDir DocumentFile representing the destination directory
     * @param fileName Filename for the patched image
     * @param customModelName Optional custom model name (null to keep the original)
     * @param watermarkStyleId Optional watermark style ID (null for no watermark)
     * @return True if the image was processed successfully
     */
    fun patchImage(
        context: Context,
        sourceUri: Uri,
        destinationDir: DocumentFile,
        fileName: String,
        customModelName: String? = null,
        watermarkStyleId: String? = null
    ): Boolean {
        if (!destinationDir.isDirectory) {
            return false
        }

        val destinationFile = destinationDir.createFile(JPEG_MIME_TYPE, fileName) ?: return false
        val tempFile = File(context.cacheDir, "exifpatcher_${System.currentTimeMillis()}.jpg")
        var workingFile: File = tempFile
        var watermarkedFile: File? = null

        return try {
            if (!copyFromUri(context, sourceUri, tempFile)) {
                destinationFile.delete()
                return false
            }

            val fileAfterWatermark = applyWatermarkIfNeeded(context, tempFile, watermarkStyleId)
            if (fileAfterWatermark != tempFile) {
                workingFile = fileAfterWatermark
                watermarkedFile = fileAfterWatermark
            }

            applyExifPatches(context, sourceUri, workingFile, customModelName)

            if (!copyToDestination(context, workingFile, destinationFile)) {
                destinationFile.delete()
                return false
            }

            true
        } catch (t: Throwable) {
            t.printStackTrace()
            destinationFile.delete()
            false
        } finally {
            tempFile.delete()
            watermarkedFile?.delete()
        }
    }

    /**
     * Process multiple images and report progress.
     *
     * @param context Application context
     * @param sourceUris URIs of the source images
     * @param destinationDir Destination directory as a DocumentFile
     * @param customModelName Optional custom model name (null to keep the original)
     * @param watermarkStyleId Optional watermark style ID (null for no watermark)
     * @param onProgress Callback invoked with (current, total)
     * @return Number of successfully processed images
     */
    suspend fun patchImages(
        context: Context,
        sourceUris: List<Uri>,
        destinationDir: DocumentFile,
        customModelName: String? = null,
        watermarkStyleId: String? = null,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        val total = sourceUris.size

        sourceUris.forEachIndexed { index, uri ->
            onProgress(index, total)

            val outputName = resolveOutputFileName(context, uri, index)
            if (patchImage(context, uri, destinationDir, outputName, customModelName, watermarkStyleId)) {
                successCount++
            }
        }

        onProgress(total, total)
        successCount
    }

    private fun copyFromUri(context: Context, uri: Uri, targetFile: File): Boolean {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
            true
        } ?: false
    }

    private fun copyToDestination(
        context: Context,
        sourceFile: File,
        destinationFile: DocumentFile
    ): Boolean {
        return context.contentResolver.openOutputStream(destinationFile.uri)?.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
            true
        } ?: false
    }

    private fun applyWatermarkIfNeeded(
        context: Context,
        sourceFile: File,
        watermarkStyleId: String?
    ): File {
        if (watermarkStyleId.isNullOrBlank()) {
            return sourceFile
        }

        return try {
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return sourceFile
            val exif = ExifInterface(sourceFile.absolutePath)
            val watermarkManager = HasselbladWatermarkManager(context)
            val watermarkedBitmap = watermarkManager.addWatermark(bitmap, exif, watermarkStyleId)
            val watermarkedFile = File(context.cacheDir, "watermarked_${System.currentTimeMillis()}.jpg")

            FileOutputStream(watermarkedFile).use { output ->
                watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
            }

            val watermarkedExif = ExifInterface(watermarkedFile.absolutePath)
            copyExifData(exif, watermarkedExif)
            watermarkedExif.saveAttributes()

            if (watermarkedBitmap !== bitmap && !watermarkedBitmap.isRecycled) {
                watermarkedBitmap.recycle()
            }
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            watermarkedFile
        } catch (t: Throwable) {
            t.printStackTrace()
            sourceFile
        }
    }

    private fun applyExifPatches(
        context: Context,
        sourceUri: Uri,
        workingFile: File,
        customModelName: String?
    ) {
        val exif = ExifInterface(workingFile.absolutePath)

        exif.setAttribute(ExifInterface.TAG_MAKE, MAKE)
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, USER_COMMENT)

        val sanitizedModel = customModelName?.takeUnless { it.isBlank() }
        if (sanitizedModel != null) {
            exif.setAttribute(ExifInterface.TAG_MODEL, sanitizedModel)
        } else {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val sourceExif = ExifInterface(input)
                sourceExif.getAttribute(ExifInterface.TAG_MODEL)?.let { model ->
                    exif.setAttribute(ExifInterface.TAG_MODEL, model)
                }
            }
        }

        updateXmpDeviceTag(exif)
        exif.saveAttributes()
    }

    private fun updateXmpDeviceTag(exif: ExifInterface) {
        val currentXmp = exif.getAttribute(ExifInterface.TAG_XMP) ?: ""
        val updatedXmp = when {
            currentXmp.contains("<Device", ignoreCase = true) ->
                currentXmp.replace(Regex("<Device[^>]*>[^<]*</Device>", RegexOption.IGNORE_CASE)) {
                    "<Device>$DEVICE_VALUE</Device>"
                }

            currentXmp.isNotEmpty() ->
                currentXmp.replace(
                    "</rdf:Description>",
                    "  <Device>$DEVICE_VALUE</Device>\n</rdf:Description>",
                    ignoreCase = false
                )

            else -> """
                <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
                <x:xmpmeta xmlns:x="adobe:ns:meta/">
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about="">
                      <Device>$DEVICE_VALUE</Device>
                    </rdf:Description>
                  </rdf:RDF>
                </x:xmpmeta>
                <?xpacket end="w"?>
            """.trimIndent()
        }

        if (updatedXmp.isNotEmpty() && updatedXmp != currentXmp) {
            exif.setAttribute(ExifInterface.TAG_XMP, updatedXmp)
        }
    }

    private fun copyExifData(source: ExifInterface, destination: ExifInterface) {
        EXIF_TAGS_TO_COPY.forEach { tag ->
            source.getAttribute(tag)?.let { value ->
                destination.setAttribute(tag, value)
            }
        }
    }

    private fun resolveOutputFileName(context: Context, uri: Uri, index: Int): String {
        val nameFromDocument = DocumentFile.fromSingleUri(context, uri)?.name
        val candidate = nameFromDocument ?: uri.lastPathSegment
        if (!candidate.isNullOrBlank()) {
            val lower = candidate.lowercase(Locale.ROOT)
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return candidate
            }
        }

        return "patched_${System.currentTimeMillis()}_$index.jpg"
    }

    private val EXIF_TAGS_TO_COPY = listOf(
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_IMAGE_LENGTH,
        ExifInterface.TAG_IMAGE_WIDTH,
        ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_WHITE_BALANCE
    )
}
