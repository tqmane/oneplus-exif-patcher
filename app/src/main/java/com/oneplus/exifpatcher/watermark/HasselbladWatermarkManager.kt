package com.oneplus.exifpatcher.watermark

import android.content.Context
import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import com.oneplus.exifpatcher.watermark.model.CameraInfo
import com.oneplus.exifpatcher.watermark.model.WatermarkStyle
import com.oneplus.exifpatcher.watermark.parser.WatermarkStyleParser
import com.oneplus.exifpatcher.watermark.renderer.HasselbladWatermarkRenderer
import com.oneplus.exifpatcher.watermark.util.ExifCameraInfoExtractor

/**
 * Hasselbladウォーターマーク機能の統合クラス
 * 
 * 使用例:
 * ```kotlin
 * val watermarkManager = HasselbladWatermarkManager(context)
 * val result = watermarkManager.addWatermark(bitmap, exif)
 * ```
 */
class HasselbladWatermarkManager(private val context: Context) {
    
    private val parser = WatermarkStyleParser(context)
    private val renderer = HasselbladWatermarkRenderer(context)
    
    /**
     * 画像にHasselbladウォーターマークを追加
     * 
     * @param sourceBitmap 元画像
     * @param exif Exif情報
     * @param styleId スタイルID（省略時はデフォルトスタイル）
     * @return ウォーターマーク付き画像
     */
    fun addWatermark(
        sourceBitmap: Bitmap,
        exif: ExifInterface,
        styleId: String? = null
    ): Bitmap {
        // カメラ情報を抽出
        val cameraInfo = ExifCameraInfoExtractor.extractCameraInfo(exif)
        
        // スタイルを読み込み
        val style = loadStyle(styleId)
        
        // ウォーターマークを描画
        return renderer.renderWatermark(sourceBitmap, style, cameraInfo)
    }
    
    /**
     * 画像にHasselbladウォーターマークを追加（カメラ情報を直接指定）
     * 
     * @param sourceBitmap 元画像
     * @param cameraInfo カメラ情報
     * @param styleId スタイルID（省略時はデフォルトスタイル）
     * @return ウォーターマーク付き画像
     */
    fun addWatermark(
        sourceBitmap: Bitmap,
        cameraInfo: CameraInfo,
        styleId: String? = null
    ): Bitmap {
        val style = loadStyle(styleId)
        return renderer.renderWatermark(sourceBitmap, style, cameraInfo)
    }
    
    /**
     * 利用可能なスタイル一覧を取得
     * 
     * @return スタイルIDとスタイル名のMap
     */
    fun getAvailableStyles(): Map<String, String> {
        val styles = parser.loadAllStyles()
        return styles.mapValues { it.value.name }
    }
    
    /**
     * スタイルを読み込み
     */
    private fun loadStyle(styleId: String?): WatermarkStyle {
        if (styleId.isNullOrBlank()) {
            return WatermarkStyleParser.createDefaultStyle()
        }
        
        // JSONファイル名を構築
        val fileName = if (styleId.endsWith(".json")) styleId else "$styleId.json"
        
        return parser.loadStyleFromAssets(fileName)
            ?: WatermarkStyleParser.createDefaultStyle()
    }
    
    /**
     * プリセットスタイルID - WatermarkStyleParser.PresetStylesを参照
     */
    object Presets {
        // Hasselblad スタイル
        const val HASSEL_STYLE_1 = WatermarkStyleParser.PresetStyles.HASSEL_STYLE_1
        const val HASSEL_STYLE_2 = WatermarkStyleParser.PresetStyles.HASSEL_STYLE_2
        const val HASSEL_STYLE_3 = WatermarkStyleParser.PresetStyles.HASSEL_STYLE_3
        const val HASSEL_STYLE_4 = WatermarkStyleParser.PresetStyles.HASSEL_STYLE_4
        const val HASSEL_TEXT_STYLE_1 = WatermarkStyleParser.PresetStyles.HASSEL_TEXT_STYLE_1
        const val HASSEL_TEXT_STYLE_2 = WatermarkStyleParser.PresetStyles.HASSEL_TEXT_STYLE_2
        
        // Brand スタイル
        const val BRAND_COLOR_OS = WatermarkStyleParser.PresetStyles.BRAND_COLOR_OS
        const val BRAND_FIND_X = WatermarkStyleParser.PresetStyles.BRAND_FIND_X
        const val BRAND_IMAGINE = WatermarkStyleParser.PresetStyles.BRAND_IMAGINE
        const val BRAND_BREAK_THROUGH = WatermarkStyleParser.PresetStyles.BRAND_BREAK_THROUGH
        const val BRAND_LUMO_IMAGE = WatermarkStyleParser.PresetStyles.BRAND_LUMO_IMAGE
        
        // Film スタイル
        const val FILM_STYLE_1 = WatermarkStyleParser.PresetStyles.FILM_STYLE_1
        const val FILM_STYLE_2 = WatermarkStyleParser.PresetStyles.FILM_STYLE_2
        const val FILM_REALME_1 = WatermarkStyleParser.PresetStyles.FILM_REALME_1
        const val FILM_REALME_2 = WatermarkStyleParser.PresetStyles.FILM_REALME_2
        
        // Master Sign スタイル
        const val MASTER_SIGN_1 = WatermarkStyleParser.PresetStyles.MASTER_SIGN_1
        const val MASTER_SIGN_2 = WatermarkStyleParser.PresetStyles.MASTER_SIGN_2
        const val MASTER_SIGN_3 = WatermarkStyleParser.PresetStyles.MASTER_SIGN_3
        const val MASTER_SIGN_4 = WatermarkStyleParser.PresetStyles.MASTER_SIGN_4
        
        // Retro Camera スタイル
        const val RETRO_CAMERA_1 = WatermarkStyleParser.PresetStyles.RETRO_CAMERA_1
        const val RETRO_CAMERA_2 = WatermarkStyleParser.PresetStyles.RETRO_CAMERA_2
        const val RETRO_CAMERA_3 = WatermarkStyleParser.PresetStyles.RETRO_CAMERA_3
        const val RETRO_CAMERA_4 = WatermarkStyleParser.PresetStyles.RETRO_CAMERA_4
        const val RETRO_CAMERA_5 = WatermarkStyleParser.PresetStyles.RETRO_CAMERA_5
        
        // Text Style スタイル
        const val TEXT_STYLE_1 = WatermarkStyleParser.PresetStyles.TEXT_STYLE_1
        const val TEXT_STYLE_2 = WatermarkStyleParser.PresetStyles.TEXT_STYLE_2
        const val TEXT_STYLE_3 = WatermarkStyleParser.PresetStyles.TEXT_STYLE_3
        const val VIDEO_TEXT_STYLE_1 = WatermarkStyleParser.PresetStyles.VIDEO_TEXT_STYLE_1
        const val VIDEO_TEXT_STYLE_2 = WatermarkStyleParser.PresetStyles.VIDEO_TEXT_STYLE_2
        const val VIDEO_TEXT_STYLE_3 = WatermarkStyleParser.PresetStyles.VIDEO_TEXT_STYLE_3
        
        // Video Hasselblad スタイル
        const val VIDEO_HASSEL_TEXT_STYLE_1 = WatermarkStyleParser.PresetStyles.VIDEO_HASSEL_TEXT_STYLE_1
        const val VIDEO_HASSEL_TEXT_STYLE_2 = WatermarkStyleParser.PresetStyles.VIDEO_HASSEL_TEXT_STYLE_2
        
        // Realme スタイル
        const val REALME_BRAND_1 = WatermarkStyleParser.PresetStyles.REALME_BRAND_1
        const val REALME_BRAND_2 = WatermarkStyleParser.PresetStyles.REALME_BRAND_2
        const val REALME_BRAND_3 = WatermarkStyleParser.PresetStyles.REALME_BRAND_3
        const val REALME_EXCLUSIVE_MEMORY_1 = WatermarkStyleParser.PresetStyles.REALME_EXCLUSIVE_MEMORY_1
        const val REALME_EXCLUSIVE_MEMORY_2 = WatermarkStyleParser.PresetStyles.REALME_EXCLUSIVE_MEMORY_2
        const val REALME_EXCLUSIVE_MEMORY_3 = WatermarkStyleParser.PresetStyles.REALME_EXCLUSIVE_MEMORY_3
        const val REALME_INSPIRATION_PHOTO_1 = WatermarkStyleParser.PresetStyles.REALME_INSPIRATION_PHOTO_1
        const val REALME_INSPIRATION_PHOTO_2 = WatermarkStyleParser.PresetStyles.REALME_INSPIRATION_PHOTO_2
        const val REALME_INSPIRATION_PHOTO_3 = WatermarkStyleParser.PresetStyles.REALME_INSPIRATION_PHOTO_3
        
        // Spring Festival スタイル
        const val SONGKRAN_HOLIDAY_2025 = WatermarkStyleParser.PresetStyles.SONGKRAN_HOLIDAY_2025
        
        /**
         * すべてのプリセットIDを取得
         */
        fun getAllPresets(): List<String> = WatermarkStyleParser.PresetStyles.getAllStyleIds()
        
        /**
         * カテゴリ別にプリセットを取得
         */
        fun getPresetsByCategory(category: String): List<String> =
            WatermarkStyleParser.PresetStyles.getStylesByCategory(category)
    }
        const val HORIZONTAL_BOTTOM = "style_048_hasselblad_horizontal_bottom_v2"
        const val HORIZONTAL_CENTER = "style_049_hasselblad_horizontal_center_v2"
    }
}
