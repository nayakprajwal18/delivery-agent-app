package com.yourteam.deliveryagent.ui.screens

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourteam.deliveryagent.core.MapsNavigationUtils
import com.yourteam.deliveryagent.data.model.Order
import com.yourteam.deliveryagent.data.model.OrderItem
import com.yourteam.deliveryagent.data.model.OrderStatus
import com.yourteam.deliveryagent.ui.screens.deliverydetails.DeliveryDetailsUiEvent
import com.yourteam.deliveryagent.ui.screens.deliverydetails.DeliveryDetailsUiState
import com.yourteam.deliveryagent.ui.theme.DeliveryAgentTheme
import kotlinx.coroutines.launch

/**
 * Shows full details of an assigned delivery with pickup/delivery confirmation.
 *
 * Features:
 * - Display order items, pickup/drop locations, distance, earnings
 * - Navigate to pickup/drop location via Google Maps Intent
 * - Call customer via phone dialer
 * - Confirm pickup (status → PICKED_UP) when status is AGENT_ASSIGNED
 * - Confirm delivery (status → DELIVERED) when status is PICKED_UP or OUT_FOR_DELIVERY
 * - Real-time updates via Realtime subscription
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailsScreen(
    orderId:            String  = "",
    uiState:            DeliveryDetailsUiState = DeliveryDetailsUiState.Loading,
    onNavigateBack:     () -> Unit = {},
    onDeliveryCompleted: () -> Unit = {},
    onConfirmPickup:    () -> Unit = {},
    onConfirmDelivery:  () -> Unit = {},
    onDismissError:     () -> Unit = {},
    onUiEvent:          (DeliveryDetailsUiEvent) -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle navigation events
    LaunchedEffect(Unit) {
        // Events are handled via callbacks; UI events could trigger nav changes
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery #$orderId") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (uiState) {
            is DeliveryDetailsUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading delivery details...")
                }
            }

            is DeliveryDetailsUiState.Success -> {
                val order = uiState.order

                // Show error snackbar if present
                LaunchedEffect(uiState.confirmationError) {
                    uiState.confirmationError?.let {
                        scope.launch {
                            snackbarHostState.showSnackbar(it)
                        }
                        onDismissError()
                    }
                }

                DeliveryDetailsContent(
                    order = order,
                    isConfirmingPickup = uiState.isConfirmingPickup,
                    isConfirmingDelivery = uiState.isConfirmingDelivery,
                    onConfirmPickup = onConfirmPickup,
                    onConfirmDelivery = onConfirmDelivery,
                    onCallCustomer = { MapsNavigationUtils.openPhoneDialer(context, order.customerPhone) },
                    onNavigatePickup = { MapsNavigationUtils.openMapsNavigation(context, order.pickupLat ?: 0.0, order.pickupLng ?: 0.0, order.shopName) },
                    onNavigateDrop = { MapsNavigationUtils.openMapsNavigation(context, order.dropLat ?: 0.0, order.dropLng ?: 0.0, order.customerName) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            is DeliveryDetailsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Error loading delivery",
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
                    OutlinedButton(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryDetailsContent(
    order: Order,
    isConfirmingPickup: Boolean,
    isConfirmingDelivery: Boolean,
    onConfirmPickup: () -> Unit,
    onConfirmDelivery: () -> Unit,
    onCallCustomer: () -> Unit,
    onNavigatePickup: () -> Unit,
    onNavigateDrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val orderStatus = OrderStatus.from(order.status)
    val canConfirmPickup = orderStatus == OrderStatus.AGENT_ASSIGNED
    val canConfirmDelivery = orderStatus in listOf(OrderStatus.PICKED_UP, OrderStatus.OUT_FOR_DELIVERY)

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Order items ──────────────────────────────────────────────────
        item {
            SectionHeader("Order Items")
            if (order.items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    order.items.forEach { item ->
                        ItemRow(item)
                    }
                }
            } else {
                Text(
                    text = "No items loaded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Pickup location ──────────────────────────────────────────────
        item {
            SectionHeader("Pickup Location")
            LocationCard(
                title = order.shopName,
                subtitle = order.pickupAddress,
                onMapTap = onNavigatePickup,
                onCallTap = null, // No phone for pickup
            )
        }

        // ── Drop location ────────────────────────────────────────────────
        item {
            SectionHeader("Drop Location")
            LocationCard(
                title = order.customerName,
                subtitle = order.dropAddress,
                onMapTap = onNavigateDrop,
                onCallTap = { if (order.customerPhone.isNotEmpty()) onCallCustomer() },
            )
        }

        // ── Summary info (distance, earnings, status) ────────────────────
        item {
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
                InfoChip(
                    label = "Distance",
                    value = "${order.distanceKm ?: 0.0} km",
                )
                InfoChip(
                    label = "Earnings",
                    value = "₹${order.deliveryFee}",
                )
                InfoChip(
                    label = "Status",
                    value = orderStatus.name.replace("_", " "),
                )
            }
        }

        // ── Action buttons ───────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (canConfirmPickup) {
                    Button(
                        onClick = onConfirmPickup,
                        enabled = !isConfirmingPickup,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isConfirmingPickup) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .then(Modifier.height(20.dp))
                                    .then(Modifier.padding(end = 8.dp)),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(if (isConfirmingPickup) "Confirming..." else "Confirm Pickup")
                    }
                }

                if (canConfirmDelivery) {
                    Button(
                        onClick = onConfirmDelivery,
                        enabled = !isConfirmingDelivery,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isConfirmingDelivery) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .then(Modifier.height(20.dp))
                                    .then(Modifier.padding(end = 8.dp)),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(if (isConfirmingDelivery) "Confirming..." else "Confirm Delivery")
                    }
                }

                if (!canConfirmPickup && !canConfirmDelivery) {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Delivery Completed ✓")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun ItemRow(item: OrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "₹${item.pricePerUnit}/${item.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${item.quantity} ${item.unit}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LocationCard(
    title: String,
    subtitle: String,
    onMapTap: () -> Unit,
    onCallTap: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                IconButton(onClick = onMapTap, modifier = Modifier.padding(0.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (onCallTap != null) {
                    IconButton(onClick = onCallTap, modifier = Modifier.padding(0.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeliveryDetailsScreenPreview() {
    DeliveryAgentTheme {
        val sampleOrder = Order(
            id = "ORD-001",
            shopId = "SHOP-001",
            customerId = "CUST-001",
            agentId = "AGENT-001",
            status = OrderStatus.PICKED_UP.name,
            deliveryFee = 50.0,
            shopName = "Fresh Mart",
            pickupAddress = "123 Market St",
            pickupLat = 28.7041,
            pickupLng = 77.1025,
            dropAddress = "456 Home St, Apt 5",
            dropLat = 28.6139,
            dropLng = 77.2090,
            customerName = "John Doe",
            customerPhone = "+919876543210",
            distanceKm = 2.4,
            items = listOf(
                OrderItem(
                    id = "ITEM-001",
                    orderId = "ORD-001",
                    productId = "PROD-001",
                    productName = "Milk (1L)",
                    quantity = 2,
                    unit = "pack",
                    pricePerUnit = 60.0,
                ),
                OrderItem(
                    id = "ITEM-002",
                    orderId = "ORD-001",
                    productId = "PROD-002",
                    productName = "Bread",
                    quantity = 1,
                    unit = "loaf",
                    pricePerUnit = 40.0,
                ),
            ),
        )

        DeliveryDetailsScreen(
            orderId = "ORD-001",
            uiState = DeliveryDetailsUiState.Success(order = sampleOrder),
        )
    }
}
