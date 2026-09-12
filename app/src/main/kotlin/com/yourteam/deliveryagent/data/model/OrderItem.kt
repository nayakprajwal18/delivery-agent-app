package com.yourteam.deliveryagent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `order_items` table.
 *
 * Columns: id, order_id, product_id, product_name, quantity, unit, price_per_unit
 */
@Serializable
data class OrderItem(
    val id: String,

    @SerialName("order_id")
    val orderId: String,

    @SerialName("product_id")
    val productId: String,

    @SerialName("product_name")
    val productName: String,

    val quantity: Int,

    /** Unit of measure, e.g. "kg", "L", "pcs" */
    val unit: String,

    @SerialName("price_per_unit")
    val pricePerUnit: Double,
)
