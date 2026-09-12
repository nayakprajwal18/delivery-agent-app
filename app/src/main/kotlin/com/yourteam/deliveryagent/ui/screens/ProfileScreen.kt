package com.yourteam.deliveryagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourteam.deliveryagent.ui.theme.DeliveryAgentTheme

/**
 * Agent profile: name, phone, vehicle info, availability toggle, total earnings, logout.
 * TODO (Phase 5): wire to ProfileViewModel.
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
) {
    var isOnline by rememberSaveable { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(20.dp))

            ProfileField(label = "Name",          value = "—")
            ProfileField(label = "Phone",         value = "—")
            ProfileField(label = "Vehicle type",  value = "—")
            ProfileField(label = "Vehicle number", value = "—")

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ── Availability toggle ────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Availability", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text  = if (isOnline) "Online — accepting deliveries" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOnline) MaterialTheme.colorScheme.primary
                                else         MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked         = isOnline,
                    onCheckedChange = {
                        isOnline = it
                        // TODO: ProfileViewModel.toggleOnline(it)
                    },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ── Total earnings ─────────────────────────────────────────────
            ProfileField(label = "Total earnings (all time)", value = "₹—")

            Spacer(Modifier.weight(1f))

            // ── Logout ─────────────────────────────────────────────────────
            Button(
                onClick  = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    DeliveryAgentTheme { ProfileScreen() }
}
