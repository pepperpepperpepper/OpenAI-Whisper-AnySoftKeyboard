#!/bin/bash

# =============================================================================
# NewSoftKeyboard English-Only Setup Script
# =============================================================================
# This script builds and installs AnySoftKeyboard with English language support
# only, avoiding unnecessary language packs.
#
# Usage: ./setup_anysoftkeyboard_english.sh
# =============================================================================

set -e  # Exit on any error

echo "🔧 Setting up NewSoftKeyboard (English only)..."

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected. Please connect your device and enable USB debugging."
    exit 1
fi

echo "✅ Device connected"

# Build the main app (this will include English language support)
echo "📦 Building NewSoftKeyboard main app..."
./gradlew :ime:app:assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

echo "✅ Build completed successfully"

# Install the main app
echo "📱 Installing NewSoftKeyboard..."
adb install -r ./ime/app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo "❌ Installation failed. Please check if the device has sufficient storage."
    exit 1
fi

echo "✅ Installation completed successfully"

# Verify installation
echo "🔍 Verifying installation..."
INSTALLED_PACKAGES=$(adb shell pm list packages | grep -i anysoft)

if [ -n "$INSTALLED_PACKAGES" ]; then
    echo "✅ NewSoftKeyboard installed successfully:"
    echo "$INSTALLED_PACKAGES"
    echo ""
    echo "🎯 Setup complete! Next steps:"
    echo "1. Go to Settings → Language & Input → Keyboard settings"
    echo "2. Enable 'NewSoftKeyboard'"
    echo "3. Switch to NewSoftKeyboard in any text input app"
    echo "4. Test the 'Saved Prompts' dialog to verify icons work correctly"
else
    echo "❌ Installation verification failed. Please check manually."
fi

echo ""
echo "🚀 All done! Your NewSoftKeyboard with English support is ready to use."
