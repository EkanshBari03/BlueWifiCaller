package com.bluewificaller.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.accompanist.permissions.*
import com.bluewificaller.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsGate(content: @Composable () -> Unit) {
    val permissions = buildList {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
        add(Manifest.permission.BLUETOOTH_ADVERTISE)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permState = rememberMultiplePermissionsState(permissions)

    if (permState.allPermissionsGranted) {
        content()
    } else {
        PermissionsScreen(permState)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(permState: MultiplePermissionsState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepNavy, Midnight))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(ElectricBlue.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "BlueWifi Caller needs the following permissions to discover nearby devices and enable peer-to-peer voice calls.",
                style = MaterialTheme.typography.bodyLarge,
                color = Subdued,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // List denied permissions
            val denied = permState.permissions.filter { !it.status.isGranted }
            denied.forEach { perm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Cancel, null, tint = AlertRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        perm.permission.substringAfterLast('.'),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { permState.launchMultiplePermissionRequest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Grant Permissions", fontWeight = FontWeight.Bold)
            }

            if (permState.shouldShowRationale) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Some permissions were denied. Please grant them in Android Settings → Apps → BlueWifi Caller → Permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmberWarm,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
