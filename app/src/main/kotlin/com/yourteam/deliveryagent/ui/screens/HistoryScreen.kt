package com.yourteam.deliveryagent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourteam.deliveryagent.data.model.Order
import com.yourteam.deliveryagent.ui.screens.history.HistoryUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Shows completed deliveries for the current agent.
 * Displays order ID, delivered date/time, and earnings.
 * Tapping a row shows full order details in a modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState = HistoryUiState.Loading,
    onSelectOrder: (orderId: String) -> Unit = {},
    onDismissOrderDetails: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery History") },
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is HistoryUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading delivery history...")
                }
            }

            is HistoryUiState.Success -> {
                if (uiState.orders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "No deliveries completed yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = Arrangement.spacedBy(16.dp),
                    ) {
                        items(uiState.orders) { order ->
                            HistoryOrderRow(
                                order = order,
                                onTap = { onSelectOrder(order.id) },
                            )
                        }
                    }
                }

                // Show order details in modal bottom sheet when selected
                if (uiState.selectedOrderId != null) {
                    val selectedOrder = uiState.orders.find { it.id == uiState.selectedOrderId }
                    if (selectedOrder != null) {
                        ModalBottomSheet(
                            onDismissRequest = onDismissOrderDetails,
                            sheetState = sheetState,
                        ) {
                            HistoryOrderDetailsContent(
                                order = selectedOrder,
                                onDismiss = onDismissOrderDetails,
                            )
                        }
                    }
                }
            }

            is HistoryUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Error loading history",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryOrderRow(
    order: Order,
    onTap: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Order #${order.id.take(8)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDeliveredTime(order.deliveredAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "₹${order.deliveryFee}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun HistoryOrderDetailsContent(
    order: Order,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Delivery Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        Divider()

        // Order ID and status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Order ID",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = order.id,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Delivered ✓",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Divider()

        // Pickup location
        Column {
            Text(
                text = "Pickup Location",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = order.shopName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = order.pickupAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Divider()

        // Drop location
        Column {
            Text(
                text = "Drop Location",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = order.customerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = order.dropAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Divider()

        // Delivered timestamp
        Column {
            Text(
                text = "Delivered",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatDeliveredTime(order.deliveredAt),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Divider()

        // Summary info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            InfoColumn(
                label = "Distance",
                value = "${order.distanceKm ?: 0.0} km",
            )
            InfoColumn(
                label = "Earnings",
                value = "₹${order.deliveryFee}",
            )
        }

        Spacer(Modifier.height(16.dp))

        // Close button
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Close")
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Format ISO 8601 timestamp to readable date/time string.
 * Example: "2024-09-12T14:30:00Z" → "Sep 12, 2:30 PM"
 */
private fun formatDeliveredTime(isoTimestamp: String?): String {
    if (isoTimestamp == null) return "Unknown"
    return try {
        val instant = Instant.parse(isoTimestamp)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MMM dd, h:mm a").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoTimestamp
    }
}
