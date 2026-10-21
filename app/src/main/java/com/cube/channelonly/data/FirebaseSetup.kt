package com.cube.channelonly.data

import android.content.Context
import com.cube.channelonly.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth

object FirebaseSetup {
    fun isConfigured(): Boolean =
        BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

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
