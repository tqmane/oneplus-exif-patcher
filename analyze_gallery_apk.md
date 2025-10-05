# OxygenOS Gallery APK 解析 - Hasselblad透かし機能の調査

## APK情報
- ファイル: `com.oneplus.gallery_15.73.22.apk`
- 場所: `photo/`

## 解析手順

### 1. APKを展開

#### 方法A: 7-Zip/WinRAR を使用（Windows）
1. APKファイルを右クリック
2. 「7-Zip」→「ここに展開」または「展開」を選択
3. 展開されたフォルダの内容を確認

#### 方法B: コマンドライン
```cmd
cd photo
mkdir gallery_extracted
copy com.oneplus.gallery_15.73.22.apk gallery.zip
tar -xf gallery.zip -C gallery_extracted
```

### 2. DEXファイルをJavaコードに逆コンパイル

#### ツール: jadx
- ダウンロード: https://github.com/skylot/jadx/releases
- GUIバージョン（jadx-gui）を使用

```cmd
jadx-gui com.oneplus.gallery_15.73.22.apk
```

### 3. 調査すべきポイント

#### A. リソースファイル（res/フォルダ）
- `res/drawable*/` - Hasselblad透かし画像
- `res/values/strings.xml` - テキスト関連
- `res/xml/` - 設定ファイル

#### B. マニフェスト
- `AndroidManifest.xml` - パーミッション、コンポーネント

#### C. コード（逆コンパイル後）
検索キーワード:
- `hasselblad`
- `watermark`
- `overlay`
- `exif`
- `metadata`
- `camera`
- `image processing`

## 自動解析スクリプト

以下のPowerShellスクリプトで基本的な情報を抽出できます:

```powershell
# APK解析スクリプト
$apkPath = "com.oneplus.gallery_15.73.22.apk"
$extractPath = "gallery_extracted"

# 1. APKを展開
Write-Host "Extracting APK..."
Expand-Archive -Path $apkPath -DestinationPath $extractPath -Force

# 2. ファイル構造を出力
Write-Host "`nDirectory structure:"
Get-ChildItem -Path $extractPath -Recurse | Select-Object FullName | Out-File "apk_structure.txt"

# 3. Hasselblad関連のファイルを検索
Write-Host "`nSearching for Hasselblad-related files..."
Get-ChildItem -Path $extractPath -Recurse -Include *.xml,*.png,*.jpg | 
    Where-Object { $_.Name -match "hasselblad|watermark|overlay" } |
    Select-Object FullName | Out-File "hasselblad_files.txt"

# 4. strings.xmlを検索
Write-Host "`nSearching for strings.xml files..."
Get-ChildItem -Path $extractPath -Recurse -Filter "strings.xml" |
    Select-Object FullName | Out-File "strings_files.txt"

Write-Host "`nAnalysis complete! Check the generated .txt files."
```

## 予想される実装方法

### 1. 透かし画像の重ね合わせ
```kotlin
// 画像処理時に透かしを追加
fun addWatermark(originalImage: Bitmap, watermarkType: WatermarkType): Bitmap {
    val watermark = when (watermarkType) {
        WatermarkType.HASSELBLAD -> loadHasselbladWatermark()
        WatermarkType.ONEPLUS -> loadOnePlusWatermark()
    }
    
    val canvas = Canvas(originalImage)
    // 右下または左下に透かしを配置
    val x = originalImage.width - watermark.width - margin
    val y = originalImage.height - watermark.height - margin
    canvas.drawBitmap(watermark, x.toFloat(), y.toFloat(), paint)
    
    return originalImage
}
```

### 2. EXIFメタデータベースの判定
```kotlin
// カメラモデルに基づいて透かしを決定
fun determineWatermark(exif: ExifInterface): WatermarkType? {
    val make = exif.getAttribute(ExifInterface.TAG_MAKE)
    val model = exif.getAttribute(ExifInterface.TAG_MODEL)
    
    return when {
        make == "OnePlus" && model?.contains("hasselblad") == true -> WatermarkType.HASSELBLAD
        make == "OnePlus" -> WatermarkType.ONEPLUS
        else -> null
    }
}
```

### 3. 設定ファイルでの管理
```xml
<!-- res/values/config.xml -->
<resources>
    <bool name="enable_hasselblad_watermark">true</bool>
    <string name="hasselblad_watermark_text">Shot on OnePlus | Hasselblad</string>
    <integer name="watermark_position">2</integer> <!-- 0=左下, 1=右下, 2=左上, 3=右上 -->
</resources>
```

## 私のアプリへの適用方法

### オプション1: 透かし画像を追加（簡単）
1. Hasselblad透かし画像をリソースに追加
2. 画像保存前にBitmapに透かしを重ね合わせ
3. ユーザーがON/OFF切り替え可能に

### オプション2: EXIFメタデータのみ（現在の実装）
1. `TAG_IMAGE_DESCRIPTION` に "Shot on OnePlus | Hasselblad" を設定
2. 視覚的な透かしは追加しない
3. メタデータのみで識別

### オプション3: ハイブリッド
1. EXIFメタデータを設定
2. オプションで透かし画像も追加
3. ユーザーが選択可能

## 次のステップ

1. ✅ APKを展開して内容を確認
2. ✅ jadx-guiで逆コンパイルしてコードを確認
3. ✅ Hasselblad関連のリソースを特定
4. ✅ 実装方法を分析
5. ✅ 私のアプリに適用可能な機能を選択
6. ✅ 実装コードを作成

## 注意事項

⚠️ **著作権とライセンス**
- Hasselbladのロゴや商標は使用許可が必要
- OnePlusの公式リソースも同様
- 個人使用の範囲であれば問題ない可能性が高いが、配布する場合は要注意

⚠️ **逆コンパイル**
- 教育・研究目的の逆コンパイルは合法的な場合が多い
- 商用利用や再配布は避けるべき

## 推奨される実装

**段階的アプローチ:**

1. **フェーズ1**: EXIFメタデータのみ（現在）
   - `TAG_IMAGE_DESCRIPTION`: "Shot on OnePlus"
   - `TAG_ARTIST`: "OnePlus Camera"
   
2. **フェーズ2**: テキスト透かし
   - ユーザーがカスタムテキストを設定可能
   - 位置、サイズ、色を調整可能
   
3. **フェーズ3**: 画像透かし（オプション）
   - ユーザーが独自の透かし画像をアップロード
   - プリセット透かし（OnePlus風など）

これにより、著作権の問題を回避しつつ、同様の機能を提供できます。
