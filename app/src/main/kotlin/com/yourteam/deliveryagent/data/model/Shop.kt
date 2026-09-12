package com.yourteam.deliveryagent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `shops` table.
 *
 * Columns: id, owner_id, name, address, latitude, longitude, created_at
 */
@Serializable
data class Shop(
    val id: String,

    @SerialName("owner_id")
    val ownerId: String,

    val name: String,

    val address: String,

    val latitude: Double? = null,

    val longitude: Double? = null,

    @SerialName("created_at")
    val createdAt: String? = null,
)
