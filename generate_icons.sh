#!/bin/bash
# Generate JW Library Auto launcher icons
# Requires: ImageMagick (brew install imagemagick)

set -e  # Exit on error

echo "🎨 Generating JW Library Auto launcher icons..."

# Purple background color (JW.org brand purple)
BG_COLOR="#4A148C"  # Deep purple

# Create directories if they don't exist
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

# Create 512x512 source image with purple background and white "JW" text
convert -size 512x512 xc:"$BG_COLOR" \
  -font Helvetica-Bold \
  -pointsize 280 \
  -fill white \
  -gravity center \
  -annotate +0+0 "JW" \
  /tmp/jw_icon_source.png

echo "✓ Created source icon"

# Generate all density variants
convert /tmp/jw_icon_source.png -resize 48x48 app/src/main/res/mipmap-mdpi/ic_launcher.png
convert /tmp/jw_icon_source.png -resize 72x72 app/src/main/res/mipmap-hdpi/ic_launcher.png
convert /tmp/jw_icon_source.png -resize 96x96 app/src/main/res/mipmap-xhdpi/ic_launcher.png
convert /tmp/jw_icon_source.png -resize 144x144 app/src/main/res/mipmap-xxhdpi/ic_launcher.png
convert /tmp/jw_icon_source.png -resize 192x192 app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

echo "✓ Generated standard icons"

# Generate round variants (same image works for round)
cp app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
cp app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
cp app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
cp app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
cp app/src/main/res/mipmap-xxxhdpi/ic_launcher.png app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png

echo "✓ Generated round icons"

# Verify all icons were created
echo ""
echo "📊 Icon generation summary:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
ls -lh app/src/main/res/mipmap-mdpi/ic_launcher*.png 2>/dev/null | awk '{print "  mdpi:    "$5" - "$9}'
ls -lh app/src/main/res/mipmap-hdpi/ic_launcher*.png 2>/dev/null | awk '{print "  hdpi:    "$5" - "$9}'
ls -lh app/src/main/res/mipmap-xhdpi/ic_launcher*.png 2>/dev/null | awk '{print "  xhdpi:   "$5" - "$9}'
ls -lh app/src/main/res/mipmap-xxhdpi/ic_launcher*.png 2>/dev/null | awk '{print "  xxhdpi:  "$5" - "$9}'
ls -lh app/src/main/res/mipmap-xxxhdpi/ic_launcher*.png 2>/dev/null | awk '{print "  xxxhdpi: "$5" - "$9}'

# Count total icons
ICON_COUNT=$(find app/src/main/res/mipmap-* -name "ic_launcher*.png" 2>/dev/null | wc -l | tr -d ' ')
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Generated $ICON_COUNT icons total (10 expected)"

if [ "$ICON_COUNT" -eq 10 ]; then
    echo "✅ All launcher icons created successfully!"
    echo ""
    echo "Next steps:"
    echo "  1. Preview: open /tmp/jw_icon_source.png"
    echo "  2. Build app: ./gradlew assembleDebug"
    echo "  3. Icons will appear on Android 7.x+ devices"
else
    echo "⚠️  Warning: Expected 10 icons, found $ICON_COUNT"
    exit 1
fi

# Clean up temp file
rm -f /tmp/jw_icon_source.png

echo ""
echo "🎉 Icon generation complete!"
