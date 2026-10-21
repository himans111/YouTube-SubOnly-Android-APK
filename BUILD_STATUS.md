# ChannelOnly build status

## Latest source and validation

- Android source is committed at the repository root on `main`.
- Latest feature changes include Android Picture-in-Picture (PiP) eligibility tied to a configured HTTPS embedded-player origin, Android 12+ auto-enter PiP, explicit PiP entry on Android 8–11, and clearing PiP eligibility on YouTube disconnect/sign-out.
- Hindi setup guide: `SETUP_HINDI.md`.
- Latest validation run for the newest PiP source: [GitHub Actions run #23](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39646607684). It is currently validating a direct emulator boot/install/start sequence.

## Previous verified build

- Run #22: Gradle unit tests and debug APK build both passed.
- Its emulator step booted an Android emulator but failed later with an ADB broken-pipe/service error before a stable install-and-launch result was established.
- APK artifact was uploaded as `channelonly-debug-apk` (artifact ID 12356977092; about 19.8 MiB). The ZIP archive was verified, but this artifact was built before the latest Android 12+ auto-enter and PiP cleanup changes.
- Earlier emulator runs showed the same ADB instability, not an application compilation failure.

## App features

- Firebase Email/Password account screen (requires real Firebase project values).
- Separate Google authorization for the YouTube Data API.
- Search channels; subscribe/unsubscribe.
- Home feed limited to subscribed channel IDs with a client-side channel-ID filter.
- Embedded YouTube player if a real HTTPS player origin is configured; otherwise an Open in YouTube fallback.
- PiP is requested only for a configured embedded player. Continuation depends on Android, the YouTube player, device, and account rules.

## Setup and release limitations

See `SETUP_HINDI.md` for Firebase, OAuth, HTTPS player origin, build and testing instructions.

Do not commit `local.properties`, authentication tokens or passwords. Firebase sign-in and YouTube OAuth require actual project credentials; compilation alone does not verify account-dependent flows. `player-origin-site/privacy.html` is still a draft and must be completed before any public release.
