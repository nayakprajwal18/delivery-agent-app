package com.yourteam.deliveryagent.core

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utility to retrieve the current FCM registration token asynchronously.
 *
 * On first launch, FirebaseMessaging generates a new token. On subsequent launches,
 * it returns the cached token. The token can change if the user uninstalls and
 * reinstalls, or if Firebase deems the token invalid.
 */
object FCMTokenManager {
    private const val TAG = "FCMTokenManager"

    /**
     * Retrieves the current FCM token.
     *
     * Returns: the token string, or null if unable to retrieve (e.g., Firebase not configured)
     *
     * Suspends the coroutine until the token is available.
     */
    suspend fun getToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token retrieved: ${token.take(20)}...")
                continuation.resume(token)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to retrieve FCM token", exception)
                continuation.resume(null)
            }
            .addOnCanceledListener {
                Log.w(TAG, "FCM token retrieval cancelled")
                continuation.resume(null)
            }
    }
}
