package com.example.nlp_final.tts

import android.content.Context
import android.util.Log
import com.example.nlp_final.audio.AudioPlayer
import com.example.nlp_final.model.ModelConfig
import com.example.nlp_final.onnx.OnnxInferenceEngine
import com.example.nlp_final.phonemizer.EspeakPhonemizerNative
import com.example.nlp_final.phonemizer.PhonemeTokenizer
import java.io.File

/**
 * Complete TTS pipeline: text -> phonemes -> IDs -> ONNX -> waveform -> playback/save
 */
class VietnameseTTSEngine(private val context: Context) {

    companion object {
        private const val TAG = "VietnameseTTS"
    }

    private val config: ModelConfig = ModelConfig.loadFromAssets(context)
    private val phonemizer: EspeakPhonemizerNative = EspeakPhonemizerNative(context)
    private val tokenizer: PhonemeTokenizer = PhonemeTokenizer(config)
    private val inferenceEngine: OnnxInferenceEngine = OnnxInferenceEngine(context, config)
    private val audioPlayer: AudioPlayer = AudioPlayer(config.sampleRate)

    private var isInitialized = false

    data class SynthesisResult(
        val success: Boolean,
        val phonemes: String = "",
        val phonemeCount: Int = 0,
        val tokenCount: Int = 0,
        val waveform: FloatArray? = null,
        val durationSec: Float = 0f,
        val inferenceTimeMs: Long = 0,
        val errorMessage: String? = null
    )

    /**
     * Initialize TTS engine
     */
    fun initialize(): Boolean {
        if (isInitialized) return true

        try {
            Log.i(TAG, "Initializing Vietnamese TTS Engine...")

            // Initialize phonemizer (will use fallback if native not available)
            phonemizer.initialize(config.voice)

            // Initialize ONNX Runtime
            val onnxSuccess = inferenceEngine.initialize()
            if (!onnxSuccess) {
                Log.e(TAG, "Failed to initialize ONNX Runtime")
                return false
            }

            isInitialized = true
            Log.i(TAG, "TTS Engine initialized successfully")
            Log.i(TAG, "Config: sampleRate=${config.sampleRate}, voice=${config.voice}, phonemes=${config.phonemeIdMap.size}")

            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TTS engine", e)
            return false
        }
    }

    /**
     * Synthesize speech from text
     * @param text input text
     * @param noiseScale controls variability (default from config)
     * @param lengthScale controls speed - higher = slower (default from config)
     * @param noiseW controls prosody variation (default from config)
     * @return synthesis result
     */
    fun synthesize(
        text: String,
        noiseScale: Float = config.noiseScale,
        lengthScale: Float = config.lengthScale,
        noiseW: Float = config.noiseW
    ): SynthesisResult {
        if (!isInitialized) {
            Log.e(TAG, "Engine not initialized")
            return SynthesisResult(false, errorMessage = "Engine not initialized")
        }

        if (text.isBlank()) {
            return SynthesisResult(false, errorMessage = "Empty text")
        }

        val totalStartTime = System.currentTimeMillis()

        try {
            // Step 1: Text normalization
            val normalizedText = normalizeText(text)
            Log.d(TAG, "Normalized text: $normalizedText")

            // Step 2: Phonemization
            val phonemes = phonemizer.phonemize(normalizedText, config.voice)
            if (phonemes.isEmpty()) {
                return SynthesisResult(false, errorMessage = "Phonemization failed")
            }
            Log.d(TAG, "Phonemes: $phonemes")

            // Step 3: Tokenization (phoneme symbols -> IDs)
            val phonemeIds = tokenizer.encode(phonemes, addBosEos = true)
            if (phonemeIds.isEmpty()) {
                return SynthesisResult(false, errorMessage = "Tokenization failed")
            }
            Log.d(TAG, "Phoneme IDs (first 50): ${phonemeIds.take(50).joinToString()}")

            // Step 4: ONNX Inference
            val inferenceStartTime = System.currentTimeMillis()
            val waveform = inferenceEngine.synthesize(phonemeIds, noiseScale, lengthScale, noiseW)
            val inferenceTime = System.currentTimeMillis() - inferenceStartTime

            if (waveform == null || waveform.isEmpty()) {
                return SynthesisResult(
                    false,
                    phonemes = phonemes,
                    phonemeCount = phonemes.length,
                    tokenCount = phonemeIds.size,
                    errorMessage = "Inference failed"
                )
            }

            val durationSec = waveform.size.toFloat() / config.sampleRate
            val totalTime = System.currentTimeMillis() - totalStartTime

            Log.i(TAG, "Synthesis successful: ${waveform.size} samples, ${String.format("%.2f", durationSec)}s, total time: ${totalTime}ms")

            return SynthesisResult(
                success = true,
                phonemes = phonemes,
                phonemeCount = phonemes.length,
                tokenCount = phonemeIds.size,
                waveform = waveform,
                durationSec = durationSec,
                inferenceTimeMs = inferenceTime
            )

        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed", e)
            return SynthesisResult(false, errorMessage = "Exception: ${e.message}")
        }
    }

    /**
     * Synthesize and play audio
     */
    suspend fun synthesizeAndPlay(
        text: String,
        noiseScale: Float = config.noiseScale,
        lengthScale: Float = config.lengthScale,
        noiseW: Float = config.noiseW
    ): SynthesisResult {
        val result = synthesize(text, noiseScale, lengthScale, noiseW)

        if (result.success && result.waveform != null) {
            audioPlayer.play(result.waveform)
        }

        return result
    }

    /**
     * Synthesize and save as WAV file
     */
    fun synthesizeAndSave(
        text: String,
        outputFile: File,
        noiseScale: Float = config.noiseScale,
        lengthScale: Float = config.lengthScale,
        noiseW: Float = config.noiseW
    ): SynthesisResult {
        val result = synthesize(text, noiseScale, lengthScale, noiseW)

        if (result.success && result.waveform != null) {
            val saved = audioPlayer.saveWav(result.waveform, outputFile)
            if (!saved) {
                Log.e(TAG, "Failed to save WAV file")
            }
        }

        return result
    }

    /**
     * Normalize text for better phonemization
     */
    private fun normalizeText(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), " ") // normalize whitespace
            .replace(Regex("[^\\p{L}\\p{N}\\p{P}\\s]"), "") // keep letters, numbers, punctuation, spaces
    }

    /**
     * Stop current playback
     */
    fun stopPlayback() {
        audioPlayer.stop()
    }

    /**
     * Get model configuration
     */
    fun getConfig(): ModelConfig = config

    /**
     * Cleanup resources
     */
    fun cleanup() {
        audioPlayer.stop()
        inferenceEngine.cleanup()
        phonemizer.cleanup()
        isInitialized = false
        Log.i(TAG, "TTS Engine cleaned up")
    }
}

