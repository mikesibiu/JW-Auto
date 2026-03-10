# JW Library Auto — Project Rules

## Device Rules (ABSOLUTE — NO EXCEPTIONS)

- NEVER install anything on the user's phone without explicit permission for that specific install
- NEVER uninstall anything from the user's phone, ever
- NEVER run `connectedDebugAndroidTest` — it uninstalls the app after tests
- Ask the user before every install: "Ready to install versionCode X?"

## QA Gate (MANDATORY — NO EXCEPTIONS)

Before asking to install, run unit tests only:
```
./gradlew test
```

All unit tests must pass before requesting install permission.
After the user approves, run `./gradlew installDebug` only.

On-device verification (`ContentVerificationTest`) must be run manually by asking the user to run:
```
./gradlew connectedDebugAndroidTest
```
only when they are prepared for the app to be uninstalled and reinstalled.

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
