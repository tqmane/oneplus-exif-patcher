# Hasselblad透かし調査 - 完了報告

## 📅 調査日
2025年10月5日

## 🎯 調査目的
OxygenOSのOnePlus GalleryアプリからHasselblad透かし機能を調査し、独自アプリへの統合可能性を評価する。

## ✅ 調査完了項目

### 1. データソースの分析
- ✅ APKファイル展開 (`com.oneplus.gallery_15.73.22.apk`)
- ✅ アプリデータの抽出
  - `/data/data/com.oneplus.gallery` → `1com.oneplus.gallery.zip`
  - `/sdcard/Android/data/com.oneplus.gallery` → `2com.oneplus.gallery.zip`
- ✅ すべてのデータを展開・分析

### 2. 重要な発見

#### A. 透かしスタイル定義ファイル
**場所**: `photo/gallery_extracted/assets/watermark_master_styles/`

**Hasselbladスタイル (8個)**:
1. `hassel_style_1.json` (7,265 bytes)
2. `hassel_style_2.json` (3,820 bytes)
3. `hassel_style_3.json` (4,031 bytes)
4. `hassel_style_4.json` (18,244 bytes)
5. `hassel_text_style_1.json` (27,546 bytes)
6. `hassel_text_style_2.json` (10,331 bytes)
7. `video_hassel_text_style_1.json` (27,604 bytes)
8. `video_hassel_text_style_2.json` (10,337 bytes)

**特徴**:
- JSON形式で完全に構造化
- レイアウト、フォント、色、配置情報を含む
- textSource: 0 = デバイスモデル名
- textSource: 1 = カメラ情報（焦点距離、絞り、ISO等）

#### B. フォントファイル
**場所**: `photo/1_data_data_extracted/files/watermark_master/materia/`

| ファイル | サイズ | 用途 |
|---------|--------|------|
| AvenirNext.ttc | 4,994,280 bytes | メインフォント（複数スタイル含む） |
| ButlerBold.otf | 43,024 bytes | 太字フォント |
| ButlerMedium.otf | 45,516 bytes | ミディアムフォント |
| FZCYSK.TTF | 10,517,788 bytes | 中国語フォント |

**オンラインソース**:
```
URL: https://videoclipfsg.coloros.com/dirfile/icon/2024/11/06/d0f56219-1be1-41b5-84ca-847857d8bff3.zip
MD5: 33ca1145fa326691413fa356abb82753
```

#### C. 画像リソース
**必要な画像** (APKから抽出が必要):
1. `hasselblad_watermark_dark` - メインHasselbladロゴ
2. `hassel_watermark_h_logo_dark` - Hロゴ（ダーク）
3. `hassel_watermark_h_logo_text_style` - Hロゴ（テキストスタイル用）
4. `hasselblad_watermark_text_style` - Hasselbladロゴ（テキストスタイル用）

**問題**: リソース名が難読化されているため、JADX-GUIでのdecompileが必要

#### D. ネイティブライブラリ
- **ファイル**: `liblocal_ai_master_watermark_drawer.so`
- **サイズ**: 322,768 bytes
- **場所**: `lib/arm64-v8a/`
- **実装クラス**: `com.oplus.tbluniformeditor.plugins.watermark.function.WatermarkAlgoImpl`

#### E. 設定ファイル
- **ファイル**: `com.oneplus.gallery_preferences.xml`
- **重要な設定**: `is_restrict_watermark_downloaded: true`
- **その他**: 透かし関連の設定を確認済み

### 3. 技術仕様の解析

#### 透かしの構造
```
Watermark = {
  Layout: JSON定義
  ├── Text Elements
  │   ├── Device Model (textSource: 0)
  │   └── Camera Info (textSource: 1)
  ├── Image Elements
  │   └── Hasselblad Logo
  └── Decoration Elements
      └── Orange Circle (#FF8933)
}
```

#### レイアウトパターン
1. **横並び配置** (hassel_style_1, 2)
   - 左: デバイス名
   - 右: Hasselbladロゴ + カメラ情報

2. **中央配置** (hassel_text_style_1)
   - 中央: Hロゴ
   - 下: デバイス名 + カメラ情報

#### スケーリングシステム
- **基準サイズ**: 360px (baseImageSize)
- **スケール計算**: `scale = imageWidth / 360`
- **すべての要素**: スケールに応じて拡大縮小

## 📚 作成したドキュメント

### メインドキュメント (4個)

1. **HASSELBLAD_SUMMARY.md**
   - エグゼクティブサマリー
   - 重要な発見のまとめ
   - 次のステップの提案

2. **HASSELBLAD_WATERMARK_INVESTIGATION.md**
   - 詳細な技術仕様
   - データ構造の完全な説明
   - JSON スキーマの解説
   - 実装例（疑似コード）

3. **HASSELBLAD_IMPLEMENTATION_GUIDE.md**
   - ステップバイステップの実装ガイド
   - 完全なKotlinサンプルコード
   - データクラス、パーサー、レンダラーの実装
   - UI統合の例
   - テストコード

4. **photo/EXTRACT_RESOURCES_GUIDE.md**
   - 画像リソース抽出方法
   - JADX-GUIの使用方法
   - APKToolの使用方法
   - トラブルシューティング

### スクリプト (2個)

5. **photo/investigate_watermark.ps1**
   - 詳細調査スクリプト（エラーあり）

6. **photo/investigate_watermark_simple.ps1**
   - 簡略版調査スクリプト（動作確認済み）
   - リソースの自動検出
   - サマリー出力

### アップデート (1個)

7. **README.md**
   - Hasselblad透かし機能のセクション追加
   - ドキュメントへのリンク追加

## 🎨 JSONスタイル例

### hassel_style_1.json の構造
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
  "bitmaps": [{
    "elements": [
      {
        "content": {
          "type": "text",
          "textSource": 0  // デバイス名
        },
        "paint": {
          "fontName": "OplusSans",
          "textSize": 13,
          "fontWeight": 700,
          "colors": ["#000000"]
        }
      },
      {
        "content": {
          "type": "image",
          "bitmap": "hasselblad_watermark_dark",
          "width": 112.8,
          "height": 8.96
        }
      },
      {
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
  }]
}
```

## 💡 実装可能性の評価

### ✅ 実装可能な要素

1. **スタイル定義** - 完全に利用可能
   - JSONファイルはそのまま使用可能
   - パーサーは簡単に実装可能

2. **フォントファイル** - 完全に抽出済み
   - すべてのフォントがローカルに保存済み
   - assetsフォルダに配置するだけ

3. **レイアウトエンジン** - 独自実装可能
   - Canvas APIで実装可能
   - サンプルコードを提供済み

### ⚠️ 要作業の要素

1. **画像リソース** - 抽出が必要
   - JADX-GUIで抽出可能
   - 手順は文書化済み
   - 推定作業時間: 30分-1時間

2. **描画エンジン** - 実装が必要
   - Kotlinで実装
   - サンプルコードあり
   - 推定作業時間: 4-6時間

### ❌ 再現困難な要素

1. **ネイティブライブラリの完全再現**
   - 複雑すぎる
   - しかし、Canvas APIで十分な品質が得られる

2. **OpenGLシェーダー**
   - 一部のエフェクトに使用されている可能性
   - 基本機能には不要

## 📊 実装見積もり

| フェーズ | タスク | 時間 | 難易度 |
|---------|--------|------|--------|
| Phase 1 | リソース抽出 | 1h | 🟢 Easy |
| Phase 1 | リソース配置 | 0.5h | 🟢 Easy |
| Phase 2 | データクラス作成 | 1h | 🟢 Easy |
| Phase 2 | JSONパーサー | 2h | 🟡 Medium |
| Phase 2 | 描画エンジン | 4-6h | 🟡 Medium |
| Phase 3 | アプリ統合 | 2h | 🟢 Easy |
| Phase 3 | UI実装 | 1h | 🟢 Easy |
| Phase 3 | テスト | 2-4h | 🟡 Medium |
| **合計** | - | **13.5-17.5h** | **🟡 Medium** |

## 🚀 次のステップ

### 即座に実行可能

1. **JADX-GUIをダウンロード**
   ```
   https://github.com/skylot/jadx/releases
   ```

2. **APKをdecompile**
   ```
   jadx-gui.bat
   File → Open → photo/com.oneplus.gallery_15.73.22.apk
   Resources → 検索: "hasselblad"
   ```

3. **画像を抽出**
   - hasselblad_watermark_dark
   - hassel_watermark_h_logo_dark
   - hassel_watermark_h_logo_text_style
   - hasselblad_watermark_text_style

### 実装開始

4. **プロジェクトセットアップ**
   ```kotlin
   app/src/main/assets/
   ├── watermark_styles/
   ├── watermark_fonts/
   └── watermark_images/
   ```

5. **コード実装**
   - HASSELBLAD_IMPLEMENTATION_GUIDE.mdを参照
   - サンプルコードをコピー＆カスタマイズ

## ⚠️ 重要な注意事項

### 法的考慮
- **Hasselbladの商標**: Hasselblad ABの所有物
- **ロゴの使用**: 商用利用には許可が必要な可能性
- **推奨**: 個人使用または教育目的に限定
- **代替案**: 独自のロゴに置き換えることも可能

### 技術的制限
- ネイティブライブラリの完全な機能は再現困難
- 一部のエフェクトは簡略化が必要
- 大きな画像ではパフォーマンスチューニングが必要

## 📝 成果物サマリー

### ドキュメント
- ✅ 調査報告書 (4個のマークダウンファイル)
- ✅ 実装ガイド (完全なサンプルコード付き)
- ✅ リソース抽出ガイド
- ✅ README更新

### データ
- ✅ JSONスタイル定義 (42個、うちHasselblad 8個)
- ✅ フォントファイル (4個、計15.4MB)
- ✅ ネイティブライブラリ情報
- ✅ 設定ファイル分析

### スクリプト
- ✅ PowerShell調査スクリプト (2個)
- ✅ リソース検索スクリプト

## 🎯 結論

**Hasselblad透かし機能は完全に実装可能です！**

### 成功要因
1. ✅ すべての必要なリソースを特定
2. ✅ データ構造を完全に解析
3. ✅ 実装方法を文書化
4. ✅ サンプルコードを提供

### 残作業
1. ⚠️ 画像リソースの抽出 (30分)
2. ⚠️ コードの実装 (13-17時間)
3. ⚠️ テストとデバッグ (2-4時間)

### 推奨アプローチ
1. **MVP (Minimum Viable Product)**
   - 1つのスタイルのみサポート
   - 基本的な描画機能
   - 推定時間: 6-8時間

2. **フル機能版**
   - すべてのスタイルをサポート
   - プレビュー機能
   - カスタマイズ機能
   - 推定時間: 13-17時間

## 📞 質問がある場合

すべての情報は以下のドキュメントに記載されています：

1. **概要を知りたい** → HASSELBLAD_SUMMARY.md
2. **技術詳細を知りたい** → HASSELBLAD_WATERMARK_INVESTIGATION.md
3. **実装方法を知りたい** → HASSELBLAD_IMPLEMENTATION_GUIDE.md
4. **画像抽出方法を知りたい** → photo/EXTRACT_RESOURCES_GUIDE.md

---

**調査完了日**: 2025年10月5日 19:00
**総作業時間**: 約2時間
**ステータス**: ✅ 完了

実装、頑張ってください！🚀✨
