# JW Library Auto — Project Rules

## QA Gate (MANDATORY — NO EXCEPTIONS)

Before every install, run the full QA sequence via the QA agent:
```
./gradlew test installDebug && ./gradlew connectedDebugAndroidTest
```

1. Unit tests must pass (`./gradlew test`)
2. Install the APK (`installDebug`)
3. On-device instrumented tests must pass (`connectedDebugAndroidTest`)

Never skip any step. Never assume tests pass. If any test fails: fix it, re-run, then proceed.

## After Every Bug That Reaches the User

Add a test that would have caught it before pushing to the device.

## Commits

One commit per feature or fix. Never bundle unrelated changes.
Bump `versionCode` in `app/build.gradle.kts` with every install.

## Build

Always use Java 17:
```
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew ...
```

## Threading Rules

Room database calls are never allowed on the main thread.
Any code in `Service.onCreate()` runs on the main thread — use `runBlocking(Dispatchers.IO)` to call suspend DB functions from there.
