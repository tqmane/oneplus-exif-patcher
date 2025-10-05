# OxygenOS Hasselblad Watermark Adaptation

## 概要
- OxygenOS ギャラリー APK から抽出した `watermark_master_styles/*.json` を既存のパーサーで解釈できるように整理しました。
- フォント／アイコンなど外部リソースを `app/src/main/assets` および `res/drawable-nodpi` に取り込み、JSON の `bitmap` / `fontName` 参照が解決されるようマッピングしています（Avenir Next / Butler / FZYaSong の正式フォントを同梱）。
- `HasselbladWatermarkRenderer` にフォント別名テーブルとアセット検出ロジックを実装し、OEM フォント名をアプリに同梱したフォントでフォールバックできるようにしました。
- 水平・縦向き双方のスタイル定義で既存の `WatermarkStyle` モデルへ落とし込み済み。`WatermarkStyleParser` 側の変更は不要でした。

## 取り込んだリソース
### フォント
| OEM 名称 (`fontName`) | 導入ファイル | 備考 |
| --- | --- | --- |
| `AvenirNext.zip` / `AveNextMedium.ttc` | `app/src/main/assets/fonts/AvenirNext.ttc` | OxygenOS から抽出した TTC を同梱。`ttcIndex` の指定は現在未使用（フェイス0を参照）。 |
| `Butler.zip` / `ButlerBold.zip` / `BulterBold.zip` | `app/src/main/assets/fonts/ButlerMedium.otf` / `app/src/main/assets/fonts/ButlerBold.otf` | ウェイト値に応じて Medium / Bold を動的に切替。 |
| `FZYaSongDB1GBK.zip` | `app/src/main/assets/fonts/FZCYSK.TTF` | 中国語表示向けの公式フォント。 |
| `OplusSans` | システムフォント (`sans-serif-medium`) | 実端末と同等のウェイトになるよう Android システムフォントを使用。 |
| `RadomirTinkovGilroy-Medium` | `app/src/main/assets/fonts/RadomirTinkovGilroy-Medium.otf` | 旧来のプレースホルダーフォント。OEM 名称に存在しない場合のバックアップとして残置。 |

> **代替フォントの差し替え:** 別リビジョンのフォントに更新する場合は `app/src/main/assets/fonts` に配置し、`HasselbladWatermarkRenderer.fontAliasMap` を調整してください。

### アイコン／ビットマップ
以下を `app/src/main/res/drawable-nodpi/` に追加しました。
- `color_card_explore.webp`
- `color_card_inspiration.webp`
- `color_card_perspective.webp`
- `hyper_image.webp`
- `hyper_image_dark.webp`
- `songkran_watermark_icon.png`
- `songkran_watermark_style_thumbnail.png` (暫定: `songkran_watermark_icon.png` を複製)
- `songkran_watermark_style_large_thumbnail.png` (暫定: 同上)

既存で取り込んでいた `classic_*`, `master_sign_*`, `realme_*`, `hasselblad_*` なども JSON 側で参照されています。

## 実装ポイント
### フォント解決 (`HasselbladWatermarkRenderer`)
- `fontAliasMap` に OEM 由来名称 → アセット相対パスのマッピングを定義。
- `resolveTypefaceFromAssets` が alias → `app/src/main/assets/fonts` を探索し、存在確認後に `Typeface.createFromAsset` で読み込み。
- フォントウェイト（数値／`bold`）に応じて Bold 用アセットへ切り替えるロジックを追加。
- `systemFontAliasMap` 経由で `OplusSans` などは Android システムフォントにフォールバック。
- `Typeface` のキャッシュキーを正規化済み文字列＋スタイルで構成し、描画コストを削減。

### パーサー／モデル
- `WatermarkStyleParser` のレガシー JSON 変換でテキスト要素の `fontFamily` がそのまま保持されるため、レンダラー側で alias を解決するだけで十分でした。
- `WatermarkStyle` / `WatermarkElement` へのモデル変換ロジックはそのまま利用可能。追加作業は不要です。

### 画像リソース
- `loadBitmap` は `Context#getIdentifier()` で drawable リソースを解決するため、`bitmap` 名と同名のファイルを `drawable-nodpi` 配下に配置すれば描画できます。
- Songkran 用サムネイルは暫定でアイコンの複製を置いています。正式素材が入手できたら差し替えてください。

## 組み込み手順サマリー
1. `app/src/main/assets/watermark/` に OEM スタイル JSON を配置 (`hassel_style_*`, `personalize_*` など)。
2. `WatermarkStyleParser.loadStyleFromAssets("<ファイル名>")` で `WatermarkStyle` を取得。
3. `HasselbladWatermarkRenderer.renderWatermark()` に元ビットマップ・スタイル・カメラ情報を渡して描画。
4. 必要に応じて `WatermarkElement` 内の `text`／`textSource` を差し替えてカスタマイズ。

## 制限事項 / 今後の TODO
- `ttcIndex` で指定されたサブフェイスの完全再現（例: Avenir Next の別ウェイト）は今後の課題。現状は TTC 内のデフォルトフェイスを参照しています。
- Songkran スタイルのサムネイル画像は暫定差し替えです。公式素材が入手でき次第置き換えてください。
- OEM 側で AI / IPU を利用する高度な描画機能（シェーダーやネイティブライブラリ）は未複製。既存の 2D レイヤ描画のみ再現対象としています。

## 検証
- 追加リソースが読み込めることをローカル環境で手動確認。
- Gradle ビルドは後続の sanity check で実行予定です。
