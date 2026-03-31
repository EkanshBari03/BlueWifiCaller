package com.bluewificaller.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.bluewificaller.ui.theme.*
import com.bluewificaller.viewmodel.MainViewModel

@Composable
fun IncomingCallScreen(viewModel: MainViewModel) {
    val peer by viewModel.connectedPeer.collectAsState()
    val peerName = peer?.name ?: "Unknown Caller"

    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "r1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "a1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1500, 500, easing = LinearEasing), RepeatMode.Restart),
        label = "r2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, 500, easing = LinearEasing), RepeatMode.Restart),
        label = "a2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF051015), Color(0xFF020810))))
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-100).dp)
                .background(
                    Brush.radialGradient(listOf(NeonGreen.copy(0.06f), Color.Transparent))
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(60.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Incoming Call", style = MaterialTheme.typography.titleMedium, color = Subdued)
                Spacer(Modifier.height(40.dp))

                // Pulsing avatar
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Ring 1
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(ring1)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = ring1Alpha))
                    )
                    // Ring 2
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(ring2)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = ring2Alpha))
                    )
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonGreen.copy(0.3f), Surface2)))
                            .border(2.5.dp, NeonGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            peerName.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = NeonGreen
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(peerName, style = MaterialTheme.typography.headlineMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    peer?.connectionType?.name ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Subdued
                )
            }

            // Accept / Reject buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reject
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AlertRed)
                            .clickable { viewModel.rejectCall() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Reject", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Decline", style = MaterialTheme.typography.labelSmall, color = Subdued)
                }

                // Accept
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                            .clickable { viewModel.acceptCall() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept", tint = Midnight, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Accept", style = MaterialTheme.typography.labelSmall, color = NeonGreen)
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
