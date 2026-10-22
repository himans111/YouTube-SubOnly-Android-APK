# ChannelOnly — YouTube Subscription-Only Android App

ChannelOnly is an Android app starter for a focused YouTube experience: its in-app home feed is built from videos uploaded by channels in the signed-in YouTube account's subscriptions.

## Login model

- The **Continue with Google** button uses Google's official Android AuthorizationClient to choose the Google account and request the YouTube API permission in one flow.
- The explicit button tap asks Google to show the account picker; the access token returned after consent is used for YouTube Data API requests. The app does not try to combine a separate identity token with a different authorization token.
- There is no Firebase Authentication, Firebase project, separate ChannelOnly account, or Web OAuth Client ID requirement.
- The app never asks for a Google password and does not read Gmail.
- Setup requires a Google Cloud **Android OAuth client** registered with package `com.cube.channelonly` and the exact installed APK signing SHA-1, plus YouTube Data API v3 enabled. A successful build does not prove the OAuth project is configured.
- At startup, the app silently tries to reacquire a previously granted YouTube token; if account selection or new consent is needed, the login button remains available.

## Included

- Direct Google/YouTube OAuth login, account/consent selection, and permission disconnect.
- Search YouTube channels and subscribe/unsubscribe.
- Home feed limited to recent uploads from subscribed channels, with a final client-side channel-ID filter and duplicate removal.
- Embedded YouTube player when a valid HTTPS player origin is configured; otherwise an **Open in YouTube** fallback.
- Android Picture-in-Picture (PiP) request when leaving the app while the embedded player is active. PiP and playback continuation depend on device and YouTube player restrictions.
- Unit tests for subscribed-feed filtering, duplicate-video removal, and HTTPS player-origin validation.

## Setup overview

Read **[SETUP_HINDI.md](SETUP_HINDI.md)** for the full Hindi walkthrough.

1. In Google Cloud Console, create/select a project and enable **YouTube Data API v3**.
2. Complete the OAuth consent screen. If the app is in Testing mode, add your Google account as a test user.
3. Create **OAuth client ID → Android** with package name `com.cube.channelonly` and the SHA-1 fingerprint of the exact APK you install. For a debug APK, use `apksigner verify --print-certs app-debug.apk` or the fingerprint in the matching `channelonly-oauth-build-metadata` CI artifact. Do not reuse a fingerprint from another build.
4. Install that APK and tap **Continue with Google**. Choose the Google account used for YouTube and approve the requested permission.
5. For embedded playback/PiP, publish `player-origin-site/` to an HTTPS origin you control and set `playerOrigin=https://your-real-origin.example` in local `local.properties`. Otherwise the app opens videos in YouTube.

## Build and test

From the project root:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Important notes

- Firebase and a separate Web OAuth client ID are not used. The Android OAuth client and its matching package/signing certificate are required for YouTube authorization.
- Google Cloud OAuth registration and enabling YouTube Data API v3 are required for private subscription data; a successful build does not prove the OAuth project is configured.
- Never put passwords or access tokens in source control.
- `player-origin-site/privacy.html` is a draft. Complete the actual operator, contact details, data practices, and account-deletion process before public release.
- YouTube playback continues to use the standard embedded player and controls. ChannelOnly filters its own feed; it does not modify YouTube's player UI or guarantee that YouTube hides related videos.
- The current artifact is a debug APK, not a signed production release. Test OAuth and YouTube API calls with a real account/device before release.
