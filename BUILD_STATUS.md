# ChannelOnly build status

## What has been committed
- `.github/workflows/android-build-and-emulator.yml`: CI workflow for unit tests, debug APK assembly, Android emulator launch/crash smoke check, and APK artifact upload.
- This status file.

## Why the APK build has not started successfully
The complete Android source tree is not yet committed to `main`. The source ZIP exists in the ChatGPT working environment, but the available GitHub text-file write path does not provide a binary ZIP upload. Attempts to pass the ZIP as a single base64 text payload are truncated by the tool input limit, so the resulting content is not a complete archive. I have not claimed that the APK compiled or that emulator testing passed.

## Required project files
Upload the contents of the top-level `ChannelOnly/` directory from `ChannelOnly_Android_Project.zip` into this repository root. The required items include:
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/cube/channelonly/MainActivity.kt`
- `app/src/main/java/com/cube/channelonly/data/YouTubeApi.kt`
- `app/src/main/java/com/cube/channelonly/data/FirebaseSetup.kt`
- root Gradle files and `gradle/wrapper/`

After the source files are present, the workflow can run on push or from **Actions → Android build and emulator smoke test**. It will build the APK and attempt an emulator smoke test.

## Security
Do not commit `local.properties`, Firebase secrets, OAuth tokens, or passwords. Firebase and YouTube OAuth setup is separate from a successful CI build.
