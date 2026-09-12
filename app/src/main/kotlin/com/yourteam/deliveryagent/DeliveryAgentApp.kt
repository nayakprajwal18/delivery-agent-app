package com.yourteam.deliveryagent

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.yourteam.deliveryagent.core.AppContainer

class DeliveryAgentApp : Application() {

    /** Accessed by ViewModels: (context.applicationContext as DeliveryAgentApp).container */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
    }

    /**
     * FCM requires a notification channel on API 26+.
     * Created here so it exists before any message can arrive.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Deliveries",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "New delivery and order status notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "deliveries"
    }
}
