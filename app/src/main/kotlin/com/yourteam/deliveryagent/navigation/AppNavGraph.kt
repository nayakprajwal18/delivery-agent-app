package com.yourteam.deliveryagent.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourteam.deliveryagent.ui.screens.AvailableDeliveriesScreen
import com.yourteam.deliveryagent.ui.screens.DeliveryDetailsScreen
import com.yourteam.deliveryagent.ui.screens.HistoryScreen
import com.yourteam.deliveryagent.ui.screens.HomeScreen
import com.yourteam.deliveryagent.ui.screens.LoginScreen
import com.yourteam.deliveryagent.ui.screens.ProfileScreen

/**
 * Root composable that owns the [NavHostController] and the bottom navigation bar.
 *
 * [startDestination] is determined in [MainActivity] by checking for an active session:
 *   - Active session → NavRoutes.HOME
 *   - No session     → NavRoutes.LOGIN
 */
@Composable
fun AppNavGraph(
    startDestination: String = NavRoutes.LOGIN,
    navController: NavHostController = rememberNavController(),
) {
    // Routes on which the bottom nav bar should be visible.
    val bottomNavRoutes = setOf(
        NavRoutes.HOME,
        NavRoutes.AVAILABLE_DELIVERIES,
        NavRoutes.HISTORY,
        NavRoutes.PROFILE,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding),
        ) {

            // ── Auth ─────────────────────────────────────────────────────────
            composable(NavRoutes.LOGIN) {
                com.yourteam.deliveryagent.ui.screens.login.LoginScreenWrapper(
                    onLoginSuccess = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // ── Main screens (bottom nav) ────────────────────────────────────
            composable(NavRoutes.HOME) {
                com.yourteam.deliveryagent.ui.screens.dashboard.DashboardScreenWrapper(
                    onNavigateToAvailableDeliveries = {
                        navController.navigate(NavRoutes.AVAILABLE_DELIVERIES)
                    },
                    onNavigateToDeliveryDetails = { orderId ->
                        navController.navigate(NavRoutes.deliveryDetails(orderId))
                    },
                )
            }

            composable(NavRoutes.AVAILABLE_DELIVERIES) {
                com.yourteam.deliveryagent.ui.screens.availabledeliveries.AvailableDeliveriesScreenWrapper(
                    onNavigateToDeliveryDetails = { orderId ->
                        navController.navigate(NavRoutes.deliveryDetails(orderId))
                    }
                )
            }

            composable(NavRoutes.HISTORY) {
                HistoryScreen()
            }

            composable(NavRoutes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Delivery details (no bottom bar) ─────────────────────────────
            composable(
                route     = NavRoutes.DELIVERY_DETAILS,
                arguments = listOf(
                    navArgument(NavRoutes.ARG_ORDER_ID) { type = NavType.StringType }
                ),
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString(NavRoutes.ARG_ORDER_ID) ?: ""
                com.yourteam.deliveryagent.ui.screens.deliverydetails.DeliveryDetailsScreenWrapper(
                    orderId = orderId,
                    onNavigateBack = { navController.popBackStack() },
                    onDeliveryCompleted = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.HOME) { inclusive = false }
                        }
                    },
                )
            }
        }
    }
}

// ── Bottom navigation bar ─────────────────────────────────────────────────────

private val bottomNavItems = listOf(
    Triple(BottomNavDestination.HOME,       Icons.Filled.Home,            "Home"),
    Triple(BottomNavDestination.DELIVERIES, Icons.Filled.DeliveryDining,  "Deliveries"),
    Triple(BottomNavDestination.HISTORY,    Icons.Filled.History,         "History"),
    Triple(BottomNavDestination.PROFILE,    Icons.Filled.Person,          "Profile"),
)

@Composable
private fun AppBottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { (destination, icon, label) ->
            val selected = currentDestination?.hierarchy
                ?.any { it.route == destination.route } == true

            NavigationBarItem(
                selected = selected,
                onClick  = {
                    navController.navigate(destination.route) {
                        // Pop up to the start destination to avoid stacking the same screen.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}
