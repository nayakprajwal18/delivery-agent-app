package com.yourteam.deliveryagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourteam.deliveryagent.core.DistanceUtils
import com.yourteam.deliveryagent.data.model.Order
import com.yourteam.deliveryagent.ui.screens.availabledeliveries.AvailableDeliveriesUiEvent
import com.yourteam.deliveryagent.ui.screens.availabledeliveries.AvailableDeliveriesUiState
import com.yourteam.deliveryagent.ui.theme.DeliveryAgentTheme
import kotlinx.coroutines.launch

/**
 * Available Deliveries screen — shows READY_FOR_PICKUP orders available for pickup.
 * Each card shows shop, drop location, distance, earnings, and "Accept" button.
 * Accepts update in real-time via Supabase Realtime.
 * Concurrent accept is protected: only succeeds if order is still unassigned.
 */
@Composable
fun AvailableDeliveriesScreen(
    uiState: AvailableDeliveriesUiState,
    onNavigateToDeliveryDetails: (orderId: String) -> Unit = {},
    onAcceptOrder: (orderId: String) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Show snackbar on error or concurrent accept failure
    LaunchedEffect(uiState) {
        when (uiState) {
            is AvailableDeliveriesUiState.Success -> {
                if (uiState.lastAcceptError != null) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message  = uiState.lastAcceptError,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }

            is AvailableDeliveriesUiState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message  = uiState.message,
                        duration = SnackbarDuration.Long,
                    )
                }
            }

            else -> {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text  = "Available Deliveries",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Real-time available orders ready for pickup",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Content
            when (uiState) {
                is AvailableDeliveriesUiState.Loading -> {
                    LoadingState()
                }

                is AvailableDeliveriesUiState.Success -> {
                    SuccessState(
                        uiState                     = uiState,
                        onNavigateToDeliveryDetails = onNavigateToDeliveryDetails,
                        onAcceptOrder               = onAcceptOrder,
                    )
                }

                is AvailableDeliveriesUiState.Empty -> {
                    EmptyState(onRefresh = onRefresh)
                }

                is AvailableDeliveriesUiState.Error -> {
                    ErrorState(message = uiState.message, onRetry = onRefresh)
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier
                    .align(Alignment.End)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Loading deliveries...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SuccessState(
    uiState: AvailableDeliveriesUiState.Success,
    onNavigateToDeliveryDetails: (orderId: String) -> Unit,
    onAcceptOrder: (orderId: String) -> Unit,
) {
    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(uiState.orders) { order ->
            DeliveryCard(
                order             = order,
                isAccepting       = uiState.acceptingOrderId == order.id,
                onAccept          = { onAcceptOrder(order.id) },
                onTapCard         = { onNavigateToDeliveryDetails(order.id) },
            )
        }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector        = Icons.Filled.LocalShipping,
            contentDescription = "No deliveries",
            modifier           = Modifier.align(Alignment.CenterHorizontally),
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text  = "No deliveries available right now",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text  = "Failed to load deliveries",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

// ── Delivery Card ─────────────────────────────────────────────────────────────

@Composable
fun DeliveryCard(
    order: Order,
    isAccepting: Boolean = false,
    onAccept: () -> Unit = {},
    onTapCard: () -> Unit = {},
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: Order ID + Shop Name
            Column {
                Text(
                    text  = "#${order.id.take(8)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text  = order.shopName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Drop address
            Text(
                text  = order.dropAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Stats row: distance, earnings
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column {
                    Text(
                        text  = "Distance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text  = order.distanceKm?.let { DistanceUtils.formatDistance(it) } ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Column {
                    Text(
                        text  = "Earnings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text  = "₹${order.deliveryFee.toInt()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Action buttons
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick  = onTapCard,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Details")
                }

                Button(
                    onClick  = onAccept,
                    modifier = Modifier.weight(1f),
                    enabled  = !isAccepting,
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeliveryCardPreview() {
    DeliveryAgentTheme {
        DeliveryCard(
            order = Order(
                id              = "ord-123456",
                shopId          = "shop-1",
                customerId      = "cust-1",
                agentId         = null,
                status          = "READY_FOR_PICKUP",
                deliveryFee     = 35.0,
                shopName        = "Ramesh General Store",
                pickupAddress   = "Shop 4, Main Market",
                pickupLat       = 28.7041,
                pickupLng       = 77.1025,
                dropAddress     = "B-12, Shiv Nagar Colony",
                dropLat         = 28.6139,
                dropLng         = 77.2090,
                distanceKm      = 2.4,
                customerName    = "Priya Sharma",
                customerPhone   = "+919876543210",
            ),
        )
    }
}

// Needed for LazyColumn contentPadding
@Composable
private fun PaddingValues(value: Int): androidx.compose.foundation.layout.PaddingValues {
    return androidx.compose.foundation.layout.PaddingValues(value.dp)
}
