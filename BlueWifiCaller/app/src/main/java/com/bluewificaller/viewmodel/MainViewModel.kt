package com.bluewificaller.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bluewificaller.model.*
import com.bluewificaller.service.CallService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private var callService: CallService? = null
    private var isBound = false

    // Exposed state (mirrors service state)
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

    private val _deviceName = MutableStateFlow("MyPhone")
    val deviceName: StateFlow<String> = _deviceName

    private val _selectedConnectionType = MutableStateFlow(ConnectionType.BLUETOOTH)
    val selectedConnectionType: StateFlow<ConnectionType> = _selectedConnectionType

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val service = (binder as CallService.LocalBinder).getService()
            callService = service
            isBound = true
            service.setDeviceName(_deviceName.value)
            collectServiceFlows(service)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            callService = null
            isBound = false
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, CallService::class.java)
        ctx.startService(intent)
        ctx.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun collectServiceFlows(service: CallService) {
        viewModelScope.launch { service.callState.collect { _callState.value = it } }
        viewModelScope.launch { service.peers.collect { _peers.value = it } }
        viewModelScope.launch { service.connectedPeer.collect { _connectedPeer.value = it } }
        viewModelScope.launch { service.activeSession.collect { _activeSession.value = it } }
        viewModelScope.launch { service.isMuted.collect { _isMuted.value = it } }
        viewModelScope.launch { service.isSpeaker.collect { _isSpeaker.value = it } }
        viewModelScope.launch { service.errorMessage.collect { _errorMessage.emit(it) } }
    }

    fun setDeviceName(name: String) {
        _deviceName.value = name
        callService?.setDeviceName(name)
    }

    fun setConnectionType(type: ConnectionType) {
        _selectedConnectionType.value = type
    }

    fun startDiscovery() {
        _isDiscovering.value = true
        if (_selectedConnectionType.value == ConnectionType.BLUETOOTH) {
            callService?.startBluetoothDiscovery()
        } else {
            callService?.startWifiDiscovery()
        }
    }

    fun stopDiscovery() {
        _isDiscovering.value = false
        callService?.stopDiscovery()
    }

    fun callPeer(peer: Peer) {
        callService?.callPeer(peer)
    }

    fun acceptCall() = callService?.acceptCall()
    fun rejectCall() = callService?.rejectCall()
    fun endCall() = callService?.endCall()
    fun toggleMute() = callService?.toggleMute()
    fun toggleSpeaker() = callService?.toggleSpeaker()

    override fun onCleared() {
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
        super.onCleared()
    }
}
