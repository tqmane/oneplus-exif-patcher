package com.oneplus.exifpatcher.watermark.renderer

import android.content.Context
import android.graphics.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.oneplus.exifpatcher.watermark.model.CameraInfo
import com.oneplus.exifpatcher.watermark.model.TextSource
import com.oneplus.exifpatcher.watermark.model.WatermarkElement
import com.oneplus.exifpatcher.watermark.model.WatermarkStyle

/**
 * Hasselbladウォーターマークを画像に描画するレンダラー
 */
class HasselbladWatermarkRenderer(private val context: Context) {
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fontCache = mutableMapOf<String, Typeface>()
    
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
        // 元画像のコピーを作成（変更しないように）
        val resultBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        
        // スケール計算（スタイル定義は固定サイズなので画像サイズに合わせる）
        val scale = calculateScale(resultBitmap.width, resultBitmap.height, style)
        
        // 各要素を描画
        for (element in style.elements) {
            when (element.type) {
                "text" -> renderText(canvas, element, cameraInfo, scale)
                "image" -> renderImage(canvas, element, scale)
                "shape" -> renderShape(canvas, element, scale)
            }
        }
        
        return resultBitmap
    }
    
    /**
     * スケール計算
     */
    private fun calculateScale(imageWidth: Int, imageHeight: Int, style: WatermarkStyle): Float {
        // 画像の短辺を基準にスケールを計算
        val baseSize = minOf(imageWidth, imageHeight)
        val styleBaseSize = if (style.orientation == 0) style.height else style.width
        
        // ウォーターマークサイズは画像の10%程度に調整
        return (baseSize * 0.1f) / styleBaseSize
    }
    
    /**
     * テキスト要素を描画
     */
    private fun renderText(
        canvas: Canvas,
        element: WatermarkElement,
        cameraInfo: CameraInfo,
        scale: Float
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
        val x = element.x * scale
        val y = element.y * scale + paint.textSize  // ベースライン調整
        
        // 描画
        canvas.drawText(text, x, y, paint)
    }
    
    /**
     * 画像要素を描画
     */
    private fun renderImage(
        canvas: Canvas,
        element: WatermarkElement,
        scale: Float
    ) {
        val bitmap = loadBitmap(element.bitmap ?: return) ?: return
        
        // 位置とサイズ計算
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = RectF(
            element.x * scale,
            element.y * scale,
            (element.x + element.width) * scale,
            (element.y + element.height) * scale
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
        scale: Float
    ) {
        // 位置とサイズ計算
        val rect = RectF(
            element.x * scale,
            element.y * scale,
            (element.x + element.width) * scale,
            (element.y + element.height) * scale
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
        val cacheKey = "${fontFamily}_$fontWeight"
        
        return fontCache.getOrPut(cacheKey) {
            try {
                // assetsフォルダからカスタムフォントを読み込み
                when (fontFamily?.lowercase()) {
                    "aveNextMedium.ttc" -> "fonts/AvenirNext.ttc"
                    "butlerbold.otf" -> "fonts/ButlerBold.otf"
                    "butlermedium.otf" -> "fonts/ButlerMedium.otf"
                    "fzcysk.ttf" -> "fonts/FZCYSK.TTF"
                    else -> null
                }?.let { fontPath ->
                    Typeface.createFromAsset(context.assets, fontPath)
                } ?: getSystemTypeface(fontWeight)
            } catch (e: Exception) {
                e.printStackTrace()
                getSystemTypeface(fontWeight)
            }
        }
    }
    
    /**
     * システムフォントを取得
     */
    private fun getSystemTypeface(fontWeight: String?): Typeface {
        return when (fontWeight?.lowercase()) {
            "bold" -> Typeface.DEFAULT_BOLD
            else -> Typeface.DEFAULT
        }
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
}
