package com.yourteam.deliveryagent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `orders` table (with selected joined fields from `shops` and `profiles`
 * that the backend view / RPC returns alongside each order row).
 *
 * Core columns: id, shop_id, customer_id, agent_id, status, delivery_fee,
 *               drop_address, drop_lat, drop_lng, distance_km,
 *               customer_name, customer_phone,
 *               created_at, picked_up_at, delivered_at
 *
 * Joined / computed:
 *   shop_name   — from shops.name
 *   pickup_lat  — from shops.latitude
 *   pickup_lng  — from shops.longitude
 *   pickup_address — from shops.address
 *   items       — populated separately from order_items
 */
@Serializable
data class Order(
    val id: String,

    @SerialName("shop_id")
    val shopId: String,

    @SerialName("customer_id")
    val customerId: String,

    @SerialName("agent_id")
    val agentId: String? = null,

    /** See [OrderStatus] for valid values. */
    val status: String,

    @SerialName("delivery_fee")
    val deliveryFee: Double,

    // ── Pickup (from shops join) ─────────────────────────────────────────
    @SerialName("shop_name")
    val shopName: String = "",

    @SerialName("pickup_address")
    val pickupAddress: String = "",

    @SerialName("pickup_lat")
    val pickupLat: Double? = null,

    @SerialName("pickup_lng")
    val pickupLng: Double? = null,

    // ── Drop ─────────────────────────────────────────────────────────────
    @SerialName("drop_address")
    val dropAddress: String,

    @SerialName("drop_lat")
    val dropLat: Double? = null,

    @SerialName("drop_lng")
    val dropLng: Double? = null,

    // ── Customer (from profiles join) ────────────────────────────────────
    @SerialName("customer_name")
    val customerName: String = "",

    @SerialName("customer_phone")
    val customerPhone: String = "",

    // ── Computed ─────────────────────────────────────────────────────────
    @SerialName("distance_km")
    val distanceKm: Double? = null,

    // ── Timestamps ───────────────────────────────────────────────────────
    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("picked_up_at")
    val pickedUpAt: String? = null,

    @SerialName("delivered_at")
    val deliveredAt: String? = null,

    /** Populated via a separate query to order_items — not a DB column. */
    val items: List<OrderItem> = emptyList(),
)

/** Mirrors the status values used across all three apps. */
enum class OrderStatus {
    PLACED,
    CONFIRMED,
    READY_FOR_PICKUP,
    AGENT_ASSIGNED,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    companion object {
        fun from(value: String): OrderStatus =
            entries.firstOrNull { it.name == value } ?: PLACED
    }
}
