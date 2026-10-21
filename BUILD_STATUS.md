# ChannelOnly build status

## Latest verified source build

- GitHub Actions run #23: https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39646607684
- The latest committed source, including Android 12+ auto-enter PiP and clearing PiP eligibility on YouTube disconnect/sign-out, passed Gradle unit tests and debug APK assembly.
- Latest APK artifact: `channelonly-debug-apk`, artifact ID 12366998634, approximately 19.8 MiB.
- The downloaded APK was checked: artifact ZIP is valid, the contained APK passes ZIP integrity checks, and it is a real Android package. SHA-256: `299f2f5e8b8858d6d6d3e355d884af3536ce49f28677323434e19a5687fe0d0b`.
- Emulator smoke test in run #23: the direct emulator creation/boot step is still in progress; install/start/crash inspection has not yet been confirmed.

## What caused the previous stall

Previous runs built and tested successfully, but the hosted Android emulator became unstable after boot: ADB/package/settings commands returned broken-pipe/service errors. The workflow was changed to create a lightweight Android 11 AVD directly, explicitly wait for boot completion, install and launch the APK, and capture emulator/logcat diagnostics if it fails.

## Implemented app behaviour

- Firebase Email/Password account screen (requires real Firebase project values).
- Separate Google authorization for the YouTube Data API.
- Search channels; subscribe/unsubscribe.
- Home feed limited to the authorized account's subscribed channel IDs with a client-side filter.
- Embedded YouTube player if a real HTTPS player origin is configured; otherwise an Open in YouTube fallback.
- Android PiP request when leaving an active embedded-player screen. Android 12+ auto-enter is configured; playback continuation remains subject to Android, the YouTube player, device and account restrictions.

## Setup and release limitations

See `SETUP_HINDI.md` for Firebase, OAuth, HTTPS player origin, build and testing instructions.

Do not commit `local.properties`, access tokens or passwords. Firebase sign-in and YouTube OAuth require real project credentials; compiling successfully does not verify account-dependent flows. `player-origin-site/privacy.html` remains a draft and must be completed before public release.
