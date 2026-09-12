package com.yourteam.deliveryagent.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
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
 * TODO (Phase 8): persist token, handle foreground messages as in-app banners.
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
        val container  = (applicationContext as DeliveryAgentApp).container
        val agentId    = container.authRepository.currentUserId() ?: return

        serviceScope.launch {
            container.agentRepository.saveFcmToken(agentId, token)
        }
    }

    /**
     * Called when a message arrives while the app is in the foreground,
     * or for data-only messages regardless of foreground/background state.
     *
     * Expected data payload keys:
     *   type     — "new_delivery" | "order_assigned"
     *   order_id — UUID string (present for "order_assigned")
     *   title    — notification title
     *   body     — notification body
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type    = message.data["type"]    ?: return
        val orderId = message.data["order_id"]
        val title   = message.data["title"]   ?: message.notification?.title ?: "Delivery"
        val body    = message.data["body"]    ?: message.notification?.body  ?: ""

        // Build the deep-link intent based on notification type.
        val deepLinkRoute = when (type) {
            "new_delivery"   -> NavRoutes.AVAILABLE_DELIVERIES
            "order_assigned" -> if (orderId != null) NavRoutes.deliveryDetails(orderId) else NavRoutes.HOME
            else             -> NavRoutes.HOME
        }

        showNotification(
            notificationId = System.currentTimeMillis().toInt(),
            title          = title,
            body           = body,
            deepLinkRoute  = deepLinkRoute,
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun showNotification(
        notificationId: Int,
        title:          String,
        body:           String,
        deepLinkRoute:  String,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEPLINK_ROUTE, deepLinkRoute)
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    companion object {
        /** Intent extra key for the navigation route to open on tap. */
        const val EXTRA_DEEPLINK_ROUTE = "deeplink_route"
    }
}
