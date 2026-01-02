# Quick Start Guide - Vietnamese TTS App

## Immediate Setup (5 minutes)

### Step 1: Verify Model Files
Ensure these files exist in `app/src/main/assets/`:
- ✓ `model.onnx` (your Piper VITS model)
- ✓ `model.onnx.json` (provided configuration)

### Step 2: Build the Project

Open terminal in project root and run:

```bash
# Sync Gradle dependencies
./gradlew clean

# Build the app
./gradlew assembleDebug
```

**OR** use Android Studio:
- Open project in Android Studio
- Wait for Gradle sync to complete
- Click **Build** → **Make Project**

### Step 3: Run on Device

**Requirements:**
- Android device with API 24+ (Android 7.0+)
- USB debugging enabled

**Install:**
```bash
./gradlew installDebug
```

**OR** in Android Studio:
- Click **Run** → **Run 'app'**

## First Use

1. Open the app
2. Click **"Initialize Engine"** button (takes ~1 second)
3. Default text will be loaded: "Xin chào! Đây là inference bằng ONNX của Piper."
4. Click **"Speak"** to hear the synthesized speech
5. Adjust sliders to change voice characteristics
6. Click **"Save WAV"** to export audio file

## Testing the Implementation

### Verify Each Component

**1. Model Loading:**
```
Expected log: "ONNX Runtime initialized successfully"
Expected UI: "Engine initialized successfully!"
```

**2. Phonemization:**
```
Check logs for: "Phonemes: [phoneme string]"
Token count should be > 0
```

**3. Inference:**
```
Expected log: "Inference successful: XXXXX samples"
Check details view shows waveform sample count
```

**4. Audio Output:**
```
Should hear Vietnamese speech
Duration shown in UI (e.g., "2.34s")
```

### Run Automated Tests

```bash
# Connect Android device via USB
./gradlew connectedAndroidTest

# Check test results
# Look for: TTSInstrumentedTest results
```

## Troubleshooting First Run

### Issue: "Failed to load ONNX model"
**Solution:**
- Verify `model.onnx` is in `app/src/main/assets/`
- Check file size > 0 bytes
- Rebuild: `./gradlew clean assembleDebug`

### Issue: "Phoneme map is empty"
**Solution:**
- Verify `model.onnx.json` has valid JSON
- Check `phoneme_id_map` field exists
- File should be ~25-50KB with 256 phoneme mappings

### Issue: "No audio output"
**Solution:**
- Check device volume
- Verify AudioTrack permissions
- Look in logs for "Playback completed" message

### Issue: Native library errors
**Solution:**
- The espeak native library uses a fallback mock
- This is expected - app will use character-level phonemization
- Check logs for: "Loaded espeak_phonemizer native library"

## Parameter Guide

### Noise Scale (Variability)
- **0.0 - 0.5**: Very consistent, robotic
- **0.667** (default): Natural variation
- **1.0 - 2.0**: High variation, expressive

### Length Scale (Speed)
- **0.5**: 2x faster (may sound rushed)
- **1.0** (default): Normal speed
- **1.5**: 1.5x slower (clearer for learning)

### Noise W (Prosody)
- **0.0 - 0.5**: Flat intonation
- **0.8** (default): Natural prosody
- **1.0 - 2.0**: Exaggerated intonation

## Sample Test Inputs

### Basic Test
```
Xin chào!
```

### Full Sentence Test
```
Xin chào! Đây là inference bằng ONNX của Piper.
```

### Punctuation Test
```
Câu hỏi? Câu trả lời! Câu bình thường.
```

### Numbers Test
```
Một, hai, ba, bốn, năm.
```

### Long Text Test
```
Việt Nam là một quốc gia nằm ở phía Đông bán đảo Đông Dương thuộc khu vực Đông Nam Á.
```

## Expected Output Locations

### WAV Files
```
/storage/emulated/0/Android/data/com.example.nlp_final/files/Music/TTS/
```

Files named: `tts_YYYYMMDD_HHMMSS.wav`

### Log Files
Use Android Studio Logcat with filters:
- `MainActivity`
- `VietnameseTTS`
- `OnnxInference`
- `AudioPlayer`

## Performance Expectations

### Typical Timings (Mid-range Device)
- **Initialization**: 500-1000ms (one-time)
- **Phonemization**: 10-50ms per sentence
- **ONNX Inference**: 100-500ms (varies with length)
- **Total Speak Time**: 200-600ms for typical sentence

### Memory Usage
- **App startup**: ~50MB
- **After init**: ~150MB
- **During inference**: ~200MB peak

## Next Steps

### For Better Vietnamese Phonemization

The current implementation uses a fallback phonemizer. For production quality:

1. **Build real espeak-ng** for Android
2. **Add Vietnamese voice data** from espeak-ng
3. **Replace mock JNI** in `espeak_jni.cpp`

See README.md "Espeak-ng Integration" section for details.

### For Production Deployment

- [ ] Add proper error handling UI
- [ ] Implement permission requests for storage
- [ ] Add progress indicators for long texts
- [ ] Optimize model loading time
- [ ] Add caching for repeated phrases
- [ ] Implement background service for TTS

## Support

If you encounter issues:
1. Check Logcat for error messages
2. Verify model files are present
3. Test on physical device (not emulator)
4. Check minimum Android version (API 24+)

---

**Ready to synthesize!** 🎤

