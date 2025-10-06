package com.oneplus.exifpatcher.watermark.renderer

import android.content.Context
import android.graphics.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.oneplus.exifpatcher.watermark.model.CameraInfo
import com.oneplus.exifpatcher.watermark.model.TextSource
import com.oneplus.exifpatcher.watermark.model.WatermarkElement
import com.oneplus.exifpatcher.watermark.model.WatermarkStyle
import java.util.Locale

/**
 * Hasselbladウォーターマークを画像に描画するレンダラー
 */
class HasselbladWatermarkRenderer(private val context: Context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fontCache = mutableMapOf<String, Typeface>()
    private data class WatermarkLayout(val scale: Float, val originX: Float, val originY: Float)
    private data class FontAsset(val normal: String, val bold: String? = null) {
        fun pathFor(style: Int): String = if (style == Typeface.BOLD) bold ?: normal else normal
    }

    private val fontAliasMap = mapOf(
        "avenirnext" to FontAsset("fonts/AvenirNext.ttc"),
        "avenirnext.ttc" to FontAsset("fonts/AvenirNext.ttc"),
        "avenirnext.zip" to FontAsset("fonts/AvenirNext.ttc"),
        "fonts/avenirnext.ttc" to FontAsset("fonts/AvenirNext.ttc"),
        "fonts/avenirnext.zip" to FontAsset("fonts/AvenirNext.ttc"),
        "avenextmedium.ttc" to FontAsset("fonts/AvenirNext.ttc"),
        "avenextmedium" to FontAsset("fonts/AvenirNext.ttc"),
        "butler" to FontAsset("fonts/ButlerMedium.otf", bold = "fonts/ButlerBold.otf"),
        "butler.zip" to FontAsset("fonts/ButlerMedium.otf", bold = "fonts/ButlerBold.otf"),
        "fonts/butler.zip" to FontAsset("fonts/ButlerMedium.otf", bold = "fonts/ButlerBold.otf"),
        "butlerbold" to FontAsset("fonts/ButlerBold.otf"),
        "bulterbold" to FontAsset("fonts/ButlerBold.otf"),
        "butlermedium" to FontAsset("fonts/ButlerMedium.otf"),
        "fzyasongdb1gbk" to FontAsset("fonts/FZCYSK.TTF"),
        "fzyasongdb1gbk.zip" to FontAsset("fonts/FZCYSK.TTF"),
        "fonts/fzyasongdb1gbk.zip" to FontAsset("fonts/FZCYSK.TTF"),
        "fzcysk" to FontAsset("fonts/FZCYSK.TTF"),
        "radomirtinkovgilroy-medium" to FontAsset("fonts/RadomirTinkovGilroy-Medium.otf"),
        "radomirtinkovgilroymedium" to FontAsset("fonts/RadomirTinkovGilroy-Medium.otf"),
        "radomirtinkovgilroy-medium.otf" to FontAsset("fonts/RadomirTinkovGilroy-Medium.otf"),
        "fonts/radomirtinkovgilroy-medium.otf" to FontAsset("fonts/RadomirTinkovGilroy-Medium.otf"),
        "gilroy" to FontAsset("fonts/RadomirTinkovGilroy-Medium.otf")
    )
    private val systemFontAliasMap = mapOf(
        "oplussans" to "sans-serif-medium"
    )
    
    /**
     * 画像にウォーターマークを描画
     * 
     * @param sourceBitmap 元画像
     * @param style ウォーターマークスタイル
     * @param cameraInfo カメラ情報
     * @return ウォーターマーク付き画像
     */
    fun renderWatermark(
        sourceBitmap: Bitmap,
        style: WatermarkStyle,
        cameraInfo: CameraInfo
    ): Bitmap {
        val bandColor = resolveBandColor(style)
        val bandHeight = resolveBandHeight(style, sourceBitmap.height)

        val resultBitmap = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height + bandHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

        if (bandHeight > 0) {
            val bandTop = sourceBitmap.height.toFloat()
            val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = bandColor
            }
            canvas.drawRect(0f, bandTop, resultBitmap.width.toFloat(), resultBitmap.height.toFloat(), backgroundPaint)
        }

        val layout = calculateLayout(resultBitmap.width, resultBitmap.height, style, bandHeight)

        if (layout.scale <= 0f) {
            return resultBitmap
        }

        val scale = layout.scale
        val originX = layout.originX
        val originY = layout.originY

        // 各要素を描画
        for (element in style.elements) {
            when (element.type) {
                "text" -> renderText(canvas, element, cameraInfo, scale, originX, originY)
                "image" -> renderImage(canvas, element, scale, originX, originY)
                "shape" -> renderShape(canvas, element, scale, originX, originY)
            }
        }

        return resultBitmap
    }

    private fun calculateLayout(
        imageWidth: Int,
        imageHeight: Int,
        style: WatermarkStyle,
        bandHeight: Int
    ): WatermarkLayout {
        val scale = calculateDynamicWidthScale(imageWidth, imageHeight, style)
        if (scale <= 0f) {
            return WatermarkLayout(0f, 0f, 0f)
        }

        val (styleWidth, styleHeight) = resolveStyleSize(style)
        val watermarkWidth = styleWidth * scale
        val watermarkHeight = styleHeight * scale

        val maxWidth = imageWidth * 0.9f
        val targetWidth = watermarkWidth.coerceAtMost(maxWidth)
        val adjustedScale = targetWidth / watermarkWidth

        val horizontalMargin = (imageWidth - targetWidth) / 2f
        val verticalMargin = if (bandHeight > 0) bandHeight * 0.32f else imageHeight * 0.08f

        val originX = horizontalMargin
        val originY = imageHeight - (watermarkHeight * adjustedScale) - verticalMargin

        return WatermarkLayout(scale * adjustedScale, originX, originY)
    }

    /**
     * スケール計算
     */
    private fun calculateDynamicWidthScale(
        imageWidth: Int,
        imageHeight: Int,
        style: WatermarkStyle
    ): Float {
        val (styleWidth, styleHeight) = resolveStyleSize(style)
        if (styleWidth <= 0f || styleHeight <= 0f) {
            return 0f
        }

        val horizontalMargin = (imageWidth * 0.045f).coerceAtLeast(16f)
        val verticalMargin = (imageHeight * 0.035f).coerceAtLeast(12f)
        val availableWidth = (imageWidth - horizontalMargin * 2f).coerceAtLeast(1f)
        val availableHeight = (imageHeight * 0.24f).coerceAtLeast(1f)

        val scaleByWidth = availableWidth / styleWidth
        val scaleByHeight = availableHeight / styleHeight
        return minOf(scaleByWidth, scaleByHeight)
    }

    private fun resolveStyleSize(style: WatermarkStyle): Pair<Float, Float> {
        if (style.width > 0f && style.height > 0f) {
            return style.width to style.height
        }

        val maxX = style.elements.maxOfOrNull { it.x + it.width } ?: style.width
        val minX = style.elements.minOfOrNull { it.x } ?: 0f
        val maxY = style.elements.maxOfOrNull { it.y + it.height } ?: style.height
        val minY = style.elements.minOfOrNull { it.y } ?: 0f

        val resolvedWidth = when {
            style.width > 0f -> style.width
            maxX > minX -> maxX - minX
            else -> style.elements.maxOfOrNull { it.width } ?: style.width
        }.coerceAtLeast(1f)

        val resolvedHeight = when {
            style.height > 0f -> style.height
            maxY > minY -> maxY - minY
            else -> style.elements.maxOfOrNull { it.height } ?: style.height
        }.coerceAtLeast(1f)

        return resolvedWidth to resolvedHeight
    }
    
    /**
     * テキスト要素を描画
     */
    private fun renderText(
        canvas: Canvas,
        element: WatermarkElement,
        cameraInfo: CameraInfo,
        scale: Float,
        originX: Float,
        originY: Float
    ) {
        // テキスト内容を決定
        val text = when (element.textSource) {
            TextSource.DEVICE_NAME -> cameraInfo.deviceName ?: android.os.Build.MODEL
            TextSource.CAMERA_INFO -> cameraInfo.format()
            TextSource.HASSELBLAD -> "HASSELBLAD"
            else -> element.text ?: ""
        }
        
        if (text.isBlank()) return
        
        // ペイント設定
        paint.reset()
        paint.isAntiAlias = true
        paint.color = parseColor(element.color ?: "#FFFFFF")
        paint.alpha = ((element.alpha ?: 1.0f) * 255).toInt()
        paint.textSize = (element.fontSize ?: 24f) * scale
        paint.typeface = loadFont(element.fontFamily, element.fontWeight)
        paint.textAlign = parseTextAlign(element.textAlign)
        
        // 位置計算
        val x = originX + element.x * scale
        val y = originY + element.y * scale + paint.textSize  // ベースライン調整
        
        // 描画
        canvas.drawText(text, x, y, paint)
    }
    
    /**
     * 画像要素を描画
     */
    private fun renderImage(
        canvas: Canvas,
        element: WatermarkElement,
        scale: Float,
        originX: Float,
        originY: Float
    ) {
        val bitmap = loadBitmap(element.bitmap ?: return) ?: return
        
        // 位置とサイズ計算
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = RectF(
            originX + element.x * scale,
            originY + element.y * scale,
            originX + (element.x + element.width) * scale,
            originY + (element.y + element.height) * scale
        )
        
        // ペイント設定
        paint.reset()
        paint.isAntiAlias = true
        paint.alpha = ((element.alpha ?: 1.0f) * 255).toInt()
        
        // 回転が設定されている場合
        if (element.rotation != null && element.rotation != 0f) {
            canvas.save()
            canvas.rotate(
                element.rotation,
                dstRect.centerX(),
                dstRect.centerY()
            )
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
            canvas.restore()
        } else {
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        }
    }
    
    /**
     * 図形要素を描画
     */
    private fun renderShape(
        canvas: Canvas,
        element: WatermarkElement,
        scale: Float,
        originX: Float,
        originY: Float
    ) {
        // 位置とサイズ計算
        val rect = RectF(
            originX + element.x * scale,
            originY + element.y * scale,
            originX + (element.x + element.width) * scale,
            originY + (element.y + element.height) * scale
        )
        
        // 塗りつぶし
        if (element.fillColor != null) {
            paint.reset()
            paint.isAntiAlias = true
            paint.style = Paint.Style.FILL
            paint.color = parseColor(element.fillColor)
            paint.alpha = ((element.alpha ?: 1.0f) * 255).toInt()
            
            when (element.shape) {
                "rectangle" -> {
                    val cornerRadius = (element.cornerRadius ?: 0f) * scale
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                }
                "circle" -> {
                    canvas.drawOval(rect, paint)
                }
            }
        }
        
        // ストローク
        if (element.strokeColor != null && element.strokeWidth != null) {
            paint.reset()
            paint.isAntiAlias = true
            paint.style = Paint.Style.STROKE
            paint.color = parseColor(element.strokeColor)
            paint.strokeWidth = element.strokeWidth * scale
            paint.alpha = ((element.alpha ?: 1.0f) * 255).toInt()
            
            when (element.shape) {
                "rectangle" -> {
                    val cornerRadius = (element.cornerRadius ?: 0f) * scale
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                }
                "circle" -> {
                    canvas.drawOval(rect, paint)
                }
            }
        }
    }
    
    /**
     * カラーコードをパース
     */
    private fun parseColor(colorString: String): Int {
        return try {
            Color.parseColor(colorString)
        } catch (e: Exception) {
            Color.WHITE
        }
    }
    
    /**
     * テキストアラインをパース
     */
    private fun parseTextAlign(align: String?): Paint.Align {
        return when (align?.lowercase()) {
            "center" -> Paint.Align.CENTER
            "right" -> Paint.Align.RIGHT
            else -> Paint.Align.LEFT
        }
    }
    
    /**
     * フォントを読み込み
     */
    private fun loadFont(fontFamily: String?, fontWeight: String?): Typeface {
        val trimmedFamily = fontFamily?.trim()
        val normalizedFamily = trimmedFamily?.lowercase(Locale.ROOT)
        val style = parseTypefaceStyle(fontWeight)
        val cacheKey = "${normalizedFamily}_$style"

        return fontCache.getOrPut(cacheKey) {
            resolveTypefaceFromAssets(trimmedFamily, normalizedFamily, style)?.let { return@getOrPut it }
            resolveSystemTypeface(normalizedFamily, style)?.let { return@getOrPut it }

            if (fontFamily != null && fontFamily.startsWith("/")) {
                runCatching { Typeface.createFromFile(fontFamily) }.getOrNull()?.let { return@getOrPut it }
            }

            getSystemTypeface(style)
        }
    }

    private fun resolveTypefaceFromAssets(
        rawFamily: String?,
        normalizedFamily: String?,
        style: Int
    ): Typeface? {
        rawFamily?.takeIf { assetExists(it) }?.let {
            return runCatching { Typeface.createFromAsset(context.assets, it) }.getOrNull()
        }

        val sanitized = normalizedFamily
            ?.replace(" ", "")
            ?.removePrefix("fonts/")
            ?.removeSuffix(".zip")

        val aliasCandidates = mutableListOf<String?>()
        aliasCandidates += normalizedFamily
        aliasCandidates += sanitized
        sanitized?.let { alias ->
            aliasCandidates += "fonts/$alias"
            if (!alias.endsWith(".ttf") && !alias.endsWith(".otf") && !alias.endsWith(".ttc")) {
                aliasCandidates += "$alias.ttf"
                aliasCandidates += "$alias.otf"
                aliasCandidates += "$alias.ttc"
                aliasCandidates += "fonts/$alias.ttf"
                aliasCandidates += "fonts/$alias.otf"
                aliasCandidates += "fonts/$alias.ttc"
            }
        }

        var assetPath: String? = null
        for (candidate in aliasCandidates) {
            val path = candidate?.let { fontAliasMap[it]?.pathFor(style) }
            if (path != null) {
                assetPath = path
                break
            }
        }

        return assetPath?.takeIf { assetExists(it) }?.let {
            runCatching { Typeface.createFromAsset(context.assets, it) }.getOrNull()
        }
    }

    private fun resolveSystemTypeface(normalizedFamily: String?, style: Int): Typeface? {
        val alias = normalizedFamily?.replace(" ", "") ?: return null

        systemFontAliasMap[alias]?.let { familyName ->
            return Typeface.create(familyName, style)
        }

        return null
    }

    /**
     * システムフォントを取得
     */
    private fun getSystemTypeface(style: Int): Typeface {
        return Typeface.create(Typeface.DEFAULT, style)
    }

    private fun parseTypefaceStyle(fontWeight: String?): Int {
        if (fontWeight.isNullOrBlank()) return Typeface.NORMAL

        val normalized = fontWeight.lowercase(Locale.ROOT)
        if (normalized == "bold") return Typeface.BOLD

        val weightValue = normalized.toIntOrNull()
        if (weightValue != null) {
            return if (weightValue >= 600) Typeface.BOLD else Typeface.NORMAL
        }

        return Typeface.NORMAL
    }

    private fun assetExists(path: String): Boolean {
        if (path.isBlank()) return false
        return runCatching {
            context.assets.open(path).use { }
        }.isSuccess
    }

    /**
     * 画像リソースを読み込み
     */
    private fun loadBitmap(resourceName: String): Bitmap? {
        return try {
            // drawable-nodpiからリソースを読み込み
            val resourceId = context.resources.getIdentifier(
                resourceName,
                "drawable",
                context.packageName
            )
            
            if (resourceId != 0) {
                ContextCompat.getDrawable(context, resourceId)?.toBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resolveBandColor(style: WatermarkStyle): Int {
        val colorString = style.bottomBackgroundColor
        if (!colorString.isNullOrBlank()) {
            runCatching { Color.parseColor(colorString) }.getOrNull()?.let { return it }
        }

        return if (style.id.contains("dark", ignoreCase = true)) {
            Color.parseColor("#101010")
        } else {
            Color.WHITE
        }
    }

    private fun resolveBandHeight(style: WatermarkStyle, imageHeight: Int): Int {
        val baseRatio = if (style.id.contains("hassel", ignoreCase = true)) 0.18f else 0.12f
        val ratio = style.offsetY?.takeIf { it > 0f }?.let { offset ->
            (offset / 360f).coerceIn(0.05f, 0.25f)
        } ?: baseRatio
        return (imageHeight * ratio).toInt()
    }
}
