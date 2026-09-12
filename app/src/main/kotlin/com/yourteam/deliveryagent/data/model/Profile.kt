package com.yourteam.deliveryagent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maps to the `profiles` table.
 *
 * Columns: id, full_name, phone, created_at, updated_at
 */
@Serializable
data class Profile(
    val id: String,

    @SerialName("full_name")
    val fullName: String,

    val phone: String,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,
)
