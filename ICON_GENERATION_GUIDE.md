# Launcher Icon Generation Guide

## CRITICAL: Missing Bitmap Launcher Icons

The app currently uses adaptive icons (Android 8.0+) but **requires bitmap fallback icons** for Android 7.x devices (minSdk 24).

### Required Files

Generate PNG launcher icons for these density buckets:

```
app/src/main/res/mipmap-mdpi/ic_launcher.png       (48x48px)
app/src/main/res/mipmap-hdpi/ic_launcher.png       (72x72px)
app/src/main/res/mipmap-xhdpi/ic_launcher.png      (96x96px)
app/src/main/res/mipmap-xxhdpi/ic_launcher.png     (144x144px)
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png    (192x192px)
```

And round variants:

```
app/src/main/res/mipmap-mdpi/ic_launcher_round.png       (48x48px)
app/src/main/res/mipmap-hdpi/ic_launcher_round.png       (72x72px)
app/src/main/res/mipmap-xhdpi/ic_launcher_round.png      (96x96px)
app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png     (144x144px)
app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png    (192x192px)
```

### Design Specifications

- **Background**: JW Blue (#4A6DA7)
- **Foreground**: White (#FFFFFF) Bible/book icon
- **Safe Zone**: Center 66% of canvas (remaining 17% on each side is cropped by some launchers)
- **Style**: Simple, recognizable silhouette suitable for small sizes

### Generation Methods

#### Method 1: Android Studio Image Asset Studio (Recommended)

1. Right-click `res/` folder in Android Studio
2. Select **New** → **Image Asset**
3. Choose **Launcher Icons (Adaptive and Legacy)**
4. Select foreground layer (use `/res/drawable/ic_launcher_foreground.xml`)
5. Set background color to `#4A6DA7` (JW Blue)
6. Click **Next** → **Finish**
7. Android Studio will generate all density variants automatically

#### Method 2: Online Icon Generator

Use [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html):

1. Upload a 512x512px source image (white icon on transparent background)
2. Set background color: `#4A6DA7`
3. Choose "Foreground scaling: Trim"
4. Download the generated asset pack
5. Extract to `app/src/main/res/`

#### Method 3: Manual Export from Design Tool

Using Figma/Sketch/Adobe XD:

1. Create artboards for each size (48, 72, 96, 144, 192px)
2. Use JW Blue (#4A6DA7) background
3. Place white Bible icon in center 66% safe zone
4. Export as PNG with proper naming scheme
5. Place in respective mipmap-* directories

### Validation

After generating icons, verify:

```bash
# Check all densities exist
ls -R app/src/main/res/mipmap-*/ic_launcher*.png

# Should show:
# app/src/main/res/mipmap-mdpi/ic_launcher.png
# app/src/main/res/mipmap-mdpi/ic_launcher_round.png
# ... (and all other densities)
```

### Why This Is Critical

- **Android 7.x Compatibility**: These devices (minSdk 24) don't support adaptive icons
- **App Crashes**: Missing bitmap icons cause crashes on Android 7.x when launching
- **Google Play Rejection**: Play Store requires bitmap icons for apps supporting API 24-25
- **Brand Consistency**: Ensures icon looks correct across all devices and launchers

### Current Status

✅ Adaptive icons (Android 8.0+) - Complete
❌ Bitmap icons (Android 7.x fallback) - **MISSING - CRITICAL**
⚠️  Round bitmap icons - **MISSING - REQUIRED**

### Next Steps

1. Generate bitmap icons using Method 1 (Android Studio) - **5 minutes**
2. Test on Android 7.0-7.1 device or emulator
3. Verify icon displays correctly in launcher
4. Commit generated assets to repository

**PRIORITY**: Complete before production deployment or Google Play submission.
