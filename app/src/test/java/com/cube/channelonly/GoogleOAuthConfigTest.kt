package com.cube.channelonly

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleOAuthConfigTest {
    @Test
    fun acceptsGoogleOAuthClientIdShape() {
        assertTrue(isConfiguredGoogleWebClientId("123456789012-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com"))
        assertTrue(isConfiguredGoogleWebClientId("123456789012-a_B-c.apps.googleusercontent.com"))
    }

    @Test
    fun rejectsMissingOrObviousPlaceholderClientIds() {
        assertFalse(isConfiguredGoogleWebClientId(""))
        assertFalse(isConfiguredGoogleWebClientId("YOUR_WEB_CLIENT_ID"))
        assertFalse(isConfiguredGoogleWebClientId("123456789012-YOUR_CLIENT_ID.apps.googleusercontent.com"))
        assertFalse(isConfiguredGoogleWebClientId("https://example.com"))
        assertFalse(isConfiguredGoogleWebClientId("123456789012-abc.example.com"))
    }
}
