package com.cube.channelonly.data

import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeApiErrorMessageTest {
    @Test
    fun explainsExpiredAuthorization() {
        val message = describeYouTubeApiError(401, "authError", "Invalid Credentials")
        assertTrue(message.contains("Reconnect YouTube"))
        assertTrue(message.contains("Invalid Credentials"))
    }

    @Test
    fun explainsDisabledDataApi() {
        val message = describeYouTubeApiError(403, "accessNotConfigured", "API has not been used")
        assertTrue(message.contains("YouTube Data API v3 is not enabled"))
    }

    @Test
    fun explainsQuotaErrors() {
        val message = describeYouTubeApiError(403, "quotaExceeded", "Quota exceeded")
        assertTrue(message.contains("quota or rate limit"))
        assertTrue(message.contains("try again later"))
    }

    @Test
    fun explainsMissingYouTubePermission() {
        val message = describeYouTubeApiError(403, "insufficientPermissions", "Insufficient Permission")
        assertTrue(message.contains("has not granted the required YouTube permission"))
        assertTrue(message.contains("Continue with Google"))
    }

    @Test
    fun preservesGenericApiStatusAndDetail() {
        val message = describeYouTubeApiError(500, null, "Internal error")
        assertTrue(message.contains("HTTP 500"))
        assertTrue(message.contains("Internal error"))
    }
}
