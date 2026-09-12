package com.yourteam.deliveryagent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `notifications` table.
 *
 * Columns: id, user_id, type, title, body, order_id, is_read, created_at
 */
@Serializable
data class Notification(
    val id: String,

    @SerialName("user_id")
    val userId: String,

    /** e.g. "new_delivery", "order_assigned", "order_status_update" */
    val type: String,

    val title: String,

    val body: String,

    @SerialName("order_id")
    val orderId: String? = null,

    @SerialName("is_read")
    val isRead: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null,
)
