# OnePlus EXIF Patcher

[日本語](#日本語) | [English](#english)

---

## 日本語

### 概要

OnePlus EXIF Patcherは、画像のEXIFメタデータにOnePlus固有の情報を自動的に追加するAndroidアプリケーションです。複数の画像を一括処理し、OnePlusデバイス互換の形式に変換できます。

### 🚀 主な機能

- ✅ **Kotlin + Jetpack Compose** - モダンなAndroid開発技術を採用
- ✅ **Material Design 3 UI** - 美しく直感的なユーザーインターフェース
- ✅ **複数画像選択** - フォトピッカーで複数の画像を一度に選択
- ✅ **自動EXIFパッチ適用** - 以下の情報を自動的に追加:
  - **Device**: `0xcdcc8c3fff`
  - **UserComment**: `oplus_1048864`
  - **Make**: `OnePlus`
- ✅ **保存先選択** - ユーザーが処理済み画像の保存先を選択可能
- ✅ **バックグラウンド処理** - 進捗表示付きの非同期処理
- ✅ **クリーンなアーキテクチャ** - MVVM パターンを採用

### 📋 必要要件

- Android 7.0 (API Level 24) 以上
- Kotlin 1.9.20
- Gradle 8.2.0

### 🏗️ プロジェクト構造

```
app/
├── src/main/
│   ├── java/com/oneplus/exifpatcher/
│   │   ├── MainActivity.kt           # メインアクティビティと UI
│   │   ├── MainViewModel.kt          # 状態管理とビジネスロジック
│   │   ├── data/
│   │   │   └── ImageRepository.kt    # データ処理レイヤー
│   │   ├── util/
│   │   │   └── ExifPatcher.kt        # EXIF編集ユーティリティ
│   │   └── ui/theme/                 # Material Design 3 テーマ
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml           # 文字列リソース
│   │   │   ├── colors.xml            # カラーリソース
│   │   │   └── themes.xml            # テーマ定義
│   │   └── xml/                      # バックアップルール
│   └── AndroidManifest.xml           # アプリケーション設定
└── build.gradle.kts                  # アプリレベルのビルド設定
```

### 🎯 使い方

1. **アプリを起動**
2. **「画像を選択」ボタンをタップ** - フォトピッカーから処理したい画像を選択
3. **「保存先を選択」ボタンをタップ** - 処理済み画像を保存するフォルダを選択
4. **「画像を処理」ボタンをタップ** - 処理が開始されます
5. **完了を待つ** - 進捗バーで処理状況を確認できます

### 🛠️ ビルド方法

#### 簡単なビルド（推奨）

**Windows:**
```cmd
build.bat
```

**Linux/macOS:**
```bash
chmod +x build.sh
./build.sh
```

#### 詳細なビルド手順

```bash
# リポジトリをクローン
git clone https://github.com/tqmane/oneplus-exif-patcher.git
cd oneplus-exif-patcher

# デバッグ版ビルド
./gradlew assembleDebug

# リリース版ビルド（署名設定が必要）
./gradlew assembleRelease

# APKをインストール
./gradlew installDebug
```

**📝 署名とリリースビルドの詳細**: [BUILD_AND_SIGNING.md](BUILD_AND_SIGNING.md) を参照してください。

### 📱 技術スタック

- **言語**: Kotlin
- **UI フレームワーク**: Jetpack Compose
- **アーキテクチャ**: MVVM (Model-View-ViewModel)
- **非同期処理**: Kotlin Coroutines
- **画像処理**: AndroidX ExifInterface
- **デザインシステム**: Material Design 3

### 🔧 主要コンポーネント

#### ExifPatcher
EXIFメタデータの編集を担当するユーティリティクラス。OnePlus固有のタグを画像に追加します。

#### ImageRepository
画像処理操作を管理するリポジトリクラス。UIレイヤーとデータレイヤーを分離します。

#### MainViewModel
アプリケーションの状態管理とビジネスロジックを処理。UIの状態をFlowで管理します。

#### MainActivity
Jetpack Composeを使用したメインUI。Material Design 3に準拠したモダンなインターフェース。

### 📄 ライセンス

このプロジェクトはオープンソースです。

### 🤝 貢献

プルリクエストを歓迎します！バグ報告や機能提案はIssuesでお願いします。

---

## English

### Overview

OnePlus EXIF Patcher is an Android application that automatically adds OnePlus-specific information to image EXIF metadata. It can batch process multiple images and convert them to OnePlus device-compatible format.

### 🚀 Key Features

- ✅ **Kotlin + Jetpack Compose** - Modern Android development technologies
- ✅ **Material Design 3 UI** - Beautiful and intuitive user interface
- ✅ **Multiple Image Selection** - Select multiple images at once with photo picker
- ✅ **Automatic EXIF Patching** - Automatically adds the following information:
  - **Device**: `0xcdcc8c3fff`
  - **UserComment**: `oplus_1048864`
  - **Make**: `OnePlus`
- ✅ **Destination Selection** - Users can choose where to save processed images
- ✅ **Background Processing** - Asynchronous processing with progress display
- ✅ **Clean Architecture** - MVVM pattern implementation

### 📋 Requirements

- Android 7.0 (API Level 24) or higher
- Kotlin 1.9.20
- Gradle 8.2.0

### 🏗️ Project Structure

```
app/
├── src/main/
│   ├── java/com/oneplus/exifpatcher/
│   │   ├── MainActivity.kt           # Main activity and UI
│   │   ├── MainViewModel.kt          # State management and business logic
│   │   ├── data/
│   │   │   └── ImageRepository.kt    # Data processing layer
│   │   ├── util/
│   │   │   └── ExifPatcher.kt        # EXIF editing utility
│   │   └── ui/theme/                 # Material Design 3 theme
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml           # String resources
│   │   │   ├── colors.xml            # Color resources
│   │   │   └── themes.xml            # Theme definitions
│   │   └── xml/                      # Backup rules
│   └── AndroidManifest.xml           # Application configuration
└── build.gradle.kts                  # App-level build configuration
```

### 🎯 How to Use

1. **Launch the app**
2. **Tap "Select Images" button** - Choose images to process from the photo picker
3. **Tap "Select Destination" button** - Choose a folder to save processed images
4. **Tap "Process Images" button** - Processing will start
5. **Wait for completion** - Monitor progress with the progress bar

### 🛠️ Build Instructions

```bash
# Clone the repository
git clone https://github.com/tqmane/oneplus-exif-patcher.git
cd oneplus-exif-patcher

# Build the project
./gradlew build

# Install APK
./gradlew installDebug
```

### 📱 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Async Processing**: Kotlin Coroutines
- **Image Processing**: AndroidX ExifInterface
- **Design System**: Material Design 3

### 🔧 Key Components

#### ExifPatcher
Utility class responsible for editing EXIF metadata. Adds OnePlus-specific tags to images.

#### ImageRepository
Repository class managing image processing operations. Separates UI layer from data layer.

#### MainViewModel
Handles application state management and business logic. Manages UI state with Flow.

#### MainActivity
Main UI using Jetpack Compose. Modern interface compliant with Material Design 3.

### 📄 License

This project is open source.

### 🤝 Contributing

Pull requests are welcome! Please report bugs or suggest features through Issues.