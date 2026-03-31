package com.bluewificaller.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bluewificaller.BlueWifiCallerApp
import com.bluewificaller.MainActivity
import com.bluewificaller.R
import com.bluewificaller.model.*
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class CallService : Service() {

    inner class LocalBinder : Binder() {
        fun getService() = this@CallService
    }

    @Inject lateinit var btManager: BluetoothManager
    @Inject lateinit var wifiManager: WifiDirectManager
    @Inject lateinit var audioEngine: AudioEngine

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val binder = LocalBinder()
    private val gson = Gson()
    private var audioManager: AudioManager? = null

    // --- State Flows ---
    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    val peers: StateFlow<List<Peer>> = _peers

    private val _connectedPeer = MutableStateFlow<Peer?>(null)
    val connectedPeer: StateFlow<Peer?> = _connectedPeer

    private val _activeSession = MutableStateFlow<CallSession?>(null)
    val activeSession: StateFlow<CallSession?> = _activeSession

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isSpeaker = MutableStateFlow(false)
    val isSpeaker: StateFlow<Boolean> = _isSpeaker

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errorMessage: SharedFlow<String> = _errorMessage

    private val peerMap = mutableMapOf<String, Peer>()
    private var callTimerJob: Job? = null
    private var myDeviceName: String = "Unknown"
    private var currentConnectionType: ConnectionType? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        setupEventListeners()
        wifiManager.initialize()
        Log.d("CallService", "Service created")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        return START_STICKY
    }

    fun setDeviceName(name: String) {
        myDeviceName = name
        btManager.setDeviceName(name)
    }

    // ─── Discovery ───────────────────────────────────────────────────────────

    fun startBluetoothDiscovery() {
        currentConnectionType = ConnectionType.BLUETOOTH
        btManager.startDiscovery()
        btManager.startServer()
    }

    fun startWifiDiscovery() {
        currentConnectionType = ConnectionType.WIFI_DIRECT
        wifiManager.startDiscovery()
    }

    fun stopDiscovery() {
        btManager.stopDiscovery()
        wifiManager.stopDiscovery()
    }

    // ─── Calling ─────────────────────────────────────────────────────────────

    fun callPeer(peer: Peer) {
        scope.launch {
            _callState.value = CallState.OUTGOING
            updateNotification("Calling ${peer.name}…")

            // Connect transport
            if (peer.connectionType == ConnectionType.BLUETOOTH) {
                btManager.connectToPeer(peer)
            } else {
                wifiManager.connectToPeer(peer)
            }
        }
    }

    fun acceptCall() {
        val peer = _connectedPeer.value ?: return
        sendSignal(MessageType.CALL_ACCEPT, peer)
        _callState.value = CallState.ACTIVE
        startAudio()
        startCallTimer()
        updateNotification("In call with ${peer.name}")
    }

    fun rejectCall() {
        val peer = _connectedPeer.value ?: return
        sendSignal(MessageType.CALL_REJECT, peer)
        _callState.value = CallState.ENDED
        resetState()
    }

    fun endCall() {
        val peer = _connectedPeer.value ?: return
        sendSignal(MessageType.CALL_END, peer)
        finalizeCall()
    }

    fun toggleMute() {
        val muted = !_isMuted.value
        _isMuted.value = muted
        audioEngine.setMuted(muted)
    }

    fun toggleSpeaker() {
        val speaker = !_isSpeaker.value
        _isSpeaker.value = speaker
        audioManager?.apply {
            mode = if (speaker) AudioManager.MODE_NORMAL else AudioManager.MODE_IN_COMMUNICATION
            isSpeakerphoneOn = speaker
        }
    }

    // ─── Private ─────────────────────────────────────────────────────────────

    private fun setupEventListeners() {
        // Bluetooth events
        scope.launch {
            btManager.events.collect { event -> handleEvent(event, ConnectionType.BLUETOOTH) }
        }
        // WiFi events
        scope.launch {
            wifiManager.events.collect { event -> handleEvent(event, ConnectionType.WIFI_DIRECT) }
        }
    }

    private fun handleEvent(event: ConnectionEvent, type: ConnectionType) {
        when (event) {
            is ConnectionEvent.PeerFound -> {
                peerMap[event.peer.id] = event.peer
                _peers.value = peerMap.values.toList()
            }
            is ConnectionEvent.PeerLost -> {
                peerMap.remove(event.peerId)
                _peers.value = peerMap.values.toList()
            }
            is ConnectionEvent.Connected -> {
                val peer = event.peer.copy(connectionType = type, status = PeerStatus.CONNECTED)
                _connectedPeer.value = peer
                peerMap[peer.id] = peer
                _peers.value = peerMap.values.toList()
                // If we initiated the call, send CALL_REQUEST
                if (_callState.value == CallState.OUTGOING) {
                    sendSignal(MessageType.CALL_REQUEST, peer)
                }
            }
            is ConnectionEvent.Disconnected -> {
                if (_callState.value == CallState.ACTIVE) finalizeCall()
                else resetState()
            }
            is ConnectionEvent.MessageReceived -> handleMessage(event.message)
            is ConnectionEvent.Error -> {
                scope.launch { _errorMessage.emit(event.message) }
                if (_callState.value == CallState.OUTGOING || _callState.value == CallState.INCOMING) {
                    _callState.value = CallState.IDLE
                }
            }
        }
    }

    private fun handleMessage(msg: Message) {
        when (msg.type) {
            MessageType.CALL_REQUEST -> {
                _callState.value = CallState.INCOMING
                vibrate()
                updateNotification("Incoming call from ${msg.senderName}")
            }
            MessageType.CALL_ACCEPT -> {
                _callState.value = CallState.ACTIVE
                startAudio()
                startCallTimer()
                val peer = _connectedPeer.value ?: return
                updateNotification("In call with ${peer.name}")
            }
            MessageType.CALL_REJECT -> {
                scope.launch { _errorMessage.emit("Call rejected") }
                _callState.value = CallState.ENDED
                resetState()
            }
            MessageType.CALL_END -> finalizeCall()
            MessageType.AUDIO_CHUNK -> {
                // payload is base64-encoded audio bytes
                try {
                    val bytes = android.util.Base64.decode(msg.payload, android.util.Base64.DEFAULT)
                    audioEngine.playAudioChunk(bytes)
                } catch (_: Exception) {}
            }
            MessageType.HEARTBEAT -> { /* keep-alive, ignore */ }
        }
    }

    private fun sendSignal(type: MessageType, peer: Peer) {
        val msg = Message(
            type = type,
            senderId = myDeviceName,
            senderName = myDeviceName
        )
        if (peer.connectionType == ConnectionType.BLUETOOTH) {
            btManager.sendMessage(msg)
        } else {
            wifiManager.sendMessage(msg)
        }
    }

    private fun startAudio() {
        audioManager?.apply {
            mode = AudioManager.MODE_IN_COMMUNICATION
            isSpeakerphoneOn = false
        }
        audioEngine.startPlayback()
        audioEngine.startCapture()

        val peer = _connectedPeer.value ?: return
        audioEngine.onAudioCaptured = { bytes ->
            // Encode audio as base64 and send as message
            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
            val msg = Message(
                type = MessageType.AUDIO_CHUNK,
                senderId = myDeviceName,
                senderName = myDeviceName,
                payload = b64
            )
            if (peer.connectionType == ConnectionType.BLUETOOTH) {
                btManager.sendMessage(msg)
            } else {
                wifiManager.sendMessage(msg)
            }
        }
    }

    private fun startCallTimer() {
        val startTime = System.currentTimeMillis()
        val peer = _connectedPeer.value ?: return
        _activeSession.value = CallSession(
            sessionId = UUID.randomUUID().toString(),
            peer = peer,
            callState = CallState.ACTIVE,
            startTimeMs = startTime
        )
        callTimerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val duration = System.currentTimeMillis() - startTime
                _activeSession.value = _activeSession.value?.copy(durationMs = duration)
            }
        }
    }

    private fun finalizeCall() {
        callTimerJob?.cancel()
        audioEngine.stopAll()
        audioManager?.mode = AudioManager.MODE_NORMAL
        _callState.value = CallState.ENDED
        scope.launch {
            delay(1500)
            resetState()
        }
        updateNotification("BlueWifi Caller active")
    }

    private fun resetState() {
        _callState.value = CallState.IDLE
        _connectedPeer.value = null
        _activeSession.value = null
        _isMuted.value = false
        _isSpeaker.value = false
        audioEngine.onAudioCaptured = null
        btManager.disconnect()
        wifiManager.disconnect()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(android.os.VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(android.os.Vibrator::class.java)
        }
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun startForegroundCompat() {
        val notification = buildNotification("BlueWifi Caller is active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(1, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, BlueWifiCallerApp.CHANNEL_CALL)
            .setContentTitle("BlueWifi Caller")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1, buildNotification(text))
    }

    override fun onDestroy() {
        scope.cancel()
        audioEngine.stopAll()
        btManager.disconnect()
        wifiManager.disconnect()
        super.onDestroy()
    }
}
