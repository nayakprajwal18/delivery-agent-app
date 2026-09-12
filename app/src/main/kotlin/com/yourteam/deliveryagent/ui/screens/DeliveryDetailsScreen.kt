package com.yourteam.deliveryagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourteam.deliveryagent.ui.theme.DeliveryAgentTheme

/**
 * Shows full details of an assigned delivery.
 * TODO (Phase 5): wire to DeliveryDetailsViewModel.
 * TODO (Phase 6): hook map buttons to Google Maps Intent.
 * TODO (Phase 9): add ConfirmationBottomSheet for action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailsScreen(
    orderId:            String  = "",
    onNavigateBack:     () -> Unit = {},
    onDeliveryCompleted: () -> Unit = {},
) {
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
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // ── Order items (placeholder) ─────────────────────────────
                SectionHeader("Order Items")
                Text(
                    "Items will appear here",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

                // ── Pickup location ───────────────────────────────────────
                SectionHeader("Pickup Location")
                LocationRow(
                    label     = "Shop Name · Shop Address",
                    onMapTap  = { /* TODO: openGoogleMaps(pickupLat, pickupLng) */ },
                )

                // ── Drop location ─────────────────────────────────────────
                SectionHeader("Drop Location")
                LocationRow(
                    label    = "Customer Name · Drop Address",
                    onMapTap = { /* TODO: openGoogleMaps(dropLat, dropLng) */ },
                )

                // ── Earnings / distance chips ─────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("📍 — km", style = MaterialTheme.typography.labelLarge)
                    Text("💰 ₹—",   style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.weight(1f))

                // ── Primary action button ─────────────────────────────────
                // Text changes based on order status; wired in Phase 9.
                Button(
                    onClick  = { /* TODO: show ConfirmationBottomSheet */ },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Confirm Pickup")   // or "Confirm Delivery" based on status
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun LocationRow(
    label:   String,
    onMapTap: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text     = label,
            modifier = Modifier.weight(1f),
            style    = MaterialTheme.typography.bodyMedium,
        )
        IconButton(onClick = onMapTap) {
            Icon(Icons.Filled.Map, contentDescription = "Open in Maps")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeliveryDetailsScreenPreview() {
    DeliveryAgentTheme {
        DeliveryDetailsScreen(orderId = "1042")
    }
}
