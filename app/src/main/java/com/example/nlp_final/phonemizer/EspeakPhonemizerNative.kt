package com.example.nlp_final.phonemizer

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Native wrapper for espeak-ng phonemization
 */
class EspeakPhonemizerNative(private val context: Context) {

    companion object {
        private const val TAG = "EspeakNative"
        private var initialized = false

        init {
            try {
                System.loadLibrary("espeak_phonemizer")
                Log.i(TAG, "Loaded espeak_phonemizer native library")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load espeak_phonemizer library", e)
            }
        }
    }

    /**
     * Initialize espeak-ng with data path
     * @param dataPath path to espeak-ng-data directory
     * @return true if successful
     */
    private external fun nativeInit(dataPath: String): Boolean

    /**
     * Phonemize text using espeak-ng
     * @param text input text
     * @param voice voice identifier (e.g., "vi")
     * @return phoneme string or null on error
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

        try {
            // Extract espeak-ng-data from assets if needed
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
                Log.e(TAG, "Failed to initialize espeak")
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Exception during espeak initialization", e)
            return false
        }
    }

    /**
     * Phonemize text to IPA symbols
     */
    fun phonemize(text: String, voice: String = "vi"): String {
        if (!initialized) {
            if (!initialize(voice)) {
                Log.w(TAG, "Espeak not initialized, returning fallback")
                return fallbackPhonemize(text)
            }
        }

        return try {
            nativePhonemize(text, voice) ?: fallbackPhonemize(text)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during phonemization", e)
            fallbackPhonemize(text)
        }
    }

    /**
     * Simple fallback phonemizer (character-level)
     */
    private fun fallbackPhonemize(text: String): String {
        // For Vietnamese, this is a very naive fallback
        // Real espeak would produce proper IPA, but this gives us something
        return text.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Extract espeak-ng-data from assets
     */
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
                // It's a file
                copyAssetFile(assetPath, targetPath)
            } else {
                // It's a folder
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
                nativeCleanup()
                initialized = false
                Log.i(TAG, "Espeak cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}

