package com.example.nlp_final.model

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.IOException

/**
 * Model configuration loaded from model.onnx.json
 */
data class ModelConfig(
    val sampleRate: Int = 22050,
    val voice: String = "vi",
    val phonemeType: String = "espeak",
    val numSymbols: Int = 256,
    val numSpeakers: Int = 1,
    val phonemeIdMap: Map<String, Int> = emptyMap(),
    val speakerIdMap: Map<String, Int> = mapOf("default" to 0),
    val noiseScale: Float = 0.667f,
    val lengthScale: Float = 1.0f,
    val noiseW: Float = 0.8f
) {
    companion object {
        private const val TAG = "ModelConfig"

        fun loadFromAssets(context: Context, path: String = "model.onnx.json"): ModelConfig {
            return try {
                val json = context.assets.open(path).use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                }
                parseFromJson(json)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to load config from assets: $path", e)
                ModelConfig() // return defaults
            }
        }

        private fun parseFromJson(jsonString: String): ModelConfig {
            try {
                val json = JSONObject(jsonString)

                val sampleRate = json.optJSONObject("audio")?.optInt("sample_rate", 22050) ?: 22050
                val voice = json.optJSONObject("espeak")?.optString("voice", "vi") ?: "vi"
                val phonemeType = json.optString("phoneme_type", "espeak")
                val numSymbols = json.optInt("num_symbols", 256)
                val numSpeakers = json.optInt("num_speakers", 1)

                // Parse phoneme_id_map
                val phonemeIdMap = mutableMapOf<String, Int>()
                if (json.has("phoneme_id_map")) {
                    val mapObj = json.getJSONObject("phoneme_id_map")
                    val keys = mapObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        try {
                            val arr = mapObj.getJSONArray(key)
                            if (arr.length() > 0) {
                                phonemeIdMap[key] = arr.getInt(0)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse phoneme map for key: $key", e)
                        }
                    }
                }

                // Parse speaker_id_map
                val speakerIdMap = mutableMapOf<String, Int>()
                if (json.has("speaker_id_map")) {
                    val mapObj = json.getJSONObject("speaker_id_map")
                    val keys = mapObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        speakerIdMap[key] = mapObj.getInt(key)
                    }
                }

                // Parse inference defaults
                val inference = json.optJSONObject("inference")
                val noiseScale = inference?.optDouble("noise_scale", 0.667)?.toFloat() ?: 0.667f
                val lengthScale = inference?.optDouble("length_scale", 1.0)?.toFloat() ?: 1.0f
                val noiseW = inference?.optDouble("noise_w", 0.8)?.toFloat() ?: 0.8f

                Log.i(TAG, "Loaded config: sampleRate=$sampleRate, voice=$voice, phonemes=${phonemeIdMap.size}")

                return ModelConfig(
                    sampleRate = sampleRate,
                    voice = voice,
                    phonemeType = phonemeType,
                    numSymbols = numSymbols,
                    numSpeakers = numSpeakers,
                    phonemeIdMap = phonemeIdMap,
                    speakerIdMap = speakerIdMap,
                    noiseScale = noiseScale,
                    lengthScale = lengthScale,
                    noiseW = noiseW
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON config", e)
                return ModelConfig()
            }
        }
    }
}

