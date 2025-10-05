package com.oneplus.exifpatcher.watermark.parser

import android.content.Context
import com.google.gson.Gson
import com.oneplus.exifpatcher.watermark.model.WatermarkStyle
import java.util.Locale

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
            context.assets.open("watermark/$fileName").use { inputStream ->
                val json = inputStream.bufferedReader().use { it.readText() }
                parseModernStyle(json)
                    ?: parseLegacyStyle(json, fileName.removeSuffix(".json"))
                    ?: createDefaultStyle()
            }
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

    private fun parseModernStyle(json: String): WatermarkStyle? {
        return try {
            gson.fromJson(json, WatermarkStyle::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLegacyStyle(json: String, fallbackId: String): WatermarkStyle? {
        return try {
            val rawStyle = gson.fromJson(json, RawWatermarkStyle::class.java)
            convertLegacyStyle(rawStyle, fallbackId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun convertLegacyStyle(raw: RawWatermarkStyle, fallbackId: String): WatermarkStyle? {
        val baseSize = raw.baseImageSize ?: 360f
        val legacyElements = mutableListOf<LegacyRenderableElement>()
        val bitmaps = raw.bitmaps.orEmpty()

        if (bitmaps.isEmpty()) {
            return null
        }

        bitmaps.forEach { bitmap ->
            val containerWidth = bitmap.width?.takeIf { it > 0f }
                ?: (baseSize - (raw.size?.totalHorizontalMargin() ?: 0f))
            val containerHeight = bitmap.height?.takeIf { it > 0f }
                ?: maxOf(
                    raw.size?.bottomMargin ?: 0f,
                    raw.size?.topMargin ?: 0f,
                    baseSize * 0.25f
                )
            val effectiveWidth = containerWidth.coerceAtLeast(baseSize * 0.3f)
            val effectiveHeight = containerHeight.coerceAtLeast(baseSize * 0.15f)

            val context = LegacyLayoutContext(
                originX = raw.size?.leftMargin ?: 0f,
                originY = raw.size?.topMargin ?: 0f,
                width = effectiveWidth,
                height = effectiveHeight,
                orientation = bitmap.orientation ?: 0
            )

            processLegacyElements(bitmap.elements.orEmpty(), context, legacyElements)
        }

        if (legacyElements.isEmpty()) {
            return null
        }

        val minX = legacyElements.minOf { it.x }
        val minY = legacyElements.minOf { it.y }
        val maxX = legacyElements.maxOf { it.x + it.width }
        val maxY = legacyElements.maxOf { it.y + it.height }

        val normalizedElements = legacyElements.map {
            it.toWatermarkElement(it.x - minX, it.y - minY)
        }

        val styleWidth = (maxX - minX).coerceAtLeast(1f)
        val styleHeight = (maxY - minY).coerceAtLeast(1f)

        return WatermarkStyle(
            id = raw.styleId ?: fallbackId,
            name = raw.styleName ?: raw.name ?: raw.styleId ?: fallbackId,
            width = styleWidth,
            height = styleHeight,
            orientation = bitmaps.firstOrNull()?.orientation ?: 0,
            elements = normalizedElements
        )
    }

    private fun processLegacyElements(
        elements: List<RawElement>,
        context: LegacyLayoutContext,
        output: MutableList<LegacyRenderableElement>
    ) {
        var cursorX = 0f
        var cursorY = 0f

        elements.forEach { element ->
            if (element.visible == false) {
                return@forEach
            }

            val content = element.content ?: return@forEach
            val position = element.position
            val leftMargin = position?.leftMargin ?: 0f
            val topMargin = position?.topMargin ?: 0f
            val rightMargin = position?.rightMargin ?: 0f
            val bottomMargin = position?.bottomMargin ?: 0f
            val gravity = position?.layoutGravity?.lowercase(Locale.ROOT) ?: ""

            val elementWidth = computeElementWidth(content, element.paint, element.elements, context.width)
            val elementHeight = computeElementHeight(content, element.paint, element.elements, context.height)

            var x = context.originX + cursorX + leftMargin
            var y = context.originY + cursorY + topMargin

            if (gravity.contains("right")) {
                x = context.originX + context.width - elementWidth - rightMargin
            } else if (gravity.contains("center") && !gravity.contains("vertical")) {
                x = context.originX + (context.width - elementWidth) / 2f
            }

            if (gravity.contains("bottom")) {
                y = context.originY + context.height - elementHeight - bottomMargin
            } else if (gravity.contains("verticalcenter") || (gravity.contains("center") && gravity.contains("vertical"))) {
                y = context.originY + (context.height - elementHeight) / 2f
            }

            if (content.type == "elements" && !element.elements.isNullOrEmpty()) {
                val childContext = LegacyLayoutContext(
                    originX = x,
                    originY = y,
                    width = if (elementWidth > 0f) elementWidth else context.width - leftMargin - rightMargin,
                    height = if (elementHeight > 0f) elementHeight else context.height - topMargin - bottomMargin,
                    orientation = content.orientation ?: context.orientation
                )
                processLegacyElements(element.elements, childContext, output)
            } else if (content.type == "space") {
                // 空白要素はレイアウト計算にのみ使用
            } else {
                createRenderableElement(content, element.paint, x, y, elementWidth, elementHeight)?.let {
                    output += it
                }
            }

            if (context.orientation == 0) {
                cursorX += leftMargin + elementWidth + rightMargin
            } else {
                cursorY += topMargin + elementHeight + bottomMargin
            }
        }
    }

    private fun computeElementWidth(
        content: RawContent,
        paint: RawPaint?,
        children: List<RawElement>?,
        fallbackWidth: Float
    ): Float {
        content.width?.takeIf { it > 0f }?.let { return it }
        content.diameter?.takeIf { it > 0f }?.let { return it }

        return when (content.type) {
            "elements" -> {
                val orientation = content.orientation ?: 0
                val childWidths = children.orEmpty().map { child ->
                    val childContent = child.content
                    if (childContent != null) computeElementWidth(childContent, child.paint, child.elements, fallbackWidth / 2f) else 0f
                }
                if (orientation == 0) childWidths.sum() else childWidths.maxOrNull() ?: fallbackWidth
            }
            "image" -> fallbackWidth * 0.35f
            "text" -> (paint?.textSize ?: fallbackWidth * 0.15f) * 6f
            "shape" -> fallbackWidth * 0.1f
            else -> fallbackWidth * 0.3f
        }
    }

    private fun computeElementHeight(
        content: RawContent,
        paint: RawPaint?,
        children: List<RawElement>?,
        fallbackHeight: Float
    ): Float {
        content.height?.takeIf { it > 0f }?.let { return it }
        content.diameter?.takeIf { it > 0f }?.let { return it }

        return when (content.type) {
            "elements" -> {
                val orientation = content.orientation ?: 1
                val childHeights = children.orEmpty().map { child ->
                    val childContent = child.content
                    if (childContent != null) computeElementHeight(childContent, child.paint, child.elements, fallbackHeight / 2f) else 0f
                }
                if (orientation == 0) childHeights.maxOrNull() ?: fallbackHeight else childHeights.sum()
            }
            "image" -> fallbackHeight * 0.3f
            "text" -> paint?.textSize ?: fallbackHeight * 0.25f
            "shape" -> fallbackHeight * 0.1f
            else -> fallbackHeight * 0.2f
        }
    }

    private fun createRenderableElement(
        content: RawContent,
        paint: RawPaint?,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ): LegacyRenderableElement? {
        val type = content.type ?: return null
        val fontWeight = when ((paint?.fontWeight ?: 400)) {
            in 600..1000 -> "bold"
            else -> "normal"
        }
        val alpha = paint?.alpha ?: paint?.lightAlpha ?: 1f
        val primaryColor = paint?.colors?.firstOrNull() ?: paint?.lightColor ?: "#FFFFFF"

        return when (type) {
            "text" -> LegacyRenderableElement(
                type = "text",
                x = x,
                y = y,
                width = width,
                height = height,
                text = content.text,
                textSource = content.textSource,
                fontFamily = paint?.fontName ?: paint?.font,
                fontSize = paint?.textSize,
                fontWeight = fontWeight,
                textAlign = null,
                color = primaryColor,
                alpha = alpha
            )
            "image" -> LegacyRenderableElement(
                type = "image",
                x = x,
                y = y,
                width = width,
                height = height,
                bitmap = content.bitmapResName ?: content.bitmap,
                alpha = alpha
            )
            "shape" -> LegacyRenderableElement(
                type = "shape",
                x = x,
                y = y,
                width = width,
                height = height,
                shape = content.shape ?: "rectangle",
                fillColor = primaryColor,
                strokeColor = paint?.darkColor
            )
            else -> null
        }
    }

    private data class RawWatermarkStyle(
        @com.google.gson.annotations.SerializedName("styleId") val styleId: String? = null,
        @com.google.gson.annotations.SerializedName("styleName") val styleName: String? = null,
        @com.google.gson.annotations.SerializedName("name") val name: String? = null,
        @com.google.gson.annotations.SerializedName("baseImageSize") val baseImageSize: Float? = null,
        @com.google.gson.annotations.SerializedName("size") val size: RawStyleSize? = null,
        @com.google.gson.annotations.SerializedName("imageOffset") val imageOffset: RawImageOffset? = null,
        @com.google.gson.annotations.SerializedName("background") val background: RawBackground? = null,
        @com.google.gson.annotations.SerializedName("bitmaps") val bitmaps: List<RawBitmap>? = null
    )

    private data class RawStyleSize(
        @com.google.gson.annotations.SerializedName("leftMargin") val leftMargin: Float? = 0f,
        @com.google.gson.annotations.SerializedName("topMargin") val topMargin: Float? = 0f,
        @com.google.gson.annotations.SerializedName("rightMargin") val rightMargin: Float? = 0f,
        @com.google.gson.annotations.SerializedName("bottomMargin") val bottomMargin: Float? = 0f
    )

    private data class RawImageOffset(
        @com.google.gson.annotations.SerializedName("startX") val startX: Float? = 0f,
        @com.google.gson.annotations.SerializedName("startY") val startY: Float? = 0f
    )

    private data class RawBackground(
        @com.google.gson.annotations.SerializedName("backgroundType") val backgroundType: Int? = null,
        @com.google.gson.annotations.SerializedName("color") val color: String? = null
    )

    private data class RawBitmap(
        @com.google.gson.annotations.SerializedName("direction") val direction: Int? = null,
        @com.google.gson.annotations.SerializedName("orientation") val orientation: Int? = null,
        @com.google.gson.annotations.SerializedName("position") val position: RawPosition? = null,
        @com.google.gson.annotations.SerializedName("width") val width: Float? = null,
        @com.google.gson.annotations.SerializedName("height") val height: Float? = null,
        @com.google.gson.annotations.SerializedName("elements") val elements: List<RawElement>? = null
    )

    private data class RawElement(
        @com.google.gson.annotations.SerializedName("id") val id: Int? = null,
        @com.google.gson.annotations.SerializedName("visible") val visible: Boolean? = true,
        @com.google.gson.annotations.SerializedName("editable") val editable: Boolean? = false,
        @com.google.gson.annotations.SerializedName("content") val content: RawContent? = null,
        @com.google.gson.annotations.SerializedName("position") val position: RawPosition? = null,
        @com.google.gson.annotations.SerializedName("paint") val paint: RawPaint? = null,
        @com.google.gson.annotations.SerializedName("elements") val elements: List<RawElement>? = null,
        @com.google.gson.annotations.SerializedName("layoutWeight") val layoutWeight: Float? = null,
        @com.google.gson.annotations.SerializedName("spaceUse") val spaceUse: String? = null
    )

    private data class RawContent(
        @com.google.gson.annotations.SerializedName("type") val type: String? = null,
        @com.google.gson.annotations.SerializedName("orientation") val orientation: Int? = null,
        @com.google.gson.annotations.SerializedName("width") val width: Float? = null,
        @com.google.gson.annotations.SerializedName("height") val height: Float? = null,
        @com.google.gson.annotations.SerializedName("bitmap") val bitmap: String? = null,
        @com.google.gson.annotations.SerializedName("bitmapResName") val bitmapResName: String? = null,
        @com.google.gson.annotations.SerializedName("scaleType") val scaleType: Int? = null,
        @com.google.gson.annotations.SerializedName("text") val text: String? = null,
        @com.google.gson.annotations.SerializedName("textSource") val textSource: Int? = null,
        @com.google.gson.annotations.SerializedName("shape") val shape: String? = null,
        @com.google.gson.annotations.SerializedName("diameter") val diameter: Float? = null,
        @com.google.gson.annotations.SerializedName("modelSuffix") val modelSuffix: String? = null,
        @com.google.gson.annotations.SerializedName("isExtendModel") val isExtendModel: Boolean? = null
    )

    private data class RawPosition(
        @com.google.gson.annotations.SerializedName("layoutGravity") val layoutGravity: String? = null,
        @com.google.gson.annotations.SerializedName("layoutGravityEnable") val layoutGravityEnable: Boolean? = null,
        @com.google.gson.annotations.SerializedName("leftMargin") val leftMargin: Float? = 0f,
        @com.google.gson.annotations.SerializedName("topMargin") val topMargin: Float? = 0f,
        @com.google.gson.annotations.SerializedName("rightMargin") val rightMargin: Float? = 0f,
        @com.google.gson.annotations.SerializedName("bottomMargin") val bottomMargin: Float? = 0f
    )

    private data class RawPaint(
        @com.google.gson.annotations.SerializedName("fontType") val fontType: Int? = null,
        @com.google.gson.annotations.SerializedName("fontName") val fontName: String? = null,
        @com.google.gson.annotations.SerializedName("font") val font: String? = null,
        @com.google.gson.annotations.SerializedName("fontFileType") val fontFileType: Int? = null,
        @com.google.gson.annotations.SerializedName("fontWeight") val fontWeight: Int? = null,
        @com.google.gson.annotations.SerializedName("textSize") val textSize: Float? = null,
        @com.google.gson.annotations.SerializedName("letterSpacing") val letterSpacing: Float? = null,
        @com.google.gson.annotations.SerializedName("lineHeight") val lineHeight: Float? = null,
        @com.google.gson.annotations.SerializedName("alpha") val alpha: Float? = null,
        @com.google.gson.annotations.SerializedName("lightAlpha") val lightAlpha: Float? = null,
        @com.google.gson.annotations.SerializedName("gradientType") val gradientType: Int? = null,
        @com.google.gson.annotations.SerializedName("colors") val colors: List<String>? = null,
        @com.google.gson.annotations.SerializedName("lightColor") val lightColor: String? = null,
        @com.google.gson.annotations.SerializedName("darkColor") val darkColor: String? = null,
        @com.google.gson.annotations.SerializedName("colorPositions") val colorPositions: List<Float>? = null
    )

    private data class LegacyLayoutContext(
        val originX: Float,
        val originY: Float,
        val width: Float,
        val height: Float,
        val orientation: Int
    )

    private data class LegacyRenderableElement(
        val type: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val text: String? = null,
        val textSource: Int? = null,
        val fontFamily: String? = null,
        val fontSize: Float? = null,
        val fontWeight: String? = null,
        val textAlign: String? = null,
        val color: String? = null,
        val alpha: Float? = null,
        val bitmap: String? = null,
        val shape: String? = null,
        val fillColor: String? = null,
        val strokeColor: String? = null,
        val strokeWidth: Float? = null,
        val cornerRadius: Float? = null,
        val rotation: Float? = null
    ) {
        fun toWatermarkElement(normalizedX: Float, normalizedY: Float): com.oneplus.exifpatcher.watermark.model.WatermarkElement {
            return com.oneplus.exifpatcher.watermark.model.WatermarkElement(
                type = type,
                x = normalizedX,
                y = normalizedY,
                width = width,
                height = height,
                text = text,
                textSource = textSource,
                fontFamily = fontFamily,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign,
                color = color,
                alpha = alpha,
                bitmap = bitmap,
                shape = shape,
                fillColor = fillColor,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius,
                rotation = rotation
            )
        }
    }

    private fun RawStyleSize.totalHorizontalMargin(): Float {
        return (leftMargin ?: 0f) + (rightMargin ?: 0f)
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
