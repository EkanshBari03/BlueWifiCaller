package com.bluewificaller.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.*
import android.os.Build
import android.util.Log
import com.bluewificaller.model.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WifiDirect"
        const val PORT = 8988
        private const val BUFFER_SIZE = 4096
        const val GROUP_OWNER_IP = "192.168.49.1"   // WiFi Direct GO always gets this IP
    }

    private val wifiP2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ConnectionEvent> = _events

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isGroupOwner = false
    private var connectedPeerAddress: String? = null
    private var deviceName: String = "Unknown"

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    Log.d(TAG, "WiFi P2P state: $state")
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    wifiP2pManager?.requestPeers(channel) { peerList ->
                        peerList.deviceList.forEach { device ->
                            val peer = Peer(
                                id = device.deviceAddress,
                                name = device.deviceName.ifEmpty { "WiFi Device" },
                                connectionType = ConnectionType.WIFI_DIRECT,
                                status = PeerStatus.DISCOVERED,
                                deviceAddress = device.deviceAddress
                            )
                            scope.launch { _events.emit(ConnectionEvent.PeerFound(peer)) }
                        }
                    }
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, android.net.NetworkInfo::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    }

                    if (networkInfo?.isConnected == true) {
                        wifiP2pManager?.requestConnectionInfo(channel) { info ->
                            info?.let { handleConnectionInfo(it) }
                        }
                    } else {
                        scope.launch {
                            _events.emit(ConnectionEvent.Disconnected(connectedPeerAddress ?: ""))
                        }
                    }
                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                    device?.let { deviceName = it.deviceName }
                }
            }
        }
    }

    fun initialize(): Boolean {
        return try {
            channel = wifiP2pManager?.initialize(context, context.mainLooper, null)
            val filter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            context.registerReceiver(wifiReceiver, filter)
            channel != null
        } catch (e: Exception) {
            Log.e(TAG, "Init failed: ${e.message}")
            false
        }
    }

    fun startDiscovery() {
        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d(TAG, "Discovery started") }
            override fun onFailure(reason: Int) {
                scope.launch { _events.emit(ConnectionEvent.Error("WiFi discovery failed: $reason")) }
            }
        })
    }

    fun stopDiscovery() {
        wifiP2pManager?.stopPeerDiscovery(channel, null)
    }

    fun connectToPeer(peer: Peer) {
        val config = WifiP2pConfig().apply {
            deviceAddress = peer.deviceAddress
            wps.setup = android.net.wifi.WpsInfo.PBC
        }
        wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d(TAG, "Connect initiated") }
            override fun onFailure(reason: Int) {
                scope.launch { _events.emit(ConnectionEvent.Error("WiFi connect failed: $reason")) }
            }
        })
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        isGroupOwner = info.isGroupOwner
        val peerIp = if (info.isGroupOwner) null else info.groupOwnerAddress?.hostAddress
        connectedPeerAddress = peerIp ?: GROUP_OWNER_IP

        val peer = Peer(
            id = connectedPeerAddress ?: "wifi_peer",
            name = "WiFi Peer",
            connectionType = ConnectionType.WIFI_DIRECT,
            status = PeerStatus.CONNECTED,
            deviceAddress = connectedPeerAddress ?: ""
        )

        if (info.isGroupOwner) {
            // We are the server
            startWifiServer(peer)
        } else {
            // We are the client
            scope.launch {
                delay(1000) // Give server time to start
                connectAsWifiClient(GROUP_OWNER_IP, peer)
            }
        }
    }

    private fun startWifiServer(peer: Peer) {
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "WiFi server listening on port $PORT")
                clientSocket = serverSocket!!.accept()
                outputStream = clientSocket!!.outputStream
                inputStream = clientSocket!!.inputStream
                _events.emit(ConnectionEvent.Connected(peer))
                startReading()
            } catch (e: Exception) {
                Log.e(TAG, "WiFi server error: ${e.message}")
                _events.emit(ConnectionEvent.Error("WiFi server error: ${e.message}"))
            }
        }
    }

    private fun connectAsWifiClient(ip: String, peer: Peer) {
        scope.launch {
            var retries = 0
            while (retries < 5) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(ip, PORT), 5000)
                    clientSocket = socket
                    outputStream = socket.outputStream
                    inputStream = socket.inputStream
                    _events.emit(ConnectionEvent.Connected(peer))
                    startReading()
                    return@launch
                } catch (e: Exception) {
                    retries++
                    Log.w(TAG, "Retry $retries: ${e.message}")
                    delay(1500)
                }
            }
            _events.emit(ConnectionEvent.Error("Cannot connect to peer after retries"))
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
                    while (sb.contains('\n')) {
                        val idx = sb.indexOf('\n')
                        val line = sb.substring(0, idx).trim()
                        sb.delete(0, idx + 1)
                        if (line.isNotEmpty()) parseAndEmit(line)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "WiFi read error: ${e.message}")
                _events.emit(ConnectionEvent.Disconnected(connectedPeerAddress ?: ""))
            }
        }
    }

    private suspend fun parseAndEmit(json: String) {
        try {
            val msg = gson.fromJson(json, Message::class.java)
            _events.emit(ConnectionEvent.MessageReceived(msg))
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: $json")
        }
    }

    fun sendMessage(message: Message) {
        scope.launch {
            try {
                val json = gson.toJson(message) + "\n"
                outputStream?.write(json.toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "WiFi send error: ${e.message}")
            }
        }
    }

    fun sendAudioChunk(data: ByteArray) {
        scope.launch {
            try {
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
                Log.e(TAG, "WiFi audio send error: ${e.message}")
            }
        }
    }

    fun disconnect() {
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        wifiP2pManager?.removeGroup(channel, null)
        outputStream = null
        inputStream = null
        clientSocket = null
        serverSocket = null
        try { context.unregisterReceiver(wifiReceiver) } catch (_: Exception) {}
    }
}
