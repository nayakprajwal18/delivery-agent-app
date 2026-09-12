package com.yourteam.deliveryagent.core

import com.yourteam.deliveryagent.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Singleton that creates and holds the single [SupabaseClient] for the app.
 *
 * Usage:
 *   val client = SupabaseClientProvider.client
 *
 * URL and anon key are injected at build time via BuildConfig fields that read
 * from local.properties — they are never hardcoded here.
 */
object SupabaseClientProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl    = BuildConfig.SUPABASE_URL,
            supabaseKey    = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
