package com.bluewificaller.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.bluewificaller.ui.theme.*
import com.bluewificaller.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel
) {
    val deviceName by viewModel.deviceName.collectAsState()
    var nameInput by remember { mutableStateOf(deviceName) }
    var showSaved by remember { mutableStateOf(false) }

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
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurface)
                }
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Section: Identity
                SettingsSectionHeader("Identity")
                Spacer(Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Device Name", style = MaterialTheme.typography.labelSmall, color = Subdued)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = Surface3,
                                focusedTextColor = OnSurface,
                                unfocusedTextColor = OnSurface,
                                cursorColor = ElectricBlue
                            ),
                            placeholder = {
                                Text("e.g. Ravi's Phone", color = Subdued)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Subdued)
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (nameInput.isNotBlank()) {
                                    viewModel.setDeviceName(nameInput.trim())
                                    showSaved = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text("Save Name", fontWeight = FontWeight.SemiBold)
                        }
                        if (showSaved) {
                            Spacer(Modifier.height(8.dp))
                            Text("✓ Saved!", style = MaterialTheme.typography.labelSmall, color = NeonGreen)
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(2000)
                                showSaved = false
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Section: About
                SettingsSectionHeader("About")
                Spacer(Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        AboutRow(Icons.Default.Info, "Version", "1.0.0")
                        Divider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AboutRow(Icons.Default.Bluetooth, "Bluetooth", "RFCOMM SPP")
                        Divider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AboutRow(Icons.Default.Wifi, "WiFi", "Wi-Fi Direct P2P")
                        Divider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AboutRow(Icons.Default.Android, "Min Android", "Android 12 (API 31)")
                        Divider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AboutRow(Icons.Default.RecordVoiceOver, "Audio", "16 kHz PCM Mono")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Section: How It Works
                SettingsSectionHeader("How It Works")
                Spacer(Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HowItWorksStep("1", "Both users open the app and choose the same connection type (BT or WiFi).")
                        HowItWorksStep("2", "One user taps 'Start Scan'. Nearby devices running the app will appear.")
                        HowItWorksStep("3", "Tap the call button next to a device. The other person sees an incoming call screen.")
                        HowItWorksStep("4", "They tap Accept — the call connects with real-time voice, no internet needed!")
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = ElectricBlue,
        letterSpacing = 1.5.sp
    )
}

@Composable
fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Subdued, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Subdued)
    }
}

@Composable
fun HowItWorksStep(step: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(ElectricBlue.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(step, style = MaterialTheme.typography.labelSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}
