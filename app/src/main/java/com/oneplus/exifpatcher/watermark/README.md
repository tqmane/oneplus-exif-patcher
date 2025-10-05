# Hasselblad Watermark Implementation

このドキュメントは、OnePlus Gallery APKから抽出したHasselbladウォーターマーク機能の実装説明です。

## 📁 実装したファイル

### データモデル
- **`watermark/model/WatermarkModels.kt`**
  - `WatermarkStyle`: スタイル定義
  - `WatermarkElement`: 個別要素（テキスト、画像、図形）
  - `CameraInfo`: カメラ情報
  - `TextSource`: テキストソース定数

### パーサー
- **`watermark/parser/WatermarkStyleParser.kt`**
  - JSONスタイル定義の読み込み
  - assetsフォルダからの動的ロード
  - デフォルトスタイルのフォールバック

### レンダラー
- **`watermark/renderer/HasselbladWatermarkRenderer.kt`**
  - Canvas描画ロジック
  - テキスト、画像、図形の描画
  - スケール計算
  - フォント/画像リソースの読み込み

### ユーティリティ
- **`watermark/util/ExifCameraInfoExtractor.kt`**
  - Exif情報からカメラ情報を抽出
  - 絞り値、シャッタースピード、ISO、焦点距離
  - デバイス名の取得

### 統合クラス
- **`watermark/HasselbladWatermarkManager.kt`**
  - すべての機能を統合
  - シンプルなAPIを提供
  - プリセットスタイル定数

## 🚀 使用方法

### 基本的な使い方

```kotlin
import com.oneplus.exifpatcher.watermark.HasselbladWatermarkManager
import androidx.exifinterface.media.ExifInterface

// 1. マネージャーを初期化
val watermarkManager = HasselbladWatermarkManager(context)

// 2. ウォーターマークを追加
val originalBitmap: Bitmap = // 元画像
val exif: ExifInterface = // Exif情報

val resultBitmap = watermarkManager.addWatermark(
    sourceBitmap = originalBitmap,
    exif = exif
)

// 3. 結果を保存
// resultBitmapを保存処理に渡す
```

### スタイルを指定する

```kotlin
// プリセットスタイルを使用
val resultBitmap = watermarkManager.addWatermark(
    sourceBitmap = originalBitmap,
    exif = exif,
    styleId = HasselbladWatermarkManager.Presets.LANDSCAPE
)
```

### 利用可能なプリセットスタイル

```kotlin
HasselbladWatermarkManager.Presets.LANDSCAPE            // 横位置
HasselbladWatermarkManager.Presets.LANDSCAPE_RIGHT      // 横位置（右寄せ）
HasselbladWatermarkManager.Presets.VERTICAL_TOP         // 縦位置（上部）
HasselbladWatermarkManager.Presets.VERTICAL_BOTTOM      // 縦位置（下部）
HasselbladWatermarkManager.Presets.VERTICAL_CENTER      // 縦位置（中央）
HasselbladWatermarkManager.Presets.HORIZONTAL_TOP       // 横位置（上部）
HasselbladWatermarkManager.Presets.HORIZONTAL_BOTTOM    // 横位置（下部）
HasselbladWatermarkManager.Presets.HORIZONTAL_CENTER    // 横位置（中央）
```

### カメラ情報を手動で指定

```kotlin
import com.oneplus.exifpatcher.watermark.model.CameraInfo

val cameraInfo = CameraInfo(
    aperture = "f/1.8",
    shutterSpeed = "1/100",
    iso = "ISO400",
    focalLength = "24mm",
    deviceName = "OnePlus 9 Pro"
)

val resultBitmap = watermarkManager.addWatermark(
    sourceBitmap = originalBitmap,
    cameraInfo = cameraInfo
)
```

### スタイル一覧を取得

```kotlin
val availableStyles = watermarkManager.getAvailableStyles()
// Map<String, String> = { "style_042_..." to "Hasselblad Landscape", ... }

availableStyles.forEach { (id, name) ->
    println("$id: $name")
}
```

## 📦 必要なリソース

### 1. 画像ファイル（drawable-nodpi/）
- ✅ `hasselblad_watermark_dark.webp` (1.89KB)
- ✅ `hassel_watermark_h_logo_dark.webp` (0.85KB)
- ✅ `hassel_watermark_h_logo_text_style.png` (50.95KB)
- ✅ `hasselblad_watermark_text_style.webp` (11.44KB)

### 2. フォントファイル（assets/fonts/）
- `AvenirNext.ttc` (4.8MB)
- `ButlerBold.otf` (42KB)
- `ButlerMedium.otf` (44KB)
- `FZCYSK.TTF` (10MB)

### 3. JSONスタイル定義（assets/watermark/）
- `style_042_hasselblad_landscape_v2.json`
- `style_043_hasselblad_landscape_right_v2.json`
- `style_044_hasselblad_vertical_top_v2.json`
- `style_045_hasselblad_vertical_bottom_v2.json`
- `style_046_hasselblad_vertical_center_v2.json`
- `style_047_hasselblad_horizontal_top_v2.json`
- `style_048_hasselblad_horizontal_bottom_v2.json`
- `style_049_hasselblad_horizontal_center_v2.json`

## 🔧 統合手順

### MainViewModelへの統合例

```kotlin
class MainViewModel : ViewModel() {
    private lateinit var watermarkManager: HasselbladWatermarkManager
    
    fun initWatermark(context: Context) {
        watermarkManager = HasselbladWatermarkManager(context)
    }
    
    suspend fun applyWatermark(
        imageUri: Uri,
        context: Context
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // 1. 画像を読み込み
            val bitmap = context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@withContext Result.failure(Exception("Failed to load image"))
            
            // 2. Exif情報を読み込み
            val exif = context.contentResolver.openInputStream(imageUri)?.use {
                ExifInterface(it)
            } ?: return@withContext Result.failure(Exception("Failed to load EXIF"))
            
            // 3. ウォーターマークを追加
            val resultBitmap = watermarkManager.addWatermark(bitmap, exif)
            
            // 4. 結果を保存
            val outputUri = saveImage(context, resultBitmap, "watermarked_${System.currentTimeMillis()}.jpg")
            
            Result.success(outputUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## 📝 JSONスタイル定義の例

```json
{
  "id": "style_042_hasselblad_landscape_v2",
  "name": "Hasselblad Landscape",
  "width": 1080,
  "height": 200,
  "orientation": 0,
  "elements": [
    {
      "type": "image",
      "x": 50,
      "y": 50,
      "width": 150,
      "height": 50,
      "bitmap": "hasselblad_watermark_dark"
    },
    {
      "type": "text",
      "x": 220,
      "y": 60,
      "width": 500,
      "height": 40,
      "textSource": 1,
      "fontFamily": "ButlerMedium.otf",
      "fontSize": 24,
      "fontWeight": "normal",
      "textAlign": "left",
      "color": "#FFFFFF",
      "alpha": 0.9
    }
  ]
}
```

## 🎨 カスタムスタイルの作成

1. **JSONファイルを作成**
   - `assets/watermark/my_custom_style.json`

2. **要素を定義**
   - `type`: "text", "image", "shape"
   - `x`, `y`: 位置
   - `width`, `height`: サイズ

3. **テキスト要素の場合**
   - `textSource`: 0（デバイス名）, 1（カメラ情報）, 2（HASSELBLAD）
   - `fontFamily`: フォント名
   - `fontSize`: フォントサイズ
   - `color`: カラーコード

4. **使用**
   ```kotlin
   watermarkManager.addWatermark(
       sourceBitmap = bitmap,
       exif = exif,
       styleId = "my_custom_style"
   )
   ```

## 🐛 トラブルシューティング

### リソースが見つからない

**症状**: `ResourceNotFoundException` が発生

**解決策**:
1. `app/src/main/res/drawable-nodpi/` に画像ファイルが存在するか確認
2. ファイル名が正しいか確認（大文字小文字を含む）
3. ビルド後に`R.drawable.xxx` でアクセスできるか確認

### フォントが読み込めない

**症状**: デフォルトフォントが使用される

**解決策**:
1. `app/src/main/assets/fonts/` にフォントファイルが存在するか確認
2. JSONスタイル定義のフォント名が正しいか確認
3. フォントファイルが破損していないか確認

### JSONスタイルが読み込めない

**症状**: デフォルトスタイルが常に使用される

**解決策**:
1. `app/src/main/assets/watermark/` にJSONファイルが存在するか確認
2. JSON構文が正しいか確認（JSONLintで検証）
3. エラーログを確認

## 📚 関連ドキュメント

- [HASSELBLAD_WATERMARK_INVESTIGATION.md](../../HASSELBLAD_WATERMARK_INVESTIGATION.md) - 詳細な技術調査
- [HASSELBLAD_IMPLEMENTATION_GUIDE.md](../../HASSELBLAD_IMPLEMENTATION_GUIDE.md) - 実装ガイド
- [photo/HASSELBLAD_IMAGES_FOUND.md](../../photo/HASSELBLAD_IMAGES_FOUND.md) - 画像リソース一覧

## ✅ 実装完了チェックリスト

- [x] データモデル作成
- [x] JSONパーサー実装
- [x] Canvas描画レンダラー実装
- [x] Exif情報抽出ユーティリティ実装
- [x] 統合マネージャークラス作成
- [x] Gson依存関係追加
- [x] 画像リソース配置（drawable-nodpi）
- [ ] フォントファイル配置（assets/fonts）
- [ ] JSONスタイル定義配置（assets/watermark）
- [ ] MainViewModelへの統合
- [ ] UI実装（スタイル選択機能）
- [ ] テスト実行

## 🎯 次のステップ

1. **リソースファイルの配置**
   - フォントファイルを`assets/fonts/`にコピー
   - JSONスタイル定義を`assets/watermark/`にコピー

2. **MainViewModelへの統合**
   - `HasselbladWatermarkManager`のインスタンス化
   - ウォーターマーク適用機能の追加

3. **UI実装**
   - スタイル選択UI
   - プレビュー機能
   - オン/オフ切り替え

4. **テスト**
   - 各スタイルでのウォーターマーク描画テスト
   - 異なる画像サイズでのテスト
   - Exif情報がない画像でのテスト
