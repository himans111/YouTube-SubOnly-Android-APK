# ChannelOnly build status

## Current source checkpoint

- Repository: https://github.com/himans111/YouTube-SubOnly-Android-APK
- Latest application logic update: commit `5374cd420a72b1bfab28b5672f4186e1aa37b782`.
- The subscribed feed now reads from all returned subscriptions (not just the first 50), gets up to five recent uploads per channel, and makes playlist requests in groups of five concurrent calls.
- API authentication/quota/server failures are surfaced instead of being silently swallowed; a deleted or missing playlist (HTTP 404) is skipped.
- Unit tests still verify channel IDs, filtering out non-subscribed channels, and duplicate-video removal.

## Build and emulator validation

- Previous verified build: [GitHub Actions run #23](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39646607684). Unit tests and debug APK assembly succeeded, and artifact `channelonly-debug-apk` was uploaded.
- The run #23 APK was checked as a valid Android package. Its SHA-256 is `299f2f5e8b8858d6d6d3e355d884af3536ce49f28677323434e19a5687fe0d0b`. This artifact predates the more recent subscribed-feed improvement, so use a newer CI artifact for that change.
- [Run #24](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39648481524) and [run #25](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39648722920) are retries created while diagnosing the emulator wait. Their emulator boot steps have not yet been confirmed successful.
- [Run #26](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39649062356) uses the latest source and workflow with explicit timeouts on ADB operations, faster detection of an exited emulator process, and boot diagnostics. At the last status check, Android SDK package setup was in progress; no emulator pass is claimed.
- The earlier stall likely occurs because an ADB shell command can hang inside a boot-loop condition before the elapsed-time check executes. Run #26 now puts a timeout around those commands and should either proceed to app launch or return diagnostics rather than wait forever.

## Implemented behavior

- Firebase Email/Password account screen (requires real Firebase project values).
- Separate Google authorization for YouTube Data API access.
- Search channels; subscribe/unsubscribe.
- Home feed limited to recent uploads from the signed-in account's subscriptions, with a final client-side channel-ID guard.
- Embedded YouTube player if an actual HTTPS player origin is configured; otherwise an Open in YouTube fallback.
- Android PiP is requested for the embedded player when leaving the app. Actual PiP/background playback remains subject to Android device and YouTube player/account restrictions.

## Items still requiring real project details

- Firebase API key, app ID, project ID and sender ID must be supplied locally and Email/Password enabled in Firebase Console.
- Google Cloud needs YouTube Data API v3 enabled, consent/testing configuration, and an Android OAuth client for package `com.cube.channelonly` with the correct signing SHA-1.
- Embedded playback/PiP requires publishing `player-origin-site/` to an HTTPS origin controlled by the developer and setting `playerOrigin` locally.
- `player-origin-site/privacy.html` is still a draft: the operator's legal name, support contact, account-deletion process and actual data practices cannot be invented. Complete it before public release.
- Do not commit `local.properties`, access tokens, passwords, or signing secrets. A build succeeding does not itself verify account sign-in, YouTube authorization, playback or PiP on a real device.
