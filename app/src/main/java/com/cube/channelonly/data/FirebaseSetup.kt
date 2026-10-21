package com.cube.channelonly.data

import android.content.Context
import com.cube.channelonly.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth

object FirebaseSetup {
    fun isConfigured(): Boolean {
        val apiKey = BuildConfig.FIREBASE_API_KEY.trim()
        val appId = BuildConfig.FIREBASE_APP_ID.trim()
        val projectId = BuildConfig.FIREBASE_PROJECT_ID.trim()

        // Non-empty sample placeholders must not be mistaken for real Firebase credentials.
        return apiKey.isNotBlank() &&
            !apiKey.contains("YOUR_", ignoreCase = true) &&
            appId.isNotBlank() &&
            !appId.contains("YOUR_", ignoreCase = true) &&
            !appId.startsWith("1:1234567890:") &&
            projectId.isNotBlank() &&
            !projectId.contains("YOUR_", ignoreCase = true) &&
            !projectId.startsWith("your-", ignoreCase = true)
    }

    fun initialize(context: Context): FirebaseAuth? {
        if (!isConfigured()) return null
        return try {
            val options = FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .apply {
                    if (BuildConfig.FIREBASE_SENDER_ID.isNotBlank()) {
                        setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                    }
                }
                .build()
            val app = FirebaseApp.getApps(context).firstOrNull()
                ?: FirebaseApp.initializeApp(context, options)
                ?: return null
            FirebaseAuth.getInstance(app)
        } catch (_: Exception) {
            null
        }
    }
}
