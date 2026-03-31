package com.bluewificaller.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bluewificaller.model.ConnectionType
import com.bluewificaller.ui.theme.*
import com.bluewificaller.viewmodel.MainViewModel
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPeers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel
) {
    val deviceName by viewModel.deviceName.collectAsState()
    val selectedType by viewModel.selectedConnectionType.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepNavy, Midnight, Color(0xFF060D18))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BlueWifi",
                        style = MaterialTheme.typography.displayLarge,
                        color = ElectricBlue,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Caller",
                        style = MaterialTheme.typography.displayLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Light
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Subdued)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "No internet needed. Call nearby.",
                style = MaterialTheme.typography.bodyMedium,
                color = Subdued
            )

            Spacer(Modifier.height(48.dp))

            // Device Name Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface1)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Your Device Name", style = MaterialTheme.typography.labelSmall, color = Subdued)
                        Text(deviceName, style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Connection Type Selector
            Text("Connection Type", style = MaterialTheme.typography.labelSmall, color = Subdued, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConnectionTypeCard(
                    modifier = Modifier.weight(1f),
                    title = "Bluetooth",
                    subtitle = "Up to 10m",
                    icon = Icons.Default.Bluetooth,
                    selected = selectedType == ConnectionType.BLUETOOTH,
                    onClick = { viewModel.setConnectionType(ConnectionType.BLUETOOTH) }
                )
                ConnectionTypeCard(
                    modifier = Modifier.weight(1f),
                    title = "WiFi Direct",
                    subtitle = "Up to 100m",
                    icon = Icons.Default.Wifi,
                    selected = selectedType == ConnectionType.WIFI_DIRECT,
                    onClick = { viewModel.setConnectionType(ConnectionType.WIFI_DIRECT) }
                )
            }

            Spacer(Modifier.weight(1f))

            // Find People Button
            Button(
                onClick = onNavigateToPeers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue
                )
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Find Nearby People", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }

        // Glow effect top
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 100.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ElectricBlue.copy(alpha = 0.06f), androidx.compose.ui.graphics.Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun ConnectionTypeCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) ElectricBlue else Surface3
    val bgColor = if (selected) ElectricBlue.copy(alpha = 0.1f) else Surface1

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) ElectricBlue else Subdued,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) ElectricBlue else OnSurface, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Subdued)
        }
    }
}
