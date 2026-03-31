package com.bluewificaller.service

import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.bluewificaller.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager

@Singleton
class BluetoothManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BTManager"
        // Custom UUID for our app's SPP (Serial Port Profile)
        val SERVICE_UUID: UUID = UUID.fromString("e8e10f95-1a70-4b27-9ccf-02010264e9c8")
        const val SERVICE_NAME = "BlueWifiCaller"
        private const val BUFFER_SIZE = 2048
    }

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ConnectionEvent> = _events

    private val discoveredPeers = mutableMapOf<String, Peer>()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isListening = false
    private var deviceName: String = "Unknown"

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        else
                            @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                    device?.let { btDevice ->
                        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
                        val name = try { btDevice.name ?: "Unknown Device" } catch (e: SecurityException) { "Unknown" }
                        val peer = Peer(
                            id = btDevice.address,
                            name = name,
                            connectionType = ConnectionType.BLUETOOTH,
                            status = PeerStatus.DISCOVERED,
                            deviceAddress = btDevice.address,
                            rssi = rssi
                        )
                        discoveredPeers[btDevice.address] = peer
                        scope.launch { _events.emit(ConnectionEvent.PeerFound(peer)) }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Discovery finished")
                }
            }
        }
    }

    fun isAvailable() = bluetoothAdapter != null
    fun isEnabled() = bluetoothAdapter?.isEnabled == true

    fun setDeviceName(name: String) { deviceName = name }

    fun startDiscovery(): Boolean {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return false
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return false

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, filter)

        return try {
            bluetoothAdapter?.startDiscovery() == true
        } catch (e: SecurityException) {
            Log.e(TAG, "Discovery permission denied: ${e.message}")
            false
        }
    }

    fun stopDiscovery() {
        try {
            bluetoothAdapter?.cancelDiscovery()
            context.unregisterReceiver(discoveryReceiver)
        } catch (_: Exception) {}
    }

    fun startServer() {
        if (isListening) return
        isListening = true
        scope.launch {
            try {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return@launch
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                Log.d(TAG, "Server listening...")
                val socket = serverSocket?.accept() // blocks until connection
                socket?.let {
                    clientSocket = it
                    outputStream = it.outputStream
                    inputStream = it.inputStream
                    val deviceNameConn = try { it.remoteDevice?.name ?: "Unknown" } catch (e: SecurityException) { "Unknown" }
                    val peer = Peer(
                        id = it.remoteDevice?.address ?: "",
                        name = deviceNameConn,
                        connectionType = ConnectionType.BLUETOOTH,
                        status = PeerStatus.CONNECTED,
                        deviceAddress = it.remoteDevice?.address ?: ""
                    )
                    _events.emit(ConnectionEvent.Connected(peer))
                    startReading()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
                _events.emit(ConnectionEvent.Error("Bluetooth server error: ${e.message}"))
            }
        }
    }

    fun connectToPeer(peer: Peer) {
        scope.launch {
            try {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return@launch
                bluetoothAdapter?.cancelDiscovery()
                val device = bluetoothAdapter?.getRemoteDevice(peer.deviceAddress)
                    ?: run { _events.emit(ConnectionEvent.Error("Device not found")); return@launch }

                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()
                clientSocket = socket
                outputStream = socket.outputStream
                inputStream = socket.inputStream

                val connectedPeer = peer.copy(status = PeerStatus.CONNECTED)
                _events.emit(ConnectionEvent.Connected(connectedPeer))
                startReading()
            } catch (e: Exception) {
                Log.e(TAG, "Connect error: ${e.message}")
                _events.emit(ConnectionEvent.Error("Connection failed: ${e.message}"))
            }
        }
    }

    private fun startReading() {
        scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            val sb = StringBuilder()
            try {
                while (true) {
                    val bytes = inputStream?.read(buffer) ?: break
                    if (bytes == -1) break
                    sb.append(String(buffer, 0, bytes))
                    // Process complete JSON messages delimited by newline
                    while (sb.contains('\n')) {
                        val idx = sb.indexOf('\n')
                        val line = sb.substring(0, idx).trim()
                        sb.delete(0, idx + 1)
                        if (line.isNotEmpty()) {
                            parseAndEmit(line)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Read error: ${e.message}")
                _events.emit(ConnectionEvent.Disconnected(""))
            }
        }
    }

    private suspend fun parseAndEmit(json: String) {
        try {
            val msg = com.google.gson.Gson().fromJson(json, com.bluewificaller.model.Message::class.java)
            _events.emit(ConnectionEvent.MessageReceived(msg))
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: $json")
        }
    }

    fun sendMessage(message: com.bluewificaller.model.Message) {
        scope.launch {
            try {
                val json = com.google.gson.Gson().toJson(message) + "\n"
                outputStream?.write(json.toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Send error: ${e.message}")
            }
        }
    }

    fun sendAudioChunk(data: ByteArray) {
        scope.launch {
            try {
                // Send raw audio preceded by 4-byte length header
                val header = byteArrayOf(
                    (data.size shr 24 and 0xFF).toByte(),
                    (data.size shr 16 and 0xFF).toByte(),
                    (data.size shr 8 and 0xFF).toByte(),
                    (data.size and 0xFF).toByte()
                )
                outputStream?.write(header)
                outputStream?.write(data)
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Audio send error: ${e.message}")
            }
        }
    }

    fun disconnect() {
        isListening = false
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        outputStream = null
        inputStream = null
        clientSocket = null
        serverSocket = null
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
