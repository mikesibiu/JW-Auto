#!/bin/bash
# Build and install script for JW Library Auto

# Set Java 17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

echo "🏗️  Building JW Library Auto..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "📦 APK location:"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""

    # Check if device is connected
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l)

    if [ $DEVICE_COUNT -gt 0 ]; then
        echo "📱 Device detected. Installing..."
        ./gradlew installDebug

        if [ $? -eq 0 ]; then
            echo ""
            echo "✅ App installed successfully!"
            echo ""
            echo "🔧 Enabling Android Auto developer mode..."
            adb shell settings put global android_auto_dev_mode 1
            echo ""
            echo "🚗 Next steps:"
            echo "   1. Open Android Auto app (or install it from Play Store)"
            echo "   2. Enable 'Unknown sources' in Android Auto settings:"
            echo "      - Tap version number 10 times to enable developer mode"
            echo "      - Settings > Developer settings > Unknown sources"
            echo "   3. Launch Android Auto"
            echo "   4. Find 'JW Library Auto' in media sources"
            echo "   5. Browse and play content"
            echo ""
            echo "📖 Troubleshooting: See TESTING_GUIDE.md"
        fi
    else
        echo "⚠️  No device connected"
        echo ""
        echo "📱 To install on device:"
        echo "   1. Connect device via USB or start emulator"
        echo "   2. Run: ./gradlew installDebug"
        echo ""
        echo "🖥️  To start emulator:"
        echo "   ~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1 &"
    fi
else
    echo "❌ Build failed. Check errors above."
    exit 1
fi
