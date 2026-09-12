package com.yourteam.deliveryagent.data.repository

import com.yourteam.deliveryagent.data.model.DeliveryAgent
import com.yourteam.deliveryagent.data.model.OrderStatus
import com.yourteam.deliveryagent.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

/** Stats for the agent's current calendar day. */
data class TodayStats(
    val completedCount: Int,
    val earnings: Double,
)

class AgentRepository(private val supabase: SupabaseClient) {

    /** Returns both [Profile] and [DeliveryAgent] rows for [userId]. */
    suspend fun fetchAgentProfile(userId: String): Result<Pair<Profile, DeliveryAgent>> =
        runCatching {
            val profile = supabase.postgrest["profiles"]
                .select(Columns.ALL) { filter { eq("id", userId) } }
                .decodeSingle<Profile>()

            val agent = supabase.postgrest["delivery_agents"]
                .select(Columns.ALL) { filter { eq("profile_id", userId) } }
                .decodeSingle<DeliveryAgent>()

            profile to agent
        }

    /** Persists the agent's online/offline status. */
    suspend fun toggleOnlineStatus(agentId: String, isOnline: Boolean): Result<Unit> =
        runCatching {
            supabase.postgrest["delivery_agents"]
                .update(mapOf("is_online" to isOnline)) {
                    filter { eq("id", agentId) }
                }
        }

    /** Saves the FCM registration token so the backend can target push messages. */
    suspend fun saveFcmToken(agentId: String, token: String): Result<Unit> = runCatching {
        supabase.postgrest["delivery_agents"]
            .update(mapOf("fcm_token" to token)) {
                filter { eq("id", agentId) }
            }
    }

    /**
     * Returns count + earnings sum for orders delivered today.
     * Note: for MVP we fetch the list and aggregate in-memory to avoid a
     * custom RPC call.  Replace with an RPC/view if the dataset grows.
     */
    suspend fun fetchTodayStats(agentId: String): Result<TodayStats> = runCatching {
        val todayPrefix = java.time.LocalDate.now().toString()   // "YYYY-MM-DD"

        val rows = supabase.postgrest["orders"]
            .select(Columns.raw("delivery_fee, delivered_at")) {
                filter {
                    eq("agent_id", agentId)
                    eq("status", OrderStatus.DELIVERED.name)
                    gte("delivered_at", "${todayPrefix}T00:00:00")
                    lte("delivered_at", "${todayPrefix}T23:59:59")
                }
            }
            .decodeList<DeliveredOrderSummary>()

        TodayStats(
            completedCount = rows.size,
            earnings       = rows.sumOf { it.deliveryFee },
        )
    }

    /** All-time sum of delivery fees earned by [agentId]. */
    suspend fun fetchTotalEarnings(agentId: String): Result<Double> = runCatching {
        val rows = supabase.postgrest["orders"]
            .select(Columns.raw("delivery_fee")) {
                filter {
                    eq("agent_id", agentId)
                    eq("status", OrderStatus.DELIVERED.name)
                }
            }
            .decodeList<DeliveredOrderSummary>()
        rows.sumOf { it.deliveryFee }
    }

    // ── Updates ──────────────────────────────────────────────────────────────

    /**
     * Update a profile row (e.g., full_name).
     */
    suspend fun updateProfile(
        userId: String,
        fullName: String,
    ): Result<Unit> = runCatching {
        supabase.postgrest["profiles"]
            .update(mapOf("full_name" to fullName)) {
                filter { eq("id", userId) }
            }
    }

    /**
     * Update a delivery_agent row (vehicle info and availability).
     */
    suspend fun updateAgent(
        agentId: String,
        vehicleType: String,
        vehicleNumber: String,
        isOnline: Boolean,
    ): Result<Unit> = runCatching {
        supabase.postgrest["delivery_agents"]
            .update(
                mapOf(
                    "vehicle_type" to vehicleType,
                    "vehicle_number" to vehicleNumber,
                    "is_online" to isOnline,
                )
            ) {
                filter { eq("id", agentId) }
            }
    }
}

/** Minimal projection used for earnings aggregation. */
@kotlinx.serialization.Serializable
private data class DeliveredOrderSummary(
    @kotlinx.serialization.SerialName("delivery_fee")
    val deliveryFee: Double,
    @kotlinx.serialization.SerialName("delivered_at")
    val deliveredAt: String? = null,
)
