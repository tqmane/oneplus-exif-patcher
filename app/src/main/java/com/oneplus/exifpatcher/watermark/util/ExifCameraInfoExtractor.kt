package com.oneplus.exifpatcher.watermark.util

import androidx.exifinterface.media.ExifInterface
import com.oneplus.exifpatcher.watermark.model.CameraInfo

/**
 * Exif情報からカメラ情報を抽出するユーティリティ
 */
object ExifCameraInfoExtractor {
    
    /**
     * ExifInterfaceからカメラ情報を抽出
     * 
     * @param exif ExifInterfaceオブジェクト
     * @return CameraInfo
     */
    fun extractCameraInfo(exif: ExifInterface): CameraInfo {
        return CameraInfo(
            aperture = extractAperture(exif),
            shutterSpeed = extractShutterSpeed(exif),
            iso = extractIso(exif),
            focalLength = extractFocalLength(exif),
            deviceName = extractDeviceName(exif)
        )
    }
    
    /**
     * 絞り値を抽出
     * 
     * @return 例: "f/1.8"
     */
    private fun extractAperture(exif: ExifInterface): String? {
        val apertureValue = exif.getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE, 0.0)
        val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
        
        val aperture = when {
            fNumber > 0 -> fNumber
            apertureValue > 0 -> Math.pow(Math.sqrt(2.0), apertureValue)
            else -> null
        }
        
        return aperture?.let {
            val rounded = (it * 10).toInt() / 10.0  // 小数第1位で丸める
            "f/$rounded"
        }
    }
    
    /**
     * シャッタースピードを抽出
     * 
     * @return 例: "1/100"
     */
    private fun extractShutterSpeed(exif: ExifInterface): String? {
        val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()
        
        return exposureTime?.let {
            when {
                it >= 1.0 -> "${it.toInt()}s"
                it > 0 -> {
                    val reciprocal = (1.0 / it).toInt()
                    "1/$reciprocal"
                }
                else -> null
            }
        }
    }
    
    /**
     * ISO感度を抽出
     * 
     * @return 例: "ISO400"
     */
    private fun extractIso(exif: ExifInterface): String? {
        // Use TAG_PHOTOGRAPHIC_SENSITIVITY (recommended) instead of deprecated TAG_ISO_SPEED_RATINGS
        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.toIntOrNull()
            ?: exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0).takeIf { it > 0 }
        
        return iso?.let { "ISO$it" }
    }
    
    /**
     * 焦点距離を抽出
     * 
     * @return 例: "24mm"
     */
    private fun extractFocalLength(exif: ExifInterface): String? {
        val focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
        
        return if (focalLength > 0) {
            "${focalLength.toInt()}mm"
        } else {
            null
        }
    }
    
    /**
     * デバイス名を抽出
     * 
     * @return 例: "OnePlus 9 Pro"
     */
    private fun extractDeviceName(exif: ExifInterface): String? {
        // まずExifのMakeとModelを確認
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
        
        return when {
            !make.isNullOrBlank() && !model.isNullOrBlank() -> {
                // Makeがmodelに含まれている場合は重複を避ける
                if (model.contains(make, ignoreCase = true)) {
                    model
                } else {
                    "$make $model"
                }
            }
            !model.isNullOrBlank() -> model
            !make.isNullOrBlank() -> make
            else -> android.os.Build.MODEL  // Exifに情報がない場合は端末情報を使用
        }
    }
    
    /**
     * フォーマット済みカメラ情報を取得
     * 
     * @param exif ExifInterfaceオブジェクト
     * @return 例: "f/1.8  1/100s  ISO400  24mm"
     */
    fun getFormattedCameraInfo(exif: ExifInterface): String {
        return extractCameraInfo(exif).format()
    }
    
    /**
     * すべての情報をデバッグ用に取得
     */
    fun getDebugInfo(exif: ExifInterface): String {
        val info = extractCameraInfo(exif)
        return buildString {
            appendLine("Device: ${info.deviceName}")
            appendLine("Aperture: ${info.aperture}")
            appendLine("Shutter: ${info.shutterSpeed}")
            appendLine("ISO: ${info.iso}")
            appendLine("Focal: ${info.focalLength}")
            appendLine("Formatted: ${info.format()}")
        }
    }
}
