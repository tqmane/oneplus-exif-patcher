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
            // assetsフォルダからすべてのJSONファイルを検索
            val watermarkFiles = context.assets.list("watermark") ?: emptyArray()
            
            for (fileName in watermarkFiles) {
                if (fileName.endsWith(".json")) {
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
     * プリセットスタイルID - 全42種類
     */
    object PresetStyles {
        // Hasselblad スタイル (6種類)
        const val HASSEL_STYLE_1 = "hassel_style_1"
        const val HASSEL_STYLE_2 = "hassel_style_2"
        const val HASSEL_STYLE_3 = "hassel_style_3"
        const val HASSEL_STYLE_4 = "hassel_style_4"
        const val HASSEL_TEXT_STYLE_1 = "hassel_text_style_1"
        const val HASSEL_TEXT_STYLE_2 = "hassel_text_style_2"
        
        // Brand スタイル (6種類)
        const val BRAND_COLOR_OS = "personalize_brand_1"
        const val BRAND_FIND_X = "personalize_brand_2"
        const val BRAND_IMAGINE = "personalize_brand_3"
        const val BRAND_BREAK_THROUGH = "personalize_brand_5"
        const val BRAND_LUMO_IMAGE = "personalize_brand_6"
        
        // Film スタイル (4種類)
        const val FILM_STYLE_1 = "personalize_film_1"
        const val FILM_STYLE_2 = "personalize_film_2"
        const val FILM_REALME_1 = "personalize_film_realme_1"
        const val FILM_REALME_2 = "personalize_film_realme_2"
        
        // Master Sign スタイル (4種類)
        const val MASTER_SIGN_1 = "personalize_masterSign_1"
        const val MASTER_SIGN_2 = "personalize_masterSign_2"
        const val MASTER_SIGN_3 = "personalize_masterSign_3"
        const val MASTER_SIGN_4 = "personalize_masterSign_4"
        
        // Retro Camera スタイル (5種類)
        const val RETRO_CAMERA_1 = "personalize_retroCamera_1"
        const val RETRO_CAMERA_2 = "personalize_retroCamera_2"
        const val RETRO_CAMERA_3 = "personalize_retroCamera_3"
        const val RETRO_CAMERA_4 = "personalize_retroCamera_4"
        const val RETRO_CAMERA_5 = "personalize_retroCamera_5"
        
        // Text Style スタイル (6種類)
        const val TEXT_STYLE_1 = "text_style_1"
        const val TEXT_STYLE_2 = "text_style_2"
        const val TEXT_STYLE_3 = "text_style_3"
        const val VIDEO_TEXT_STYLE_1 = "video_text_style_1"
        const val VIDEO_TEXT_STYLE_2 = "video_text_style_2"
        const val VIDEO_TEXT_STYLE_3 = "video_text_style_3"
        
        // Video Hasselblad スタイル (2種類)
        const val VIDEO_HASSEL_TEXT_STYLE_1 = "video_hassel_text_style_1"
        const val VIDEO_HASSEL_TEXT_STYLE_2 = "video_hassel_text_style_2"
        
        // Realme Brand Imprint スタイル (9種類)
        const val REALME_BRAND_1 = "realme_brand_imprint_brand_1"
        const val REALME_BRAND_2 = "realme_brand_imprint_brand_2"
        const val REALME_BRAND_3 = "realme_brand_imprint_brand_3"
        const val REALME_EXCLUSIVE_MEMORY_1 = "realme_brand_imprint_exclusiveMemory_1"
        const val REALME_EXCLUSIVE_MEMORY_2 = "realme_brand_imprint_exclusiveMemory_2"
        const val REALME_EXCLUSIVE_MEMORY_3 = "realme_brand_imprint_exclusiveMemory_3"
        const val REALME_INSPIRATION_PHOTO_1 = "realme_brand_imprint_inspirationPhoto_1"
        const val REALME_INSPIRATION_PHOTO_2 = "realme_brand_imprint_inspirationPhoto_2"
        const val REALME_INSPIRATION_PHOTO_3 = "realme_brand_imprint_inspirationPhoto_3"
        
        // Spring Festival スタイル (1種類)
        const val SONGKRAN_HOLIDAY_2025 = "restrict_local_songkran_holiday_2025_style_1"
        
        /**
         * すべてのスタイルIDを取得
         */
        fun getAllStyleIds(): List<String> = listOf(
            // Hasselblad
            HASSEL_STYLE_1, HASSEL_STYLE_2, HASSEL_STYLE_3, HASSEL_STYLE_4,
            HASSEL_TEXT_STYLE_1, HASSEL_TEXT_STYLE_2,
            // Brand
            BRAND_COLOR_OS, BRAND_FIND_X, BRAND_IMAGINE, BRAND_BREAK_THROUGH, BRAND_LUMO_IMAGE,
            // Film
            FILM_STYLE_1, FILM_STYLE_2, FILM_REALME_1, FILM_REALME_2,
            // Master Sign
            MASTER_SIGN_1, MASTER_SIGN_2, MASTER_SIGN_3, MASTER_SIGN_4,
            // Retro Camera
            RETRO_CAMERA_1, RETRO_CAMERA_2, RETRO_CAMERA_3, RETRO_CAMERA_4, RETRO_CAMERA_5,
            // Text Style
            TEXT_STYLE_1, TEXT_STYLE_2, TEXT_STYLE_3,
            VIDEO_TEXT_STYLE_1, VIDEO_TEXT_STYLE_2, VIDEO_TEXT_STYLE_3,
            // Video Hasselblad
            VIDEO_HASSEL_TEXT_STYLE_1, VIDEO_HASSEL_TEXT_STYLE_2,
            // Realme
            REALME_BRAND_1, REALME_BRAND_2, REALME_BRAND_3,
            REALME_EXCLUSIVE_MEMORY_1, REALME_EXCLUSIVE_MEMORY_2, REALME_EXCLUSIVE_MEMORY_3,
            REALME_INSPIRATION_PHOTO_1, REALME_INSPIRATION_PHOTO_2, REALME_INSPIRATION_PHOTO_3,
            // Spring Festival
            SONGKRAN_HOLIDAY_2025
        )
        
        /**
         * カテゴリ別にスタイルIDを取得
         */
        fun getStylesByCategory(category: String): List<String> = when (category.lowercase()) {
            "hasselblad" -> listOf(
                HASSEL_STYLE_1, HASSEL_STYLE_2, HASSEL_STYLE_3, HASSEL_STYLE_4,
                HASSEL_TEXT_STYLE_1, HASSEL_TEXT_STYLE_2
            )
            "brand" -> listOf(
                BRAND_COLOR_OS, BRAND_FIND_X, BRAND_IMAGINE, BRAND_BREAK_THROUGH, BRAND_LUMO_IMAGE
            )
            "film" -> listOf(
                FILM_STYLE_1, FILM_STYLE_2, FILM_REALME_1, FILM_REALME_2
            )
            "mastersign" -> listOf(
                MASTER_SIGN_1, MASTER_SIGN_2, MASTER_SIGN_3, MASTER_SIGN_4
            )
            "retrocamera" -> listOf(
                RETRO_CAMERA_1, RETRO_CAMERA_2, RETRO_CAMERA_3, RETRO_CAMERA_4, RETRO_CAMERA_5
            )
            "text" -> listOf(
                TEXT_STYLE_1, TEXT_STYLE_2, TEXT_STYLE_3,
                VIDEO_TEXT_STYLE_1, VIDEO_TEXT_STYLE_2, VIDEO_TEXT_STYLE_3
            )
            "video_hasselblad" -> listOf(
                VIDEO_HASSEL_TEXT_STYLE_1, VIDEO_HASSEL_TEXT_STYLE_2
            )
            "realme" -> listOf(
                REALME_BRAND_1, REALME_BRAND_2, REALME_BRAND_3,
                REALME_EXCLUSIVE_MEMORY_1, REALME_EXCLUSIVE_MEMORY_2, REALME_EXCLUSIVE_MEMORY_3,
                REALME_INSPIRATION_PHOTO_1, REALME_INSPIRATION_PHOTO_2, REALME_INSPIRATION_PHOTO_3
            )
            "festival" -> listOf(SONGKRAN_HOLIDAY_2025)
            else -> emptyList()
        }
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
