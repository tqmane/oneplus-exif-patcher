@echo off
REM Build script for Windows

echo Building OnePlus EXIF Patcher...

REM Check if gradlew.bat exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found!
    exit /b 1
)

REM Clean build
echo Cleaning previous build...
call gradlew.bat clean

REM Build debug APK
echo Building debug APK...
call gradlew.bat assembleDebug

REM Build release APK if keystore is configured
if exist "keystore.properties" (
    echo Building release APK...
    call gradlew.bat assembleRelease
    echo.
    echo Release APK built successfully!
    echo   Location: app\build\outputs\apk\release\app-release.apk
) else (
    echo.
    echo Skipping release build (keystore.properties not found)
    echo   To build release APK, create keystore.properties from keystore.properties.example
)

echo.
echo Debug APK built successfully!
echo   Location: app\build\outputs\apk\debug\app-debug.apk

echo.
echo Build completed!
