package com.oneplus.exifpatcher.watermark.model

import com.google.gson.annotations.SerializedName

/**
 * Hasselbladウォーターマークのスタイル定義
 */
data class WatermarkStyle(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("elements")
    val elements: List<WatermarkElement> = emptyList(),

    @SerializedName("width")
    val width: Float = 0f,

    @SerializedName("height")
    val height: Float = 0f,

    @SerializedName("orientation")
    val orientation: Int = 0  // 0=横, 1=縦
)

/**
 * ウォーターマークの個別要素（テキスト、画像、図形）
 */
data class WatermarkElement(
    @SerializedName("type")
    val type: String,  // "text", "image", "shape"
    
    // 位置
    @SerializedName("x")
    val x: Float,
    
    @SerializedName("y")
    val y: Float,
    
    @SerializedName("width")
    val width: Float,
    
    @SerializedName("height")
    val height: Float,
    
    // テキスト要素用
    @SerializedName("text")
    val text: String? = null,
    
    @SerializedName("textSource")
    val textSource: Int? = null,  // 0=デバイス名, 1=カメラ情報
    
    @SerializedName("fontFamily")
    val fontFamily: String? = null,
    
    @SerializedName("fontSize")
    val fontSize: Float? = null,
    
    @SerializedName("fontWeight")
    val fontWeight: String? = null,  // "normal", "bold"
    
    @SerializedName("textAlign")
    val textAlign: String? = null,  // "left", "center", "right"
    
    @SerializedName("color")
    val color: String? = null,  // "#FFFFFF"
    
    @SerializedName("alpha")
    val alpha: Float? = 1.0f,
    
    // 画像要素用
    @SerializedName("bitmap")
    val bitmap: String? = null,  // リソース名
    
    // 図形要素用
    @SerializedName("shape")
    val shape: String? = null,  // "rectangle", "circle"
    
    @SerializedName("fillColor")
    val fillColor: String? = null,
    
    @SerializedName("strokeColor")
    val strokeColor: String? = null,
    
    @SerializedName("strokeWidth")
    val strokeWidth: Float? = null,
    
    @SerializedName("cornerRadius")
    val cornerRadius: Float? = null,
    
    // レイアウト
    @SerializedName("rotation")
    val rotation: Float? = 0f,
    
    @SerializedName("paddingLeft")
    val paddingLeft: Float? = 0f,
    
    @SerializedName("paddingTop")
    val paddingTop: Float? = 0f,
    
    @SerializedName("paddingRight")
    val paddingRight: Float? = 0f,
    
    @SerializedName("paddingBottom")
    val paddingBottom: Float? = 0f
)

/**
 * テキストソースの定数
 */
object TextSource {
    const val DEVICE_NAME = 0  // デバイス名（例: "OnePlus 9 Pro"）
    const val CAMERA_INFO = 1  // カメラ情報（例: "f/1.8 1/100s ISO400"）
    const val HASSELBLAD = 2   // "HASSELBLAD"固定テキスト
}

/**
 * カメラ情報データ
 */
data class CameraInfo(
    val aperture: String? = null,      // 絞り値 (例: "f/1.8")
    val shutterSpeed: String? = null,  // シャッタースピード (例: "1/100")
    val iso: String? = null,           // ISO感度 (例: "ISO400")
    val focalLength: String? = null,   // 焦点距離 (例: "24mm")
    val deviceName: String? = null     // デバイス名 (例: "OnePlus 9 Pro")
) {
    /**
     * カメラ情報を文字列として整形
     * 例: "f/1.8  1/100s  ISO400"
     */
    fun format(): String {
        val parts = mutableListOf<String>()
        aperture?.let { parts.add(it) }
        shutterSpeed?.let { parts.add("${it}s") }
        iso?.let { parts.add(it) }
        focalLength?.let { parts.add(it) }
        
        return parts.joinToString("  ")
    }
}
