# Hasselblad透かし機能の解析結果

## 🔍 発見した内容

### 透かし設定ファイル（JSON）

OnePlus GalleryはJSON形式で透かしのスタイルを定義しています。

**場所**: `assets/watermark_master_styles/`

**主要なスタイルファイル**:
- `hassel_style_1.json` - Hasselbladブランド透かし スタイル1
- `hassel_style_2.json` - Hasselbladブランド透かし スタイル2
- `hassel_style_3.json` - Hasselbladブランド透かし スタイル3
- `hassel_style_4.json` - Hasselbladブランド透かし スタイル4
- `hassel_text_style_1.json` - Hasselbladテキスト透かし
- `hassel_text_style_2.json` - Hasselbladテキスト透かし
- `text_style_1.json` - 汎用テキスト透かし
- その他多数のスタイル

### 透かしの構造（hassel_style_1.jsonから）

```json
{
  "styleId": "hassel_style_1",
  "baseImageSize": 360,
  "size": {
    "bottomMargin": 60  // 下部に60pxのマージン
  },
  "background": {
    "color": "#FFFFFF"
  },
  "bitmaps": [
    {
      "direction": 3,  // 配置方向
      "height": 60,
      "elements": [
        // 左側: デバイスモデル名
        {
          "id": 1,
          "content": {
            "type": "text",
            "textSource": 0  // デバイスモデル名
          },
          "paint": {
            "fontName": "OplusSans",
            "textSize": 13,
            "colors": ["#000000"]
          }
        },
        // 右側: Hasselbladロゴ
        {
          "id": 4,
          "content": {
            "type": "image",
            "bitmap": "hasselblad_watermark_dark",
            "width": 112.8,
            "height": 8.96
          }
        },
        // 撮影情報（焦点距離など）
        {
          "id": 7,
          "content": {
            "type": "text",
            "textSource": 1  // カメラ情報
          },
          "paint": {
            "textSize": 7.2,
            "alpha": 0.7
          }
        }
      ]
    }
  ]
}
```

## 📊 透かしの要素

### 1. **Hasselbladロゴ画像**
- リソース名: `hasselblad_watermark_dark`
- サイズ: 112.8 x 8.96 dp
- 位置: 右下

### 2. **デバイス名テキスト**
- テキストソース: `textSource: 0` = デバイスモデル名
- フォント: OplusSans, 13sp, 太字
- 位置: 左下

### 3. **カメラ情報テキスト**
- テキストソース: `textSource: 1` = カメラ情報（焦点距離など）
- フォント: OplusSans, 7.2sp
- 透明度: 70%

### 4. **装飾要素**
- オレンジの円（直径7.2dp）
- カラー: #FF8933

## 🎨 透かしレイアウト

```
┌─────────────────────────────────────────┐
│                                         │
│         画像コンテンツエリア             │
│                                         │
│                                         │
├─────────────────────────────────────────┤
│ OnePlus 12        🟠 50mm ƒ/1.8    HB  │ ← 透かしバー (60dp高さ)
└─────────────────────────────────────────┘
   ↑                ↑              ↑
   モデル名         撮影情報    Hasselbladロゴ
```

## 💡 実装への適用方法

### オプション1: シンプルなテキスト透かし

```kotlin
fun addWatermark(bitmap: Bitmap, deviceModel: String, cameraInfo: String?): Bitmap {
    val result = bitmap.copy(bitmap.config, true)
    val canvas = Canvas(result)
    
    // 透かしバーの背景
    val barHeight = 60.dpToPx()
    val barPaint = Paint().apply {
        color = Color.WHITE
    }
    canvas.drawRect(
        0f,
        result.height - barHeight,
        result.width.toFloat(),
        result.height.toFloat(),
        barPaint
    )
    
    // デバイス名テキスト
    val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 13.spToPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(
        deviceModel,
        24.dpToPx(),
        result.height - 30.dpToPx(),
        textPaint
    )
    
    // カメラ情報（オプション）
    cameraInfo?.let {
        val infoPaint = Paint().apply {
            color = Color.BLACK
            alpha = (255 * 0.7).toInt()
            textSize = 7.spToPx()
        }
        canvas.drawText(
            it,
            result.width - 150.dpToPx(),
            result.height - 20.dpToPx(),
            infoPaint
        )
    }
    
    return result
}
```

### オプション2: カスタム透かしビルダー

```kotlin
data class WatermarkStyle(
    val deviceModel: String,
    val showCameraInfo: Boolean = true,
    val cameraInfo: String? = null,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM,
    val backgroundColor: Int = Color.WHITE,
    val textColor: Int = Color.BLACK,
    val brandLogo: Bitmap? = null
)

enum class WatermarkPosition {
    BOTTOM, TOP, BOTTOM_LEFT, BOTTOM_RIGHT
}

class WatermarkBuilder(private val style: WatermarkStyle) {
    fun applyToImage(source: Bitmap): Bitmap {
        // 透かしを適用
    }
}
```

### オプション3: Compose Canvasで実装

```kotlin
@Composable
fun WatermarkedImage(
    bitmap: ImageBitmap,
    watermarkStyle: WatermarkStyle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // 画像を描画
        drawImage(bitmap)
        
        // 透かしバーを描画
        drawRect(
            color = Color.White,
            topLeft = Offset(0f, size.height - 60.dp.toPx()),
            size = Size(size.width, 60.dp.toPx())
        )
        
        // テキストを描画
        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.BLACK
                textSize = 13.sp.toPx()
                typeface = Typeface.DEFAULT_BOLD
            }
            
            canvas.nativeCanvas.drawText(
                watermarkStyle.deviceModel,
                24.dp.toPx(),
                size.height - 30.dp.toPx(),
                paint
            )
        }
    }
}
```

## 🚀 推奨される実装

### フェーズ1: 基本的なテキスト透かし（最小限）

1. **EXIFデータから情報取得**
   - デバイスモデル
   - 焦点距離、絞り値（利用可能な場合）

2. **シンプルな透かしバーを追加**
   - 下部に白いバー
   - 左にデバイス名
   - 右にカメラ情報（オプション）

3. **ユーザー設定**
   - ON/OFF切り替え
   - 位置選択（上/下）
   - 色選択（白/黒/透明）

### フェーズ2: カスタマイズ可能な透かし

1. **複数のスタイルプリセット**
   - シンプル（テキストのみ）
   - プロフェッショナル（カメラ情報付き）
   - ミニマル（デバイス名のみ）

2. **編集可能な要素**
   - テキスト内容のカスタマイズ
   - フォントサイズ
   - 透明度
   - 位置

3. **カスタムロゴのサポート**
   - ユーザーが独自の画像をアップロード
   - ロゴのサイズと位置を調整

## ⚠️ 注意事項

### 著作権について

**Hasselbladロゴと商標**:
- OnePlusとHasselbladのパートナーシップで使用されている
- Hasselbladロゴは商標登録されている
- 無断使用は法的問題になる可能性がある

**推奨される代替案**:
1. ユーザーが独自のロゴをアップロード
2. 汎用的なカメラアイコンを使用
3. テキストのみの透かし
4. "Powered by [ユーザー名]" など個人化されたブランディング

### リソースの抽出について

- OnePlus Galleryから画像リソースを直接抽出するのは避けるべき
- 教育・研究目的の解析は許可される場合が多い
- 商用利用や配布には許可が必要

## 📝 次のステップ

1. ✅ **基本的なテキスト透かし機能を実装**
   - ViewModelに透かし設定を追加
   - ExifPatcherに透かし描画機能を追加
   - UIで透かしのON/OFF切り替え

2. ✅ **プリセットスタイルの実装**
   - JSONベースのスタイル設定
   - 複数のプリセットから選択

3. ✅ **カスタマイズ機能**
   - テキスト編集
   - 位置調整
   - 色選択

実装してみましょうか？
