# ChannelOnly build status

## Latest validated build

- Repository: https://github.com/himans111/YouTube-SubOnly-Android-APK
- Latest completed full validation: [GitHub Actions run #32](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39664156806), source commit `acb485244a4b5c521191a56962851914167240b0`.
- Passed: source validation, Gradle unit tests, debug APK assembly, emulator creation/boot, APK installation, activity launch, and app-process/crash-log inspection.
- Latest APK artifact: `channelonly-debug-apk`, artifact ID `12376716395`; extracted APK is 20,488,707 bytes and SHA-256 `0f7e51c578c9c5ff3b2c1751f37135a51d9e2399ccb6555f88562a147a0df389`.
- Recent hardening included rejecting placeholder HTTPS player origins for PiP, leaving optional `playerOrigin` blank in the example config, and rejecting non-empty sample Firebase credentials rather than enabling a misleading login screen.
- Hosted emulator smoke test proves only that the app installs and launches. It does not verify live Firebase login, Google OAuth, YouTube API behavior, embedded playback, or PiP on a physical device.

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
