package com.example.nlp_final.phonemizer

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Native wrapper for espeak-ng phonemization
 * Falls back to VietnamesePhonemeMapper when native library is not available
 */
class EspeakPhonemizerNative(private val context: Context) {

    companion object {
        private const val TAG = "EspeakNative"
        private var initialized = false
        private var nativeAvailable = false

        init {
            try {
                System.loadLibrary("espeak_phonemizer")
                nativeAvailable = true
                Log.i(TAG, "Loaded espeak_phonemizer native library")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Native espeak library not available, using fallback phonemizer")
                nativeAvailable = false
            }
        }
    }

    // Fallback phonemizer for when native is not available
    private val fallbackMapper = VietnamesePhonemeMapper()

    /**
     * Initialize espeak-ng with data path
     */
    private external fun nativeInit(dataPath: String): Boolean

    /**
     * Phonemize text using espeak-ng
     */
    private external fun nativePhonemize(text: String, voice: String): String?

    /**
     * Cleanup espeak resources
     */
    private external fun nativeCleanup()

    /**
     * Initialize espeak with bundled data
     */
    fun initialize(voice: String = "vi"): Boolean {
        if (initialized) return true

        // If native library is not available, use fallback
        if (!nativeAvailable) {
            Log.i(TAG, "Using fallback Vietnamese phoneme mapper")
            initialized = true
            return true
        }

        try {
            val dataDir = File(context.filesDir, "espeak-ng-data")
            if (!dataDir.exists()) {
                Log.i(TAG, "Extracting espeak-ng-data from assets...")
                extractEspeakData(dataDir)
            }

            val success = nativeInit(dataDir.absolutePath)
            if (success) {
                initialized = true
                Log.i(TAG, "Espeak initialized successfully with voice: $voice")
            } else {
                Log.w(TAG, "Native espeak init failed, using fallback phonemizer")
                initialized = true
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception during espeak initialization, using fallback", e)
            initialized = true
            return true
        }
    }

    /**
     * Phonemize text to IPA symbols
     */
    fun phonemize(text: String, voice: String = "vi"): String {
        if (!initialized) {
            if (!initialize(voice)) {
                return ""
            }
        }

        // Try native phonemization first if available
        if (nativeAvailable) {
            try {
                val out = nativePhonemize(text, voice)
                if (!out.isNullOrBlank()) {
                    Log.d(TAG, "Native phonemization: '$text' -> '$out'")
                    return out
                }
            } catch (e: Exception) {
                Log.w(TAG, "Native phonemization failed, using fallback", e)
            }
        }

        // Use fallback Vietnamese phoneme mapper
        val result = fallbackMapper.phonemize(text)
        Log.d(TAG, "Fallback phonemization: '$text' -> '$result'")
        return result
    }

    private fun extractEspeakData(targetDir: File) {
        try {
            targetDir.mkdirs()
            copyAssetFolder("espeak-ng-data", targetDir.absolutePath)
            Log.i(TAG, "Extracted espeak-ng-data to ${targetDir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract espeak data", e)
        }
    }

    private fun copyAssetFolder(assetPath: String, targetPath: String) {
        val assets = context.assets
        try {
            val files = assets.list(assetPath) ?: arrayOf()
            if (files.isEmpty()) {
                copyAssetFile(assetPath, targetPath)
            } else {
                val targetDir = File(targetPath)
                targetDir.mkdirs()
                for (file in files) {
                    val newAssetPath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                    val newTargetPath = "$targetPath/$file"
                    copyAssetFolder(newAssetPath, newTargetPath)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying asset folder: $assetPath", e)
        }
    }

    private fun copyAssetFile(assetPath: String, targetPath: String) {
        try {
            context.assets.open(assetPath).use { input ->
                File(targetPath).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying asset file: $assetPath", e)
        }
    }

    fun cleanup() {
        if (initialized) {
            try {
                if (nativeAvailable) {
                    nativeCleanup()
                }
                initialized = false
                Log.i(TAG, "Espeak cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}

