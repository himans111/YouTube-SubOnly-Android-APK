# ChannelOnly — YouTube Subscription-Only Android App

ChannelOnly is an Android starter project for a focused YouTube experience: the in-app home feed is built from channels in the signed-in YouTube account's subscriptions. Channel search and subscribe/unsubscribe use the YouTube Data API with explicit Google authorization.

## Included

- Firebase Email/Password sign-in and account creation (requires your Firebase project settings).
- Separate Google authorization for YouTube subscriptions; the app never asks for a Google password directly.
- Search for YouTube channels and subscribe/unsubscribe.
- Home feed built only from subscribed channels, with an additional client-side channel-ID filter.
- Embedded YouTube player when a valid HTTPS player origin is configured; otherwise the app provides an Open in YouTube fallback.
- Android Picture-in-Picture (PiP) request when leaving the app while an embedded player is active. PiP behaviour and playback continuation still depend on Android device support and the YouTube embedded player.
- Unit tests for the feed's channel filter and duplicate-video removal.

## Setup overview

Read **[SETUP_HINDI.md](SETUP_HINDI.md)** for the full Hindi walkthrough, including Firebase, YouTube Data API, Android OAuth/SHA-1, embedded-player origin, build commands, and limitations.

1. Create a Firebase project, register Android package `com.cube.channelonly`, enable Email/Password authentication, and copy the app's Firebase values into a local `local.properties` file.
2. Enable YouTube Data API v3 in Google Cloud and configure an Android OAuth client for the same package and your debug signing SHA-1.
3. For embedded playback/PiP, publish `player-origin-site/` to a real HTTPS origin you control and set `playerOrigin=https://your-real-origin.example` in `local.properties`. Do not keep the example domain.
4. Build and test from the project root:

   ```bash
   ./gradlew :app:testDebugUnitTest :app:assembleDebug
   ```

   The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Important notes

- `local.properties` is deliberately ignored by Git. Never commit it or put passwords, private client secrets, or access tokens in source control.
- A successful build does not mean Firebase sign-in, Google authorization, YouTube API quotas, or actual video playback have been configured. Those require your own credentials and a real-device/account test.
- YouTube playback continues to use the standard embedded player and its controls. ChannelOnly filters its in-app feed; it does not modify YouTube's own player UI or guarantee that YouTube hides related videos.
- PiP requires a compatible Android device and a valid configured embedded player. Android/YouTube may restrict background or PiP playback; this is not guaranteed on every device/account.
- The project is a starter implementation and should receive privacy, accessibility, error-handling, and real-device testing before public release.
