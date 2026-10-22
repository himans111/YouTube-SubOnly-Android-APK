# ChannelOnly सेटअप गाइड — बिना Firebase के

इस ऐप में अलग ChannelOnly अकाउंट नहीं है। पहले Android Credential Manager से Google अकाउंट साइन-इन होता है, फिर Android AuthorizationClient से YouTube API permission ली जाती है। **Firebase की जरूरत नहीं है, लेकिन Google अकाउंट की पहचान के लिए असली Web OAuth Client ID आवश्यक है।** इसके अतिरिक्त Android OAuth client और YouTube Data API v3 भी configure करें।

## 1. Google Cloud का एक बार का सेटअप

1. [Google Cloud Console](https://console.cloud.google.com/) खोलें और नया प्रोजेक्ट बनाएँ या अपना मौजूदा प्रोजेक्ट चुनें।
2. **APIs & Services → Library** में जाकर **YouTube Data API v3** सक्षम करें।
3. **Google Auth Platform → Branding** में ऐप का नाम और आवश्यक संपर्क जानकारी भरें।
4. **Google Auth Platform → Clients** में पहले **Create OAuth client ID → Web application** चुनें। वास्तविक Web Client ID को project root की `local.properties` file में `googleWebClientId=YOUR_REAL_WEB_CLIENT_ID.apps.googleusercontent.com` के रूप में रखें। यह client ID secret नहीं है; client secret को APK में कभी न रखें।
5. अब **Create OAuth client ID → Android** चुनें। Package name बिल्कुल `com.cube.channelonly` रखें। SHA-1 fingerprint उसी APK के signing certificate का होना चाहिए जिसे फोन में इंस्टॉल कर रहे हैं।
6. यदि ऐप Testing mode में है, तो अपने Google खाते को Test users में जोड़ें; यह OAuth consent सेटअप के लिए है।
   - **हर APK का SHA-1 अलग हो सकता है।** उसी CI build के `channelonly-oauth-build-metadata` artifact में `apk-signing-certificate.txt` देखें, या कंप्यूटर पर अपने डाउनलोड किए APK के लिए नीचे वाला `apksigner` कमांड चलाएँ। किसी पुराने run की fingerprint कॉपी न करें।
   - कंप्यूटर पर Android SDK उपलब्ध है तो APK की signing SHA-1 इस कमांड से देखें:

        apksigner verify --print-certs app-debug.apk

   - अलग build या Play Store से इंस्टॉल किए गए APK का certificate fingerprint अलग हो सकता है; उसी APK के सही SHA-1 के साथ Android OAuth client बनाएँ।
6. बदलाव Google Cloud में सेव होने के बाद ऐप खोलकर Google लॉगिन जाँचें।

## 2. ऐप में Google/YouTube लॉगिन

1. ChannelOnly खोलें और **Continue with Google** दबाएँ।
2. वही Google अकाउंट चुनें जिसका YouTube इस्तेमाल करते हैं।
3. पहली बार Google YouTube access की अनुमति माँग सकता है। अनुमति स्क्रीन में दिखाई गई subscription permissions पढ़कर स्वीकार करें।
4. अनुमति सफल होने पर ऐप आपके YouTube subscriptions लोड करेगा। Home feed केवल उन्हीं सब्सक्राइब किए चैनलों के हाल के वीडियो दिखाने के लिए फ़िल्टर करता है।
5. ऐप बंद करके दोबारा खोलने पर यह पहले से दर्ज Google account के लिए YouTube grant दोबारा पाने की कोशिश करता है। यदि permission उपलब्ध नहीं हो, अनुमति रद्द हो चुकी हो या re-authentication जरूरी हो, तो Continue with Google दबाकर फिर से अनुमति दें।
6. ऐप Google password नहीं माँगता और Gmail inbox scan नहीं करता। Firebase या अलग ChannelOnly account की जरूरत नहीं है।

## 3. अगर Google लॉगिन काम न करे

- सुनिश्चित करें कि फोन में Google Play services मौजूद और अद्यतन हैं।
- YouTube Data API v3 चालू है और OAuth consent screen पूरी की गई है।
- Web OAuth client ID वास्तविक है और `local.properties` में `googleWebClientId` सेट है; Android OAuth client में package `com.cube.channelonly` तथा installed APK का सही signing SHA-1 है।
- यदि ऐप Testing mode में है, तो लॉगिन करने वाला Google अकाउंट Test users में होना चाहिए।
- इंटरनेट कनेक्शन जाँचें। अगर Google error दिखाता है कि ऐप unregistered है, तो package/SHA-1 और Cloud OAuth client की सेटिंग मिलाएँ।

## 4. YouTube feed और channel management

- Search में चैनल ढूँढकर Subscribe/Unsubscribe करें।
- Home feed में केवल YouTube API द्वारा लौटाए गए subscribed channel IDs के वीडियो दिखते हैं; ऐप अंतिम client-side channel-ID filter भी लगाता है।
- YouTube Data API quota और Google OAuth account restrictions लागू हो सकते हैं।
- permission हटानी हो, तो Account menu में Disconnect करें; Google Account की security settings से भी ऐप का access हटाया जा सकता है।

## 5. Embedded player और Picture-in-Picture (PiP)

1. `player-origin-site/` की static files को ऐसी HTTPS वेबसाइट पर publish करें जिसे आप नियंत्रित करते हैं।
2. Project root में `local.properties` बनाएँ या खोलें। Android Studio द्वारा मौजूद `sdk.dir` पंक्ति हटाएँ नहीं।
3. केवल origin सेट करें, path या file name नहीं:

        playerOrigin=https://your-live-domain.example

   ऊपर वाला domain सिर्फ उदाहरण है। उसे अपनी वास्तविक HTTPS website से बदलें।
4. APK rebuild/install करें। Feed में वीडियो खोलें, playback शुरू करें और Android Home gesture/button से बाहर जाएँ।
5. सही configuration और समर्थित device पर PiP आ सकता है। YouTube/Android playback restrictions के कारण PiP या background playback हर device/account पर guaranteed नहीं है।
6. Origin खाली होने पर ऐप “Open in YouTube” विकल्प दिखाता है और in-app PiP request नहीं करता।

## 6. Build और automated tests

Project root से चलाएँ:

        ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug

Debug APK यहाँ बनता है:

        app/build/outputs/apk/debug/app-debug.apk

GitHub Actions पर unit tests, build, emulator install/launch और login-screen screenshot की जाँच की जाती है। Emulator smoke test असली Google login या YouTube API access प्रमाणित नहीं करता।

## 7. Release से पहले

- `player-origin-site/privacy.html` अभी draft है। वास्तविक ऐप ऑपरेटर, support contact, डेटा उपयोग, retention और account-deletion प्रक्रिया डालें।
- Debug APK production release नहीं है। Real Android device पर Google authorization, subscription feed, subscribe/unsubscribe, network errors, playback, PiP और account-disconnect जाँचें।
- Passwords या OAuth access tokens को GitHub में commit न करें।
