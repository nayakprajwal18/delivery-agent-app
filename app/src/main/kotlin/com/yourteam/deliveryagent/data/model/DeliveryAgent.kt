package com.yourteam.deliveryagent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `delivery_agents` table.
 *
 * Columns: id, profile_id, vehicle_type, vehicle_number, is_online,
 *          fcm_token, total_earnings, created_at, updated_at
 */
@Serializable
data class DeliveryAgent(
    val id: String,

    @SerialName("profile_id")
    val profileId: String,

    @SerialName("vehicle_type")
    val vehicleType: String,

    @SerialName("vehicle_number")
    val vehicleNumber: String,

    @SerialName("is_online")
    val isOnline: Boolean = false,

    @SerialName("fcm_token")
    val fcmToken: String? = null,

    @SerialName("total_earnings")
    val totalEarnings: Double = 0.0,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,
)
