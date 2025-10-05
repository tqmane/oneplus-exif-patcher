# ビルドと署名の手順

## ローカルでのビルド

### Windows

```cmd
build.bat
```

### Linux/macOS

```bash
chmod +x build.sh
./build.sh
```

または、直接Gradleコマンドを使用:

```bash
# Linux/macOS
./gradlew assembleDebug
./gradlew assembleRelease

# Windows
gradlew.bat assembleDebug
gradlew.bat assembleRelease
```

---

## 署名の設定

### 1. キーストアの作成

リリース版のAPKに署名するには、まずキーストアを作成する必要があります。

#### Windowsの場合:

```cmd
keytool -genkey -v -keystore release.keystore -alias your_key_alias -keyalg RSA -keysize 2048 -validity 10000
```

#### Linux/macOSの場合:

```bash
keytool -genkey -v -keystore release.keystore -alias your_key_alias -keyalg RSA -keysize 2048 -validity 10000
```

**重要な情報（必ず安全に保管してください）:**
- **キーストアパスワード**: キーストアファイル自体のパスワード
- **キーエイリアス**: 鍵の名前（例: `my-release-key`）
- **キーパスワード**: その鍵のパスワード

### 2. keystore.propertiesファイルの作成

プロジェクトルートに `keystore.properties` ファイルを作成します:

```bash
cp keystore.properties.example keystore.properties
```

`keystore.properties` を編集して、実際の値を入力:

```properties
storeFile=release.keystore
storePassword=your_store_password_here
keyAlias=your_key_alias_here
keyPassword=your_key_password_here
```

**注意**: `keystore.properties` と `release.keystore` はGitにコミットしないでください！

---

## GitHub Actionsでの署名（詳細手順）

GitHub Actionsでリリース版APKをビルドして署名するには、キーストアをGitHubのSecretsに保存する必要があります。

### ステップ1: キーストアをBase64エンコード

キーストアファイルはバイナリなので、GitHubのSecretsに保存するためにBase64形式に変換します。

#### Linux/macOS の場合:

```bash
base64 -i release.keystore -o keystore.base64
```

#### Windows (PowerShell) の場合:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File keystore.base64 -Encoding ASCII
```

#### Windows (Git Bash) の場合:

```bash
base64 -w 0 release.keystore > keystore.base64
```

これで `keystore.base64` ファイルが作成されます。

---

### ステップ2: GitHubリポジトリのSecretsに追加

#### 2-1. GitHubリポジトリを開く

ブラウザで https://github.com/tqmane/oneplus-exif-patcher を開きます。

#### 2-2. Settings → Secrets and variables → Actions に移動

1. リポジトリのページ上部の **Settings** タブをクリック
2. 左側のメニューから **Secrets and variables** → **Actions** を選択
3. 右上の **New repository secret** ボタンをクリック

#### 2-3. 以下の4つのシークレットを追加

各シークレットについて、**New repository secret** ボタンをクリックして追加します:

##### 1. KEYSTORE_BASE64

- **Name**: `KEYSTORE_BASE64`
- **Secret**: `keystore.base64` ファイルの内容全体をコピー&ペースト
  ```bash
  # ファイルの内容を表示してコピー
  cat keystore.base64
  ```
  または、テキストエディタで `keystore.base64` を開いて全文をコピー
- **Add secret** をクリック

##### 2. KEYSTORE_PASSWORD

- **Name**: `KEYSTORE_PASSWORD`
- **Secret**: キーストア作成時に設定したパスワード（例: `myStorePassword123`）
- **Add secret** をクリック

##### 3. KEY_ALIAS

- **Name**: `KEY_ALIAS`
- **Secret**: キーストア作成時に設定したエイリアス（例: `my-release-key`）
- **Add secret** をクリック

##### 4. KEY_PASSWORD

- **Name**: `KEY_PASSWORD`
- **Secret**: キー作成時に設定したパスワード（例: `myKeyPassword123`）
- **Add secret** をクリック

---

### ステップ3: シークレットが正しく設定されたか確認

1. **Settings** → **Secrets and variables** → **Actions** のページで、以下の4つのシークレットが表示されていることを確認:
   - ✅ KEYSTORE_BASE64
   - ✅ KEYSTORE_PASSWORD
   - ✅ KEY_ALIAS
   - ✅ KEY_PASSWORD

2. シークレットの値は表示されませんが、名前と「Updated」の日時が表示されます。

---

### ステップ4: GitHub Actionsでビルドを実行

シークレットを設定したら、GitHub Actionsが自動的にリリースAPKをビルドします。

#### 自動ビルド

以下の場合、自動的にビルドが実行されます:
- `main` または `develop` ブランチへのプッシュ
- プルリクエストの作成

#### 手動ビルド

1. リポジトリの **Actions** タブを開く
2. 左側から **Android Build** ワークフローを選択
3. 右上の **Run workflow** ボタンをクリック
4. ブランチを選択して **Run workflow** をクリック

---

### ステップ5: ビルド結果の確認

1. **Actions** タブでビルドの進行状況を確認
2. ビルドが完了したら、ワークフローの実行ページを開く
3. 下にスクロールして **Artifacts** セクションを確認
4. 以下の2つのAPKがダウンロード可能:
   - **app-debug**: デバッグ版APK
   - **app-release**: 署名済みリリース版APK（シークレットが設定されている場合）

---

### 🔒 セキュリティのヒント

1. **keystore.base64ファイルを削除**: Secretsに追加した後は、ローカルの `keystore.base64` ファイルを削除してください
   ```bash
   rm keystore.base64
   ```

2. **キーストアファイルのバックアップ**: `release.keystore` ファイルは安全な場所にバックアップしてください。紛失すると、アプリの更新ができなくなります。

3. **パスワードの管理**: キーストアとキーのパスワードは、パスワードマネージャーなどで安全に保管してください。

---

### ❓ トラブルシューティング

#### リリースAPKが生成されない

**症状**: GitHub Actionsでデバッグ版だけがビルドされ、リリース版がビルドされない

**原因**: `KEYSTORE_BASE64` シークレットが設定されていない、または空

**解決策**: 
1. Secretsページで `KEYSTORE_BASE64` が存在するか確認
2. 値が正しく設定されているか確認（空白や改行がないか）

#### 署名エラーが発生する

**症状**: ビルドログに "Keystore was tampered with, or password was incorrect" などのエラー

**原因**: パスワードまたはエイリアスが間違っている

**解決策**:
1. `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` の値を確認
2. ローカルでビルドして、同じ設定で動作するか確認
   ```bash
   ./gradlew assembleRelease
   ```

#### Base64エンコードエラー

**症状**: ビルドログに "Keystore file not found" や "Invalid keystore format"

**原因**: Base64エンコードが正しくない

**解決策**:
1. `keystore.base64` ファイルに余分な改行や空白がないか確認
2. Windowsの場合、エンコーディングを `ASCII` に指定
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File keystore.base64 -Encoding ASCII -NoNewline
   ```

---

### 📸 スクリーンショット付き手順

#### 1. Settings → Secrets and variables → Actions

![GitHub Settings](https://docs.github.com/assets/cb-21851/images/help/repository/repo-actions-settings.png)

#### 2. New repository secret

シークレットの追加画面で:
- **Name**: シークレット名（例: `KEYSTORE_BASE64`）
- **Secret**: 値を貼り付け
- **Add secret** をクリック

![Add Secret](https://docs.github.com/assets/cb-49108/images/help/settings/actions-secrets-add-secret.png)

---

### ✅ まとめ

GitHub Actionsでリリース版をビルドするには:

1. ✅ キーストアをBase64エンコード
2. ✅ 4つのシークレットを追加（KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD）
3. ✅ コードをプッシュまたは手動でワークフローを実行
4. ✅ Artifactsから署名済みAPKをダウンロード

これで、GitHub Actionsが自動的に署名済みリリースAPKをビルドします！
3. **KEY_ALIAS**: キーのエイリアス
4. **KEY_PASSWORD**: キーのパスワード

### 3. ワークフローの実行

これらのシークレットを設定すると、GitHub Actionsが自動的にリリースAPKをビルドして署名します。

---

## APKの場所

ビルド後のAPKは以下の場所に生成されます:

- **Debug**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `app/build/outputs/apk/release/app-release.apk`

---

## セキュリティに関する注意事項

⚠️ **重要**: 以下のファイルは絶対にGitにコミットしないでください:

- `release.keystore` (またはその他の .keystore ファイル)
- `keystore.properties`
- `keystore.base64`

これらのファイルは `.gitignore` に追加されています。

紛失した場合、同じ署名でアプリを更新できなくなるため、キーストアファイルとパスワードは安全な場所にバックアップしてください。

---

## トラブルシューティング

### 「Keystore file not found」エラー

`keystore.properties` の `storeFile` パスが正しいか確認してください。相対パスまたは絶対パスを使用できます。

### GitHub Actionsでリリースビルドがスキップされる

`KEYSTORE_BASE64` シークレットが正しく設定されているか確認してください。シークレットが設定されていない場合、リリースビルドは自動的にスキップされます（これは意図的な動作です）。

### 署名エラー

キーストアパスワード、キーエイリアス、キーパスワードが正しいか確認してください。
