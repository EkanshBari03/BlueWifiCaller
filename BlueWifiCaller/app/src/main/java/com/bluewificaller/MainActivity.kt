package com.bluewificaller

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Scaffold
import com.bluewificaller.ui.AppNavGraph
import com.bluewificaller.ui.theme.BlueWifiCallerTheme
import com.bluewificaller.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requiredPermissions: Array<String> get() {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
        // Android 12+ Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list += Manifest.permission.BLUETOOTH_SCAN
            list += Manifest.permission.BLUETOOTH_CONNECT
            list += Manifest.permission.BLUETOOTH_ADVERTISE
        }
        // Android 13+ Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.POST_NOTIFICATIONS
            list += Manifest.permission.NEARBY_WIFI_DEVICES
        }
        return list.toTypedArray()
    }

    private var onPermissionsResult: ((Boolean) -> Unit)? = null
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        onPermissionsResult?.invoke(allGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlueWifiCallerTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()
                val viewModel: MainViewModel = hiltViewModel()

                // Request permissions on launch
                LaunchedEffect(Unit) {
                    onPermissionsResult = { granted ->
                        if (!granted) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Some permissions denied. Bluetooth/WiFi calling may not work."
                                )
                            }
                        }
                    }
                    permissionLauncher.launch(requiredPermissions)
                }

                // Show errors from ViewModel
                LaunchedEffect(Unit) {
                    viewModel.errorMessage.collect { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ) { _ ->
                    AppNavGraph(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}
