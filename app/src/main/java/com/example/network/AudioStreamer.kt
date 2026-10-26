package com.example.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles low-latency local P2P audio streaming using AudioRecord and AudioTrack over UDP.
 */
class AudioStreamer(
    private val context: Context,
    private val audioPort: Int = NetworkUtils.AUDIO_PORT
) {
    private val TAG = "AudioStreamer"
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
    private val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var recordJob: Job? = null
    private var playbackJob: Job? = null

    private var recordSocket: DatagramSocket? = null
    private var playbackSocket: DatagramSocket? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private val isMuted = AtomicBoolean(false)
    private val isStreaming = AtomicBoolean(false)

    fun setMuted(muted: Boolean) {
        isMuted.set(muted)
    }

    /**
     * Starts listening for incoming audio datagrams on UDP port [audioPort].
     */
    fun startReceiving(scope: CoroutineScope) {
        if (playbackJob?.isActive == true) return

        playbackJob = scope.launch(Dispatchers.IO) {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT
                ).coerceAtLeast(1024)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val format = AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG_OUT)
                    .build()

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build().apply {
                        play()
                    }

                playbackSocket = DatagramSocket(audioPort).apply {
                    reuseAddress = true
                }
                Log.i(TAG, "Audio receiver listening on UDP $audioPort")

                val packetBuffer = ByteArray(1280) // 40ms of 16kHz 16-bit mono
                val packet = DatagramPacket(packetBuffer, packetBuffer.size)

                while (isActive && playbackSocket?.isClosed == false) {
                    try {
                        playbackSocket?.receive(packet)
                        if (packet.length > 0 && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack?.write(packet.data, 0, packet.length)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio playback error: ${e.message}")
            } finally {
                stopPlaybackInternal()
            }
        }
    }

    /**
     * Starts recording and streaming microphone audio to [targetIp]:[audioPort].
     */
    @SuppressLint("MissingPermission")
    fun startSending(scope: CoroutineScope, targetIp: String) {
        if (recordJob?.isActive == true) return
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Cannot start audio stream: RECORD_AUDIO permission missing")
            return
        }

        recordJob = scope.launch(Dispatchers.IO) {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT
                ).coerceAtLeast(1024)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_IN,
                    AUDIO_FORMAT,
                    minBufferSize * 2
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed")
                    return@launch
                }

                audioRecord?.startRecording()
                recordSocket = DatagramSocket()
                val targetAddress = InetAddress.getByName(targetIp)

                isStreaming.set(true)
                Log.i(TAG, "Audio streaming to $targetIp:$audioPort started")

                val chunkBuffer = ByteArray(640) // 20ms chunk at 16kHz 16-bit
                val silentBuffer = ByteArray(640)

                while (isActive && isStreaming.get()) {
                    val bytesRead = audioRecord?.read(chunkBuffer, 0, chunkBuffer.size) ?: -1
                    if (bytesRead > 0) {
                        val toSend = if (isMuted.get()) silentBuffer else chunkBuffer
                        val packet = DatagramPacket(toSend, bytesRead, targetAddress, audioPort)
                        recordSocket?.send(packet)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio recording/send error: ${e.message}")
            } finally {
                stopRecordInternal()
            }
        }
    }

    fun stop() {
        isStreaming.set(false)
        stopRecordInternal()
        stopPlaybackInternal()
    }

    private fun stopRecordInternal() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null
        try {
            recordSocket?.close()
        } catch (ignored: Exception) {}
        recordSocket = null
        recordJob?.cancel()
        recordJob = null
    }

    private fun stopPlaybackInternal() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
        try {
            playbackSocket?.close()
        } catch (ignored: Exception) {}
        playbackSocket = null
        playbackJob?.cancel()
        playbackJob = null
    }
}
