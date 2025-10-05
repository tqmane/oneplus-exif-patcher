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
     * プリセットスタイルID
     */
    object Presets {
        const val LANDSCAPE = "style_042_hasselblad_landscape_v2"
        const val LANDSCAPE_RIGHT = "style_043_hasselblad_landscape_right_v2"
        const val VERTICAL_TOP = "style_044_hasselblad_vertical_top_v2"
        const val VERTICAL_BOTTOM = "style_045_hasselblad_vertical_bottom_v2"
        const val VERTICAL_CENTER = "style_046_hasselblad_vertical_center_v2"
        const val HORIZONTAL_TOP = "style_047_hasselblad_horizontal_top_v2"
        const val HORIZONTAL_BOTTOM = "style_048_hasselblad_horizontal_bottom_v2"
        const val HORIZONTAL_CENTER = "style_049_hasselblad_horizontal_center_v2"
    }
}
