# ChannelOnly — YouTube Subscription-Only Android App

ChannelOnly is an Android app starter for a focused YouTube experience: its in-app home feed is built from videos uploaded by channels in the signed-in YouTube account's subscriptions.

## Login model

- The app uses Google's official Android authorization flow directly for YouTube access. There is no Firebase login and no separate ChannelOnly account.
- Tap **Continue with Google**, choose the Google account used for YouTube, and approve the requested YouTube permission on first use.
- Google may show an account picker or consent screen on first use. On later launches, the app silently tries to reacquire a previously granted YouTube access token. A prompt may return if consent has been revoked or the account requires re-authentication.
- The app never asks for a Google password and does not read Gmail.
- A Google Cloud OAuth Android client and YouTube Data API v3 configuration are still required. This is Google's standard authorization registration, not a Firebase setup.

## Included

- Direct Google/YouTube OAuth login, account/consent selection, and permission disconnect.
- Search YouTube channels and subscribe/unsubscribe.
- Home feed limited to recent uploads from subscribed channels, with a final client-side channel-ID filter and duplicate removal.
- Embedded YouTube player when a valid HTTPS player origin is configured; otherwise an **Open in YouTube** fallback.
- Android Picture-in-Picture (PiP) request when leaving the app while the embedded player is active. PiP and playback continuation depend on device and YouTube player restrictions.
- Unit tests for subscribed-feed filtering, duplicate-video removal, and HTTPS player-origin validation.

## Setup overview

Read **[SETUP_HINDI.md](SETUP_HINDI.md)** for the full Hindi walkthrough.

1. In Google Cloud Console, create or select a project and enable **YouTube Data API v3**.
2. Configure the OAuth consent screen. If the app is in Testing mode, add your Google account as a test user.
3. Create an **OAuth client ID → Android** using package `com.cube.channelonly` and the SHA-1 signing-certificate fingerprint of the APK you install. For a debug APK, the SHA-1 can be read using the Android SDK's `apksigner verify --print-certs app-debug.apk`. A Play Store release needs the corresponding release/App Signing certificate.
4. Install the APK and tap **Continue with Google**. Google will select the account and request YouTube access when needed.
5. For embedded playback/PiP, publish `player-origin-site/` to an HTTPS origin you control and set `playerOrigin=https://your-real-origin.example` in local `local.properties`. Otherwise the app opens the video in YouTube.

## Build and test

From the project root:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Important notes

- No Firebase API key, Firebase project ID, or Web client ID is required by this direct mobile OAuth implementation.
- Google Cloud OAuth registration and enabling YouTube Data API v3 are required for private subscription data; a successful build does not prove the OAuth project is configured.
- Never put passwords or access tokens in source control.
- `player-origin-site/privacy.html` is a draft. Complete the actual operator, contact details, data practices, and account-deletion process before public release.
- YouTube playback continues to use the standard embedded player and controls. ChannelOnly filters its own feed; it does not modify YouTube's player UI or guarantee that YouTube hides related videos.
- The current artifact is a debug APK, not a signed production release. Test OAuth and YouTube API calls with a real account/device before release.
