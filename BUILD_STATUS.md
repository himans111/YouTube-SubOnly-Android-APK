# ChannelOnly build status

## Current source state
The Android project is committed at the repository root. The latest source adds Android Picture-in-Picture support for a configured embedded player and hides the app's navigation chrome while the Activity is in PiP.

## Latest verified build

- GitHub Actions run: https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39626459638
- Gradle build: successful
- Unit tests: successful
- Debug APK: uploaded as artifact `channelonly-debug-apk` (artifact ID 12356977092; approximately 19.8 MiB)
- APK ZIP integrity: verified after download; the APK contains 233 ZIP entries.
- Emulator smoke test: still running on the hosted GitHub Actions runner; its final result has not been confirmed yet.

## Implemented app behaviour

- Firebase Email/Password account screen (requires the user's own Firebase values).
- Separate Google authorization for the YouTube Data API.
- Search channels; subscribe/unsubscribe.
- Feed sourced from the authorized YouTube account's subscriptions, with a client-side subscription-channel ID guard.
- Embedded YouTube player when a real HTTPS player origin is configured; otherwise an Open in YouTube fallback.
- Android PiP is requested when leaving the app from an active embedded-player screen. Continuation is subject to Android, YouTube player, device and account rules.

## Setup and limitations

See `SETUP_HINDI.md` for Firebase, OAuth, HTTPS player origin, build, and testing steps. `README.md` includes an English overview.

Do not commit `local.properties`, authentication tokens, or passwords. Firebase login and YouTube OAuth require actual project credentials; source compilation alone does not verify these account-dependent flows. The privacy policy under `player-origin-site/privacy.html` remains a draft and must be completed before a public release.
