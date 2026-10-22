# ChannelOnly build status

## Latest validated build

- Repository: https://github.com/himans111/YouTube-SubOnly-Android-APK
- Latest completed full validation: [GitHub Actions run #40](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39731888585), conclusion **success**, workflow snapshot `6184bd7a7b2d33dafd7c1d6985c4c80178bb1cda`.
- Passed: Gradle unit tests, debug APK build, emulator boot, APK installation, activity launch/process/crash-log checks, and login-screen screenshot capture/upload.
- Latest APK artifact: `channelonly-debug-apk`, artifact ID `12404569335`; extracted APK size 20,522,376 bytes, SHA-256 `5be92f72dd468315537a46725d70a9e922f446748f16d6277ee5dc360c4e9d36`.
- Latest login screenshot artifact: `channelonly-login-screen`, artifact ID `12405855721`; emulator screenshot captured from a 1080 × 1920 Android virtual device.
- Google account flow: after Continue with Google succeeds, YouTube authorization is requested using the same selected Google account; if YouTube consent is already granted, the access token is returned without launching another consent screen. On app startup the app silently attempts to reacquire a prior grant; if consent is missing, it leaves a visible same-account connect action.
- Email/Password login remains available. App does not read Gmail. YouTube permission is a separate Google OAuth grant from Firebase app sign-in and may require consent the first time.
- Unit tests cover choosing a YouTube authorization account only for Google-backed sessions and rejecting missing/invalid addresses, in addition to feed filtering/deduplication and player-origin validation.
- Emulator smoke test only verifies install/launch and immediate process stability. It does not validate live Firebase authentication, Google OAuth consent, YouTube account/API actions, embedded playback, or PiP on a physical device.
- Real Firebase values/providers, a valid Google Web OAuth client/Android OAuth setup, and a real HTTPS player origin for embedded playback/PiP remain necessary. `player-origin-site/privacy.html` is still a draft; complete the real operator, contact, data-practices and account-deletion details before public release.
- This is a debug APK, not a signed production release.

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
