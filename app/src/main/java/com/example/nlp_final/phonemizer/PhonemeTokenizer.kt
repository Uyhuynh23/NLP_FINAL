package com.example.nlp_final.phonemizer

import android.util.Log
import com.example.nlp_final.model.ModelConfig

/**
 * Converts phoneme string to phoneme IDs using the model's phoneme_id_map
 */
class PhonemeTokenizer(private val config: ModelConfig) {

    companion object {
        private const val TAG = "PhonemeTokenizer"

        // Special tokens
        const val PAD = "_"
        const val BOS = "^"
        const val EOS = "$"
        const val SPACE = " "
    }

    private val phonemeToId = config.phonemeIdMap
    private val padId = phonemeToId[PAD] ?: 0
    private val bosId = phonemeToId[BOS] ?: 1
    private val eosId = phonemeToId[EOS] ?: 2
    private val spaceId = phonemeToId[SPACE] ?: 3

    /**
     * Convert phoneme string to list of phoneme IDs
     * @param phonemes phoneme string from espeak
     * @param addBosEos whether to add BOS/EOS tokens
     * @return list of phoneme IDs
     */
    fun encode(phonemes: String, addBosEos: Boolean = true): LongArray {
        val ids = mutableListOf<Long>()

        if (addBosEos && phonemeToId.containsKey(BOS)) {
            ids.add(bosId.toLong())
        }

        // Parse phoneme string into symbols
        val symbols = parsePhonemeSymbols(phonemes)

        for (symbol in symbols) {
            val id = phonemeToId[symbol]
            if (id != null) {
                ids.add(id.toLong())
            } else {
                // Unknown symbol - try space as fallback
                Log.w(TAG, "Unknown phoneme symbol: '$symbol', using space")
                ids.add(spaceId.toLong())
            }
        }

        if (addBosEos && phonemeToId.containsKey(EOS)) {
            ids.add(eosId.toLong())
        }

        if (ids.isEmpty()) {
            // Fallback: at least return space
            ids.add(spaceId.toLong())
        }

        Log.d(TAG, "Encoded ${symbols.size} phonemes to ${ids.size} IDs")
        return ids.toLongArray()
    }

    /**
     * Parse phoneme string into individual symbols
     * Handles multi-character IPA symbols and diacritics
     */
    private fun parsePhonemeSymbols(phonemes: String): List<String> {
        val symbols = mutableListOf<String>()
        var i = 0

        while (i < phonemes.length) {
            // Try to match longest symbol first (up to 3 chars for IPA + diacritics)
            var matched = false
            for (length in 3 downTo 1) {
                if (i + length <= phonemes.length) {
                    val candidate = phonemes.substring(i, i + length)
                    if (phonemeToId.containsKey(candidate)) {
                        symbols.add(candidate)
                        i += length
                        matched = true
                        break
                    }
                }
            }

            if (!matched) {
                // Single character fallback
                val char = phonemes[i].toString()
                symbols.add(char)
                i++
            }
        }

        return symbols
    }

    /**
     * Get phoneme ID for a specific symbol (for debugging)
     */
    fun getPhonemeId(symbol: String): Int? = phonemeToId[symbol]

    /**
     * Check if a symbol exists in the phoneme map
     */
    fun hasSymbol(symbol: String): Boolean = phonemeToId.containsKey(symbol)
}

