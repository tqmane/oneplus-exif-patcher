#!/bin/bash
# Build script for Linux/macOS

set -e

echo "Building OnePlus EXIF Patcher..."

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "Error: gradlew not found!"
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Clean build
echo "Cleaning previous build..."
./gradlew clean

# Build debug APK
echo "Building debug APK..."
./gradlew assembleDebug

# Build release APK if keystore is configured
if [ -f "keystore.properties" ]; then
    echo "Building release APK..."
    ./gradlew assembleRelease
    echo "✓ Release APK built successfully!"
    echo "  Location: app/build/outputs/apk/release/app-release.apk"
else
    echo "⚠ Skipping release build (keystore.properties not found)"
    echo "  To build release APK, create keystore.properties from keystore.properties.example"
fi

echo "✓ Debug APK built successfully!"
echo "  Location: app/build/outputs/apk/debug/app-debug.apk"

echo ""
echo "Build completed!"
