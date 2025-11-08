# Testing & URL Verification Guide

## Quick Start: Testing the Demo

### Step 1: Build and Install

```bash
# Build the app
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Or use Android Studio: Run > Run 'app'
```

### Step 2: Enable Android Auto Developer Mode

```bash
# Enable developer mode
adb shell settings put global android_auto_dev_mode 1

# Restart Android Auto
adb shell am force-stop com.google.android.projection.gearhead
```

### Step 3: Test Audio URLs

The app currently uses **sample URLs** that may or may not work. Here's how to verify and fix them:

## Verifying Audio URLs Work

### Method 1: Quick Browser Test

Open these URLs in your browser to test if they're accessible:

1. **Bible Reading (Genesis 1)**:
   ```
   https://download-a.akamaihd.net/files/media_audio/01/nwtsty_E_01_r720P.mp3
   ```

2. **Watchtower Study**:
   ```
   https://download-a.akamaihd.net/files/media_audio/50/w_E_202412.mp3
   ```

3. **Congregation Bible Study**:
   ```
   https://download-a.akamaihd.net/files/media_audio/bf/bhs_E.mp3
   ```

4. **Meeting Workbook**:
   ```
   https://b.jw-cdn.org/files/media_audio/mwb/202412_mwb_E.mp3
   ```

5. **Kingdom Song**:
   ```
   https://download-a.akamaihd.net/files/media_audio/sng/nwtsty_E_sng_001.mp3
   ```

**If URLs don't work**: See "Getting Real URLs" section below.

### Method 2: Test in Android App

1. Launch Android Auto (or Desktop Head Unit)
2. Find "JW Library Auto" in media sources
3. Browse to "This Week's Meeting Content"
4. Try playing "Bible Reading"
5. Check if audio plays

**Expected behavior**:
- ✅ App appears in Android Auto
- ✅ Content hierarchy displays
- ✅ Audio starts playing
- ✅ Playback controls work (play/pause/stop)
- ✅ Notification shows media controls

**If audio doesn't play**: URLs likely need updating. See below.

---

## Getting Real Audio URLs from JW.org

### Step-by-Step Instructions

#### For Meeting Workbook Audio:

1. **Open browser** to: https://www.jw.org/en/library/jw-meeting-workbook/
2. **Find current month** (e.g., "December 2024")
3. **Click on the issue** → Look for "Audio download options"
4. **Open DevTools** (F12 or Right-click → Inspect)
5. **Go to Network tab** in DevTools
6. **Click "Audio download options"** → Select MP3
7. **Find the download request** in Network tab
8. **Right-click the MP3 request** → Copy → Copy link address
9. **Paste URL** into `JWOrgContentUrls.kt` at `WORKBOOK_SAMPLE`

**Example URL format**:
```
https://b.jw-cdn.org/files/media_audio/mwb/202412_mwb_E.mp3
                                          ^^^^^^
                                          YYYYMM (year + month)
```

#### For Bible Reading Audio:

1. **Visit**: https://www.jw.org/en/library/bible/study-bible/books/
2. **Click on a book** (e.g., Genesis)
3. **Look for audio player** icon on the page
4. **Open DevTools** → Network tab
5. **Click play** on audio player
6. **Find MP3 request** in Network tab
7. **Copy URL**

**Example URL format**:
```
https://download-a.akamaihd.net/files/media_audio/01/nwtsty_E_01_r720P.mp3
                                                   ^^           ^^
                                                   Book #       Chapter
```

#### For Watchtower Study Audio:

1. **Visit**: https://www.jw.org/en/library/magazines/
2. **Find latest Watchtower**
3. **Look for study article**
4. **Use DevTools** to find audio MP3 when playing

**Example URL format**:
```
https://download-a.akamaihd.net/files/media_audio/50/w_E_202412.mp3
                                                       ^^^^^^^^
                                                       YYYYMM
```

---

## Updating URLs in Code

### Edit: `app/src/main/java/org/jw/library/auto/data/api/JWOrgContentUrls.kt`

Replace the constants with your verified URLs:

```kotlin
// Line 29: Update with current month's Watchtower
private const val WATCHTOWER_SAMPLE = "https://download-a.akamaihd.net/files/media_audio/50/w_E_202412.mp3"

// Line 35: Update with current study book
private const val CBS_SAMPLE = "https://download-a.akamaihd.net/files/media_audio/bf/bhs_E.mp3"

// Line 41: Update with current month
private const val WORKBOOK_SAMPLE = "https://b.jw-cdn.org/files/media_audio/mwb/202412_mwb_E.mp3"

// Line 47: Update song number (001-151)
private const val SONG_SAMPLE = "https://download-a.akamaihd.net/files/media_audio/sng/nwtsty_E_sng_001.mp3"
```

### Rebuild and Test

```bash
# Rebuild with updated URLs
./gradlew installDebug

# Test in Android Auto again
```

---

## Troubleshooting

### Problem: "Content not available" or no audio plays

**Causes**:
1. URLs are outdated or incorrect
2. Network connectivity issue
3. URL validation rejecting the domain

**Solutions**:

1. **Verify URL in browser first**:
   ```bash
   curl -I "https://download-a.akamaihd.net/files/media_audio/01/nwtsty_E_01_r720P.mp3"
   # Should return: HTTP/2 200
   ```

2. **Check URI validation**:
   - Open `PlaybackManager.kt` line 317
   - Verify domain is in `allowedHosts` set:
     ```kotlin
     val allowedHosts = setOf(
         "jw.org",
         "download-a.akamaihd.net",
         "download.jw.org",
         "b.jw-cdn.org"  // ← Add if missing
     )
     ```

3. **Check Android logs**:
   ```bash
   adb logcat | grep "JWLibrary\|PlaybackManager\|ExoPlayer"
   ```

   Look for:
   - `"Invalid URI rejected"` → URL validation failed
   - `"Playback error"` → Audio file issue
   - `"Unauthorized host rejected"` → Add domain to allowedHosts

### Problem: App doesn't appear in Android Auto

**Solutions**:
1. Enable developer mode (see Step 2 above)
2. Enable "Unknown sources" in Android Auto settings
3. Check manifest has correct automotive descriptor
4. Restart Android Auto: `adb shell am force-stop com.google.android.projection.gearhead`

### Problem: Audio plays but stops after a few seconds

**Causes**:
1. Audio focus not gained
2. Network buffering issue
3. URL redirects

**Solutions**:
1. Check audio focus logs:
   ```bash
   adb logcat | grep "AudioFocus"
   ```

2. Increase buffer size in `PlaybackManager.kt`:
   ```kotlin
   val loadControl = DefaultLoadControl.Builder()
       .setBufferDurationsMs(
           30000, // Increase min buffer
           60000, // Increase max buffer
           // ...
       )
   ```

---

## Testing Checklist

### Basic Functionality
- [ ] App builds without errors
- [ ] App installs on device
- [ ] App appears in Android Auto media sources
- [ ] Content hierarchy displays (This Week, Last Week, etc.)
- [ ] Can navigate into categories

### Audio Playback
- [ ] Bible Reading plays
- [ ] Watchtower Study plays
- [ ] Congregation Bible Study plays
- [ ] Meeting Workbook plays
- [ ] Daily Text plays
- [ ] Kingdom Songs play
- [ ] Bible Dramas play

### Playback Controls
- [ ] Play button starts playback
- [ ] Pause button pauses playback
- [ ] Stop button stops playback
- [ ] Audio continues in background
- [ ] Notification shows correct title
- [ ] Notification controls work

### Audio Focus
- [ ] Start Google Maps navigation → Audio ducks (lowers volume)
- [ ] End navigation → Audio returns to full volume
- [ ] Make phone call → Audio pauses
- [ ] End call → Audio doesn't resume (expected)
- [ ] Play other music app → Audio stops

### Week Calculation
- [ ] "This Week" shows correct date range
- [ ] "Last Week" shows previous week
- [ ] "Next Week" shows following week
- [ ] Week boundaries are Monday-Sunday

### Security
- [ ] Only HTTPS URLs load
- [ ] http:// URLs are rejected (check logs)
- [ ] file:// URIs are rejected
- [ ] Unknown domains are rejected

### UI/Accessibility
- [ ] Dark mode renders correctly
- [ ] Notification actions have readable labels
- [ ] Enable TalkBack → Notification announces correctly
- [ ] Purple icon appears in launcher
- [ ] Icon displays on Android 7.x (if testable)

---

## Android Auto Desktop Head Unit (DHU) Testing

### Setup DHU

1. **Download** from: https://developer.android.com/training/cars/testing
2. **Extract** and run:
   ```bash
   cd desktop-head-unit
   ./desktop-head-unit
   ```

3. **Connect phone** via USB with:
   - USB debugging enabled
   - Android Auto installed
   - App installed

### DHU Test Scenarios

1. **Browse content** → All categories visible?
2. **Play audio** → Playback UI shows?
3. **Use steering wheel controls** → Skip/pause works?
4. **Start navigation** → Audio ducks?
5. **Switch between apps** → Audio pauses?

---

## Production Readiness Checklist

Before deploying to production:

- [ ] All sample URLs replaced with real, current URLs
- [ ] Tested with content from last 3 months
- [ ] URLs work without VPN/proxy
- [ ] Certificate pins added to `network_security_config.xml`
- [ ] Tested on Android 7, 8, 10, 12, 14
- [ ] Tested with multiple Android Auto head units (or DHU)
- [ ] Tested in actual vehicle
- [ ] All accessibility checks pass
- [ ] Dark mode tested
- [ ] Audio focus tested with real navigation
- [ ] No crashes after 30 minutes continuous playback

---

## Getting Help

### Useful ADB Commands

```bash
# View logs
adb logcat | grep "JWLibrary\|PlaybackManager"

# Test audio focus
adb shell am broadcast -a android.media.AUDIO_BECOMING_NOISY

# Force dark mode
adb shell "cmd uimode night yes"

# Clear app data
adb shell pm clear org.jw.library.auto

# Restart Android Auto
adb shell am force-stop com.google.android.projection.gearhead
```

### Log Filters

Watch for these log messages:

**Success**:
- `"Package authorized: com.google.android.projection.gearhead"`
- `"Audio focus gained"`
- `"Playback state: PLAYING"`

**Errors**:
- `"Unauthorized package attempted connection"`
- `"Invalid media URI rejected"`
- `"Failed to gain audio focus"`
- `"Playback error"`

---

## Next Steps

After testing the demo successfully:

1. **Document working URLs** → Save to a config file
2. **Implement backend service** → See SOLUTIONS.md Option A
3. **Add error handling** → Retry logic for network failures
4. **Implement caching** → Offline playback support
5. **Add analytics** → Track which content is most popular
6. **Submit to Google Play** → After thorough testing

Good luck! 🚗🎵
