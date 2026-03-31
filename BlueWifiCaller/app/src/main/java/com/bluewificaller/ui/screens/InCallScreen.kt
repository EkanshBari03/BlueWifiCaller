package com.bluewificaller.ui.screens

import androidx.compose.animation.*
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
import com.bluewificaller.model.CallState
import com.bluewificaller.ui.theme.*
import com.bluewificaller.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit

@Composable
fun InCallScreen(viewModel: MainViewModel) {
    val callState by viewModel.callState.collectAsState()
    val peer by viewModel.connectedPeer.collectAsState()
    val session by viewModel.activeSession.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeaker by viewModel.isSpeaker.collectAsState()

    val peerName = peer?.name ?: "Unknown"
    val durationMs = session?.durationMs ?: 0L
    val durationText = formatDuration(durationMs)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF060D18), DeepNavy, Color(0xFF011627))
                )
            )
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (callState == CallState.ACTIVE) NeonGreen.copy(0.08f) else ElectricBlue.copy(0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(40.dp))

            // Top section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Status
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (callState) {
                        CallState.ACTIVE  -> NeonGreen.copy(alpha = 0.2f)
                        CallState.OUTGOING -> AmberWarm.copy(alpha = 0.2f)
                        else -> Subdued.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = when (callState) {
                            CallState.ACTIVE   -> "● On Call"
                            CallState.OUTGOING -> "⟳ Calling…"
                            CallState.ENDED    -> "Call Ended"
                            else -> ""
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (callState) {
                            CallState.ACTIVE  -> NeonGreen
                            CallState.OUTGOING -> AmberWarm
                            else -> Subdued
                        }
                    )
                }

                Spacer(Modifier.height(40.dp))

                // Avatar with pulse
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (callState == CallState.ACTIVE) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(pulse)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.08f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (callState == CallState.ACTIVE) NeonGreen.copy(0.25f) else ElectricBlue.copy(0.25f),
                                        Surface2
                                    )
                                )
                            )
                            .border(2.dp, if (callState == CallState.ACTIVE) NeonGreen else ElectricBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            peerName.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = if (callState == CallState.ACTIVE) NeonGreen else ElectricBlue
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(peerName, style = MaterialTheme.typography.headlineMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                if (callState == CallState.ACTIVE) {
                    Text(durationText, style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                } else if (callState == CallState.OUTGOING) {
                    Text("Ringing…", style = MaterialTheme.typography.titleMedium, color = AmberWarm)
                }
            }

            // Control buttons
            if (callState == CallState.ACTIVE) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute
                        CallControlButton(
                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (isMuted) "Unmute" else "Mute",
                            active = isMuted,
                            activeColor = AmberWarm,
                            onClick = { viewModel.toggleMute() }
                        )

                        // End Call
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(AlertRed)
                                .clickable { viewModel.endCall() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End", tint = Color.White, modifier = Modifier.size(30.dp))
                        }

                        // Speaker
                        CallControlButton(
                            icon = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            label = if (isSpeaker) "Speaker" else "Earpiece",
                            active = isSpeaker,
                            activeColor = ElectricBlue,
                            onClick = { viewModel.toggleSpeaker() }
                        )
                    }
                }
            } else if (callState == CallState.OUTGOING) {
                // Only end call for outgoing
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AlertRed)
                        .clickable { viewModel.endCall() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) activeColor.copy(alpha = 0.2f) else Surface2)
                .border(1.dp, if (active) activeColor else Surface3, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (active) activeColor else OnSurface, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Subdued)
    }
}

private fun formatDuration(ms: Long): String {
    val secs = TimeUnit.MILLISECONDS.toSeconds(ms)
    val m = secs / 60
    val s = secs % 60
    return "%02d:%02d".format(m, s)
}
