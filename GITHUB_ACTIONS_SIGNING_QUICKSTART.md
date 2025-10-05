# GitHub Actions 署名設定 クイックガイド

## 📋 必要なもの

- ✅ `release.keystore` ファイル
- ✅ キーストアのパスワード
- ✅ キーのエイリアス
- ✅ キーのパスワード

---

## 🚀 5分で設定完了！

### 1️⃣ キーストアをBase64に変換

**Windows (PowerShell):**
```powershell
cd C:\Users\Tqmane\Documents\Git\oneplus-exif-patcher
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File keystore.base64 -Encoding ASCII -NoNewline
```

**Linux/macOS:**
```bash
base64 -i release.keystore -o keystore.base64
```

これで `keystore.base64` ファイルができます。

---

### 2️⃣ GitHubでシークレットを設定

#### ブラウザで開く:
https://github.com/tqmane/oneplus-exif-patcher/settings/secrets/actions

#### 4つのシークレットを追加:

| シークレット名 | 値の取得方法 |
|--------------|------------|
| **KEYSTORE_BASE64** | `keystore.base64` をメモ帳で開いて全文コピー |
| **KEYSTORE_PASSWORD** | キーストア作成時に設定したパスワード |
| **KEY_ALIAS** | キー作成時に設定したエイリアス名 |
| **KEY_PASSWORD** | キー作成時に設定したパスワード |

#### 追加手順:
1. **New repository secret** ボタンをクリック
2. **Name** にシークレット名を入力（例: `KEYSTORE_BASE64`）
3. **Secret** に値を貼り付け
4. **Add secret** をクリック
5. 残り3つも同様に追加

---

### 3️⃣ 確認

**Settings → Secrets and variables → Actions** で4つ全て表示されていればOK:

- ✅ KEYSTORE_BASE64
- ✅ KEYSTORE_PASSWORD  
- ✅ KEY_ALIAS
- ✅ KEY_PASSWORD

---

### 4️⃣ ビルド実行

コードをプッシュすると自動でビルド開始！

または手動で実行:
1. https://github.com/tqmane/oneplus-exif-patcher/actions
2. **Android Build** → **Run workflow**

---

### 5️⃣ APKをダウンロード

1. **Actions** タブを開く
2. 完了したワークフローをクリック
3. **Artifacts** から `app-release` をダウンロード

完了！🎉

---

## 🔍 チェックリスト

設定前に確認:

- [ ] `release.keystore` ファイルが存在する
- [ ] キーストアのパスワードを覚えている
- [ ] キーのエイリアスを覚えている
- [ ] キーのパスワードを覚えている

設定後に確認:

- [ ] 4つのシークレットが全て追加された
- [ ] `keystore.base64` ファイルを削除した（セキュリティのため）
- [ ] GitHub Actionsでビルドが成功した
- [ ] `app-release` が Artifacts にある

---

## ❌ エラーが出た場合

### "Keystore file not found"
→ `KEYSTORE_BASE64` の値を確認。改行や空白がないか確認。

### "Password was incorrect"
→ `KEYSTORE_PASSWORD` と `KEY_PASSWORD` を確認。

### リリースAPKが生成されない
→ `KEYSTORE_BASE64` が設定されているか確認。

詳細は [BUILD_AND_SIGNING.md](BUILD_AND_SIGNING.md) を参照。

---

## 💡 ヒント

- キーストアとパスワードは絶対にGitにコミットしない！
- `release.keystore` は安全な場所にバックアップ！
- パスワードはパスワードマネージャーで管理！
