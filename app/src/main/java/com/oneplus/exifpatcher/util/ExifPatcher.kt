package com.oneplus.exifpatcher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap

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
    private const val PATCHED_SUFFIX = "_Patched"
    private val INVALID_FILENAME_CHARS = Regex("[\\\\/:*?\"<>|]")
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

        val patchedFileName = ensurePatchedSuffix(fileName)
        val destinationMime = mimeTypeFromFileName(patchedFileName)
            ?: resolveMimeType(context, sourceUri)
            ?: JPEG_MIME_TYPE
        val destinationFile = destinationDir.createFile(destinationMime, patchedFileName) ?: return false
        val tempFile = File(context.cacheDir, "exifpatcher_${System.currentTimeMillis()}.jpg")
        var workingFile: File = tempFile
        var watermarkedFile: File? = null

        return try {
            if (!prepareWorkingFile(context, sourceUri, tempFile)) {
                destinationFile.delete()
                return false
            }

            // First, apply EXIF patches so that watermark rendering can read updated metadata
            applyExifPatches(context, sourceUri, tempFile, customModelName)

            // Then, apply watermark (if any). This function copies EXIF from the provided file,
            // so running it after EXIF patching ensures the updated Model is used and preserved.
            val fileAfterWatermark = applyWatermarkIfNeeded(context, tempFile, watermarkStyleId)
            if (fileAfterWatermark != tempFile) {
                workingFile = fileAfterWatermark
                watermarkedFile = fileAfterWatermark
            }

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

        removeLensMetadata(exif)

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

    private fun prepareWorkingFile(context: Context, sourceUri: Uri, targetFile: File): Boolean {
        val mimeType = resolveMimeType(context, sourceUri)
        val extension = resolveFileExtension(context, sourceUri)
        return if (!shouldTranscodeToJpeg(mimeType, extension)) {
            copyFromUri(context, sourceUri, targetFile)
        } else {
            transcodeToJpeg(context, sourceUri, targetFile) != null
        }
    }

    private fun shouldTranscodeToJpeg(mimeType: String?, extension: String?): Boolean {
        val lowerMime = mimeType?.lowercase(Locale.ROOT)
        if (lowerMime != null) {
            if (lowerMime.contains("jpeg") || lowerMime.contains("jpg")) {
                return false
            }
            if (lowerMime.contains("heic") || lowerMime.contains("heif") || lowerMime.contains("hevc")) {
                return true
            }
            return false
        }

        val lowerExt = extension?.lowercase(Locale.ROOT)
        return lowerExt == "heic" || lowerExt == "heif" || lowerExt == "hevc"
    }

    private fun transcodeToJpeg(context: Context, sourceUri: Uri, targetFile: File): Pair<Int, Int>? {
        val orientation = readExifOrientation(context, sourceUri)
        val bitmap = decodeBitmap(context, sourceUri) ?: return null
        val needsManualOrientation = Build.VERSION.SDK_INT < Build.VERSION_CODES.P && orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED
        val orientedBitmap = if (needsManualOrientation) applyExifOrientation(bitmap, orientation) else bitmap
        return try {
            FileOutputStream(targetFile).use { output ->
                if (!orientedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
                    return null
                }
            }
            // Update EXIF with original attributes after transcode
            copyExifFromSource(context, sourceUri, targetFile, resetOrientation = true, newDimensions = orientedBitmap.width to orientedBitmap.height)
            orientedBitmap.width to orientedBitmap.height
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        } finally {
            if (orientedBitmap !== bitmap && !orientedBitmap.isRecycled) {
                orientedBitmap.recycle()
            }
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_UNDEFINED
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        val transformNeeded = when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(-1f, 1f)
                true
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.setRotate(180f)
                true
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setScale(1f, -1f)
                true
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
                true
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.setRotate(90f)
                true
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
                true
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.setRotate(270f)
                true
            }
            else -> false
        }

        if (!transformNeeded) {
            return bitmap
        }

        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (t: Throwable) {
            t.printStackTrace()
            bitmap
        }
    }


    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun copyExifFromSource(
        context: Context,
        sourceUri: Uri,
        destinationFile: File,
        resetOrientation: Boolean,
        newDimensions: Pair<Int, Int>?
    ) {
        runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val sourceExif = ExifInterface(input)
                val destinationExif = ExifInterface(destinationFile.absolutePath)
                copyExifData(sourceExif, destinationExif)
                if (resetOrientation) {
                    destinationExif.setAttribute(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL.toString()
                    )
                }
                newDimensions?.let { (width, height) ->
                    destinationExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
                    destinationExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
                    destinationExif.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, width.toString())
                    destinationExif.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, height.toString())
                }
            removeLensMetadata(destinationExif)
                destinationExif.saveAttributes()
            }
        }
    }

    private fun resolveMimeType(context: Context, uri: Uri): String? {
        context.contentResolver.getType(uri)?.let { return it }

        val extension = resolveFileExtension(context, uri)
        return extension?.let { ext ->
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase(Locale.ROOT))
        }
    }

    private fun resolveFileExtension(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)?.let { name ->
                    extractExtension(name)?.let { return it }
                }
            }
        }

        uri.lastPathSegment?.let { segment ->
            extractExtension(segment)?.let { return it }
        }

        val decoded = Uri.decode(uri.toString())
        return extractExtension(decoded)
    }

    private fun extractExtension(fileName: String): String? {
        val dotIndex = fileName.lastIndexOf('.')
        if (dotIndex in 0 until fileName.length - 1) {
            return fileName.substring(dotIndex + 1)
        }
        return null
    }

    private fun resolveOutputFileName(context: Context, uri: Uri, index: Int): String {
        resolveDisplayName(context, uri)?.takeIf { it.isNotBlank() }?.let { name ->
            sanitizeFileName(name).takeIf { it.isNotBlank() }?.let { sanitized ->
                return ensurePatchedSuffix(sanitized)
            }
        }

        val extension = resolveFileExtension(context, uri)
        val fallbackSource = DocumentFile.fromSingleUri(context, uri)?.name
            ?: uri.lastPathSegment
            ?: Uri.decode(uri.toString()).substringAfterLast('/')

        val sanitizedSource = fallbackSource?.let { sanitizeFileName(it) }?.takeIf { it.isNotBlank() }
        val baseName = sanitizedSource?.let { source ->
            val extLower = extension?.lowercase(Locale.ROOT)
            if (!extLower.isNullOrEmpty() && source.lowercase(Locale.ROOT).endsWith(".$extLower")) {
                source.substring(0, source.length - extLower.length - 1)
            } else {
                source
            }
        }?.takeIf { it.isNotBlank() }
            ?: "patched_${System.currentTimeMillis()}_$index"

        val candidate = if (!extension.isNullOrBlank()) {
            "$baseName.$extension"
        } else {
            baseName
        }

        return ensurePatchedSuffix(candidate)
    }

    private fun ensurePatchedSuffix(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        if (baseName.endsWith(PATCHED_SUFFIX)) {
            return fileName
        }

        val sanitizedBase = sanitizeFileName(baseName).ifEmpty { baseName }
        val suffixBase = if (sanitizedBase.isNotEmpty()) sanitizedBase else {
            val fallbackBase = "patched_${System.currentTimeMillis()}"
            baseName.takeIf { it.isNotBlank() } ?: fallbackBase
        }

        return if (dotIndex > 0) {
            suffixBase + PATCHED_SUFFIX + fileName.substring(dotIndex)
        } else {
            suffixBase + PATCHED_SUFFIX
        }
    }

    private fun mimeTypeFromFileName(fileName: String): String? {
        val extension = extractExtension(fileName)?.lowercase(Locale.ROOT) ?: return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun sanitizeFileName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ""
        var sanitized = trimmed.replace(INVALID_FILENAME_CHARS, "_")
        while (sanitized.contains("__")) {
            sanitized = sanitized.replace("__", "_")
        }
        return sanitized.trim('_')
    }

    private fun removeLensMetadata(exif: ExifInterface) {
        exif.setAttribute(ExifInterface.TAG_LENS_MODEL, null)
        runCatching {
            exif.setAttribute(ExifInterface.TAG_LENS_MAKE, null)
        }
        runCatching {
            exif.setAttribute(ExifInterface.TAG_LENS_SERIAL_NUMBER, null)
        }
        runCatching {
            exif.setAttribute(ExifInterface.TAG_LENS_SPECIFICATION, null)
        }

        val currentXmp = exif.getAttribute(ExifInterface.TAG_XMP) ?: return
        val lensRegex = Regex("<(?:(?:aux:)?LensModel|LensMake|LensSpecification|aux:Lens)\\b[^>]*>.*?</[^>]+>", RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL)
        val updatedXmp = lensRegex.replace(currentXmp, "").let { cleaned ->
            cleaned.replace(Regex("\\s+</rdf:Description>"), "\n</rdf:Description>")
        }
        if (updatedXmp != currentXmp) {
            exif.setAttribute(ExifInterface.TAG_XMP, updatedXmp.trim())
        }
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

