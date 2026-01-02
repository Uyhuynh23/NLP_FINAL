package com.example.nlp_final.onnx

import android.content.Context
import android.util.Log
import com.example.nlp_final.model.ModelConfig
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

import java.io.File

/**
 * ONNX Runtime inference engine for Piper/VITS TTS model
 */
class OnnxInferenceEngine(
    private val context: Context,
    private val config: ModelConfig
) {
    companion object {
        private const val TAG = "OnnxInference"
    }

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    /**
     * Initialize ONNX Runtime session
     */
    fun initialize(modelPath: String = "model.onnx"): Boolean {
        if (isInitialized) return true

        try {
            // Copy model from assets to files directory
            val modelFile = copyModelToFiles(modelPath)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: ${modelFile.absolutePath}")
                return false
            }

            // Create ONNX Runtime environment
            ortEnv = OrtEnvironment.getEnvironment()

            // Create session with optimizations
            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.setIntraOpNumThreads(4)
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

            ortSession = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)

            isInitialized = ortSession != null

            if (isInitialized) {
                Log.i(TAG, "ONNX Runtime initialized successfully")
                logSessionInfo()
            } else {
                Log.e(TAG, "Failed to create ONNX session")
            }

            return isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ONNX Runtime", e)
            return false
        }
    }

    /**
     * Run inference on phoneme IDs
     * @param phonemeIds phoneme ID sequence [T]
     * @param noiseScale controls variability
     * @param lengthScale controls speed (higher = slower)
     * @param noiseW controls prosody variation
     * @return float array waveform or null on error
     */
    fun synthesize(
        phonemeIds: LongArray,
        noiseScale: Float = config.noiseScale,
        lengthScale: Float = config.lengthScale,
        noiseW: Float = config.noiseW
    ): FloatArray? {
        if (!isInitialized) {
            Log.e(TAG, "Engine not initialized")
            return null
        }

        val session = ortSession ?: return null
        val env = ortEnv ?: return null
        val startTime = System.currentTimeMillis()

        // input: [1, T]
        val inputIds = arrayOf(phonemeIds)
        // input_lengths: [1]
        val inputLengths = longArrayOf(phonemeIds.size.toLong())
        // scales: [3]
        val scales = floatArrayOf(noiseScale, lengthScale, noiseW)

        var tensorInput: OnnxTensor? = null
        var tensorLengths: OnnxTensor? = null
        var tensorScales: OnnxTensor? = null
        var results: OrtSession.Result? = null

        return try {
            tensorInput = OnnxTensor.createTensor(env, inputIds)
            tensorLengths = OnnxTensor.createTensor(env, inputLengths)
            tensorScales = OnnxTensor.createTensor(env, scales)

            val inputs = mapOf(
                "input" to tensorInput,
                "input_lengths" to tensorLengths,
                "scales" to tensorScales
            )

            results = session.run(inputs)

            if (results.size() == 0) {   // ✅ size() là hàm
                Log.e(TAG, "No output from model")
                return null
            }

            val outputValue = results[0].value
            val waveform = extractFloatArray(outputValue)

            val inferenceTime = System.currentTimeMillis() - startTime
            if (waveform != null && waveform.isNotEmpty()) {
                val durationSec = waveform.size.toFloat() / config.sampleRate
                Log.i(TAG, "Inference OK: ${waveform.size} samples (${String.format("%.2f", durationSec)}s) in ${inferenceTime}ms")
            } else {
                Log.e(TAG, "Failed to extract waveform")
            }

            waveform
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            null
        } finally {
            try { results?.close() } catch (_: Exception) {}
            try { tensorInput?.close() } catch (_: Exception) {}
            try { tensorLengths?.close() } catch (_: Exception) {}
            try { tensorScales?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Extract float array from ONNX tensor output
     * Handles various tensor shapes and nesting
     */
    private fun extractFloatArray(value: Any?): FloatArray? {
        return when (value) {
            is FloatArray -> value
            is Array<*> -> flattenToFloatArray(value)
            else -> {
                Log.e(TAG, "Unexpected output type: ${value?.javaClass?.name}")
                null
            }
        }
    }

    /**
     * Recursively flatten nested arrays to float array
     */
    private fun flattenToFloatArray(arr: Array<*>): FloatArray {
        val list = mutableListOf<Float>()

        fun flatten(obj: Any?) {
            when (obj) {
                is Float -> list.add(obj)
                is Double -> list.add(obj.toFloat())
                is Number -> list.add(obj.toFloat())
                is FloatArray -> obj.forEach { list.add(it) }
                is DoubleArray -> obj.forEach { list.add(it.toFloat()) }
                is Array<*> -> obj.forEach { flatten(it) }
                is Iterable<*> -> obj.forEach { flatten(it) }
            }
        }

        flatten(arr)
        return list.toFloatArray()
    }

    /**
     * Log session input/output info for debugging
     */
    private fun logSessionInfo() {
        try {
            val session = ortSession ?: return

            Log.d(TAG, "=== ONNX Session Info ===")

            session.inputInfo.forEach { (name, info) ->
                Log.d(TAG, "Input: $name, type: ${info.info}")
            }

            session.outputInfo.forEach { (name, info) ->
                Log.d(TAG, "Output: $name, type: ${info.info}")
            }

        } catch (e: Exception) {
            Log.w(TAG, "Failed to log session info", e)
        }
    }

    /**
     * Copy model from assets to files directory
     */
    private fun copyModelToFiles(assetPath: String): File {
        val modelFile = File(context.filesDir, "model.onnx")

        // Get expected file size from assets
        val expectedSize = try {
            context.assets.openFd(assetPath).length
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine asset file size", e)
            -1L
        }

        // Check if existing file is valid
        if (modelFile.exists()) {
            val actualSize = modelFile.length()
            if (expectedSize > 0 && actualSize == expectedSize) {
                Log.d(TAG, "Model already exists and size matches at ${modelFile.absolutePath} (${actualSize} bytes)")
                return modelFile
            } else {
                Log.w(TAG, "Model file exists but size mismatch. Expected: $expectedSize, Actual: $actualSize. Re-copying...")
                modelFile.delete()
            }
        }

        try {
            Log.i(TAG, "Copying model from assets to ${modelFile.absolutePath}...")

            context.assets.open(assetPath).use { input ->
                modelFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }
                    output.flush()

                    Log.i(TAG, "Copied $totalBytes bytes to ${modelFile.absolutePath}")
                }
            }

            // Verify the copied file
            val copiedSize = modelFile.length()
            if (expectedSize > 0 && copiedSize != expectedSize) {
                Log.e(TAG, "File copy size mismatch! Expected: $expectedSize, Got: $copiedSize")
                modelFile.delete()
                throw IllegalStateException("Model file copy incomplete or corrupted")
            }

            Log.i(TAG, "Model copied and verified successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model from assets", e)
            if (modelFile.exists()) {
                modelFile.delete()
            }
            throw e
        }

        return modelFile
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            ortSession?.close()
            ortSession = null
            isInitialized = false
            Log.i(TAG, "ONNX Runtime cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}