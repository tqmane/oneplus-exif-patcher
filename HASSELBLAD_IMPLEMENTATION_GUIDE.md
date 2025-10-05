# Hasselblad透かし実装 - クイックスタートガイド

## 🎯 目標

OnePlus GalleryのHasselblad透かし機能を、あなたのEXIFパッチャーアプリに統合します。

## ✅ 準備完了項目

すでに以下のリソースが利用可能です：

### 1. JSONスタイル定義 ✅
```
photo/gallery_extracted/assets/watermark_master_styles/
├── hassel_style_1.json (7,265 bytes)
├── hassel_style_2.json (3,820 bytes)
├── hassel_style_3.json (4,031 bytes)
├── hassel_style_4.json (18,244 bytes)
├── hassel_text_style_1.json (27,546 bytes)
└── hassel_text_style_2.json (10,331 bytes)
```

### 2. フォントファイル ✅
```
photo/1_data_data_extracted/files/watermark_master/materia/
├── AvenirNext.ttc (4.8 MB) - メインフォント
├── ButlerBold.otf (42 KB) - 太字
├── ButlerMedium.otf (44 KB) - ミディアム
└── FZCYSK.TTF (10.0 MB) - 中国語
```

### 3. ネイティブライブラリの情報 ✅
```
liblocal_ai_master_watermark_drawer.so (315 KB)
実装クラス: com.oplus.tbluniformeditor.plugins.watermark.function.WatermarkAlgoImpl
```

## ⚠️ 未完了項目

### 画像リソース（要抽出）

APKから以下の4つの画像を抽出する必要があります：

1. `hasselblad_watermark_dark` - メインロゴ
2. `hassel_watermark_h_logo_dark` - Hロゴ
3. `hassel_watermark_h_logo_text_style` - Hロゴ（テキストスタイル）
4. `hasselblad_watermark_text_style` - Hasselbladロゴ（テキストスタイル）

**抽出方法**: `photo/EXTRACT_RESOURCES_GUIDE.md` を参照

## 🚀 実装手順

### Phase 1: リソースの準備

#### Step 1.1: 画像リソースの抽出
```powershell
# JADX-GUIを使ってAPKを開く
# https://github.com/skylot/jadx/releases からダウンロード

jadx-gui.bat
# File → Open → photo/com.oneplus.gallery_15.73.22.apk
# Resources → 検索: "hasselblad"
```

#### Step 1.2: リソースをアプリに配置
```
app/src/main/assets/
├── watermark_styles/
│   ├── hassel_style_1.json
│   ├── hassel_style_2.json
│   └── ...
├── watermark_fonts/
│   ├── AvenirNext.ttc
│   ├── ButlerBold.otf
│   └── ButlerMedium.otf
└── watermark_images/
    ├── hasselblad_watermark_dark.webp
    ├── hassel_h_logo_dark.webp
    └── ...
```

### Phase 2: Kotlin/Java実装

#### Step 2.1: データクラスの作成

```kotlin
// WatermarkStyle.kt
data class WatermarkStyle(
    val styleId: String,
    val baseImageSize: Int,
    val size: StyleSize,
    val background: StyleBackground,
    val bitmaps: List<BitmapElement>
)

data class StyleSize(
    val leftMargin: Int = 0,
    val topMargin: Int = 0,
    val rightMargin: Int = 0,
    val bottomMargin: Int = 0
)

data class StyleBackground(
    val backgroundType: Int = 0,
    val color: String = "#FFFFFF"
)

data class BitmapElement(
    val direction: Int,
    val orientation: Int,
    val width: Int,
    val height: Int,
    val elements: List<Element>
)

data class Element(
    val id: Int,
    val visible: Boolean = true,
    val editable: Boolean = false,
    val content: ElementContent,
    val position: ElementPosition,
    val paint: ElementPaint? = null
)

data class ElementContent(
    val type: String, // "text", "image", "shape", "space", "elements"
    val textSource: Int? = null, // 0=device model, 1=camera info
    val bitmap: String? = null,
    val bitmapResName: String? = null,
    val width: Float? = null,
    val height: Float? = null,
    val orientation: Int? = null,
    val elements: List<Element>? = null
)

data class ElementPosition(
    val layoutGravity: String? = null,
    val layoutGravityEnable: Boolean = false,
    val leftMargin: Float = 0f,
    val topMargin: Float = 0f,
    val rightMargin: Float = 0f,
    val bottomMargin: Float = 0f
)

data class ElementPaint(
    val fontType: Int = 0,
    val fontName: String? = null,
    val font: String? = null,
    val fontFileType: Int? = null,
    val ttcIndex: Int? = null,
    val fontWeight: Int = 400,
    val textSize: Float,
    val letterSpacing: Float = 0f,
    val lineHeight: Float? = null,
    val alpha: Float = 1f,
    val colors: List<String>,
    val gradientType: Int = -1
)
```

#### Step 2.2: JSONパーサーの実装

```kotlin
// WatermarkStyleParser.kt
import com.google.gson.Gson
import java.io.InputStreamReader

class WatermarkStyleParser(private val context: Context) {
    private val gson = Gson()
    
    fun loadStyle(styleId: String): WatermarkStyle? {
        return try {
            val inputStream = context.assets.open("watermark_styles/$styleId.json")
            val reader = InputStreamReader(inputStream)
            gson.fromJson(reader, WatermarkStyle::class.java)
        } catch (e: Exception) {
            Log.e("WatermarkParser", "Failed to load style: $styleId", e)
            null
        }
    }
    
    fun getAllStyles(): List<String> {
        return try {
            context.assets.list("watermark_styles")
                ?.filter { it.endsWith(".json") }
                ?.map { it.removeSuffix(".json") }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
```

#### Step 2.3: 透かしレンダラーの実装

```kotlin
// HasselbladWatermarkRenderer.kt
import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.exifinterface.media.ExifInterface

class HasselbladWatermarkRenderer(private val context: Context) {
    private val styleParser = WatermarkStyleParser(context)
    private val fontCache = mutableMapOf<String, Typeface>()
    private val imageCache = mutableMapOf<String, Bitmap>()
    
    fun addWatermark(
        sourceBitmap: Bitmap,
        styleId: String,
        exifInterface: ExifInterface? = null
    ): Bitmap {
        val style = styleParser.loadStyle(styleId) ?: return sourceBitmap
        
        // 透かし用のキャンバスを作成
        val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // デバイス情報とカメラ情報を取得
        val deviceModel = Build.MODEL
        val cameraInfo = extractCameraInfo(exifInterface)
        
        // スタイルに基づいて描画
        renderStyle(canvas, style, deviceModel, cameraInfo, result.width, result.height)
        
        return result
    }
    
    private fun renderStyle(
        canvas: Canvas,
        style: WatermarkStyle,
        deviceModel: String,
        cameraInfo: String,
        imageWidth: Int,
        imageHeight: Int
    ) {
        // スケール計算
        val scale = imageWidth / style.baseImageSize.toFloat()
        
        style.bitmaps.forEach { bitmap ->
            val y = imageHeight - (bitmap.height * scale) - (style.size.bottomMargin * scale)
            
            bitmap.elements.forEach { element ->
                renderElement(canvas, element, deviceModel, cameraInfo, 0f, y, scale)
            }
        }
    }
    
    private fun renderElement(
        canvas: Canvas,
        element: Element,
        deviceModel: String,
        cameraInfo: String,
        baseX: Float,
        baseY: Float,
        scale: Float
    ) {
        if (!element.visible) return
        
        when (element.content.type) {
            "text" -> renderText(canvas, element, deviceModel, cameraInfo, baseX, baseY, scale)
            "image" -> renderImage(canvas, element, baseX, baseY, scale)
            "shape" -> renderShape(canvas, element, baseX, baseY, scale)
            "elements" -> {
                element.content.elements?.forEach { child ->
                    renderElement(canvas, child, deviceModel, cameraInfo, baseX, baseY, scale)
                }
            }
        }
    }
    
    private fun renderText(
        canvas: Canvas,
        element: Element,
        deviceModel: String,
        cameraInfo: String,
        x: Float,
        y: Float,
        scale: Float
    ) {
        val paint = element.paint ?: return
        val text = when (element.content.textSource) {
            0 -> deviceModel
            1 -> cameraInfo
            else -> ""
        }
        
        val textPaint = Paint().apply {
            typeface = loadFont(paint)
            textSize = paint.textSize * scale
            color = Color.parseColor(paint.colors.firstOrNull() ?: "#000000")
            alpha = (paint.alpha * 255).toInt()
            isAntiAlias = true
        }
        
        val textX = x + (element.position.leftMargin * scale)
        val textY = y + (element.position.topMargin * scale) + textPaint.textSize
        
        canvas.drawText(text, textX, textY, textPaint)
    }
    
    private fun renderImage(
        canvas: Canvas,
        element: Element,
        x: Float,
        y: Float,
        scale: Float
    ) {
        val bitmapName = element.content.bitmap ?: element.content.bitmapResName ?: return
        val bitmap = loadImage(bitmapName) ?: return
        
        val width = (element.content.width ?: bitmap.width.toFloat()) * scale
        val height = (element.content.height ?: bitmap.height.toFloat()) * scale
        
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            width.toInt(),
            height.toInt(),
            true
        )
        
        val imageX = x + (element.position.leftMargin * scale)
        val imageY = y + (element.position.topMargin * scale)
        
        canvas.drawBitmap(scaledBitmap, imageX, imageY, null)
    }
    
    private fun renderShape(
        canvas: Canvas,
        element: Element,
        x: Float,
        y: Float,
        scale: Float
    ) {
        // 円形の装飾を描画（オレンジの丸など）
        val paint = element.paint ?: return
        val shapePaint = Paint().apply {
            color = Color.parseColor(paint.colors.firstOrNull() ?: "#FF8933")
            isAntiAlias = true
        }
        
        val diameter = 7.2f * scale // JSONから取得すべき
        val cx = x + (element.position.leftMargin * scale) + diameter / 2
        val cy = y + (element.position.topMargin * scale) + diameter / 2
        
        canvas.drawCircle(cx, cy, diameter / 2, shapePaint)
    }
    
    private fun loadFont(paint: ElementPaint): Typeface {
        val fontName = paint.fontName ?: "default"
        
        return fontCache.getOrPut(fontName) {
            try {
                when {
                    fontName.endsWith(".ttc") || fontName.endsWith(".otf") -> {
                        Typeface.createFromAsset(
                            context.assets,
                            "watermark_fonts/$fontName"
                        )
                    }
                    else -> Typeface.DEFAULT
                }
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
    }
    
    private fun loadImage(name: String): Bitmap? {
        return imageCache.getOrPut(name) {
            try {
                context.assets.open("watermark_images/$name.webp").use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Log.e("WatermarkRenderer", "Failed to load image: $name", e)
                null
            }
        } ?: run {
            imageCache.remove(name)
            null
        }
    }
    
    private fun extractCameraInfo(exif: ExifInterface?): String {
        if (exif == null) return ""
        
        val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
            ?.let { "${it.toFloatOrNull()?.toInt() ?: it}mm" } ?: ""
        val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
            ?.let { "f/${it}" } ?: ""
        val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: ""
        val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) ?: ""
        
        return listOf(focalLength, aperture, exposureTime, iso)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }
}
```

#### Step 2.4: アプリに統合

```kotlin
// MainActivity.kt または適切な場所
class YourExifPatcherActivity : AppCompatActivity() {
    private lateinit var watermarkRenderer: HasselbladWatermarkRenderer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        watermarkRenderer = HasselbladWatermarkRenderer(this)
    }
    
    fun processImage(imagePath: String, outputPath: String) {
        // 画像を読み込む
        val originalBitmap = BitmapFactory.decodeFile(imagePath)
        
        // EXIFを読み込む
        val exif = ExifInterface(imagePath)
        
        // 透かしを追加
        val watermarkedBitmap = watermarkRenderer.addWatermark(
            sourceBitmap = originalBitmap,
            styleId = "hassel_style_1",
            exifInterface = exif
        )
        
        // 保存
        FileOutputStream(outputPath).use { out ->
            watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        
        // EXIF情報をコピー
        copyExif(imagePath, outputPath)
    }
}
```

### Phase 3: UIの追加

```xml
<!-- activity_main.xml -->
<LinearLayout>
    <TextView
        android:text="透かしスタイル"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
    
    <Spinner
        android:id="@+id/watermarkStyleSpinner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>
    
    <Button
        android:id="@+id/applyWatermarkButton"
        android:text="Hasselblad透かしを追加"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>
</LinearLayout>
```

```kotlin
// スタイル選択の実装
val styles = listOf(
    "hassel_style_1",
    "hassel_style_2",
    "hassel_text_style_1"
)

val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, styles)
watermarkStyleSpinner.adapter = adapter

applyWatermarkButton.setOnClickListener {
    val selectedStyle = watermarkStyleSpinner.selectedItem as String
    processImage(inputPath, outputPath)
}
```

## 📦 依存関係

```gradle
// app/build.gradle.kts
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}
```

## 🧪 テスト

```kotlin
// テスト用の簡易実装
@Test
fun testWatermarkRendering() {
    val context = InstrumentationRegistry.getInstrumentation().context
    val renderer = HasselbladWatermarkRenderer(context)
    
    // テスト画像を読み込む
    val testBitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    
    // 透かしを追加
    val result = renderer.addWatermark(testBitmap, "hassel_style_1", null)
    
    assertNotNull(result)
    assertEquals(1080, result.width)
    assertEquals(1920, result.height)
}
```

## 🎨 カスタマイズ

### 独自のスタイルを作成
```json
{
  "styleId": "custom_style_1",
  "baseImageSize": 360,
  "size": {
    "bottomMargin": 60
  },
  "background": {
    "color": "#FFFFFF"
  },
  "bitmaps": [
    {
      "direction": 3,
      "elements": [
        {
          "id": 1,
          "content": {
            "type": "text",
            "textSource": 0
          },
          "paint": {
            "textSize": 14,
            "colors": ["#000000"]
          }
        }
      ]
    }
  ]
}
```

## ⚠️ 注意事項

1. **パフォーマンス**: 大きな画像では処理に時間がかかる可能性
2. **メモリ**: ビットマップのメモリ管理に注意
3. **法的**: Hasselbladロゴの使用許可を確認
4. **フォント**: ライセンスを確認

## 📚 参考資料

- `HASSELBLAD_WATERMARK_INVESTIGATION.md` - 詳細な調査結果
- `photo/EXTRACT_RESOURCES_GUIDE.md` - リソース抽出ガイド
- `WATERMARK_ANALYSIS.md` - 基本的な分析結果

## 🆘 トラブルシューティング

### 問題: 画像が表示されない
- assetsディレクトリのパスを確認
- 画像ファイル名を確認
- ログを確認: `adb logcat | grep Watermark`

### 問題: フォントが適用されない
- フォントファイルのパスを確認
- TTCの場合、ttcIndexが正しいか確認

### 問題: レイアウトがずれる
- スケール計算を確認
- baseImageSizeとの比率を確認

## 次のステップ

1. ✅ 調査完了
2. ⚠️ 画像リソースの抽出
3. ⚠️ コードの実装
4. ⚠️ テストとデバッグ
5. ⚠️ UIの改善
