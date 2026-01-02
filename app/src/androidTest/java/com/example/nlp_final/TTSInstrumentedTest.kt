package com.example.nlp_final

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nlp_final.tts.VietnameseTTSEngine
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented test for Vietnamese TTS Engine
 */
@RunWith(AndroidJUnit4::class)
class TTSInstrumentedTest {

    private lateinit var context: Context
    private lateinit var ttsEngine: VietnameseTTSEngine

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ttsEngine = VietnameseTTSEngine(context)
    }

    @Test
    fun testEngineInitialization() {
        val success = ttsEngine.initialize()
        assert(success) { "Engine initialization failed" }

        val config = ttsEngine.getConfig()
        Log.i("TTSTest", "Config: sampleRate=${config.sampleRate}, voice=${config.voice}")
        assert(config.sampleRate == 22050) { "Sample rate mismatch" }
        assert(config.phonemeIdMap.isNotEmpty()) { "Phoneme map is empty" }
    }

    @Test
    fun testVietnameseSynthesis() = runBlocking {
        ttsEngine.initialize()

        val testText = "Xin chào! Đây là inference bằng ONNX của Piper."
        val result = ttsEngine.synthesize(testText)

        Log.i("TTSTest", "=== Synthesis Test Results ===")
        Log.i("TTSTest", "Success: ${result.success}")
        Log.i("TTSTest", "Text: $testText")
        Log.i("TTSTest", "Phonemes: ${result.phonemes}")
        Log.i("TTSTest", "Phoneme length: ${result.phonemeCount}")
        Log.i("TTSTest", "Token count: ${result.tokenCount}")
        Log.i("TTSTest", "Phoneme IDs (first 50): ${result.phonemes.take(50)}")

        if (result.success && result.waveform != null) {
            Log.i("TTSTest", "Waveform samples: ${result.waveform.size}")
            Log.i("TTSTest", "Duration: ${result.durationSec}s")
            Log.i("TTSTest", "Inference time: ${result.inferenceTimeMs}ms")

            // Save test WAV
            val outputFile = File(context.getExternalFilesDir(null), "test_output.wav")
            ttsEngine.synthesizeAndSave(testText, outputFile)

            assert(outputFile.exists()) { "WAV file not created" }
            Log.i("TTSTest", "WAV saved: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
        }

        assert(result.success) { "Synthesis failed: ${result.errorMessage}" }
        assert(result.tokenCount > 0) { "No tokens generated" }
    }

    @Test
    fun testScaleParameters() = runBlocking {
        ttsEngine.initialize()

        val text = "Xin chào"

        // Test with different length scales
        val result1 = ttsEngine.synthesize(text, lengthScale = 0.5f) // faster
        val result2 = ttsEngine.synthesize(text, lengthScale = 1.5f) // slower

        if (result1.success && result2.success) {
            Log.i("TTSTest", "Fast duration: ${result1.durationSec}s")
            Log.i("TTSTest", "Slow duration: ${result2.durationSec}s")
            // Slower should produce longer audio
            assert(result2.durationSec > result1.durationSec) {
                "Length scale not affecting duration"
            }
        }
    }
}

