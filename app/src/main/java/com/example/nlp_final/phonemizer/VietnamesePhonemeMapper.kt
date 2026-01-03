package com.example.nlp_final.phonemizer

import android.util.Log

/**
 * Vietnamese text to IPA phoneme converter
 * Maps Vietnamese orthography to IPA symbols compatible with Piper/VITS model
 *
 * Reference: Vietnamese phonology and espeak-ng vi voice mapping
 */
class VietnamesePhonemeMapper {

    companion object {
        private const val TAG = "ViPhonemeMapper"

        // Vietnamese consonant initials to IPA
        private val CONSONANT_INITIALS = mapOf(
            "ngh" to "ŋ",
            "nh" to "ɲ",
            "ng" to "ŋ",
            "gh" to "ɣ",
            "gi" to "z",
            "ch" to "c",
            "th" to "tʰ",
            "tr" to "ʈ",
            "ph" to "f",
            "kh" to "x",
            "qu" to "kw",
            "b" to "ɓ",
            "c" to "k",
            "d" to "z",
            "đ" to "ɗ",
            "g" to "ɣ",
            "h" to "h",
            "k" to "k",
            "l" to "l",
            "m" to "m",
            "n" to "n",
            "p" to "p",
            "r" to "z",
            "s" to "s",
            "t" to "t",
            "v" to "v",
            "x" to "s"
        )

        // Vietnamese vowels (monophthongs) to IPA
        private val VOWELS = mapOf(
            // a variants
            "a" to "aː",
            "à" to "aː",
            "á" to "aː",
            "ả" to "aː",
            "ã" to "aː",
            "ạ" to "aː",
            "ă" to "a",
            "ằ" to "a",
            "ắ" to "a",
            "ẳ" to "a",
            "ẵ" to "a",
            "ặ" to "a",
            "â" to "ɤ",
            "ầ" to "ɤ",
            "ấ" to "ɤ",
            "ẩ" to "ɤ",
            "ẫ" to "ɤ",
            "ậ" to "ɤ",

            // e variants
            "e" to "ɛ",
            "è" to "ɛ",
            "é" to "ɛ",
            "ẻ" to "ɛ",
            "ẽ" to "ɛ",
            "ẹ" to "ɛ",
            "ê" to "e",
            "ề" to "e",
            "ế" to "e",
            "ể" to "e",
            "ễ" to "e",
            "ệ" to "e",

            // i variants
            "i" to "i",
            "ì" to "i",
            "í" to "i",
            "ỉ" to "i",
            "ĩ" to "i",
            "ị" to "i",
            "y" to "i",
            "ỳ" to "i",
            "ý" to "i",
            "ỷ" to "i",
            "ỹ" to "i",
            "ỵ" to "i",

            // o variants
            "o" to "ɔ",
            "ò" to "ɔ",
            "ó" to "ɔ",
            "ỏ" to "ɔ",
            "õ" to "ɔ",
            "ọ" to "ɔ",
            "ô" to "o",
            "ồ" to "o",
            "ố" to "o",
            "ổ" to "o",
            "ỗ" to "o",
            "ộ" to "o",
            "ơ" to "ɤː",
            "ờ" to "ɤː",
            "ớ" to "ɤː",
            "ở" to "ɤː",
            "ỡ" to "ɤː",
            "ợ" to "ɤː",

            // u variants
            "u" to "u",
            "ù" to "u",
            "ú" to "u",
            "ủ" to "u",
            "ũ" to "u",
            "ụ" to "u",
            "ư" to "ɯ",
            "ừ" to "ɯ",
            "ứ" to "ɯ",
            "ử" to "ɯ",
            "ữ" to "ɯ",
            "ự" to "ɯ"
        )

        // Vietnamese final consonants to IPA
        private val FINAL_CONSONANTS = mapOf(
            "ch" to "c",
            "nh" to "ɲ",
            "ng" to "ŋ",
            "c" to "k",
            "m" to "m",
            "n" to "n",
            "p" to "p",
            "t" to "t"
        )

        // Common Vietnamese diphthongs/triphthongs
        private val DIPHTHONGS = mapOf(
            "ai" to "aːj",
            "ao" to "aːw",
            "au" to "aw",
            "ay" to "aj",
            "âu" to "ɤw",
            "ây" to "ɤj",
            "eo" to "ɛw",
            "êu" to "ew",
            "ia" to "iə",
            "iê" to "iə",
            "iu" to "iw",
            "oa" to "waː",
            "oă" to "wa",
            "oe" to "wɛ",
            "oi" to "ɔj",
            "oo" to "ɔː",
            "ôi" to "oj",
            "ơi" to "ɤːj",
            "ua" to "uə",
            "uâ" to "uə",
            "uê" to "we",
            "ui" to "uj",
            "uo" to "uə",
            "uô" to "uə",
            "ươ" to "ɯə",
            "ưa" to "ɯə",
            "ưi" to "ɯj",
            "ưu" to "ɯw",
            "uy" to "wi",
            "yê" to "iə"
        )

        // Punctuation mapping
        private val PUNCTUATION = mapOf(
            "." to ".",
            "," to ",",
            "!" to "!",
            "?" to "?",
            ":" to ":",
            ";" to ";",
            "-" to "-",
            "'" to "'",
            "\"" to "\"",
            "(" to "(",
            ")" to ")"
        )
    }

    /**
     * Convert Vietnamese text to IPA phonemes
     */
    fun phonemize(text: String): String {
        val normalized = normalizeText(text)
        val words = normalized.split(Regex("\\s+"))
        val phonemeWords = mutableListOf<String>()

        for (word in words) {
            if (word.isBlank()) continue

            val phonemes = phonemizeWord(word.trim())
            if (phonemes.isNotEmpty()) {
                phonemeWords.add(phonemes)
            }
        }

        val result = phonemeWords.joinToString(" ")
        Log.d(TAG, "Phonemized: '$text' -> '$result'")
        return result
    }

    /**
     * Normalize Vietnamese text
     */
    private fun normalizeText(text: String): String {
        return text
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Convert a single Vietnamese word/syllable to IPA
     */
    private fun phonemizeWord(word: String): String {
        // Check if it's punctuation
        if (word.length == 1 && PUNCTUATION.containsKey(word)) {
            return PUNCTUATION[word] ?: ""
        }

        // Handle mixed punctuation at end
        var cleanWord = word
        var trailingPunct = ""
        while (cleanWord.isNotEmpty() && PUNCTUATION.containsKey(cleanWord.last().toString())) {
            trailingPunct = PUNCTUATION[cleanWord.last().toString()]!! + trailingPunct
            cleanWord = cleanWord.dropLast(1)
        }

        if (cleanWord.isEmpty()) {
            return trailingPunct
        }

        val result = StringBuilder()
        var pos = 0

        // Try to match initial consonant
        for (len in 3 downTo 1) {
            if (pos + len <= cleanWord.length) {
                val candidate = cleanWord.substring(pos, pos + len)
                if (CONSONANT_INITIALS.containsKey(candidate)) {
                    result.append(CONSONANT_INITIALS[candidate])
                    pos += len
                    break
                }
            }
        }

        // Process remaining characters (vowels and finals)
        while (pos < cleanWord.length) {
            var matched = false

            // Try diphthongs first (2-3 chars)
            for (len in 3 downTo 2) {
                if (pos + len <= cleanWord.length) {
                    val candidate = cleanWord.substring(pos, pos + len)
                    // Normalize diacritics for diphthong matching
                    val normalized = normalizeDiphthong(candidate)
                    if (DIPHTHONGS.containsKey(normalized)) {
                        result.append(DIPHTHONGS[normalized])
                        pos += len
                        matched = true
                        break
                    }
                }
            }

            if (!matched) {
                // Try single vowel
                val char = cleanWord[pos].toString()
                if (VOWELS.containsKey(char)) {
                    result.append(VOWELS[char])
                    pos++
                    matched = true
                }
            }

            if (!matched) {
                // Try final consonant
                for (len in 2 downTo 1) {
                    if (pos + len <= cleanWord.length) {
                        val candidate = cleanWord.substring(pos, pos + len)
                        if (FINAL_CONSONANTS.containsKey(candidate)) {
                            result.append(FINAL_CONSONANTS[candidate])
                            pos += len
                            matched = true
                            break
                        }
                    }
                }
            }

            if (!matched) {
                // Unknown character - try to pass through if it's a basic letter
                val char = cleanWord[pos]
                if (char.isLetter()) {
                    // Use character as-is (model might have it)
                    result.append(char)
                }
                pos++
            }
        }

        return result.toString() + trailingPunct
    }

    /**
     * Normalize diphthong by removing tone marks for matching
     */
    private fun normalizeDiphthong(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            sb.append(removeTone(c))
        }
        return sb.toString()
    }

    /**
     * Remove tone mark from a Vietnamese vowel
     */
    private fun removeTone(c: Char): Char {
        return when (c) {
            'à', 'á', 'ả', 'ã', 'ạ' -> 'a'
            'ằ', 'ắ', 'ẳ', 'ẵ', 'ặ' -> 'ă'
            'ầ', 'ấ', 'ẩ', 'ẫ', 'ậ' -> 'â'
            'è', 'é', 'ẻ', 'ẽ', 'ẹ' -> 'e'
            'ề', 'ế', 'ể', 'ễ', 'ệ' -> 'ê'
            'ì', 'í', 'ỉ', 'ĩ', 'ị' -> 'i'
            'ò', 'ó', 'ỏ', 'õ', 'ọ' -> 'o'
            'ồ', 'ố', 'ổ', 'ỗ', 'ộ' -> 'ô'
            'ờ', 'ớ', 'ở', 'ỡ', 'ợ' -> 'ơ'
            'ù', 'ú', 'ủ', 'ũ', 'ụ' -> 'u'
            'ừ', 'ứ', 'ử', 'ữ', 'ự' -> 'ư'
            'ỳ', 'ý', 'ỷ', 'ỹ', 'ỵ' -> 'y'
            else -> c
        }
    }
}

