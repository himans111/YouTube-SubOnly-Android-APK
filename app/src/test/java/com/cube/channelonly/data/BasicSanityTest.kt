package com.cube.channelonly.data

import com.cube.channelonly.googleAccountEmailForYoutubeAuthorization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BasicSanityTest {
    @Test fun channelIdIsStoredWithoutTransformation() {
        val channel = YTChannel("UC_test_123", "Test channel", "", "")
        assertEquals("UC_test_123", channel.channelId)
    }

    @Test fun feedFiltersOutVideosFromNonSubscribedChannels() {
        val allowed = YTVideo("abc123", "A title", "UC_allowed", "Allowed", "", "2026-01-01")
        val blocked = YTVideo("xyz123", "Another title", "UC_other", "Other", "", "2026-01-02")
        val result = filterSubscribedVideos(listOf(allowed, blocked), setOf("UC_allowed"))
        assertEquals(listOf("abc123"), result.map { it.videoId })
    }

    @Test fun duplicateVideoIdsAreRemovedFromFeed() {
        val one = YTVideo("abc123", "A title", "UC_allowed", "Allowed", "", "2026-01-01")
        val duplicate = one.copy(title = "Duplicate title")
        assertEquals(1, filterSubscribedVideos(listOf(one, duplicate), setOf("UC_allowed")).size)
    }

    @Test fun youtubeAuthorizationUsesEmailFromGoogleBackedSession() {
        assertEquals(
            "person@gmail.com",
            googleAccountEmailForYoutubeAuthorization(" person@gmail.com ", listOf("firebase", "google.com"))
        )
    }

    @Test fun youtubeAuthorizationDoesNotPinAnEmailPasswordOnlySessionToGoogle() {
        assertNull(googleAccountEmailForYoutubeAuthorization("person@example.com", listOf("password")))
    }

    @Test fun youtubeAuthorizationRejectsMissingOrInvalidEmail() {
        assertNull(googleAccountEmailForYoutubeAuthorization(null, listOf("google.com")))
        assertNull(googleAccountEmailForYoutubeAuthorization("not-an-email", listOf("google.com")))
    }
}
