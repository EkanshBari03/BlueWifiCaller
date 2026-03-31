package com.bluewificaller.service

import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEngine @Inject constructor() {

    companion object {
        private const val TAG = "AudioEngine"
        private const val SAMPLE_RATE = 16000       // 16 kHz — voice optimized
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var captureJob: Job? = null
    private var isMuted = false
    private var isSpeakerOn = false

    var onAudioCaptured: ((ByteArray) -> Unit)? = null

    fun startCapture() {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_IN,
                ENCODING,
                BUFFER_SIZE * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                return
            }

            audioRecord?.startRecording()

            captureJob = scope.launch {
                val buffer = ByteArray(BUFFER_SIZE)
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0 && !isMuted) {
                        onAudioCaptured?.invoke(buffer.copyOf(read))
                    }
                }
            }
            Log.d(TAG, "Capture started, buffer=$BUFFER_SIZE")
        } catch (e: SecurityException) {
            Log.e(TAG, "Audio permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Capture error: ${e.message}")
        }
    }

    fun startPlayback() {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_OUT)
                        .setEncoding(ENCODING)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            Log.d(TAG, "Playback started")
        } catch (e: Exception) {
            Log.e(TAG, "Playback start error: ${e.message}")
        }
    }

    fun playAudioChunk(data: ByteArray) {
        try {
            audioTrack?.write(data, 0, data.size)
        } catch (e: Exception) {
            Log.e(TAG, "Play chunk error: ${e.message}")
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun setSpeakerOn(speakerOn: Boolean) {
        isSpeakerOn = speakerOn
        try {
            val am = audioTrack?.let { null } // Handled by AudioManager in ViewModel
        } catch (_: Exception) {}
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun stopPlayback() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun stopAll() {
        stopCapture()
        stopPlayback()
    }
}
