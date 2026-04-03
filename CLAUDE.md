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

---

## Knowledge Base Workflow — follow this on every interaction, without being asked

### Step 1 — Search before doing anything
Before answering, suggesting an approach, writing code, or making changes:

```bash
python3 .claude/kb.py search <keywords relevant to the task>
```

Read the results and act on them:

- **outcome: LEARNED** — established knowledge for this project. Use it. Don't re-derive it. Don't second-guess it.
- **outcome: FAILED** — already tried and failed. Do NOT suggest it again. Tell the user "we already tried X and it failed because Y" and move on.

Also run orientation searches at session start:
```bash
python3 .claude/kb.py search project overview
python3 .claude/kb.py search file index
```

### Step 2 — Save what you learn
After any non-trivial interaction, save durable knowledge:

```bash
# Learned fact (always quote the whole string):
python3 .claude/kb.py learn "topic | what is true / what works | tags"

# Failed attempt:
python3 .claude/kb.py failed "topic | what was tried | why it failed | tags"
```

Save immediately when:
- A bug is found and fixed
- The user corrects a mistake
- An approach is tried and rejected
- A design decision is made
- A config quirk or non-obvious fact is discovered

Do NOT save: general knowledge, anything in official docs, temporary session context.
