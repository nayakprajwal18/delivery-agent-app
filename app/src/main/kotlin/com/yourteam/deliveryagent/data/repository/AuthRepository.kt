package com.yourteam.deliveryagent.data.repository

import com.yourteam.deliveryagent.data.model.DeliveryAgent
import com.yourteam.deliveryagent.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuthRepository(private val supabase: SupabaseClient) {

    /** Returns true if there is a non-expired local session. */
    fun hasActiveSession(): Boolean =
        supabase.auth.currentSessionOrNull() != null

    /** Returns the UUID of the currently authenticated user, or null. */
    fun currentUserId(): String? =
        supabase.auth.currentUserOrNull()?.id

    /** Returns the phone number of the currently authenticated user, or null. */
    fun currentUserPhone(): String? =
        supabase.auth.currentUserOrNull()?.phone

    /**
     * Step 1 of OTP login — sends an SMS OTP to [phone].
     * [phone] should be in E.164 format, e.g. "+919876543210".
     */
    suspend fun sendOtp(phone: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(Phone) {
            this.phone = phone
        }
    }

    /**
     * Step 2 of OTP login — verifies the [otp] the user received.
     * On success the session is persisted automatically by the SDK.
     */
    suspend fun verifyOtp(phone: String, otp: String): Result<Unit> = runCatching {
        supabase.auth.verifyPhoneOtp(
            type  = io.github.jan.supabase.auth.OtpType.Phone.SMS,
            phone = phone,
            token = otp,
        )
    }

    /**
     * After successful OTP verification, ensure a delivery_agent profile exists.
     *
     * Steps:
     * 1. Check if a `profiles` row exists for the current user (by auth UUID).
     * 2. If not, create one with role = DELIVERY_AGENT.
     * 3. Check if a `delivery_agents` row exists (matching profile_id).
     * 4. If not, create one with is_online = OFFLINE.
     *
     * This is idempotent — calling it multiple times is safe.
     */
    suspend fun ensureDeliveryAgentProfile(): Result<Unit> = runCatching {
        val userId = currentUserId() ?: throw IllegalStateException("No authenticated user")
        val phone  = currentUserPhone() ?: throw IllegalStateException("No phone for authenticated user")

        // 1. Check for existing profile
        val existingProfile = try {
            supabase.postgrest["profiles"]
                .select(Columns.ALL) { filter { eq("id", userId) } }
                .decodeSingle<Profile>()
        } catch (e: Exception) {
            null  // No profile found
        }

        // 2. Create profile if missing
        if (existingProfile == null) {
            val newProfile = Profile(
                id        = userId,
                fullName  = phone,  // placeholder; user can edit later
                phone     = phone,
            )
            supabase.postgrest["profiles"]
                .insert(newProfile)
        }

        // 3. Check for existing delivery_agent row
        val existingAgent = try {
            supabase.postgrest["delivery_agents"]
                .select(Columns.ALL) { filter { eq("profile_id", userId) } }
                .decodeSingle<DeliveryAgent>()
        } catch (e: Exception) {
            null  // No agent row found
        }

        // 4. Create delivery_agent row if missing
        if (existingAgent == null) {
            val newAgent = mapOf(
                "profile_id"     to userId,
                "vehicle_type"   to "Unknown",  // placeholder
                "vehicle_number" to "Pending",   // placeholder
                "is_online"      to false,
                "fcm_token"      to null,
                "total_earnings" to 0.0,
            )
            supabase.postgrest["delivery_agents"]
                .insert(newAgent)
        }
    }

    /** Clears the local session. */
    suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }
}
