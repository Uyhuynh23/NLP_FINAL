package com.example.nlp_final.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Audio utilities for playback and WAV file creation
 */
class AudioPlayer(private val sampleRate: Int = 22050) {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    private var currentTrack: AudioTrack? = null

    /**
     * Convert float waveform [-1, 1] to PCM16 shorts
     */
    fun floatToPcm16(floatData: FloatArray): ShortArray {
        val shortData = ShortArray(floatData.size)
        for (i in floatData.indices) {
            val sample = floatData[i].coerceIn(-1.0f, 1.0f)
            shortData[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return shortData
    }

    /**
     * Play PCM16 audio using AudioTrack
     */
    suspend fun play(waveform: FloatArray): Boolean = withContext(Dispatchers.IO) {
        try {
            // Stop any currently playing audio
            stop()

            // Convert to PCM16
            val pcmData = floatToPcm16(waveform)

            // Create AudioTrack
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
            val bufferSize = maxOf(minBufferSize, pcmData.size * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            // Convert shorts to bytes
            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                byteBuffer.putShort(sample)
            }
            val audioData = byteBuffer.array()

            // Write data and play
            track.write(audioData, 0, audioData.size)
            track.play()

            currentTrack = track

            // Calculate duration and wait for playback to finish
            val durationMs = (pcmData.size.toDouble() / sampleRate * 1000).toLong()
            delay(durationMs + 100)

            track.stop()
            track.release()
            currentTrack = null

            Log.i(TAG, "Playback completed: ${pcmData.size} samples, ${durationMs}ms")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Playback failed", e)
            false
        }
    }

    /**
     * Stop current playback
     */
    fun stop() {
        try {
            currentTrack?.apply {
                stop()
                release()
            }
            currentTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        }
    }

    /**
     * Save waveform as WAV file
     * @param waveform float array waveform
     * @param outputFile output file path
     * @return true if successful
     */
    fun saveWav(waveform: FloatArray, outputFile: File): Boolean {
        return try {
            val pcmData = floatToPcm16(waveform)
            saveWavFromPcm16(pcmData, outputFile)
            Log.i(TAG, "Saved WAV: ${outputFile.absolutePath} (${waveform.size} samples)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save WAV", e)
            false
        }
    }

    /**
     * Save PCM16 data as WAV file with proper header
     */
    private fun saveWavFromPcm16(pcmData: ShortArray, outputFile: File) {
        FileOutputStream(outputFile).use { fos ->
            val channels = 1 // mono
            val bitsPerSample = 16
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val blockAlign = channels * bitsPerSample / 8
            val dataSize = pcmData.size * 2
            val fileSize = 36 + dataSize

            // Write RIFF header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(fileSize))
            fos.write("WAVE".toByteArray())

            // Write fmt chunk
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16)) // fmt chunk size
            fos.write(shortToBytes(1)) // audio format (1 = PCM)
            fos.write(shortToBytes(channels.toShort()))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes(blockAlign.toShort()))
            fos.write(shortToBytes(bitsPerSample.toShort()))

            // Write data chunk
            fos.write("data".toByteArray())
            fos.write(intToBytes(dataSize))

            // Write PCM data
            val buffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                buffer.putShort(sample)
            }
            fos.write(buffer.array())
        }
    }

    /**
     * Convert int to little-endian bytes
     */
    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    /**
     * Convert short to little-endian bytes
     */
    private fun shortToBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }

    /**
     * Get audio duration in seconds
     */
    fun getDuration(waveform: FloatArray): Float {
        return waveform.size.toFloat() / sampleRate
    }
}

