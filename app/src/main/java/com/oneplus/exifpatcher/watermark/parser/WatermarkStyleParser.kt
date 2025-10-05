package com.oneplus.exifpatcher.watermark.parser

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oneplus.exifpatcher.watermark.model.WatermarkStyle
import java.io.InputStreamReader

/**
 * Hasselbladウォーターマークスタイル定義を読み込むパーサー
 */
class WatermarkStyleParser(private val context: Context) {
    
    private val gson = Gson()
    
    /**
     * assetsフォルダからJSONスタイル定義を読み込む
     * 
     * @param fileName JSONファイル名（例: "style_042_hasselblad_landscape_v2.json"）
     * @return WatermarkStyleオブジェクト
     */
    fun loadStyleFromAssets(fileName: String): WatermarkStyle? {
        return try {
            val inputStream = context.assets.open("watermark/$fileName")
            val reader = InputStreamReader(inputStream)
            val style = gson.fromJson(reader, WatermarkStyle::class.java)
            reader.close()
            inputStream.close()
            style
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * すべてのプリセットスタイルを読み込む
     * 
     * @return スタイルIDをキーとするMap
     */
    fun loadAllStyles(): Map<String, WatermarkStyle> {
        val styles = mutableMapOf<String, WatermarkStyle>()
        
        try {
            // assetsフォルダからHasselblad関連のJSONファイルを検索
            val watermarkFiles = context.assets.list("watermark") ?: emptyArray()
            
            for (fileName in watermarkFiles) {
                if (fileName.startsWith("style_") && fileName.endsWith(".json")) {
                    loadStyleFromAssets(fileName)?.let { style ->
                        styles[style.id] = style
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return styles
    }
    
    /**
     * プリセットスタイルID
     */
    object PresetStyles {
        const val HASSELBLAD_LANDSCAPE = "style_042_hasselblad_landscape_v2"
        const val HASSELBLAD_LANDSCAPE_RIGHT = "style_043_hasselblad_landscape_right_v2"
        const val HASSELBLAD_VERTICAL_TOP = "style_044_hasselblad_vertical_top_v2"
        const val HASSELBLAD_VERTICAL_BOTTOM = "style_045_hasselblad_vertical_bottom_v2"
        const val HASSELBLAD_VERTICAL_CENTER = "style_046_hasselblad_vertical_center_v2"
        const val HASSELBLAD_HORIZONTAL_TOP = "style_047_hasselblad_horizontal_top_v2"
        const val HASSELBLAD_HORIZONTAL_BOTTOM = "style_048_hasselblad_horizontal_bottom_v2"
        const val HASSELBLAD_HORIZONTAL_CENTER = "style_049_hasselblad_horizontal_center_v2"
    }
    
    companion object {
        /**
         * デフォルトスタイル（JSONがない場合のフォールバック）
         */
        fun createDefaultStyle(): WatermarkStyle {
            return WatermarkStyle(
                id = "default_hasselblad",
                name = "Default Hasselblad",
                width = 1080f,
                height = 200f,
                orientation = 0,
                elements = listOf(
                    // Hasselbladロゴ
                    com.oneplus.exifpatcher.watermark.model.WatermarkElement(
                        type = "image",
                        x = 50f,
                        y = 50f,
                        width = 150f,
                        height = 50f,
                        bitmap = "hasselblad_watermark_dark"
                    ),
                    // カメラ情報テキスト
                    com.oneplus.exifpatcher.watermark.model.WatermarkElement(
                        type = "text",
                        x = 220f,
                        y = 60f,
                        width = 500f,
                        height = 40f,
                        textSource = 1,  // CAMERA_INFO
                        fontFamily = "sans-serif",
                        fontSize = 24f,
                        fontWeight = "normal",
                        textAlign = "left",
                        color = "#FFFFFF",
                        alpha = 0.9f
                    )
                )
            )
        }
    }
}
