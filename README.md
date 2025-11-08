# JW Library Auto - Android Auto Media App

Stream weekly meeting content and Kingdom songs through Android Auto for safe in-car spiritual enrichment.

## 📱 Overview

**JW Library Auto** is an Android Auto-compatible media streaming app that provides access to:
- Weekly Bible reading assignments
- Watchtower Study articles
- Congregation Bible Study materials
- Meeting Workbook schedules
- Kingdom Songs
- Bible Dramas
- Daily Text

Content is organized by week (This Week, Last Week, Next Week) similar to the Alexa JW.org skill, making it easy to prepare for meetings during your commute.

## 🎯 Project Status

### ✅ Completed
- ✅ Full Android Auto MediaBrowserService architecture
- ✅ Audio playback with ExoPlayer and Media3
- ✅ Week-based content hierarchy
- ✅ Foreground service with media notifications
- ✅ Security: Package validation, URI validation, HTTPS enforcement
- ✅ Network security configuration with certificate pinning prep
- ✅ Audio focus management (ducks for navigation, pauses for calls)
- ✅ Dark theme support for automotive displays
- ✅ Accessibility: Localized notification actions, TalkBack support
- ✅ Proper resource cleanup (no memory leaks)

### ⚠️ Required Before Production
1. **Generate bitmap launcher icons** - See [ICON_GENERATION_GUIDE.md](ICON_GENERATION_GUIDE.md)
2. **Integrate jw.org API** - Replace mock audio URLs with real content API
3. **Add certificate pins** - Update `network_security_config.xml` with actual jw.org certificate pins
4. **Implement skip next/previous** - Add playlist navigation for sequential playback
5. **Build phone UI** - Add proper MainActivity layout for content browsing and settings
6. **Test on Android Auto** - Verify with Desktop Head Unit (DHU) and real vehicles

## 🏗️ Architecture

### MediaBrowserService Pattern
```
┌─────────────────────────────────────────────────────────────┐
│                      Android Auto                            │
│  (System UI running in car head unit)                       │
└────────────────────────┬────────────────────────────────────┘
                         │ Binds to Service
                         │
┌────────────────────────▼────────────────────────────────────┐
│            JWLibraryAutoService                              │
│  (MediaBrowserServiceCompat)                                 │
│                                                              │
│  ┌──────────────────┐    ┌──────────────────┐              │
│  │  ContentProvider │    │  MediaSession    │              │
│  │  (Hierarchical   │    │  (Playback       │              │
│  │   content tree)  │    │   control)       │              │
│  └──────────────────┘    └──────────────────┘              │
│                                   │                          │
│                          ┌────────▼─────────┐               │
│                          │ PlaybackManager  │               │
│                          │  (ExoPlayer +    │               │
│                          │   AudioFocus)    │               │
│                          └──────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

- **JWLibraryAutoService**: MediaBrowserService that manages content browsing and playback
- **ContentProvider**: Hierarchical content tree organized by week and content type
- **PlaybackManager**: Handles ExoPlayer, audio focus, and MediaSession callbacks
- **WeekCalculator**: Calculates Monday-Sunday meeting weeks (locale-independent)

## 🔒 Security Features

### Implemented Protections
1. **Package Validation**: Only authorized apps (Android Auto, Google Assistant) can connect
2. **URI Validation**: Whitelist of approved domains (jw.org, akamaihd.net), HTTPS-only
3. **Network Security**: Certificate pinning configuration, cleartext traffic blocked
4. **No Data Backup**: `allowBackup="false"` prevents credential exposure
5. **Input Sanitization**: String length limits, URI format validation
6. **Audio Focus**: Proper ducking prevents missing navigation instructions

### Security Checklist
- [x] Package name + UID verification in `onGetRoot()`
- [x] HTTPS-only URI validation in `validateMediaUri()`
- [x] Network security config with pinning support
- [ ] **TODO**: Add actual certificate pins for jw.org before production
- [x] ProGuard rules for Media3 (prevents class stripping in release builds)

## 🎨 Material Design & Accessibility

### Compliance
- ✅ Dark theme support (`values-night/themes.xml`)
- ✅ Adaptive icon with 108dp safe zone
- ✅ Localized notification strings
- ✅ Content descriptions for TalkBack
- ✅ Proper touch target sizes (48x48dp) in planned UI
- ⚠️  **Pending**: Bitmap icon generation for Android 7.x

### Accessibility Features
- Localized notification action labels
- Audio focus management (auto-duck for navigation)
- Support for system text scaling
- RTL layout support enabled

## 🚗 Android Auto Integration

### Setup for Development

1. **Enable Developer Mode**:
```bash
adb shell settings put global android_auto_dev_mode 1
```

2. **Install Android Auto on Phone**:
```bash
# From Google Play Store
# Or sideload: https://www.apkmirror.com/apk/google-inc/android-auto/
```

3. **Test with Desktop Head Unit (DHU)**:
```bash
# Download DHU from https://developer.android.com/training/cars/testing
./desktop-head-unit
```

4. **Enable Unknown Sources** (for development builds):
   - Open Android Auto app
   - Tap version number 10 times
   - Enable "Unknown sources"

### Testing Checklist
- [ ] App appears in Android Auto media sources
- [ ] Content hierarchy displays correctly
- [ ] Play/pause/stop controls work
- [ ] Audio ducks during navigation announcements
- [ ] Audio pauses during phone calls
- [ ] Notification controls work on phone
- [ ] Dark mode renders correctly

## 📦 Building

### Requirements
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

### Project Structure
```
app/src/main/
├── java/org/jw/library/auto/
│   ├── service/
│   │   └── JWLibraryAutoService.kt      # MediaBrowserService
│   ├── playback/
│   │   └── PlaybackManager.kt           # ExoPlayer + audio focus
│   ├── data/
│   │   ├── ContentProvider.kt           # Content hierarchy
│   │   └── model/
│   │       └── MediaContent.kt          # Data models
│   ├── util/
│   │   └── WeekCalculator.kt            # Week logic
│   └── ui/
│       └── MainActivity.kt              # Phone UI (minimal)
└── res/
    ├── values/
    │   ├── strings.xml                  # Localized strings
    │   ├── colors.xml                   # Brand colors
    │   └── themes.xml                   # Light theme
    ├── values-night/
    │   └── themes.xml                   # Dark theme
    ├── xml/
    │   ├── network_security_config.xml  # HTTPS enforcement
    │   └── automotive_app_desc.xml      # Android Auto descriptor
    ├── drawable/
    │   ├── ic_play.xml                  # Playback icons
    │   ├── ic_pause.xml
    │   └── ic_stop.xml
    └── mipmap-anydpi-v26/
        └── ic_launcher.xml              # Adaptive icon
```

## 🛠️ Configuration

### Add jw.org Certificate Pins

Before production, update `app/src/main/res/xml/network_security_config.xml`:

```bash
# Get certificate pins
echo | openssl s_client -connect jw.org:443 | \
  openssl x509 -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  base64
```

Add pins to `<pin-set>` in network security config.

### Customize Content URLs

Update `ContentProvider.kt` to fetch from jw.org API:

```kotlin
private fun getMockMediaUri(type: MeetingContentType, weekInfo: WeekInfo): String {
    // TODO: Replace with actual jw.org API call
    return JWOrgAPI.getWeeklyContent(type, weekInfo.weekStart)
}
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Test Cases

1. **Week Calculation**
   - Set device date to Monday → verify "This Week" shows current week
   - Set to Sunday → verify "This Week" doesn't jump to next week
   - Set to different locales → verify Monday-Sunday consistency

2. **Audio Focus**
   - Play content → start Google Maps navigation → verify audio ducks
   - Make phone call during playback → verify audio pauses
   - Resume playback → verify audio returns to full volume

3. **Security**
   - Attempt to connect from unauthorized app → verify connection rejected
   - Attempt to play non-HTTPS URI → verify rejected
   - Attempt to play unauthorized domain → verify rejected

4. **Accessibility**
   - Enable TalkBack → verify notification actions announce correctly
   - Enable high contrast → verify UI remains readable
   - Increase text size → verify layouts adapt

## 📝 Code Review Results

### Security Review (app-code-reviewer)
- **Fixed**: 7 CRITICAL security issues
  - Package validation
  - Network security configuration
  - URI input validation
  - ExoPlayer resource cleanup
  - Week calculation logic error
  - Foreground service notification handling
  - Audio focus management

### UI Review (ui-layout-optimizer)
- **Fixed**: 4 CRITICAL UI issues
  - Localized notification strings
  - Dark theme implementation
  - Adaptive icon foreground (108dp)
  - Accessibility content descriptions

## 🚀 Deployment

### Pre-Release Checklist
- [ ] Generate bitmap launcher icons (see ICON_GENERATION_GUIDE.md)
- [ ] Add jw.org certificate pins
- [ ] Replace mock URLs with real API integration
- [ ] Test on Android Auto head unit
- [ ] Test on Android 7.0+ devices
- [ ] Configure signing key for release builds
- [ ] Set up Play Store listing
- [ ] Prepare privacy policy (required for Play Store)

### Release Build
```bash
# Create release build
./gradlew assembleRelease

# Verify ProGuard worked
unzip -l app/build/outputs/apk/release/app-release.apk | grep -i exoplayer

# Sign APK
jarsigner -verbose -sigalg SHA256withRSA \
  -digestalg SHA-256 \
  -keystore my-release-key.jks \
  app-release.apk my-key-alias
```

## 📄 License

This is a reference implementation for educational purposes. Ensure you have appropriate licensing from jw.org for content distribution.

## 🤝 Contributing

This project follows the **sr-android-dev-agent** workflow:
1. Make code changes
2. Run `app-code-reviewer` agent for security and quality review
3. Fix ALL critical issues identified
4. Run `ui-layout-optimizer` agent for Material Design compliance
5. Fix ALL critical UI issues
6. Use `git-ops-manager` agent for commits and PRs

### Code Standards
- Kotlin best practices (data classes, sealed classes, scope functions)
- MVVM architecture pattern
- Material Design 3 components
- WCAG 2.1 Level A accessibility compliance
- 100% HTTPS for all network traffic
- ProGuard/R8 optimization for release builds

## 📞 Support

For issues or questions:
- Open an issue on GitHub
- Review Android Auto documentation: https://developer.android.com/training/cars
- Check jw.org API documentation (if available)

## 🎯 Roadmap

### Version 1.0 (MVP)
- [x] Android Auto media browsing
- [x] Weekly meeting content structure
- [x] Audio playback with ExoPlayer
- [x] Security hardening
- [ ] jw.org API integration
- [ ] Bitmap launcher icons
- [ ] Certificate pinning

### Version 1.1
- [ ] Phone UI for content browsing
- [ ] Offline content caching
- [ ] Playlist/queue management
- [ ] Skip next/previous implementation
- [ ] Language selection

### Version 1.2
- [ ] Settings screen (language, download preferences)
- [ ] Download management for offline playback
- [ ] Playback speed control
- [ ] Sleep timer
- [ ] Chromecast support

### Version 2.0
- [ ] Multi-language content support
- [ ] Sign language video support (if feasible in Android Auto)
- [ ] Content search functionality
- [ ] Bookmarks and favorites
- [ ] Listen history tracking

---

**Built with ❤️ following sr-android-dev-agent best practices**
