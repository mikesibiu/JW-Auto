# Android Auto Testing Guide

## Testing JW Library Auto with Android Auto

This guide shows how to test the app with Android Auto, both on real hardware and with the Desktop Head Unit (DHU) simulator.

---

## Method 1: Real Car Testing (Recommended)

### Prerequisites
- Car with Android Auto support OR Android Auto wireless adapter
- USB cable (for wired) OR WiFi (for wireless)
- JW Library Auto app installed on phone

### Steps

1. **Enable Developer Mode in Android Auto**
   ```bash
   # On your phone, open Android Auto app
   # Tap version number 10 times to enable developer mode
   # Go to Settings > Developer Settings
   # Enable "Unknown sources" to allow debug apps
   ```

2. **Connect Phone to Car**
   - **Wired**: Plug phone into car's USB port
   - **Wireless**: Ensure WiFi and Bluetooth enabled, pair with car

3. **Launch Android Auto**
   - Car display should show Android Auto interface
   - Look for "JW Library Auto" in the media apps section

4. **Test Content Browsing**
   - Tap "JW Library Auto" icon
   - Navigate through: This Week → Bible Reading
   - Verify content loads

5. **Test Playback**
   - Select any content item
   - Verify audio plays
   - Test pause/play/stop controls

6. **Monitor Logs While Connected**
   ```bash
   # Keep phone connected via USB to computer
   adb logcat | grep -E "(JWLibraryAutoService|PlaybackManager)"
   ```

### Expected Behavior

✅ **What Should Work**:
- App appears in Android Auto media apps
- Content hierarchy displays correctly
- Audio playback works
- Metadata shows (title, artist)
- Playback controls work (play/pause/stop)
- Background sync schedules on first connection

❌ **What Won't Work Yet** (until sync runs):
- Dynamic content from jw.org API
- Cache-based instant loading
- Offline mode

---

## Method 2: Desktop Head Unit (DHU) Simulator

### Prerequisites
```bash
# Install Android Auto DHU
# Download from: https://github.com/google/android-auto-companion/releases

# Or via Homebrew (Mac)
brew install --cask android-auto-dhu
```

### Setup

1. **Enable Developer Options on Phone**
   ```bash
   # Settings > About Phone > Tap Build Number 7 times
   # Settings > Developer Options > Enable USB Debugging
   ```

2. **Enable Android Auto Developer Mode**
   ```bash
   # Open Android Auto app
   # Tap version 10 times
   # Settings > Developer > Enable "Unknown sources"
   ```

3. **Connect Phone via USB**
   ```bash
   adb devices  # Verify phone is connected
   ```

4. **Launch DHU**
   ```bash
   desktop-head-unit

   # Or specify connection type:
   desktop-head-unit --usb
   ```

### Testing with DHU

1. **DHU Window Opens**
   - Displays Android Auto interface
   - Simulates car head unit

2. **Navigate to Media**
   - Click media icon in DHU
   - Find "JW Library Auto"

3. **Test Browsing**
   - Click through content hierarchy
   - Verify all categories appear

4. **Test Playback**
   - Select content
   - Use DHU controls to test play/pause
   - Monitor logcat for playback events

### DHU Controls
- **D-pad**: Navigate UI
- **Enter**: Select
- **Back**: Go back
- **Home**: Return to home
- **Play/Pause**: Media controls

---

## Method 3: Android Auto Emulator (Advanced)

### Setup Automotive Emulator

```bash
# Create automotive AVD
avdmanager create avd -n automotive \
  -k "system-images;android-34;google_apis;x86_64" \
  -d "automotive_1024p_landscape"

# Launch emulator
emulator -avd automotive
```

### Install and Test

```bash
# Install app
./gradlew installDebug

# Launch Android Auto app on emulator
adb shell am start -n com.google.android.projection.gearhead/.ui.MainActivity

# Navigate to JW Library Auto in media apps
```

---

## Monitoring & Debugging

### Real-Time Logs

```bash
# Watch service lifecycle
adb logcat | grep JWLibraryAutoService

# Watch playback events
adb logcat | grep PlaybackManager

# Watch content loading
adb logcat | grep ContentRepository

# Watch background sync
adb logcat | grep ContentSyncWorker

# Watch cache hits/misses
adb logcat | grep JWOrgRepository
```

### Check Background Sync Status

```bash
# List scheduled WorkManager jobs
adb shell dumpsys jobscheduler | grep org.jw.library.auto

# Check database after sync runs
adb shell "run-as org.jw.library.auto ls -la /data/data/org.jw.library.auto/databases/"
```

### Verify Content in Cache

```bash
# After 24h or manual sync trigger
# Check if database exists
adb shell "run-as org.jw.library.auto ls -la /data/data/org.jw.library.auto/databases/jw_content_cache*"
```

---

## Testing Checklist

### Initial Connection
- [ ] App appears in Android Auto media list
- [ ] Icon displays correctly
- [ ] Tapping app opens content hierarchy
- [ ] No crashes on connection

### Content Browsing
- [ ] "This Week" category loads
- [ ] "Last Week" category loads
- [ ] "Next Week" category loads
- [ ] "Songs" category loads (sample)
- [ ] Each category shows correct items

### Playback
- [ ] Selecting item starts playback
- [ ] Play/pause button works
- [ ] Stop button works
- [ ] Metadata shows in UI
- [ ] Notification appears with controls
- [ ] Audio plays through speakers

### Background Sync (After 24h)
- [ ] Database file created
- [ ] Cache populated with 4 weeks of content
- [ ] Instant loading from cache
- [ ] Logs show "Cache hit" messages

### Offline Mode
- [ ] Airplane mode ON
- [ ] Previously cached content still loads
- [ ] Uncached content falls back to hard-coded URLs
- [ ] No crashes when offline

### Android Auto Compliance
- [ ] No keyboard input while driving
- [ ] Limited interaction depth (≤2 taps to content)
- [ ] Readable text size
- [ ] Touch targets ≥48dp
- [ ] No distracting animations

---

## Common Issues & Solutions

### App Doesn't Appear in Android Auto

**Cause**: Not in developer mode or app not allowed

**Solution**:
```bash
# Enable developer mode in Android Auto app
# Settings > Developer > Unknown sources = ON

# Or check manifest
grep "android.car.application" app/src/main/AndroidManifest.xml
```

### "Connection Failed" Error

**Cause**: Package validation failing

**Solution**:
Check logs for:
```bash
adb logcat | grep "isClientAllowed"
# Should show allowed packages connecting
```

### No Audio Output

**Cause**: Wrong output stream or permissions

**Solution**:
```kotlin
// Verify AudioManager.STREAM_MUSIC is used
// Check audio focus is granted
adb logcat | grep "AudioManager"
```

### Content Shows "Sample" Instead of Real URLs

**Cause**: Cache empty, fallback URLs being used

**Solution**:
```bash
# Wait 24h for sync OR manually trigger WorkManager
# Check if sync has run:
adb logcat -d | grep ContentSyncWorker
```

---

## Performance Benchmarks

### Expected Performance (Real Device)

| Operation | Cold Start | Warm Cache | Offline |
|-----------|------------|------------|---------|
| Launch app | <500ms | <500ms | <500ms |
| Load hierarchy | <100ms | <100ms | <100ms |
| Load content list | 50-100ms | <10ms | <10ms |
| Start playback | 500-1500ms | 500-1500ms | 500-1500ms |

### Expected Performance (DHU)

DHU adds ~200-500ms latency due to USB communication.

---

## Advanced Testing

### Force Immediate Sync (Development Only)

```bash
# Trigger WorkManager sync immediately
adb shell am broadcast -a androidx.work.impl.background.systemjob.RescheduleReceiver

# Or via adb shell
adb shell cmd jobscheduler run -f org.jw.library.auto 1
```

### Inspect Database

```bash
# Pull database to local machine
adb shell "run-as org.jw.library.auto cat /data/data/org.jw.library.auto/databases/jw_content_cache" > local_cache.db

# Open with SQLite browser
sqlite3 local_cache.db "SELECT * FROM cached_content;"
```

### Clear Cache for Testing

```bash
# Clear app data (resets cache)
adb shell pm clear org.jw.library.auto

# Or just delete database
adb shell "run-as org.jw.library.auto rm -rf /data/data/org.jw.library.auto/databases/*"
```

---

## Production Testing Recommendations

Before releasing to Play Store:

1. **Test on Multiple Devices**
   - At least 3 different phone models
   - Different Android versions (7.0, 10, 13, 14)
   - Different car brands (if possible)

2. **Test Different Scenarios**
   - Fresh install (no cache)
   - After 24h (cache populated)
   - Offline mode
   - Low battery mode
   - Poor network conditions

3. **Test Edge Cases**
   - Week boundaries (Sunday → Monday)
   - Future weeks (next year)
   - Past weeks (last year)
   - Missing content weeks

4. **Performance Testing**
   - Cold start time
   - Memory usage
   - Battery drain during playback
   - Network usage

5. **Accessibility Testing**
   - TalkBack enabled
   - Large text mode
   - High contrast mode

---

## Next Steps After Testing

✅ **If Everything Works**:
- Document any issues found
- Fix critical bugs
- Prepare for beta release

⚠️ **If Issues Found**:
- Review logs for errors
- Check API responses
- Verify database schema
- Test fallback mechanisms

📝 **Documentation**:
- Update README with testing results
- Add troubleshooting guide
- Create user manual

🚀 **Release Preparation**:
- Create release build
- Test with ProGuard enabled
- Prepare Play Store listing
- Submit for review

---

## Support & Resources

- **Android Auto Documentation**: https://developer.android.com/training/cars
- **Media Apps Guide**: https://developer.android.com/training/cars/media
- **DHU Downloads**: https://github.com/google/android-auto-companion
- **JW.org API**: (internal documentation)

---

**Last Updated**: 2025-11-20
**App Version**: 1.0.0
**Status**: Ready for Testing
