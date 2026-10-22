# ChannelOnly build status

## Latest fully validated source/build

- Repository: https://github.com/himans111/YouTube-SubOnly-Android-APK
- Latest completed CI run: [GitHub Actions run #43](https://github.com/himans111/YouTube-SubOnly-Android-APK/actions/runs/39734838858)
- Tested source commit: `dc690cc8a7b16de980ff45409f60ac5aa46dfd17`
- Passed: Gradle unit tests, debug APK assembly, Android emulator boot, APK installation, app launch/process stability/crash-log inspection, and 1080 × 1920 login-screen screenshot capture.
- Downloaded APK size: 18,899,222 bytes. SHA-256: `12fd81e9a73eea7661023192d25e28a40c2fb35b53090366161a4d605a80f273`.
- APK package: `com.cube.channelonly`; minimum Android version API 26 (Android 8.0).
- SHA-1 signing-certificate fingerprint for this exact APK: `25:C9:62:9A:18:5A:E3:41:08:19:65:BE:D7:06:9C:B3:E8:5A:8C:D9`. Register this package name and SHA-1 as an Android OAuth client in Google Cloud if you want this exact APK to access YouTube. A different build/signing certificate may need a different client fingerprint.
- Login architecture: direct Google Android AuthorizationClient → YouTube OAuth. There is no Firebase authentication, no separate ChannelOnly account, no web client ID, and the app does not scan Gmail or request the Google password.
- Access token is held in app memory. On app launch, the app silently tries to reacquire a previous YouTube authorization; when new consent is required, the login button opens Google's official account/permission flow.
- YouTube Data API features implemented: list subscription pages, search channels, subscribe/unsubscribe, load recent uploads from subscribed channels, client-side channel-ID filtering, and duplicate-video removal.
- Embedded player is enabled only with a valid HTTPS origin set in local `local.properties`; otherwise the app opens the video in YouTube. PiP is available only when the embedded player/origin are configured and the device/player allow it.
- Important validation limit: CI verifies build/tests/install/launch, not a successful real Google OAuth consent or live YouTube API request. The current emulator screenshot displays the setup hint because the CI build's package/SHA-1 is not registered in a personal Google Cloud project and YouTube Data API/consent setup are not supplied.
- `player-origin-site/privacy.html` is a draft and must be completed with the actual operator, contact, age/data practices, retention and user-control information before public release.
- This is a debug APK, not a signed production release.

## Remaining user-account setup

1. In Google Cloud Console enable YouTube Data API v3 and finish Google Auth Platform consent/branding settings; add the Google account as a test user if the app is in Testing mode.
2. Create an Android OAuth client with package `com.cube.channelonly` and SHA-1 shown above for this exact APK.
3. Install the APK and test account selection, consent, feed, channel search, subscribe/unsubscribe, disconnect/reconnect, and token reauthorization on a real phone.
4. For embedded playback/PiP, publish `player-origin-site/` to a real HTTPS origin you control and set `playerOrigin` locally. Complete the real privacy policy before any public release.

## Emulator workflow notes

- The workflow uses Android API 30 Google APIs x86_64 emulator and tries KVM before falling back to explicitly opted-in software emulation.
- Emulator smoke test is not a substitute for real Google OAuth or YouTube account testing.
- ADB waits have timeouts and failure diagnostics; debug APK and screenshot artifacts are uploaded by GitHub Actions.
