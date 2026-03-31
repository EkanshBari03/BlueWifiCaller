package com.bluewificaller.model

import java.io.Serializable

enum class ConnectionType { BLUETOOTH, WIFI_DIRECT }

enum class PeerStatus { DISCOVERED, CONNECTING, CONNECTED, DISCONNECTED }

enum class CallState {
    IDLE,
    OUTGOING,   // Caller: waiting for answer
    INCOMING,   // Callee: ringing
    ACTIVE,     // In call
    ENDED
}

data class Peer(
    val id: String,                     // MAC address or device name
    val name: String,
    val connectionType: ConnectionType,
    val status: PeerStatus = PeerStatus.DISCOVERED,
    val deviceAddress: String = "",     // BT or WiFi address
    val rssi: Int = 0                   // Signal strength
) : Serializable

data class CallSession(
    val sessionId: String,
    val peer: Peer,
    val callState: CallState,
    val startTimeMs: Long = 0L,
    val durationMs: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false
)

data class Message(
    val type: MessageType,
    val senderId: String,
    val senderName: String,
    val payload: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageType {
    CALL_REQUEST,
    CALL_ACCEPT,
    CALL_REJECT,
    CALL_END,
    HEARTBEAT,
    AUDIO_CHUNK        // Raw PCM audio frames
}

sealed class ConnectionEvent {
    data class PeerFound(val peer: Peer) : ConnectionEvent()
    data class PeerLost(val peerId: String) : ConnectionEvent()
    data class Connected(val peer: Peer) : ConnectionEvent()
    data class Disconnected(val peerId: String) : ConnectionEvent()
    data class MessageReceived(val message: Message) : ConnectionEvent()
    data class Error(val message: String) : ConnectionEvent()
}
