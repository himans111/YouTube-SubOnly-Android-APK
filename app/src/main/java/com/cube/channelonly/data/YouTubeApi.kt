package com.cube.channelonly.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val API_HOST = "www.googleapis.com"

internal class YouTubeApiException(
    val statusCode: Int,
    val reason: String?,
    message: String
) : IOException(message)

/** Converts YouTube API failures into a next-step hint users can act on. */
internal fun describeYouTubeApiError(statusCode: Int, reason: String?, apiMessage: String): String {
    val detail = apiMessage.ifBlank { "HTTP $statusCode" }
    return when {
        statusCode == 401 ->
            "YouTube authorization expired or was rejected. Reconnect YouTube with Google, then retry. ($detail)"
        reason in setOf("accessNotConfigured", "SERVICE_DISABLED") ->
            "YouTube Data API v3 is not enabled for the Google Cloud project used by this app. ($detail)"
        reason in setOf("quotaExceeded", "dailyLimitExceeded", "rateLimitExceeded", "userRateLimitExceeded") ->
            "YouTube Data API quota or rate limit was reached. Check the Google Cloud quota and try again later. ($detail)"
        reason == "insufficientPermissions" ->
            "The selected Google account has not granted the required YouTube permission. Tap Continue with Google and approve access. ($detail)"
        statusCode == 403 ->
            "Google denied the YouTube API request. Check OAuth consent, the granted permission, and API quota/settings. ($detail)"
        else -> "YouTube API error (HTTP $statusCode): $detail"
    }
}

private fun defaultYouTubeHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .callTimeout(45, TimeUnit.SECONDS)
    .build()

/** YouTube Data API client. All calls are authenticated with the current user's OAuth access token. */
class YouTubeApi(
    private val baseUrl: HttpUrl = HttpUrl.Builder().scheme("https").host(API_HOST).build(),
    private val client: OkHttpClient = defaultYouTubeHttpClient()
)

    private suspend fun request(
        token: String,
        path: String,
        params: List<Pair<String, String>>,
        method: String = "GET",
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        if (token.isBlank()) throw IOException("YouTube account is not connected. Connect it again.")
        val url = baseUrl.newBuilder()
            .addPathSegments("youtube/v3/$path")
            .apply { params.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()

        val builder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
        when (method) {
            "POST" -> builder.post((body ?: JSONObject()).toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            "DELETE" -> builder.delete()
            else -> builder.get()
        }

        client.newCall(builder.build()).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = try {
                    JSONObject(responseText).optJSONObject("error")
                } catch (_: Exception) { null }
                val apiMessage = error?.optString("message").orEmpty()
                val reason = error?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason")
                throw YouTubeApiException(
                    statusCode = response.code,
                    reason = reason,
                    message = describeYouTubeApiError(response.code, reason, apiMessage)
                )
            }
            if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
        }
    }

    suspend fun getSubscriptions(token: String): List<YTChannel> {
        val all = mutableListOf<YTChannel>()
        var pageToken: String? = null
        var pages = 0
        do {
            val params = mutableListOf(
                "part" to "snippet",
                "mine" to "true",
                "maxResults" to "50"
            )
            pageToken?.let { params += "pageToken" to it }
            val json = request(token, "subscriptions", params)
            val items = json.optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val snippet = item.optJSONObject("snippet") ?: continue
                    val channelId = snippet.optJSONObject("resourceId")?.optString("channelId").orEmpty()
                    if (channelId.isBlank()) continue
                    all += YTChannel(
                        channelId = channelId,
                        title = snippet.optString("title", "YouTube channel"),
                        description = snippet.optString("description", ""),
                        thumbnailUrl = bestThumbnail(snippet.optJSONObject("thumbnails")),
                        subscriptionId = item.optString("id").takeIf { it.isNotBlank() }
                    )
                }
            }
            pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
            pages++
        } while (pageToken != null && pages < 30) // Safety cap for very large subscription lists.
        return all.distinctBy { it.channelId }
    }

    suspend fun searchChannels(token: String, query: String): List<YTChannel> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val json = request(
            token,
            "search",
            listOf("part" to "snippet", "type" to "channel", "q" to q, "maxResults" to "20")
        )
        val items = json.optJSONArray("items") ?: return emptyList()
        return buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val snippet = item.optJSONObject("snippet") ?: continue
                val channelId = item.optJSONObject("id")?.optString("channelId").orEmpty()
                if (channelId.isBlank()) continue
                add(
                    YTChannel(
                        channelId = channelId,
                        title = snippet.optString("title", "YouTube channel"),
                        description = snippet.optString("description", ""),
                        thumbnailUrl = bestThumbnail(snippet.optJSONObject("thumbnails"))
                    )
                )
            }
        }
    }

    suspend fun subscribe(token: String, channelId: String) {
        val body = JSONObject().put(
            "snippet",
            JSONObject().put(
                "resourceId",
                JSONObject().put("kind", "youtube#channel").put("channelId", channelId)
            )
        )
        request(token, "subscriptions", listOf("part" to "snippet"), method = "POST", body = body)
    }

    suspend fun unsubscribe(token: String, subscriptionId: String) {
        request(token, "subscriptions", listOf("id" to subscriptionId), method = "DELETE")
    }

    /** Loads a limited recent-upload feed from subscribed channels only; never runs a global video search. */
    suspend fun loadSubscribedFeed(token: String, subscriptions: List<YTChannel>): List<YTVideo> {
        val selected = subscriptions.distinctBy { it.channelId }
        if (selected.isEmpty()) return emptyList()

        val uploadsByChannel = mutableMapOf<String, String>()
        for (batch in selected.map { it.channelId }.distinct().chunked(50)) {
            val json = request(
                token,
                "channels",
                listOf("part" to "contentDetails", "id" to batch.joinToString(","), "maxResults" to "50")
            )
            val items = json.optJSONArray("items") ?: continue
            for (i in 0 until items.length()) {
                val channel = items.optJSONObject(i) ?: continue
                val id = channel.optString("id")
                val uploadsId = channel.optJSONObject("contentDetails")
                    ?.optJSONObject("relatedPlaylists")?.optString("uploads").orEmpty()
                if (id.isNotBlank() && uploadsId.isNotBlank()) uploadsByChannel[id] = uploadsId
            }
        }

        val allowedIds = selected.map { it.channelId }.toSet()
        val channelTitles = selected.associate { it.channelId to it.title }

        // A modest concurrency limit makes large subscription lists faster without flooding the API.
        val videoBatches = uploadsByChannel.entries.toList().chunked(5).map { batch ->
            coroutineScope {
                batch.map { entry ->
                    async {
                        val channelId = entry.key
                        val uploadsPlaylistId = entry.value
                        try {
                            val json = request(
                                token,
                                "playlistItems",
                                listOf("part" to "snippet,contentDetails", "playlistId" to uploadsPlaylistId, "maxResults" to "5")
                            )
                            val items = json.optJSONArray("items") ?: return@async emptyList<YTVideo>()
                            buildList {
                                for (i in 0 until items.length()) {
                                    val item = items.optJSONObject(i) ?: continue
                                    val snippet = item.optJSONObject("snippet") ?: continue
                                    val actualChannel = snippet.optString("channelId", channelId)
                                    val videoId = item.optJSONObject("contentDetails")?.optString("videoId").orEmpty()
                                        .ifBlank { snippet.optJSONObject("resourceId")?.optString("videoId").orEmpty() }
                                    if (videoId.isBlank() || actualChannel !in allowedIds) continue
                                    add(
                                        YTVideo(
                                            videoId = videoId,
                                            title = snippet.optString("title", "YouTube video"),
                                            channelId = actualChannel,
                                            channelTitle = snippet.optString("channelTitle", channelTitles[actualChannel] ?: "Subscribed channel"),
                                            thumbnailUrl = bestThumbnail(snippet.optJSONObject("thumbnails")),
                                            publishedAt = snippet.optString("publishedAt", "")
                                        )
                                    )
                                }
                            }
                        } catch (e: YouTubeApiException) {
                            // Skip deleted/unavailable playlists, but surface auth, quota, and other API failures.
                            if (e.statusCode == 404) emptyList() else throw e
                        }
                    }
                }.awaitAll()
            }
        }.flatten()

        return filterSubscribedVideos(videoBatches.flatten(), allowedIds).sortedByDescending { it.publishedAt }
    }

    private fun bestThumbnail(thumbnails: JSONObject?): String {
        if (thumbnails == null) return ""
        for (size in listOf("high", "medium", "default")) {
            val url = thumbnails.optJSONObject(size)?.optString("url").orEmpty()
            if (url.isNotBlank()) return url
        }
        return ""
    }
}

data class YTChannel(
    val channelId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val subscriptionId: String? = null
)

data class YTVideo(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val publishedAt: String
)


/** Final client-side guard: never render a feed item whose channel ID is not in the authorized subscription set. */
internal fun filterSubscribedVideos(videos: List<YTVideo>, subscribedChannelIds: Set<String>): List<YTVideo> =
    videos.filter { it.channelId in subscribedChannelIds }.distinctBy { it.videoId }
