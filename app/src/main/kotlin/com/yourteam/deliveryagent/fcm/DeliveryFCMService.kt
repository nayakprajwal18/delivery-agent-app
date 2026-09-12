package com.yourteam.deliveryagent.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourteam.deliveryagent.DeliveryAgentApp
import com.yourteam.deliveryagent.MainActivity
import com.yourteam.deliveryagent.navigation.NavRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles incoming FCM messages and token refresh.
 *
 * Registered in AndroidManifest.xml — do not instantiate directly.
 *
 * Scenarios handled:
 * 1. Token refresh: onNewToken() saves the token to Supabase.
 * 2. Foreground messages: onMessageReceived() is called while app is in foreground.
 * 3. Background/terminated: System shows notification automatically; user tap opens MainActivity
 *    with extras, which AppNavGraph processes via initialDeepLinkRoute.
 *
 * Expected FCM data payload keys:
 *   type     — "NEW_DELIVERY_AVAILABLE" | "ORDER_ASSIGNED_CONFIRMATION"
 *   order_id — UUID string (only for ORDER_ASSIGNED_CONFIRMATION)
 *   title    — notification title
 *   body     — notification body
 */
class DeliveryFCMService : FirebaseMessagingService() {

    // Service-scoped coroutine scope; cancelled when the service is destroyed.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when a new FCM registration token is generated (first launch or token refresh).
     * We save it to Supabase so the backend can send targeted push messages to this device.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: ${token.take(20)}...")
        
        val container = (applicationContext as DeliveryAgentApp).container
        val agentId = container.authRepository.currentUserId()
        
        if (agentId != null) {
            serviceScope.launch {
                container.agentRepository.saveFcmToken(agentId, token)
                    .onSuccess {
                        Log.d(TAG, "FCM token saved to Supabase")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to save FCM token", error)
                    }
            }
        } else {
            Log.w(TAG, "No current user; FCM token not saved (will be saved on next login)")
        }
    }

    /**
     * Called when a message arrives while the app is in the foreground,
     * or for data-only messages regardless of foreground/background state.
     *
     * Scenarios:
     * - App in foreground: This is called; we show a notification
     * - App in background/terminated: System shows the notification automatically from
     *   the RemoteMessage.notification field. When user taps it, MainActivity opens
     *   with extras, and AppNavGraph handles the deeplink.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received: type=${message.data["type"]}, from=${message.from}")

        val type = message.data["type"] ?: run {
            Log.w(TAG, "Received message without 'type' key; ignoring")
            return
        }

        val orderId = message.data["order_id"]
        val title = message.data["title"] ?: message.notification?.title
        val body = message.data["body"] ?: message.notification?.body

        if (title.isNullOrEmpty() || body.isNullOrEmpty()) {
            Log.w(TAG, "Received message without title or body; ignoring")
            return
        }

        // Map notification type to deep-link route
        val deepLinkRoute = when (type) {
            "NEW_DELIVERY_AVAILABLE" -> {
                Log.d(TAG, "New delivery available notification")
                NavRoutes.AVAILABLE_DELIVERIES
            }
            "ORDER_ASSIGNED_CONFIRMATION" -> {
                if (orderId != null) {
                    Log.d(TAG, "Order assigned: $orderId")
                    NavRoutes.deliveryDetails(orderId)
                } else {
                    Log.w(TAG, "ORDER_ASSIGNED_CONFIRMATION without order_id; defaulting to HOME")
                    NavRoutes.HOME
                }
            }
            else -> {
                Log.w(TAG, "Unknown notification type: $type; defaulting to HOME")
                NavRoutes.HOME
            }
        }

        showNotification(
            notificationId = System.currentTimeMillis().toInt(),
            title = title,
            body = body,
            deepLinkRoute = deepLinkRoute,
            type = type,
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Shows a notification with the given title, body, and deep-link route.
     *
     * The notification is tappable; tapping it opens MainActivity with the
     * EXTRA_DEEPLINK_ROUTE intent extra, which AppNavGraph processes.
     */
    private fun showNotification(
        notificationId: Int,
        title: String,
        body: String,
        deepLinkRoute: String,
        type: String,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEPLINK_ROUTE, deepLinkRoute)
            // Optional: add type for additional context
            putExtra(EXTRA_NOTIFICATION_TYPE, type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, DeliveryAgentApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)  // TODO: replace with app icon
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))  // Allow longer body text
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
        
        Log.d(TAG, "Notification shown: id=$notificationId, title=$title")
    }

    companion object {
        private const val TAG = "DeliveryFCMService"
        
        /** Intent extra key for the navigation route to open on tap. */
        const val EXTRA_DEEPLINK_ROUTE = "deeplink_route"
        
        /** Intent extra key for the notification type (optional, for debugging). */
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    }
}
