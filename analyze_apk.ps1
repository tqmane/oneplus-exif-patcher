# APK解析スクリプト
# OnePlus Gallery APKからHasselblad透かし関連の情報を抽出

$apkPath = "photo\com.oneplus.gallery_15.73.22.apk"
$extractPath = "photo\gallery_extracted"
$resultsPath = "photo\analysis_results"

Write-Host "=== OnePlus Gallery APK Analysis ===" -ForegroundColor Cyan
Write-Host ""

# ディレクトリを作成
if (Test-Path $extractPath) {
    Remove-Item -Path $extractPath -Recurse -Force
}
if (Test-Path $resultsPath) {
    Remove-Item -Path $resultsPath -Recurse -Force
}
New-Item -ItemType Directory -Path $extractPath -Force | Out-Null
New-Item -ItemType Directory -Path $resultsPath -Force | Out-Null

# 1. APKを展開
Write-Host "[1/6] Extracting APK..." -ForegroundColor Yellow
try {
    # APKをZIPとして展開
    Copy-Item $apkPath "$extractPath.zip"
    Expand-Archive -Path "$extractPath.zip" -DestinationPath $extractPath -Force
    Remove-Item "$extractPath.zip"
    Write-Host "  ✓ APK extracted successfully" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Failed to extract APK: $_" -ForegroundColor Red
    exit 1
}

# 2. ファイル構造を出力
Write-Host "[2/6] Analyzing file structure..." -ForegroundColor Yellow
Get-ChildItem -Path $extractPath -Recurse | 
    Select-Object FullName, Length, @{Name="Type";Expression={if($_.PSIsContainer){"Directory"}else{"File"}}} |
    Out-File "$resultsPath\file_structure.txt" -Encoding UTF8
Write-Host "  ✓ File structure saved to: $resultsPath\file_structure.txt" -ForegroundColor Green

# 3. Hasselblad関連のファイルを検索
Write-Host "[3/6] Searching for Hasselblad-related files..." -ForegroundColor Yellow
$hasselbladFiles = Get-ChildItem -Path $extractPath -Recurse | 
    Where-Object { 
        $_.Name -match "hasselblad|watermark|overlay|stamp|logo" -or
        $_.FullName -match "hasselblad|watermark|overlay|stamp"
    }

if ($hasselbladFiles) {
    $hasselbladFiles | Select-Object FullName, Length | 
        Out-File "$resultsPath\hasselblad_files.txt" -Encoding UTF8
    Write-Host "  ✓ Found $($hasselbladFiles.Count) Hasselblad-related files" -ForegroundColor Green
} else {
    "No Hasselblad-related files found" | Out-File "$resultsPath\hasselblad_files.txt"
    Write-Host "  ! No Hasselblad-related files found" -ForegroundColor Yellow
}

# 4. 画像ファイルを検索（透かし画像の可能性）
Write-Host "[4/6] Searching for image resources..." -ForegroundColor Yellow
$imageFiles = Get-ChildItem -Path "$extractPath\res" -Recurse -Include *.png,*.jpg,*.webp -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "watermark|logo|brand|hasselblad|oneplus|camera" }

if ($imageFiles) {
    $imageFiles | Select-Object FullName, Length | 
        Out-File "$resultsPath\watermark_images.txt" -Encoding UTF8
    Write-Host "  ✓ Found $($imageFiles.Count) potential watermark images" -ForegroundColor Green
    
    # 画像をコピー
    $imageDir = "$resultsPath\extracted_images"
    New-Item -ItemType Directory -Path $imageDir -Force | Out-Null
    $imageFiles | ForEach-Object {
        Copy-Item $_.FullName -Destination "$imageDir\$($_.Name)" -Force
    }
    Write-Host "  ✓ Images copied to: $imageDir" -ForegroundColor Green
} else {
    Write-Host "  ! No watermark images found" -ForegroundColor Yellow
}

# 5. strings.xmlを検索
Write-Host "[5/6] Extracting string resources..." -ForegroundColor Yellow
$stringsFiles = Get-ChildItem -Path "$extractPath\res" -Recurse -Filter "strings.xml" -ErrorAction SilentlyContinue

if ($stringsFiles) {
    $allStrings = @()
    foreach ($file in $stringsFiles) {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        $allStrings += "=== $($file.FullName) ===`n$content`n`n"
    }
    $allStrings | Out-File "$resultsPath\all_strings.txt" -Encoding UTF8
    Write-Host "  ✓ Found $($stringsFiles.Count) strings.xml files" -ForegroundColor Green
} else {
    Write-Host "  ! No strings.xml files found" -ForegroundColor Yellow
}

# 6. AndroidManifest.xmlを抽出
Write-Host "[6/6] Extracting AndroidManifest.xml..." -ForegroundColor Yellow
$manifestPath = "$extractPath\AndroidManifest.xml"
if (Test-Path $manifestPath) {
    Copy-Item $manifestPath -Destination "$resultsPath\AndroidManifest.xml"
    Write-Host "  ✓ AndroidManifest.xml copied" -ForegroundColor Green
} else {
    Write-Host "  ! AndroidManifest.xml not found" -ForegroundColor Yellow
}

# 7. サマリーレポートを作成
Write-Host "`n=== Creating Summary Report ===" -ForegroundColor Cyan
$summary = @"
OnePlus Gallery APK Analysis Report
====================================
Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
APK: $apkPath

File Statistics:
- Total files: $(((Get-ChildItem -Path $extractPath -Recurse -File).Count))
- Total directories: $(((Get-ChildItem -Path $extractPath -Recurse -Directory).Count))
- Hasselblad-related files: $(if($hasselbladFiles){$hasselbladFiles.Count}else{0})
- Watermark images: $(if($imageFiles){$imageFiles.Count}else{0})
- String resources: $(if($stringsFiles){$stringsFiles.Count}else{0})

Key Findings:
-------------
$( if ($hasselbladFiles) {
    "Hasselblad-related files found:`n" + ($hasselbladFiles | ForEach-Object { "  - $($_.Name)`n" })
} else {
    "No Hasselblad-related files found in file names.`n"
})

$( if ($imageFiles) {
    "Potential watermark images:`n" + ($imageFiles | ForEach-Object { "  - $($_.Name) ($([math]::Round($_.Length/1KB, 2)) KB)`n" })
} else {
    "No obvious watermark images found.`n"
})

Next Steps:
-----------
1. Review extracted images in: $imageDir
2. Search strings.xml for "hasselblad", "watermark", "shot on"
3. Use jadx-gui to decompile and search Java code
4. Look for image processing and watermark logic

Files Generated:
-----------------
- file_structure.txt: Complete file listing
- hasselblad_files.txt: Files related to Hasselblad
- watermark_images.txt: Potential watermark images
- all_strings.txt: All string resources
- AndroidManifest.xml: App manifest
- extracted_images/: Copied watermark images

Recommendations for Implementation:
------------------------------------
Based on this analysis, we should:
1. Check if any Hasselblad watermark images exist
2. Search strings for watermark-related text
3. Use jadx to find watermark application code
4. Implement similar functionality with custom/open-source assets

"@

$summary | Out-File "$resultsPath\SUMMARY.txt" -Encoding UTF8
Write-Host "`n$summary" -ForegroundColor White

Write-Host "`n=== Analysis Complete! ===" -ForegroundColor Green
Write-Host "Results saved to: $resultsPath" -ForegroundColor Cyan
Write-Host "`nTo decompile the APK code, download jadx from:" -ForegroundColor Yellow
Write-Host "https://github.com/skylot/jadx/releases" -ForegroundColor Cyan
Write-Host "`nThen run: jadx-gui $apkPath" -ForegroundColor Yellow
