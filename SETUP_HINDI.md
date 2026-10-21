# ChannelOnly सेटअप गाइड (हिन्दी)

यह guide app को अपने accounts/configuration के साथ चलाने के लिए है। सिर्फ APK build हो जाना Firebase या YouTube login के सक्रिय होने का प्रमाण नहीं है।

## 1. Firebase Email/Password login

1. Firebase Console में project बनाएँ और Android app register करें।
2. Android package name बिल्कुल यह रखें: com.cube.channelonly
3. Firebase Authentication → Sign-in method में Email/Password enable करें।
4. Project settings → General → Your apps से API key, App ID, Project ID और Project number/Sender ID लें।
6. Android project root में local.properties खोलें। अगर उसमें sdk.dir पहले से है, तो उसे हटाएँ नहीं। local.properties.example के placeholder values को अपनी Firebase values से बदलें। उदाहरण:

        firebaseApiKey=YOUR_REAL_FIREBASE_WEB_API_KEY
        firebaseAppId=1:YOUR_SENDER_ID:android:YOUR_REAL_APP_ID
        firebaseProjectId=YOUR_FIREBASE_PROJECT_ID
        firebaseSenderId=YOUR_SENDER_ID
        googleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
        playerOrigin=

   Placeholder text को जस का तस छोड़ने पर login configure नहीं होगा।
7. App rebuild/install करें और नया account बनाकर email sign-in, Google sign-in, sign-out तथा password reset जाँचें।

## 2. YouTube API और Google authorization

1. Google Cloud Console में intended project खोलें और YouTube Data API v3 enable करें।
2. Google Cloud → APIs & Services → Credentials में **OAuth 2.0 Client ID → Web application** बनाएं। उसका Client ID `googleWebClientId` में डालें। यह **Web client ID** है, Android client ID नहीं। अगर consent screen testing mode में है, तो अपने Google account को test user के रूप में जोड़ें।
3. अलग से Android OAuth client बनाएँ जिसमें package name `com.cube.channelonly` और सही debug-signing SHA-1 हो। SHA-1 निकालने के लिए project root में चलाएँ:

        ./gradlew signingReport

   अगर checkout में Gradle wrapper उपलब्ध न हो, तो installed Gradle से gradle signingReport चलाएँ या Android Studio के Gradle task panel में signingReport चुनें।
4. Login स्क्रीन पर email/password या **Continue with Google** में से किसी एक से ChannelOnly account में sign-in करें। Google sign-in के लिए Firebase Google provider enabled और `googleWebClientId` सही होना चाहिए। इसके बाद Connect YouTube with Google दबाएँ और YouTube permission स्वीकार करें। App में अपना Google password कभी न लिखें।
5. Home feed, channel search और subscribe/unsubscribe को वास्तविक account के साथ जाँचें। API quota, Google consent और account-level restrictions की वजह से requests फिर भी fail हो सकती हैं।

## 3. Embedded player और Picture-in-Picture (PiP)

PiP केवल तब request होगा जब embedded-player origin configure किया गया हो और आप video player screen पर हों। YouTube/Android playback restrictions के कारण PiP या background playback हर device/account पर guaranteed नहीं है।

1. player-origin-site/ की static files को ऐसी HTTPS website पर publish करें जिसे आप control करते हैं।
2. Root local.properties में playerOrigin को origin मात्र रखें, path या file name नहीं। उदाहरण के तौर पर:

        playerOrigin=https://your-real-domain.example

   यह example domain है; इसे अपनी live HTTPS domain से बदलें।
3. Rebuild/install करें। Subscribed feed में video खोलें, playback manually start करें और Android Home gesture/button से बाहर जाएँ। Compatible device पर PiP window आने की जाँच करें।
4. Origin configure न होने पर app “Open in YouTube” fallback दिखाता है और in-app PiP request नहीं करेगा।

## 4. Build और automated tests

Project root में चलाएँ:

        gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug

या Android Studio से Gradle sync कराकर test/build tasks चलाएँ। Debug APK output path:

        app/build/outputs/apk/debug/app-debug.apk

GitHub Actions run और APK artifacts repository के Actions tab में दिखते हैं। Hosted runner का emulator smoke test धीमा हो सकता है; build/tests और emulator result को अलग-अलग देखें।

## 5. Privacy और release readiness

- local.properties को Git में commit न करें। Firebase values, Google access tokens और passwords भी source control में न रखें।
- player-origin-site/privacy.html अभी draft है। Public release से पहले operator name, support email, date, account deletion प्रक्रिया और actual data practices भरें।
- Debug APK production release नहीं है। Real device पर authentication, channel search, subscription changes, network failures, playback, PiP, orientation, accessibility और privacy policy जाँचें।
- YouTube embedded player के controls/related-video UI को यह app बदलता नहीं है; app की तरफ़ से मुख्य restriction home-feed channel filtering है।
