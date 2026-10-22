# ChannelOnly — YouTube Subscription-Only Android App

ChannelOnly is an Android app starter for a focused YouTube experience: its in-app home feed is built from videos uploaded by channels in the signed-in YouTube account's subscriptions.

## Login model

- Google sign-in uses Android Credential Manager and Google's official Sign in with Google flow. The selected account email is kept locally for display and to pin YouTube authorization to that same account.
- The app then requests the YouTube Data API scope separately with Android AuthorizationClient. An authorization token alone does not identify the account, so the app does not treat a token by itself as proof of same-account sign-in.
- There is no Firebase Authentication, Firebase project, or separate ChannelOnly account.
- The app never asks for a Google password, does not read Gmail, and does not store Google ID/access tokens in app preferences.
- Setup requires a Google Cloud Web application OAuth client ID in root `local.properties` as `googleWebClientId=...`, an Android OAuth client for package `com.cube.channelonly` with the exact installed APK signing SHA-1, and YouTube Data API v3 enabled. These are OAuth registrations, not Firebase credentials.
- At app startup, the app tries to reacquire the existing YouTube grant only if the previously selected Google account is known. If access is missing or was revoked, tap Continue with Google again.

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
3. Create **OAuth client ID → Web application** in the same Cloud project. Add the actual Web Client ID to the project's untracked root `local.properties` as `googleWebClientId=YOUR_REAL_WEB_CLIENT_ID.apps.googleusercontent.com`. The ID is not a secret; do not put a client secret in an Android app.
4. Create **OAuth client ID → Android** with package `com.cube.channelonly` and the SHA-1 certificate fingerprint of the exact APK you install. Use `apksigner verify --print-certs app-debug.apk`, or the fingerprint in that build's `channelonly-oauth-build-metadata` CI artifact. Do not reuse a fingerprint from a different CI build or Play Store APK.
5. Enable **YouTube Data API v3**, finish the consent configuration, and add your account as a test user if the OAuth app is in Testing mode.
6. Rebuild/install the APK and tap **Continue with Google**. Credential Manager selects the Google account first; the app then requests YouTube access with that account explicitly selected.
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
