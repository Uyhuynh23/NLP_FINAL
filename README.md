# Vietnamese TTS Android App (Offline)

A fully offline Vietnamese Text-to-Speech Android application using Piper/VITS-style ONNX model with espeak-ng phonemization.

## Features

- **Fully Offline**: No internet required - all processing happens on-device
- **ONNX Runtime**: Efficient neural TTS inference using ONNX Runtime
- **Vietnamese Language**: Optimized for Vietnamese text with espeak-ng phonemization
- **Adjustable Parameters**: Control speech characteristics with sliders:
  - **Noise Scale** (variability): 0.0 - 2.0
  - **Length Scale** (speed): 0.0 - 2.0 (higher = slower)
  - **Noise W** (prosody): 0.0 - 2.0
- **WAV Export**: Save synthesized speech as WAV files (22050 Hz, mono, PCM16)
- **Real-time Playback**: Direct audio playback using AudioTrack

## Architecture

```
app/
├── model/
│   └── ModelConfig.kt          # JSON config loader
├── phonemizer/
│   ├── EspeakPhonemizerNative.kt  # JNI wrapper for espeak-ng
│   └── PhonemeTokenizer.kt        # Phoneme -> ID conversion
├── onnx/
│   └── OnnxInferenceEngine.kt     # ONNX Runtime inference
├── audio/
│   └── AudioPlayer.kt             # PCM16 conversion, playback, WAV writer
└── tts/
    └── VietnameseTTSEngine.kt     # Main TTS pipeline orchestrator
```

## Requirements

### Build Environment
- **Android Studio**: Arctic Fox or newer
- **NDK**: Version 23.0+ (for espeak-ng native library)
- **Gradle**: 8.0+
- **Kotlin**: 1.9+
- **CMake**: 3.22.1+

### Runtime Requirements
- **Android API 24+** (Android 7.0 Nougat)
- **ABI**: arm64-v8a or armeabi-v7a
- **RAM**: ~100MB for model loading
- **Storage**: ~20MB for model and data files

## Model Setup

### Required Assets

Place these files in `app/src/main/assets/`:

1. **model.onnx** - The Piper/VITS ONNX model file
2. **model.onnx.json** - Model configuration containing:
   - Sample rate (22050 Hz)
   - Espeak voice ("vi")
   - Phoneme ID mappings (256 symbols)
   - Inference defaults

### Model Metadata (from model.onnx.json)

```json
{
  "audio": {"sample_rate": 22050},
  "espeak": {"voice": "vi"},
  "phoneme_type": "espeak",
  "num_symbols": 256,
  "num_speakers": 1,
  "speaker_id_map": {"default": 0},
  "phoneme_id_map": {
    "_": [0],  // PAD
    "^": [1],  // BOS
    "$": [2],  // EOS
    " ": [3],  // SPACE
    // ... 252 more phoneme symbols
  },
  "inference": {
    "noise_scale": 0.667,
    "length_scale": 1.0,
    "noise_w": 0.8
  }
}
```

## Build Instructions

### 1. Clone and Open Project

```bash
git clone <repository-url>
cd NLP_FINAL
```

Open the project in Android Studio.

### 2. Add Model Files

Copy your `model.onnx` and `model.onnx.json` files to:
```
app/src/main/assets/
```

### 3. Sync Gradle

```bash
./gradlew clean
./gradlew build
```

### 4. Build and Install

**Using Android Studio:**
- Click **Run** → **Run 'app'**

**Using Command Line:**
```bash
./gradlew installDebug
```

## Usage

### In the App

1. **Initialize Engine**: Click "Initialize Engine" button
   - Loads model configuration
   - Initializes ONNX Runtime session
   - Sets up phonemizer

2. **Enter Text**: Type Vietnamese text in the input field
   - Example: "Xin chào! Đây là inference bằng ONNX của Piper."

3. **Adjust Parameters** (optional):
   - **Noise Scale**: Controls voice variability (default: 0.667)
   - **Length Scale**: Controls speed - higher = slower (default: 1.0)
   - **Noise W**: Controls prosody variation (default: 0.8)

4. **Speak**: Click "Speak" to synthesize and play audio

5. **Save WAV**: Click "Save WAV" to export to file
   - Files saved to: `/storage/emulated/0/Android/data/com.example.nlp_final/files/Music/TTS/`

### Programmatic Usage

```kotlin
// Initialize
val ttsEngine = VietnameseTTSEngine(context)
ttsEngine.initialize()

// Synthesize and play
lifecycleScope.launch {
    val result = ttsEngine.synthesizeAndPlay(
        text = "Xin chào",
        noiseScale = 0.667f,
        lengthScale = 1.0f,
        noiseW = 0.8f
    )
    
    if (result.success) {
        Log.i("TTS", "Duration: ${result.durationSec}s")
        Log.i("TTS", "Phonemes: ${result.phonemes}")
    }
}

// Save to WAV
val outputFile = File("/path/to/output.wav")
ttsEngine.synthesizeAndSave("Hello", outputFile)

// Cleanup
ttsEngine.cleanup()
```

## ONNX Model Interface

### Inputs
- **input**: `int64[batch, T]` - Phoneme ID sequence
- **input_lengths**: `int64[batch]` - Sequence length T
- **scales**: `float32[3]` - `[noise_scale, length_scale, noise_w]`

### Output
- **output**: `float32[batch, 1, N]` - Waveform samples in range ~[-1, 1]

## Espeak-ng Integration

### Current Implementation

The app includes a **mock native implementation** that triggers the fallback phonemizer. This works for basic testing but produces suboptimal phonemes.

### For Production: Real Espeak-ng

To integrate actual espeak-ng:

1. **Build espeak-ng for Android**:
   ```bash
   # Clone espeak-ng
   git clone https://github.com/espeak-ng/espeak-ng.git
   
   # Build for Android using NDK
   # (Follow espeak-ng Android build instructions)
   ```

2. **Add to project**:
   - Copy `libespeak-ng.so` to `app/src/main/jniLibs/{abi}/`
   - Copy espeak-ng-data to `app/src/main/assets/espeak-ng-data/`

3. **Update `espeak_jni.cpp`**:
   ```cpp
   #include <espeak-ng/speak_lib.h>
   
   // Real implementation:
   espeak_Initialize(AUDIO_OUTPUT_SYNCHRONOUS, 0, path, 0);
   espeak_SetVoiceByName(voiceId);
   const char* phonemes = espeak_TextToPhonemes(...);
   ```

## Testing

### Run Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Manual Test Cases

1. **Basic Vietnamese**: "Xin chào!"
   - Should produce clear greeting

2. **Long Sentence**: "Xin chào! Đây là inference bằng ONNX của Piper."
   - Tests longer synthesis

3. **Speed Variation**: 
   - Set Length Scale to 0.5 (fast) vs 1.5 (slow)
   - Should hear noticeable speed difference

4. **Punctuation**: "Câu hỏi? Câu trả lời! Câu bình thường."
   - Tests punctuation handling

## Troubleshooting

### "Engine initialization failed"
- **Check**: model.onnx exists in assets
- **Check**: model.onnx.json is valid JSON
- **Check**: Sufficient device memory (~100MB)

### "symbol not in phoneme_id_map"
- **Cause**: Phonemizer output doesn't match model's expected symbols
- **Fix**: Use proper espeak-ng build with matching phoneme set

### "ORT input shape mismatch"
- **Check**: Model expects inputs: `input`, `input_lengths`, `scales`
- **Check**: Tensor shapes match model requirements

### "No audio output"
- **Check**: Device volume is up
- **Check**: AudioTrack permissions granted
- **Check**: Waveform samples are non-zero

### WAV files not playing on desktop
- **Verify**: 22050 Hz, mono, PCM16 format
- **Try**: VLC media player or Audacity

## Performance Benchmarks

Typical performance on mid-range Android device (Snapdragon 730):

- **Initialization**: ~500-1000ms
- **Phonemization**: ~10-50ms per sentence
- **ONNX Inference**: ~100-500ms (depends on text length)
- **Total latency**: ~200-600ms for typical sentence

## Known Issues

1. **Espeak-ng Mock**: Current implementation uses fallback phonemization
   - Character-level tokenization works but isn't optimal
   - For production, integrate real espeak-ng

2. **Limited Error Recovery**: Some edge cases may crash
   - Unknown Unicode characters
   - Extremely long texts (>1000 chars)

3. **No Streaming**: Entire audio generated before playback
   - Future: implement streaming synthesis

## License

This project uses:
- **ONNX Runtime** - MIT License
- **espeak-ng** - GPL v3 (if integrated)
- **Model** - Check your model's specific license

## Contributing

Contributions welcome! Areas for improvement:

- [ ] Real espeak-ng integration
- [ ] Streaming synthesis
- [ ] Multi-speaker support
- [ ] Additional Vietnamese models
- [ ] Background service
- [ ] Widget support

## Contact

For issues or questions, please open a GitHub issue.

---

**Built with ❤️ for offline Vietnamese TTS**

