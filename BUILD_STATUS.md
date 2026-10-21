# ChannelOnly build status

## Latest validated build

- Repository: https://github.com/himans111/YouTube-SubOnly-Android-APK
- Latest completed full validation: [GitHub Actions run #37](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39675359818), source commit `aef35c6d8a16f2309e677b15d7604260ad43d0f5`.
- Passed: Gradle unit tests, debug APK assembly, emulator boot, APK install/launch, app-process/crash-log inspection, and login-screen screenshot capture/upload.
- Run #37 APK artifact: `channelonly-debug-apk`, artifact ID `12381432072`; extracted APK SHA-256 `46fdf1489386a2bb93bcc58d0a022e180f0105c2402c0a8191433d7e126268f3`.
- Login screen provides Firebase email/password login, password reset, account creation, and a separate Continue with Google action using Android Credential Manager and Firebase ID-token authentication.
- After visually inspecting the emulator screenshot, a follow-up compact-layout change was committed in `MainActivity.kt` to reduce header size and shorten setup/help copy so more of the login screen fits on a typical phone. Its validation is running in [GitHub Actions run #38](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39675925429); do not mark that latest layout as validated until run #38 completes.
- The emulator checks installation and launch only. Neither run proves live Firebase authentication, Google OAuth, YouTube API access, embedded playback, or PiP on a physical device.
- The current CI APK is a debug build. Real Firebase project values, a valid Web OAuth client ID, Google Cloud consent/testing setup, and an HTTPS player origin for embedded playback/PiP are still required.

## Application logic improvements

- The subscribed feed uses all subscription IDs returned by the YouTube API (not just the first 50), gets up to five recent uploads per subscribed channel, and makes playlist calls in groups of five concurrent requests.
- The client performs a final channel-ID filter and removes duplicate video IDs.
- Deleted/unavailable playlists (HTTP 404) are skipped, while authentication/quota/server errors are surfaced to the user rather than silently swallowed.
- Unit tests cover channel ID preservation, filtering out non-subscribed videos, and duplicate-video removal.

## Emulator issue and fix

- Earlier hosted-runner attempts failed because the x86_64 emulator needed hardware acceleration but the runner user could not access `/dev/kvm`.
- The workflow now attempts to grant the ephemeral runner user access to `/dev/kvm`. If KVM still is unavailable, it opts in to slow software emulation with `ANDROID_I_WANT_MY_TCG=yes` and `-accel off`.
- All ADB operations in the latest workflow have explicit timeouts and boot diagnostics to avoid indefinite waits.
- The corrected workflow completed its boot and app launch checks successfully in run #28.

## Implemented behavior

- Firebase Email/Password account screen (requires real Firebase project values).
- Separate Google authorization for YouTube Data API access.
- Search for channels and subscribe/unsubscribe.
- Home feed limited to recent uploads from the authorized YouTube account's subscriptions, with a final client-side channel-ID guard.
- Embedded YouTube player when an actual HTTPS player origin is configured; otherwise an Open in YouTube fallback.
- Android Picture-in-Picture (PiP) request for the embedded player when leaving the app. Actual PiP/background playback remains subject to Android device and YouTube player/account restrictions.

## Items requiring real project details before personal-account testing or public release

- Firebase API key, app ID, project ID and sender ID must be supplied in local `local.properties`, and Email/Password sign-in must be enabled in Firebase Console.
- Google Cloud needs YouTube Data API v3 enabled, consent/testing configuration, and an Android OAuth client for package `com.cube.channelonly` with the correct signing SHA-1.
- Embedded playback/PiP requires publishing `player-origin-site/` to an HTTPS origin controlled by the developer and setting `playerOrigin` locally.
- `player-origin-site/privacy.html` is still a draft: the actual operator name, support contact, account-deletion process and real data practices cannot be invented. Complete it before public release.
- Never commit `local.properties`, access tokens, passwords, or signing secrets. Automated launch validation does not verify Firebase login, live Google OAuth, YouTube quota, playback or PiP on a real physical device.
- This is a debug APK, not a signed production release.
