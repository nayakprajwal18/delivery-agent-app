package com.yourteam.deliveryagent.core

import android.content.Context
import com.yourteam.deliveryagent.data.repository.AgentRepository
import com.yourteam.deliveryagent.data.repository.AuthRepository
import com.yourteam.deliveryagent.data.repository.OrderRepository

/**
 * Manual dependency-injection container.
 *
 * Created once in [DeliveryAgentApp] and stored on the Application instance.
 * ViewModels access it via (LocalContext.current.applicationContext as DeliveryAgentApp).container
 *
 * No Hilt/Dagger for MVP — swap in later if needed.
 */
class AppContainer(context: Context) {

    /** Single Supabase client shared by all repositories. */
    private val supabase = SupabaseClientProvider.client

    val authRepository  : AuthRepository  = AuthRepository(supabase)
    val orderRepository : OrderRepository = OrderRepository(supabase)
    val agentRepository : AgentRepository = AgentRepository(supabase)
}
