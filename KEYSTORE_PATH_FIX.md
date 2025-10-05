# Keystore Path Fix Report

## 🔧 問題

GitHub Actionsでリリースビルドが失敗：

```
FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:validateSigningRelease'.
> Keystore file '/home/runner/work/***/****/app/release.keystore' not found for signing config 'release'.
```

## 🔍 原因

**パスの不一致**:
- **app/build.gradle.kts**: `app/release.keystore` を参照
- **GitHub Actions**: ルートディレクトリに `release.keystore` を作成

```yaml
# 修正前
run: |
  echo "$KEYSTORE_BASE64" | base64 -d > release.keystore
```

## ✅ 修正内容

### .github/workflows/android-build.yml

#### 1. Keystoreの出力先を修正

```yaml
# 修正前
- name: Decode Keystore
  run: |
    echo "$KEYSTORE_BASE64" | base64 -d > release.keystore

# 修正後
- name: Decode Keystore
  run: |
    echo "$KEYSTORE_BASE64" | base64 -d > app/release.keystore
```

#### 2. 環境変数のパスを修正

```yaml
# 修正前
- name: Build Release APK
  env:
    KEYSTORE_FILE: release.keystore

# 修正後
- name: Build Release APK
  env:
    KEYSTORE_FILE: app/release.keystore
```

## 📁 ファイル構造

```
oneplus-exif-patcher/
├── app/
│   ├── release.keystore          ← GitHub Actionsで作成（ビルド時のみ）
│   └── build.gradle.kts          ← このパスを参照
├── release.keystore               ← ローカル開発用（.gitignoreで除外）
└── .github/
    └── workflows/
        └── android-build.yml      ← 修正したファイル
```

## 🎯 動作確認

### 修正後の流れ

1. **GitHub Secretsからデコード**
   ```bash
   echo "$KEYSTORE_BASE64" | base64 -d > app/release.keystore
   ```

2. **環境変数設定**
   ```bash
   KEYSTORE_FILE=app/release.keystore
   ```

3. **Gradle Build**
   ```kotlin
   // app/build.gradle.kts
   storeFile = System.getenv("KEYSTORE_FILE")?.let { file(it) }
   // → file("app/release.keystore")
   ```

4. **APK署名成功**
   ```
   ✅ app/build/outputs/apk/release/app-release.apk
   ```

## 📝 チェックリスト

ビルドが成功するための条件:

- [x] ワークフローで `app/release.keystore` に出力
- [x] 環境変数 `KEYSTORE_FILE` を `app/release.keystore` に設定
- [ ] GitHub Secretsに以下を設定（ユーザー側で実施）:
  - `KEYSTORE_BASE64`
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`

## 🚀 次のステップ

1. **変更をコミット・プッシュ**
   ```bash
   git add .github/workflows/android-build.yml
   git commit -m "fix: Correct keystore path in GitHub Actions workflow"
   git push
   ```

2. **GitHub Secretsを設定**（まだの場合）
   - 手順は [GITHUB_ACTIONS_SIGNING_QUICKSTART.md](GITHUB_ACTIONS_SIGNING_QUICKSTART.md) を参照

3. **ビルド実行**
   - プッシュで自動実行
   - または https://github.com/tqmane/oneplus-exif-patcher/actions から手動実行

## 💡 補足

### ローカル開発の場合

`keystore.properties` で設定:
```properties
storeFile=release.keystore
storePassword=your_password
keyAlias=your_alias
keyPassword=your_password
```

→ ルートディレクトリの `release.keystore` を使用

### CI/CD（GitHub Actions）の場合

環境変数で設定:
```yaml
KEYSTORE_FILE: app/release.keystore
```

→ `app/release.keystore` を使用（ワークフローで動的に生成）

この違いにより、ローカルとCI/CD環境で適切なkeystoreが使い分けられます。

---

**修正日時**: 2025年10月5日  
**影響範囲**: GitHub Actions（リリースビルド）のみ  
**破壊的変更**: なし
