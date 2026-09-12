package com.yourteam.deliveryagent.navigation

/**
 * Single source of truth for all navigation route strings.
 *
 * Screens that accept arguments encode them directly in the path segment,
 * e.g. "delivery_details/{orderId}".  Use the helper functions to build
 * concrete routes for NavController.navigate() calls.
 */
object NavRoutes {
    const val LOGIN                = "login"
    const val HOME                 = "home"
    const val AVAILABLE_DELIVERIES = "available_deliveries"
    const val DELIVERY_DETAILS     = "delivery_details/{orderId}"
    const val HISTORY              = "history"
    const val PROFILE              = "profile"

    // ── Argument keys ────────────────────────────────────────────────────────
    const val ARG_ORDER_ID = "orderId"

    // ── Concrete route builders ──────────────────────────────────────────────

    /** Builds the concrete route string for navigating to a specific order. */
    fun deliveryDetails(orderId: String) = "delivery_details/$orderId"
}

/** The four destinations shown in the bottom navigation bar. */
enum class BottomNavDestination(
    val route: String,
    val label: String,
    val iconContentDescription: String,
) {
    HOME(
        route                = NavRoutes.HOME,
        label                = "Home",
        iconContentDescription = "Home",
    ),
    DELIVERIES(
        route                = NavRoutes.AVAILABLE_DELIVERIES,
        label                = "Deliveries",
        iconContentDescription = "Available deliveries",
    ),
    HISTORY(
        route                = NavRoutes.HISTORY,
        label                = "History",
        iconContentDescription = "Delivery history",
    ),
    PROFILE(
        route                = NavRoutes.PROFILE,
        label                = "Profile",
        iconContentDescription = "Agent profile",
    ),
}
