package com.cube.channelonly

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.util.Rational
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cube.channelonly.data.YTChannel
import com.cube.channelonly.data.YTVideo
import com.cube.channelonly.data.YouTubeApi
import com.cube.channelonly.ui.ChannelOnlyTheme
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch

private const val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube.force-ssl"
private const val IDENTITY_PREFS = "channelonly_identity"
private const val GOOGLE_EMAIL_KEY = "google_account_email"

/** One shared origin check keeps embedded playback and PiP eligibility consistent. */
internal fun isUsablePlayerOrigin(value: String): Boolean {
    val configured = value.trim()
    return try {
        val uri = java.net.URI(configured)
        val host = uri.host?.lowercase().orEmpty()
        uri.scheme.equals("https", ignoreCase = true) &&
            host.isNotBlank() &&
            host != "example.com" &&
            !host.endsWith(".example") &&
            !host.startsWith("your-site") &&
            !host.startsWith("your-real-domain") &&
            !host.startsWith("replace-me") &&
            uri.userInfo == null &&
            (uri.rawPath.isNullOrBlank() || uri.rawPath == "/") &&
            uri.query == null &&
            uri.fragment == null
    } catch (_: Exception) {
        false
    }
}

internal fun isConfiguredGoogleWebClientId(value: String): Boolean {
    val clientId = value.trim()
    return Regex("^[0-9]+-[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com$").matches(clientId) &&
        !clientId.contains("YOUR_CLIENT_ID", ignoreCase = true) &&
        !clientId.contains("REPLACE", ignoreCase = true)
}

class MainActivity : ComponentActivity() {
    private var signedInGoogleEmail by mutableStateOf<String?>(null)
    private var youtubeToken by mutableStateOf<String?>(null)
    private val credentialManager by lazy { CredentialManager.create(this) }
    private var youtubeConnectionMessage by mutableStateOf("YouTube not connected")
    private var authorizationMessage by mutableStateOf("")
    private var authorizationInProgress by mutableStateOf(false)
    private var playerVisibleForPip = false
    private var inPipMode by mutableStateOf(false)
    private val authorizationClient by lazy { Identity.getAuthorizationClient(this) }

    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        authorizationInProgress = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val authResult = authorizationClient.getAuthorizationResultFromIntent(result.data)
                acceptYoutubeToken(authResult.accessToken)
            } catch (e: Exception) {
                authorizationMessage = authorizationErrorMessage(e)
                youtubeConnectionMessage = authorizationMessage
            }
        } else {
            authorizationMessage = "Google sign-in or YouTube permission was cancelled. Tap Continue with Google to try again."
            youtubeConnectionMessage = "YouTube connection was cancelled."
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signedInGoogleEmail = getSharedPreferences(IDENTITY_PREFS, MODE_PRIVATE)
            .getString(GOOGLE_EMAIL_KEY, null)
        setContent {
            ChannelOnlyTheme {
                if (youtubeToken == null) {
                    AuthScreen(
                        isConnecting = authorizationInProgress,
                        message = authorizationMessage,
                        onContinueWithGoogle = { signInWithGoogle() }
                    )
                } else {
                    MainScreen(
                        userEmail = signedInGoogleEmail ?: "Google account signed in",
                        googleAccountSignedIn = signedInGoogleEmail != null,
                        token = youtubeToken,
                        connectionMessage = youtubeConnectionMessage,
                        onConnectYoutube = { requestYoutubeAccess(allowInteractive = true) },
                        onDisconnectYoutube = {
                            updatePipEligibility(false)
                            disconnectYoutube()
                        },
                        inPipMode = inPipMode,
                        onPlayerVisibilityChanged = { visible -> updatePipEligibility(visible) },
                        onSignOut = {
                            updatePipEligibility(false)
                            disconnectYoutube()
                        }
                    )
                }
            }
        }

        // AuthorizationClient returns a token, not user identity. Only restore a grant
        // after this app has recorded the account selected through Credential Manager.
        if (!signedInGoogleEmail.isNullOrBlank()) {
            requestYoutubeAccess(allowInteractive = false)
        }
    }

    private fun isConfiguredPlayerOrigin(): Boolean =
        isUsablePlayerOrigin(BuildConfig.PLAYER_ORIGIN)

    private fun updatePipEligibility(visible: Boolean) {
        val canEnterPip = visible && isConfiguredPlayerOrigin()
        playerVisibleForPip = canEnterPip

        // Android 12+ recommends enabling auto-enter early for smoother gesture-navigation PiP.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setAutoEnterEnabled(canEnterPip)
                .build()
            setPictureInPictureParams(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 8–11 need an explicit call. On Android 12+ the current params enable auto-enter.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && playerVisibleForPip && !inPipMode) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: IllegalStateException) {
                // PiP may be unavailable for an activity transition or device policy.
            } catch (_: UnsupportedOperationException) {
                // Respect devices that do not offer Picture-in-Picture.
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipMode = isInPictureInPictureMode
    }

    private fun requestYoutubeAccess(allowInteractive: Boolean = true) {
        if (authorizationInProgress) return
        authorizationInProgress = true
        authorizationMessage = ""

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(YOUTUBE_SCOPE)))
            .build()

        authorizationClient.authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    authorizationInProgress = false
                    if (!allowInteractive) {
                        authorizationMessage = "Choose Continue with Google to select your YouTube account and approve access."
                        return@addOnSuccessListener
                    }
                    val pendingIntent = result.pendingIntent
                    if (pendingIntent == null) {
                        authorizationMessage = "Google did not return the account/permission screen. Check Google Play services and try again."
                    } else {
                        authorizationInProgress = true
                        try {
                            authorizationLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        } catch (e: Exception) {
                            authorizationInProgress = false
                            authorizationMessage = e.localizedMessage ?: "Could not open Google's authorization screen."
                        }
                    }
                } else {
                    authorizationInProgress = false
                    acceptYoutubeToken(result.accessToken)
                }
            }
            .addOnFailureListener { error ->
                authorizationInProgress = false
                authorizationMessage = authorizationErrorMessage(error)
            }
    }

    private fun authorizationErrorMessage(error: Exception): String {
        val apiCode = (error as? ApiException)?.statusCode
        val detail = error.localizedMessage.orEmpty()
        return if (apiCode == 10 || detail.contains("whitelisted", ignoreCase = true)) {
            "Google setup needed: register an Android OAuth client for package com.cube.channelonly and this APK's SHA-1 in Google Cloud, and enable YouTube Data API v3. See SETUP_HINDI.md."
        } else {
            "Google/YouTube login failed: ${detail.ifBlank { "check internet and Google Play services, then retry" }}"
        }
    }

    private fun disconnectYoutube() {
        // Return to the Google login screen immediately, then revoke this app's YouTube grant.
        youtubeToken = null
        authorizationMessage = ""
        youtubeConnectionMessage = "YouTube not connected"
        val revokeRequest = RevokeAccessRequest.builder()
            .setScopes(listOf(Scope(YOUTUBE_SCOPE)))
            .build()
        authorizationClient.revokeAccess(revokeRequest)
            .addOnCompleteListener { task ->
                youtubeConnectionMessage = if (task.isSuccessful) {
                    "YouTube access disconnected"
                } else {
                    "Disconnected on this device. If needed, remove ChannelOnly from Google Account security settings."
                }
            }
    }

    private fun acceptYoutubeToken(token: String?) {
        authorizationInProgress = false
        if (token.isNullOrBlank()) {
            youtubeToken = null
            authorizationMessage = "Google returned no YouTube access token. Tap Continue with Google and approve the requested permission."
            youtubeConnectionMessage = "YouTube not connected"
        } else {
            authorizationMessage = ""
            youtubeToken = token
            youtubeConnectionMessage = "YouTube account connected"
        }
    }
}

@Composable
private fun AuthScreen(
    isConnecting: Boolean,
    message: String,
    onContinueWithGoogle: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color(0xFF272727)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "▶",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "ChannelOnly",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Your subscriptions. Your feed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Continue with the Google account you use for YouTube. On the first sign-in, Google will ask you to approve access to your YouTube subscriptions.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onContinueWithGoogle,
                        enabled = !isConnecting,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.size(10.dp))
                            Text("Connecting to Google…")
                        } else {
                            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.size(10.dp))
                            Text("Continue with Google")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No separate app account or Google password is needed inside this app. Google may still show its official account picker or permission screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private enum class MainTab { HOME, SEARCH, SUBSCRIPTIONS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    userEmail: String,
    googleAccountSignedIn: Boolean,
    token: String?,
    connectionMessage: String,
    onConnectYoutube: () -> Unit,
    onDisconnectYoutube: () -> Unit,
    inPipMode: Boolean,
    onPlayerVisibilityChanged: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val api = remember { YouTubeApi() }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var subscriptions by remember { mutableStateOf<List<YTChannel>>(emptyList()) }
    var feed by remember { mutableStateOf<List<YTVideo>>(emptyList()) }
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<YTChannel>>(emptyList()) }
    var selectedVideo by remember { mutableStateOf<YTVideo?>(null) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var searchLoading by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    fun refreshFeed() {
        val liveToken = token ?: return
        scope.launch {
            loading = true
            message = ""
            try {
                val latestSubscriptions = api.getSubscriptions(liveToken)
                subscriptions = latestSubscriptions
                feed = api.loadSubscribedFeed(liveToken, latestSubscriptions)
                message = if (latestSubscriptions.isEmpty()) "No subscriptions yet. Search for a channel to get started." else ""
            } catch (e: Exception) {
                message = e.localizedMessage ?: "Could not load your subscriptions. Reconnect YouTube and try again."
            } finally { loading = false }
        }
    }

    LaunchedEffect(token) {
        if (token != null) refreshFeed()
        else {
            subscriptions = emptyList()
            feed = emptyList()
            searchResults = emptyList()
            selectedVideo = null
            onPlayerVisibilityChanged(false)
        }
    }

    Scaffold(
        topBar = {
            if (!inPipMode) {
                TopAppBar(
                    title = {
                        if (selectedVideo == null) Text("▶ ChannelOnly", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        else Text("Now playing", fontWeight = FontWeight.SemiBold)
                    },
                    navigationIcon = {
                        if (selectedVideo != null) androidx.compose.material3.IconButton(onClick = {
                            selectedVideo = null
                            onPlayerVisibilityChanged(false)
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showAccountMenu = true }) { Text("Account") }
                    }
                )
            }
        },
        bottomBar = {
            if (!inPipMode && selectedVideo == null) NavigationBar {
                NavigationBarItem(selected = tab == MainTab.HOME, onClick = { tab = MainTab.HOME }, icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = tab == MainTab.SEARCH, onClick = { tab = MainTab.SEARCH }, icon = { Icon(Icons.Default.Search, contentDescription = "Search") }, label = { Text("Search") })
                NavigationBarItem(selected = tab == MainTab.SUBSCRIPTIONS, onClick = { tab = MainTab.SUBSCRIPTIONS }, icon = { Icon(Icons.Default.Subscriptions, contentDescription = "Subscriptions") }, label = { Text("Subscriptions") })
            }
        }
    ) { padding ->
        if (showAccountMenu) {
            AlertDialog(
                onDismissRequest = { showAccountMenu = false },
                title = { Text("Account") },
                text = { Column { Text(userEmail); Spacer(Modifier.height(8.dp)); Text(connectionMessage) } },
                confirmButton = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showAccountMenu = false; onConnectYoutube() }) { Text(if (token == null) "Connect YouTube" else "Reconnect") }
                        if (token != null) TextButton(onClick = { showAccountMenu = false; onDisconnectYoutube() }) { Text("Disconnect") }
                    }
                },
                dismissButton = { TextButton(onClick = { showAccountMenu = false; onSignOut() }) { Text("Sign out") } }
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (token == null) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (googleAccountSignedIn) "Finish connecting YouTube" else "Connect your YouTube account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (googleAccountSignedIn) {
                            "You're already signed in with Google. We'll use the same account; you only need to approve YouTube access if it hasn't been granted yet."
                        } else {
                            "Email/password sign-in is separate. Connect the Google account you use for YouTube and approve the requested subscription permissions."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onConnectYoutube) {
                        Text(if (googleAccountSignedIn) "Continue with the same Google account" else "Connect YouTube with Google")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(connectionMessage, style = MaterialTheme.typography.bodySmall)
                }
            } else if (selectedVideo != null) {
                val playerContext = LocalContext.current
                PlayerScreen(video = selectedVideo!!, onOpenYouTube = { openYouTube(it, playerContext) }, pipMode = inPipMode)
            } else {
                when (tab) {
                    MainTab.HOME -> HomeScreen(
                        feed = feed,
                        subscriptionsCount = subscriptions.size,
                        loading = loading,
                        message = message,
                        onRefresh = { refreshFeed() },
                        onPlay = {
                            selectedVideo = it
                            onPlayerVisibilityChanged(true)
                        }
                    )
                    MainTab.SEARCH -> SearchScreen(
                        query = searchText,
                        onQueryChange = { searchText = it },
                        loading = searchLoading,
                        results = searchResults,
                        subscriptions = subscriptions,
                        message = message,
                        onSearch = {
                            val q = searchText.trim()
                            if (q.isNotBlank()) scope.launch {
                                searchLoading = true; message = ""
                                try { searchResults = api.searchChannels(token, q) }
                                catch (e: Exception) { message = e.localizedMessage ?: "Channel search failed" }
                                finally { searchLoading = false }
                            }
                        },
                        onToggleSubscription = { channel ->
                            scope.launch {
                                loading = true; message = ""
                                try {
                                    val existing = subscriptions.firstOrNull { it.channelId == channel.channelId }
                                    if (existing?.subscriptionId != null) api.unsubscribe(token, existing.subscriptionId)
                                    else if (existing == null) api.subscribe(token, channel.channelId)
                                    subscriptions = api.getSubscriptions(token)
                                    feed = api.loadSubscribedFeed(token, subscriptions)
                                } catch (e: Exception) { message = e.localizedMessage ?: "Subscription change failed" }
                                finally { loading = false }
                            }
                        }
                    )
                    MainTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                        subscriptions = subscriptions,
                        loading = loading,
                        message = message,
                        onSearch = { tab = MainTab.SEARCH },
                        onUnsubscribe = { channel ->
                            val subId = channel.subscriptionId
                            if (subId != null) scope.launch {
                                loading = true
                                try {
                                    api.unsubscribe(token, subId)
                                    subscriptions = api.getSubscriptions(token)
                                    feed = api.loadSubscribedFeed(token, subscriptions)
                                } catch (e: Exception) { message = e.localizedMessage ?: "Unsubscribe failed" }
                                finally { loading = false }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    feed: List<YTVideo>,
    subscriptionsCount: Int,
    loading: Boolean,
    message: String,
    onRefresh: () -> Unit,
    onPlay: (YTVideo) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Your subscribed channels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${subscriptionsCount} channels connected · filtered feed", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onRefresh, enabled = !loading) { Text("Refresh") }
        }
        if (loading) LinearLoading()
        if (message.isNotBlank()) Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
        if (!loading && feed.isEmpty() && message.isBlank()) Text("No recent videos were found from your subscribed channels.", modifier = Modifier.padding(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(feed, key = { it.videoId }) { video -> VideoRow(video, onClick = { onPlay(video) }) }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    loading: Boolean,
    results: List<YTChannel>,
    subscriptions: List<YTChannel>,
    message: String,
    onSearch: () -> Unit,
    onToggleSubscription: (YTChannel) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, onQueryChange, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Search YouTube channels") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            Spacer(Modifier.size(8.dp))
            Button(onClick = onSearch, enabled = query.isNotBlank() && !loading) { Icon(Icons.Default.Search, contentDescription = "Search") }
        }
        Text("Search channels, then subscribe to add them to your feed.", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
        if (loading) LinearLoading()
        if (message.isNotBlank()) Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
        LazyColumn(Modifier.fillMaxSize()) {
            items(results, key = { it.channelId }) { channel ->
                val existing = subscriptions.firstOrNull { it.channelId == channel.channelId }
                ChannelRow(channel, isSubscribed = existing != null, onClick = { onToggleSubscription(channel) })
            }
        }
    }
}

@Composable
private fun SubscriptionsScreen(
    subscriptions: List<YTChannel>,
    loading: Boolean,
    message: String,
    onSearch: () -> Unit,
    onUnsubscribe: (YTChannel) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subscribed channels", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = onSearch) { Text("Find channels") }
        }
        if (loading) LinearLoading()
        if (message.isNotBlank()) Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
        LazyColumn(Modifier.fillMaxSize()) {
            items(subscriptions, key = { it.channelId }) { channel ->
                ChannelRow(channel, isSubscribed = true, onClick = { onUnsubscribe(channel) }, actionLabel = "Unsubscribe")
            }
        }
    }
}

@Composable
private fun VideoRow(video: YTVideo, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(bottom = 18.dp)) {
        AsyncImage(
            model = video.thumbnailUrl.takeIf { it.isNotBlank() },
            contentDescription = video.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(video.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(video.channelTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun ChannelRow(
    channel: YTChannel,
    isSubscribed: Boolean,
    onClick: () -> Unit,
    actionLabel: String = if (isSubscribed) "Subscribed" else "Subscribe"
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = channel.thumbnailUrl.takeIf { it.isNotBlank() },
            contentDescription = channel.title,
            modifier = Modifier.size(54.dp),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(channel.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (channel.description.isNotBlank()) Text(channel.description.take(80), style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        if (isSubscribed && actionLabel == "Subscribed") {
            OutlinedButton(onClick = onClick) { Text("Unsubscribe") }
        } else {
            Button(onClick = onClick) { Text(actionLabel) }
        }
    }
    Divider(color = MaterialTheme.colorScheme.surface)
}

@Composable
private fun PlayerScreen(video: YTVideo, onOpenYouTube: (YTVideo) -> Unit, pipMode: Boolean) {
    val origin = BuildConfig.PLAYER_ORIGIN.trim().trimEnd('/')
    Column(Modifier.fillMaxSize().padding(if (pipMode) 0.dp else 12.dp)) {
        // Keep the WebView first and mounted across PiP transitions so video playback is less likely to reset.
        if (isUsablePlayerOrigin(origin)) {
            EmbeddedYouTubeFrame(video = video, origin = origin, pipMode = pipMode)
        } else {
            Surface(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("To embed the player, configure your HTTPS player origin in local.properties.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onOpenYouTube(video) }) { Text("Open in YouTube") }
                }
            }
        }
        if (!pipMode) {
            Spacer(Modifier.height(12.dp))
            Text(video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(video.channelTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(12.dp))
            Text("The YouTube player keeps its standard controls and related-video experience. The app filters the home feed to subscribed channel IDs.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmbeddedYouTubeFrame(video: YTVideo, origin: String, pipMode: Boolean) {
    val context = LocalContext.current
    val html = remember(video.videoId, origin) {
        val encodedOrigin = Uri.encode(origin)
        """<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><meta name=\"referrer\" content=\"strict-origin-when-cross-origin\"><style>html,body{margin:0;padding:0;background:#000;width:100%;height:100%;overflow:hidden}iframe{border:0;width:100%;height:100%;}</style></head><body><iframe title=\"YouTube video player\" type=\"text/html\" src=\"https://www.youtube.com/embed/${video.videoId}?enablejsapi=1&playsinline=1&origin=$encodedOrigin\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen></iframe></body></html>"""
    }
    androidx.compose.runtime.key(video.videoId, origin) {
        AndroidView(
            modifier = if (pipMode) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL(origin, html, "text/html", "UTF-8", null)
                }
            },
            update = { }
        )
    }
}

@Composable
private fun LinearLoading() {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

private fun openYouTube(video: YTVideo, activityContext: android.content.Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.videoId}"))
    try { activityContext.startActivity(intent) }
    catch (_: Exception) { }
}
