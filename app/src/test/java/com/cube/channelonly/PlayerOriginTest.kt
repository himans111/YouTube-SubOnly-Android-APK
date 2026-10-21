package com.cube.channelonly

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerOriginTest {
    @Test
    fun acceptsHttpsOriginWithoutPath() {
        assertTrue(isUsablePlayerOrigin("https://player.channelonly.org"))
        assertTrue(isUsablePlayerOrigin("https://player.channelonly.org/"))
    }

    @Test
    fun rejectsMissingOrInsecureOrigin() {
        assertFalse(isUsablePlayerOrigin(""))
        assertFalse(isUsablePlayerOrigin("http://player.channelonly.org"))
        assertFalse(isUsablePlayerOrigin("not a url"))
    }

    @Test
    fun rejectsSampleAndPlaceholderDomains() {
        assertFalse(isUsablePlayerOrigin("https://example.com"))
        assertFalse(isUsablePlayerOrigin("https://your-site.example"))
        assertFalse(isUsablePlayerOrigin("https://your-real-domain.example"))
        assertFalse(isUsablePlayerOrigin("https://replace-me-host.org"))
    }

    @Test
    fun rejectsPathsCredentialsQueriesAndFragments() {
        assertFalse(isUsablePlayerOrigin("https://player.channelonly.org/path"))
        assertFalse(isUsablePlayerOrigin("https://user:pass@player.channelonly.org"))
        assertFalse(isUsablePlayerOrigin("https://player.channelonly.org?mode=1"))
        assertFalse(isUsablePlayerOrigin("https://player.channelonly.org#player"))
    }
}
