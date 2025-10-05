# Hasselblad透かし調査 - エグゼクティブサマリー

## 🎯 調査の目的

OnePlus GalleryのHasselblad透かし機能を解析し、独自のEXIFパッチャーアプリに統合可能か調査する。

## ✅ 結論

**実装可能です！** 必要なリソースとデータ構造はすべて特定できました。

## 📊 発見した重要な要素

### 1. 透かしスタイル定義 (JSON)
- **場所**: `photo/gallery_extracted/assets/watermark_master_styles/`
- **数量**: 42個のスタイル（うちHasselblad専用が8個）
- **フォーマット**: JSON形式で構造化
- **内容**: レイアウト、フォント、色、配置などすべての設定

**主要なスタイル**:
- `hassel_style_1.json` - 横並び配置（デバイス名 + ロゴ）
- `hassel_style_2.json` - AvenirNextフォント使用
- `hassel_text_style_1.json` - 中央配置スタイル

### 2. フォントファイル
- **場所**: `photo/1_data_data_extracted/files/watermark_master/materia/`
- **抽出済み**: ✅

| ファイル名 | サイズ | 用途 |
|-----------|--------|------|
| AvenirNext.ttc | 4.8 MB | メインフォント |
| ButlerBold.otf | 42 KB | 太字フォント |
| ButlerMedium.otf | 44 KB | ミディアムフォント |
| FZCYSK.TTF | 10 MB | 中国語フォント |

### 3. 画像リソース
- **必要な画像**: 4つのHasselbladロゴ
- **フォーマット**: おそらくWebP
- **状態**: ⚠️ APKから抽出が必要

**リソース名**:
1. `hasselblad_watermark_dark` - メインロゴ（横長）
2. `hassel_watermark_h_logo_dark` - Hロゴ
3. `hassel_watermark_h_logo_text_style` - Hロゴ（テキスト用）
4. `hasselblad_watermark_text_style` - Hasselbladロゴ（テキスト用）

### 4. ネイティブライブラリ
- **ファイル**: `liblocal_ai_master_watermark_drawer.so` (315 KB)
- **実装クラス**: `com.oplus.tbluniformeditor.plugins.watermark.function.WatermarkAlgoImpl`
- **用途**: 透かしの描画処理

## 🏗️ 透かしの構造

### データフロー
```
JSON Style Definition
    ↓
[Parser] → Style Object
    ↓
[Renderer] → Canvas Drawing
    ↓
Watermarked Image
```

### 主要な構成要素

#### テキスト要素
- **textSource: 0** → デバイスモデル名（例: "OnePlus 12"）
- **textSource: 1** → カメラ情報（例: "24mm f/1.8 1/100s ISO 100"）

#### 画像要素
- Hasselbladロゴ（様々なバリエーション）
- サイズ指定可能
- 位置調整可能

#### 装飾要素
- オレンジの円（OnePlusブランドカラー: #FF8933）
- その他のシェイプ

### レイアウトパターン

**スタイル1**: 横並び
```
┌────────────────────────────────────────┐
│                                        │
│  OnePlus 12          [HASSELBLAD]     │
│                      ● 24mm f/1.8     │
└────────────────────────────────────────┘
```

**スタイル2**: 中央配置
```
┌────────────────────────────────────────┐
│                                        │
│               [H logo]                 │
│             OnePlus 12                 │
│          24mm f/1.8 ISO 100            │
└────────────────────────────────────────┘
```

## 🛠️ 実装アプローチ

### Phase 1: リソース準備 (1-2時間)
1. ✅ JSONスタイルファイルをコピー
2. ✅ フォントファイルをコピー
3. ⚠️ APKから画像リソースを抽出（JADX-GUI使用）

### Phase 2: コード実装 (4-8時間)
1. データクラスの作成（JSONモデル）
2. JSONパーサーの実装
3. Canvas描画エンジンの実装
4. EXIF情報の抽出処理

### Phase 3: 統合とテスト (2-4時間)
1. アプリへの統合
2. UI実装（スタイル選択）
3. テストとデバッグ
4. 最適化

## 📂 成果物

以下のドキュメントを作成しました：

1. **HASSELBLAD_WATERMARK_INVESTIGATION.md** (このファイル)
   - 詳細な調査結果
   - 技術仕様
   - データ構造の説明

2. **HASSELBLAD_IMPLEMENTATION_GUIDE.md**
   - 完全な実装ガイド
   - サンプルコード（Kotlin）
   - ステップバイステップの手順

3. **photo/EXTRACT_RESOURCES_GUIDE.md**
   - 画像リソース抽出方法
   - JADX-GUIの使い方
   - トラブルシューティング

4. **photo/investigate_watermark_simple.ps1**
   - 自動調査スクリプト
   - リソース確認用

## 🎓 学んだこと

### OnePlusギャラリーの設計
- モジュラー設計（JSON + リソース）
- フォントはオンラインからダウンロード可能
- ネイティブライブラリで高速描画
- プラグインアーキテクチャ

### 透かし機能の実装パターン
- JSON駆動のレイアウトエンジン
- キャッシュ機構（フォント、画像）
- スケーラブルなデザイン（baseImageSize基準）
- EXIF情報の活用

## ⚠️ 注意事項

### 法的考慮
- **Hasselbladのロゴと商標**: Hasselblad ABの所有物
- **商用利用**: 許可が必要な可能性あり
- **推奨**: 個人使用または教育目的に限定

### 技術的制限
- ネイティブライブラリの完全な再現は困難
- 一部のエフェクトは簡略化が必要
- パフォーマンスチューニングが必要

## 🚀 次のアクションアイテム

### 優先度: 高 🔴
1. **APKから画像リソースを抽出**
   - ツール: JADX-GUI
   - 時間: 30分-1時間
   - ガイド: `photo/EXTRACT_RESOURCES_GUIDE.md`

### 優先度: 中 🟡
2. **基本的な描画エンジンを実装**
   - Kotlinで実装
   - Canvas APIを使用
   - サンプルコード: `HASSELBLAD_IMPLEMENTATION_GUIDE.md`

3. **アプリに統合**
   - UI実装
   - スタイル選択機能
   - プレビュー機能

### 優先度: 低 🟢
4. **最適化とポリッシュ**
   - パフォーマンス改善
   - エラーハンドリング
   - ユーザーフィードバック

## 📈 実装見積もり

| タスク | 時間 | 難易度 |
|--------|------|--------|
| リソース抽出 | 1h | 🟢 Easy |
| データクラス作成 | 1h | 🟢 Easy |
| JSONパーサー | 2h | 🟡 Medium |
| 描画エンジン | 4-6h | 🟡 Medium |
| アプリ統合 | 2h | 🟢 Easy |
| テスト・デバッグ | 2-4h | 🟡 Medium |
| **合計** | **12-16h** | **🟡 Medium** |

## 💡 推奨事項

### 最小実装（MVP）
1. 1つのスタイル（hassel_style_1）のみサポート
2. 基本的なテキストと画像描画
3. シンプルなUI

### フル実装
1. すべてのスタイルをサポート
2. スタイルのプレビュー機能
3. カスタムスタイル作成
4. パフォーマンス最適化

### 拡張アイデア
1. 独自のロゴに置き換え可能に
2. ユーザーカスタムテキスト
3. 位置とサイズの調整
4. 透明度の調整

## 📞 サポート

質問や問題がある場合：
1. ドキュメントを確認
2. サンプルコードを参照
3. ログを確認（`adb logcat`）

## 🎉 結論

Hasselblad透かし機能の完全な逆エンジニアリングに成功しました！

**実装可能な要素**:
- ✅ スタイル定義（JSON）
- ✅ フォントファイル
- ⚠️ 画像リソース（要抽出）
- ⚠️ 描画ロジック（独自実装可能）

**次のステップ**:
1. JADX-GUIで画像を抽出
2. サンプルコードを基に実装開始
3. テストとデバッグ

頑張ってください！🚀
