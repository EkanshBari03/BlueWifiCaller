package com.bluewificaller.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bluewificaller.model.*
import com.bluewificaller.ui.theme.*
import com.bluewificaller.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeersScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel
) {
    val peers by viewModel.peers.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val selectedType by viewModel.selectedConnectionType.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepNavy, Midnight)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.stopDiscovery()
                    onNavigateBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurface)
                }
                Text(
                    "Nearby Devices",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                // Chip showing connection type
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ElectricBlue.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (selectedType == ConnectionType.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (selectedType == ConnectionType.BLUETOOTH) "Bluetooth" else "WiFi Direct",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricBlue
                        )
                    }
                }
            }

            // Radar Animation
            ScannerRadar(isScanning = isDiscovering, peerCount = peers.size)

            Spacer(Modifier.height(16.dp))

            // Scan Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isDiscovering) {
                    Button(
                        onClick = { viewModel.startDiscovery() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start Scan", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.stopDiscovery() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AlertRed)
                    ) {
                        Icon(Icons.Default.Close, null, tint = AlertRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Stop", color = AlertRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (peers.isEmpty()) {
                EmptyPeersState(isDiscovering)
            } else {
                Text(
                    "${peers.size} device${if (peers.size > 1) "s" else ""} found",
                    style = MaterialTheme.typography.labelSmall,
                    color = Subdued,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(peers, key = { it.id }) { peer ->
                        PeerCard(peer = peer, onCall = { viewModel.callPeer(peer) })
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerRadar(isScanning: Boolean, peerCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, 700, easing = LinearEasing), RepeatMode.Restart),
        label = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, 1400, easing = LinearEasing), RepeatMode.Restart),
        label = "ring3"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isScanning) {
            // Ripple rings
            listOf(ring1 to 0.2f, ring2 to 0.15f, ring3 to 0.1f).forEach { (scale, alpha) ->
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = alpha * (1f - scale)))
                )
            }
        }
        // Center circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(ElectricBlue.copy(0.3f), Surface2))
                )
                .border(2.dp, if (isScanning) ElectricBlue else Subdued, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isScanning) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isScanning) ElectricBlue else Subdued,
                modifier = Modifier.size(30.dp)
            )
        }
        // Status text
        if (isScanning) {
            Text(
                "Scanning…",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricBlue,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun PeerCard(peer: Peer, onCall: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(1.dp, Surface3)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (peer.connectionType == ConnectionType.BLUETOOTH)
                            ElectricBlue.copy(alpha = 0.15f)
                        else
                            NeonGreen.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (peer.connectionType == ConnectionType.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (peer.connectionType == ConnectionType.BLUETOOTH) ElectricBlue else NeonGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(peer.name, style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.SemiBold)
                Text(
                    peer.deviceAddress.ifEmpty { peer.connectionType.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Subdued
                )
                if (peer.rssi != 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SignalCellularAlt, null, tint = Subdued, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${peer.rssi} dBm", style = MaterialTheme.typography.labelSmall, color = Subdued)
                    }
                }
            }

            // Call button
            FloatingActionButton(
                onClick = onCall,
                modifier = Modifier.size(42.dp),
                containerColor = NeonGreen,
                contentColor = Midnight
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EmptyPeersState(isScanning: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PersonSearch,
                contentDescription = null,
                tint = Subdued,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isScanning) "Looking for nearby devices…" else "No devices found yet",
                style = MaterialTheme.typography.bodyLarge,
                color = Subdued
            )
            if (!isScanning) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap Start Scan to search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Subdued.copy(alpha = 0.6f)
                )
            }
        }
    }
}
