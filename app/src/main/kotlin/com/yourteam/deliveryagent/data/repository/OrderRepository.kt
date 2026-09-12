package com.yourteam.deliveryagent.data.repository

import com.yourteam.deliveryagent.data.model.Order
import com.yourteam.deliveryagent.data.model.OrderItem
import com.yourteam.deliveryagent.data.model.OrderStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

class OrderRepository(private val supabase: SupabaseClient) {

    // ── Available orders ─────────────────────────────────────────────────────

    /** All READY_FOR_PICKUP orders that have not yet been assigned to any agent. */
    suspend fun fetchAvailableOrders(): Result<List<Order>> = runCatching {
        supabase.postgrest["orders"]
            .select(Columns.ALL) {
                filter {
                    eq("status", OrderStatus.READY_FOR_PICKUP.name)
                    isNull("agent_id")
                }
            }
            .decodeList<Order>()
    }

    // ── Active order for this agent ──────────────────────────────────────────

    /**
     * Returns the single order currently assigned to [agentId] that is not yet
     * delivered, or null if the agent has no active order.
     */
    suspend fun fetchActiveOrder(agentId: String): Result<Order?> = runCatching {
        val activeStatuses = listOf(
            OrderStatus.AGENT_ASSIGNED.name,
            OrderStatus.PICKED_UP.name,
            OrderStatus.OUT_FOR_DELIVERY.name,
        )
        supabase.postgrest["orders"]
            .select(Columns.ALL) {
                filter {
                    eq("agent_id", agentId)
                    isIn("status", activeStatuses)
                }
            }
            .decodeList<Order>()
            .firstOrNull()
    }

    /** Fetches a single order by its primary key. */
    suspend fun fetchOrderById(orderId: String): Result<Order> = runCatching {
        supabase.postgrest["orders"]
            .select(Columns.ALL) {
                filter { eq("id", orderId) }
            }
            .decodeSingle<Order>()
    }

    /** All DELIVERED orders for [agentId], most recent first. */
    suspend fun fetchCompletedOrders(agentId: String): Result<List<Order>> = runCatching {
        supabase.postgrest["orders"]
            .select(Columns.ALL) {
                filter {
                    eq("agent_id", agentId)
                    eq("status", OrderStatus.DELIVERED.name)
                }
                order("delivered_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<Order>()
    }

    /** Fetches all items belonging to [orderId]. */
    suspend fun fetchOrderItems(orderId: String): Result<List<OrderItem>> = runCatching {
        supabase.postgrest["order_items"]
            .select(Columns.ALL) {
                filter { eq("order_id", orderId) }
            }
            .decodeList<OrderItem>()
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    /**
     * Atomically accepts an order by setting agent_id only if it is still NULL.
     * This prevents two agents from accepting the same order simultaneously.
     */
    suspend fun acceptOrder(orderId: String, agentId: String): Result<Unit> = runCatching {
        supabase.postgrest["orders"]
            .update(
                mapOf(
                    "agent_id" to agentId,
                    "status"   to OrderStatus.AGENT_ASSIGNED.name,
                )
            ) {
                filter {
                    eq("id", orderId)
                    isNull("agent_id")      // guard: only claim unassigned orders
                }
            }
    }

    /**
     * Updates [orderId] to [newStatus].
     * Timestamp columns (picked_up_at / delivered_at) are set server-side via
     * a Postgres trigger, so we only update `status` here.
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> =
        runCatching {
            supabase.postgrest["orders"]
                .update(mapOf("status" to newStatus.name)) {
                    filter { eq("id", orderId) }
                }
        }
}
