# Hasselblad透かし機能の詳細調査結果

## 📋 概要

OnePlus GalleryのHasselblad透かし機能を詳細に調査し、以下の重要な情報を発見しました。

## 🎯 重要な発見

### 1. 透かし画像リソース

以下の4つのHasselbladロゴ画像リソースが使用されています：

1. **hasselblad_watermark_dark** - メインのHasselbladロゴ(ダークモード)
2. **hassel_watermark_h_logo_dark** - Hロゴ(ダークモード)  
3. **hassel_watermark_h_logo_text_style** - Hロゴ(テキストスタイル用)
4. **hasselblad_watermark_text_style** - Hasselbladロゴ(テキストスタイル用)

**問題**: これらの画像ファイルはAPK内で難読化されており、リソース名からファイルへのマッピングが必要です。

### 2. 透かしスタイル定義ファイル

`/assets/watermark_master_styles/` に以下の8つのHasselbladスタイルがあります：

#### 静止画用スタイル
- `hassel_style_1.json` (7,265 bytes)
- `hassel_style_2.json` (3,820 bytes)
- `hassel_style_3.json` (4,031 bytes)
- `hassel_style_4.json` (18,244 bytes)
- `hassel_text_style_1.json` (27,546 bytes)
- `hassel_text_style_2.json` (10,331 bytes)

#### 動画用スタイル
- `video_hassel_text_style_1.json` (27,604 bytes)
- `video_hassel_text_style_2.json` (10,337 bytes)

### 3. フォントファイル

`/data/data/com.oneplus.gallery/files/watermark_master/materia/` にダウンロードされたフォント：

- **AvenirNext.ttc** (4,994,280 bytes) - メインフォント
- **ButlerBold.otf** (43,024 bytes) - 太字フォント
- **ButlerMedium.otf** (45,516 bytes) - ミディアムフォント
- **FZCYSK.TTF** (10,517,788 bytes) - 中国語フォント

**オンラインソース**: 
```
https://videoclipfsg.coloros.com/dirfile/icon/2024/11/06/d0f56219-1be1-41b5-84ca-847857d8bff3.zip
MD5: 33ca1145fa326691413fa356abb82753
```

### 4. ネイティブライブラリ

**liblocal_ai_master_watermark_drawer.so** (322,768 bytes)
- 透かしの描画処理を行うネイティブライブラリ
- 場所: `/lib/arm64-v8a/`

### 5. Java実装クラス

設定ファイルから判明：
```
com.oplus.tbluniformeditor.plugins.watermark.function.WatermarkAlgoImpl
```

## 📐 透かしの構造解析

### hassel_style_1の構造

```json
{
  "styleId": "hassel_style_1",
  "baseImageSize": 360,
  "size": {
    "bottomMargin": 60
  },
  "background": {
    "color": "#FFFFFF"
  },
  "elements": [
    {
      "左側": {
        "type": "text",
        "textSource": 0,  // デバイスモデル名
        "font": "OplusSans",
        "fontSize": 13,
        "fontWeight": 700,
        "color": "#000000"
      }
    },
    {
      "右側": {
        "type": "image",
        "bitmap": "hasselblad_watermark_dark",
        "width": 112.8,
        "height": 8.96
      }
    },
    {
      "カメラ情報": {
        "type": "text",
        "textSource": 1,  // 焦点距離などのカメラ情報
        "fontSize": 7.2,
        "alpha": 0.7,
        "decorationCircle": {
          "diameter": 7.2,
          "color": "#FF8933"  // OnePlusのオレンジ
        }
      }
    }
  ]
}
```

### textSourceの種類

- **textSource: 0** = デバイスモデル名 (例: "OnePlus 12")
- **textSource: 1** = カメラ情報 (例: "24mm f/1.8 1/100s ISO 100")

## 🎨 レイアウトパターン

### パターン1: 横並び配置 (hassel_style_1, 2)
```
┌────────────────────────────────────┐
│                                    │
│  [デバイス名]      [Hasselbladロゴ]│
│                    [●] [カメラ情報]│
└────────────────────────────────────┘
```

### パターン2: 中央配置 (hassel_text_style_1)
```
┌────────────────────────────────────┐
│                                    │
│              [Hロゴ]               │
│           [デバイス名]             │
│          [カメラ情報]              │
└────────────────────────────────────┘
```

## 🔧 技術的な実装詳細

### JSONスキーマの主要フィールド

| フィールド | 説明 |
|----------|------|
| `styleId` | スタイルの一意識別子 |
| `baseImageSize` | 基準画像サイズ (360px) |
| `size.bottomMargin` | 下部マージン |
| `background.color` | 背景色 |
| `bitmaps[].direction` | 配置方向 (3=水平右寄せ, 4=中央) |
| `elements[].content.type` | 要素タイプ (text/image/shape/space) |
| `elements[].position.layoutGravity` | 配置位置 |
| `paint.fontType` | フォントタイプ (0=システム, 2=カスタム) |
| `paint.fontFileType` | フォントファイルタイプ (0=ZIP, 1=TTF) |
| `paint.ttcIndex` | TTCファイル内のフォントインデックス |

### フォント指定方法

```json
{
  "fontType": 2,
  "fontName": "AvenirNext.zip",
  "fontFileType": 0,
  "ttcIndex": 5,
  "font": "https://...",
  "md5": "33ca1145fa326691413fa356abb82753"
}
```

## 💡 アプリへの統合方法

### 必要なステップ

#### 1. 画像リソースの取得
APKをdecompileして、以下のリソースを抽出：
- `hasselblad_watermark_dark`
- `hassel_watermark_h_logo_dark`
- `hassel_watermark_h_logo_text_style`
- `hasselblad_watermark_text_style`

**方法**:
1. APKTool または JADX-GUIを使用
2. リソースIDからファイル名をマッピング
3. `res/` ディレクトリから該当ファイルをコピー

#### 2. フォントファイルの取得
すでにローカルに保存されているフォントを使用：
```
photo/1_data_data_extracted/files/watermark_master/materia/
- AvenirNext.ttc
- ButlerBold.otf
- ButlerMedium.otf
```

#### 3. JSONスタイルファイルの統合
既にあるファイルを使用：
```
photo/gallery_extracted/assets/watermark_master_styles/
- hassel_style_1.json
- hassel_style_2.json
- hassel_text_style_1.json
等
```

#### 4. 描画ロジックの実装
ネイティブライブラリを解析するか、JSONを基に独自実装：

```kotlin
// 例: JSONパーサー
data class WatermarkStyle(
    val styleId: String,
    val baseImageSize: Int,
    val size: Size,
    val background: Background,
    val bitmaps: List<BitmapElement>
)

// 描画エンジン
class WatermarkRenderer {
    fun renderWatermark(
        sourceImage: Bitmap,
        style: WatermarkStyle,
        deviceModel: String,
        cameraInfo: String
    ): Bitmap {
        // 1. キャンバス作成
        // 2. スタイルに基づいて要素を配置
        // 3. テキストソースを置換
        // 4. 合成
    }
}
```

## 🚀 次のアクション

### 優先度: 高
1. **APKをdecompile**して画像リソースを抽出
   - ツール: JADX-GUI または APKTool
   - ターゲット: `hasselblad_watermark_dark` などの画像

2. **リソースマッピングの作成**
   - `resources.arsc` を解析
   - リソースID → ファイル名のマッピング

### 優先度: 中
3. **透かし描画エンジンの実装**
   - Kotlin/Javaで実装
   - Canvas APIを使用
   - JSONパーサーとレンダラー

4. **フォントの統合**
   - AvenirNext.ttcをassetsに追加
   - Typefaceとして読み込み

### 優先度: 低
5. **ネイティブライブラリの解析**(オプション)
   - Ghidra または IDA Proで解析
   - アルゴリズムの理解

## 📝 実装例（疑似コード）

```kotlin
class HasselbladWatermarkRenderer(context: Context) {
    private val styles = loadStyles()
    private val fonts = loadFonts()
    private val logos = loadLogos()
    
    fun addWatermark(
        image: Bitmap,
        styleId: String,
        exifData: ExifInterface
    ): Bitmap {
        val style = styles[styleId] ?: return image
        val deviceModel = Build.MODEL
        val cameraInfo = extractCameraInfo(exifData)
        
        val canvas = Canvas(image)
        
        // スタイルに基づいて描画
        style.bitmaps.forEach { bitmap ->
            bitmap.elements.forEach { element ->
                when (element.content.type) {
                    "text" -> drawText(canvas, element, deviceModel, cameraInfo)
                    "image" -> drawImage(canvas, element)
                    "shape" -> drawShape(canvas, element)
                }
            }
        }
        
        return image
    }
    
    private fun extractCameraInfo(exif: ExifInterface): String {
        val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
        val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
        val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
        val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        
        return "$focalLength f/$aperture $exposureTime ISO $iso"
    }
}
```

## ⚠️ 注意事項

### 法的考慮
- Hasselbladのロゴと商標は Hasselblad AB の所有物
- 商用利用には許可が必要な可能性
- 個人使用/教育目的での実装を推奨

### 技術的制限
- ネイティブライブラリの完全な再現は困難
- OpenGLシェーダーの使用も検討
- パフォーマンス最適化が必要

## 🔍 さらなる調査が必要な項目

1. ✅ 画像リソースの抽出方法
2. ✅ フォントファイルの統合
3. ⚠️ ネイティブライブラリの詳細な動作
4. ⚠️ シェーダーの使用方法
5. ⚠️ HDR画像への対応

## 📚 参考ファイル

- `photo/gallery_extracted/assets/watermark_master_styles/` - スタイル定義
- `photo/1_data_data_extracted/files/watermark_master/materia/` - フォント
- `photo/gallery_extracted/lib/arm64-v8a/liblocal_ai_master_watermark_drawer.so` - ネイティブライブラリ
- `photo/gallery_extracted/assets/tbluniformeditor/plugin/plugin_watermark_config.xml` - プラグイン設定

## 結論

Hasselblad透かし機能は**実装可能**です！

主な要素：
1. ✅ JSONスタイル定義 - 利用可能
2. ✅ フォントファイル - 抽出済み
3. ⚠️ ロゴ画像 - decompileで抽出可能
4. ⚠️ 描画エンジン - 独自実装が必要

次のステップとして、APKをJADX-GUIでdecompileし、画像リソースを抽出することを推奨します。
