# ChannelOnly build status

## Latest validated build

- Repository: https://github.com/himans111/YouTube-SubOnly-Android-APK
- Latest completed full validation: [GitHub Actions run #39](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39688775159), source/trigger commit `b3b6381bdebca29634ad8d169c47f9bde7bbf353`.
- Passed: source validation, all Gradle unit tests, debug APK assembly, emulator creation/boot, APK installation, activity launch, app-process/crash-log inspection, and login-screen screenshot capture/upload.
- Latest APK artifact: `channelonly-debug-apk`, artifact ID `12386469673`; downloaded APK size 20,522,850 bytes and SHA-256 `b5e4e933d35e346b7f031871783ee9a19a614ac723d53ac8cf4682b89533e621`.
- Latest login screenshot artifact: `channelonly-login-screen`, artifact ID `12388490045`; PNG dimensions 1080 × 1920.
- Login UI: email/password sign-in, account creation, password reset, and Continue with Google via Android Credential Manager → Firebase Google ID-token sign-in.
- Shared HTTPS player-origin validation now gates both embedded playback and PiP eligibility; new unit tests cover valid origins and reject blank, insecure, placeholder, or path/query/fragment-containing origins.
- Sample Firebase values are treated as unconfigured, and `local.properties.example` leaves `playerOrigin` blank by default.
- Emulator smoke test checks install/launch and immediate process stability. It does not validate live Firebase authentication, Google OAuth, YouTube account/API actions, embedded playback, or PiP on a physical device.
- To activate real-account sign-in, the operator must configure Firebase values/providers and a valid Google Web OAuth client/Android OAuth client. Embedded playback/PiP additionally needs a real HTTPS origin the operator controls.
- `player-origin-site/privacy.html` remains a draft; complete the real operator, contact, data-practices and account-deletion details before public release.
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
