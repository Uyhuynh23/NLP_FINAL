package com.example.nlp_final.phonemizer

import android.content.Context
import android.util.Log

/**
 * Simple fallback phonemizer for Vietnamese
 * Used when espeak-ng native library is not available
 */
class SimplePhonemizer(private val context: Context) {

    companion object {
        private const val TAG = "SimplePhonemizer"
    }

    /**
     * Simple character-level phonemization for Vietnamese
     * This is a basic fallback - real espeak-ng would produce proper IPA
     */
    fun phonemize(text: String, voice: String = "vi"): String {
        // Normalize text
        val normalized = text
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

        // For Vietnamese, we'll do simple character-level with space preservation
        // In a real implementation, espeak would convert to IPA phonemes
        val result = StringBuilder()

        for (char in normalized) {
            when {
                char.isWhitespace() -> result.append(' ')
                char.isLetterOrDigit() -> result.append(char)
                char in ".,!?;:\"'-" -> result.append(char)
                else -> {
                    // Skip unknown characters
                    Log.w(TAG, "Skipping unknown character: $char")
                }
            }
        }

        val phonemes = result.toString().trim()
        Log.d(TAG, "Fallback phonemization: '$text' -> '$phonemes'")

        return phonemes
    }
}

