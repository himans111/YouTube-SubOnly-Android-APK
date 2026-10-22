package com.cube.channelonly.data

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class YouTubeApiRequestTest {
    private lateinit var server: MockWebServer
    private lateinit var api: YouTubeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = YouTubeApi(server.url("/"), OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getSubscriptionsUsesBearerTokenPaginatesAndDeduplicatesChannels() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "nextPageToken":"PAGE_2",
                  "items":[{
                    "id":"SUB_A",
                    "snippet":{
                      "title":"Alpha",
                      "description":"Alpha description",
                      "resourceId":{"channelId":"UC_ALPHA"},
                      "thumbnails":{"high":{"url":"https://img.example/alpha.jpg"}}
                    }
                  }]
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "items":[
                    {"id":"SUB_A_DUP","snippet":{"title":"Alpha duplicate","resourceId":{"channelId":"UC_ALPHA"}}},
                    {"id":"SUB_B","snippet":{"title":"Beta","resourceId":{"channelId":"UC_BETA"}}}
                  ]
                }
                """.trimIndent()
            )
        )

        val channels = api.getSubscriptions("access-token-123")

        assertEquals(listOf("UC_ALPHA", "UC_BETA"), channels.map { it.channelId })
        assertEquals("Alpha", channels.first().title)
        assertEquals("SUB_A", channels.first().subscriptionId)
        assertEquals("https://img.example/alpha.jpg", channels.first().thumbnailUrl)

        val first = server.takeRequest(2, TimeUnit.SECONDS)
        val second = server.takeRequest(2, TimeUnit.SECONDS)
        assertNotNull(first)
        assertNotNull(second)
        assertEquals("GET", first!!.method)
        assertEquals("/youtube/v3/subscriptions", first.requestUrl!!.encodedPath)
        assertEquals("true", first.requestUrl!!.queryParameter("mine"))
        assertEquals("snippet", first.requestUrl!!.queryParameter("part"))
        assertEquals("50", first.requestUrl!!.queryParameter("maxResults"))
        assertEquals("Bearer access-token-123", first.getHeader("Authorization"))
        assertEquals("PAGE_2", second!!.requestUrl!!.queryParameter("pageToken"))
    }

    @Test
    fun searchChannelsTrimsQueryAndReturnsParsedChannel() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "items":[{
                    "id":{"channelId":"UC_FOUND"},
                    "snippet":{
                      "title":"Found Channel",
                      "description":"About it",
                      "thumbnails":{"medium":{"url":"https://img.example/found.jpg"}}
                    }
                  }]
                }
                """.trimIndent()
            )
        )

        val empty = api.searchChannels("token", "   ")
        assertTrue(empty.isEmpty())
        assertEquals(0, server.requestCount)

        val channels = api.searchChannels("token", "  science  ")

        assertEquals(1, channels.size)
        assertEquals("UC_FOUND", channels.single().channelId)
        assertEquals("Found Channel", channels.single().title)
        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("/youtube/v3/search", request!!.requestUrl!!.encodedPath)
        assertEquals("science", request.requestUrl!!.queryParameter("q"))
        assertEquals("channel", request.requestUrl!!.queryParameter("type"))
        assertEquals("Bearer token", request.getHeader("Authorization"))
    }

    @Test
    fun subscribeAndUnsubscribeUseExpectedMethodsAndIds() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"SUB_CREATED"}"""))
        server.enqueue(MockResponse().setResponseCode(204))

        api.subscribe("token", "UC_TO_SUBSCRIBE")
        api.unsubscribe("token", "SUB_TO_DELETE")

        val post = server.takeRequest(2, TimeUnit.SECONDS)
        assertNotNull(post)
        assertEquals("POST", post!!.method)
        assertEquals("/youtube/v3/subscriptions", post.requestUrl!!.encodedPath)
        assertEquals("snippet", post.requestUrl!!.queryParameter("part"))
        assertEquals("Bearer token", post.getHeader("Authorization"))
        val body = post.body.readUtf8()
        assertTrue(body.contains("\"kind\":\"youtube#channel\""))
        assertTrue(body.contains("\"channelId\":\"UC_TO_SUBSCRIBE\""))

        val delete = server.takeRequest(2, TimeUnit.SECONDS)
        assertNotNull(delete)
        assertEquals("DELETE", delete!!.method)
        assertEquals("/youtube/v3/subscriptions", delete.requestUrl!!.encodedPath)
        assertEquals("SUB_TO_DELETE", delete.requestUrl!!.queryParameter("id"))
        assertEquals("Bearer token", delete.getHeader("Authorization"))
    }

    @Test
    fun subscribedFeedFiltersUnsubscribedChannelsAndDeduplicatesVideos() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val url = request.requestUrl ?: return MockResponse().setResponseCode(400)
                return when (url.encodedPath) {
                    "/youtube/v3/channels" -> MockResponse().setResponseCode(200).setBody(
                        """
                        {
                          "items":[
                            {"id":"UC_ALPHA","contentDetails":{"relatedPlaylists":{"uploads":"UPLOADS_ALPHA"}}},
                            {"id":"UC_BETA","contentDetails":{"relatedPlaylists":{"uploads":"UPLOADS_BETA"}}}
                          ]
                        }
                        """.trimIndent()
                    )
                    "/youtube/v3/playlistItems" -> when (url.queryParameter("playlistId")) {
                        "UPLOADS_ALPHA" -> MockResponse().setResponseCode(200).setBody(
                            """
                            {"items":[
                              {"snippet":{"channelId":"UC_ALPHA","channelTitle":"Alpha","title":"Shared video","publishedAt":"2026-10-20T10:00:00Z","thumbnails":{}},"contentDetails":{"videoId":"VIDEO_SHARED"}},
                              {"snippet":{"channelId":"UC_NOT_SUBSCRIBED","channelTitle":"Outside","title":"Wrong channel video","publishedAt":"2026-10-21T10:00:00Z","thumbnails":{}},"contentDetails":{"videoId":"VIDEO_OUTSIDE"}}
                            ]}
                            """.trimIndent()
                        )
                        "UPLOADS_BETA" -> MockResponse().setResponseCode(200).setBody(
                            """
                            {"items":[
                              {"snippet":{"channelId":"UC_BETA","channelTitle":"Beta","title":"Shared video duplicate","publishedAt":"2026-10-19T10:00:00Z","thumbnails":{}},"contentDetails":{"videoId":"VIDEO_SHARED"}},
                              {"snippet":{"channelId":"UC_BETA","channelTitle":"Beta","title":"Beta video","publishedAt":"2026-10-22T10:00:00Z","thumbnails":{}},"contentDetails":{"videoId":"VIDEO_BETA"}}
                            ]}
                            """.trimIndent()
                        )
                        else -> MockResponse().setResponseCode(404)
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val subscriptions = listOf(
            YTChannel("UC_ALPHA", "Alpha", "", "", "SUB_ALPHA"),
            YTChannel("UC_BETA", "Beta", "", "", "SUB_BETA")
        )
        val videos = api.loadSubscribedFeed("token", subscriptions)

        assertEquals(setOf("VIDEO_SHARED", "VIDEO_BETA"), videos.map { it.videoId }.toSet())
        assertFalse(videos.any { it.channelId == "UC_NOT_SUBSCRIBED" })
        assertEquals(2, videos.size)
        assertEquals(listOf("2026-10-22T10:00:00Z", "2026-10-20T10:00:00Z"), videos.map { it.publishedAt })
    }

    @Test
    fun returnsActionableTypedErrorForDisabledYouTubeApi() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(403).setBody(
                """
                {"error":{"code":403,"message":"API has not been used","errors":[{"reason":"accessNotConfigured"}]}}
                """.trimIndent()
            )
        )

        try {
            api.getSubscriptions("token")
            throw AssertionError("Expected YouTubeApiException")
        } catch (error: YouTubeApiException) {
            assertEquals(403, error.statusCode)
            assertEquals("accessNotConfigured", error.reason)
            assertTrue(error.message!!.contains("YouTube Data API v3 is not enabled"))
        }
    }
}
