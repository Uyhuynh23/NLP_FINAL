package com.example.nlp_final

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.nlp_final.tts.VietnameseTTSEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var ttsEngine: VietnameseTTSEngine
    private var currentJob: Job? = null

    // UI elements
    private lateinit var inputText: EditText
    private lateinit var btnInitialize: Button
    private lateinit var btnSpeak: Button
    private lateinit var btnSaveWav: Button
    private lateinit var btnStop: Button
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var progress: ProgressBar

    // Sliders
    private lateinit var sliderNoiseScale: SeekBar
    private lateinit var sliderLengthScale: SeekBar
    private lateinit var sliderNoiseW: SeekBar
    private lateinit var labelNoiseScale: TextView
    private lateinit var labelLengthScale: TextView
    private lateinit var labelNoiseW: TextView

    // Current scale values
    private var noiseScale = 0.667f
    private var lengthScale = 1.0f
    private var noiseW = 0.8f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize TTS engine
        ttsEngine = VietnameseTTSEngine(this)

        // Initialize UI elements
        initializeUI()
        setupListeners()
    }

    private fun initializeUI() {
        inputText = findViewById(R.id.input_text)
        btnInitialize = findViewById(R.id.btn_initialize)
        btnSpeak = findViewById(R.id.btn_speak)
        btnSaveWav = findViewById(R.id.btn_save_wav)
        btnStop = findViewById(R.id.btn_stop)
        status = findViewById(R.id.status)
        details = findViewById(R.id.details)
        progress = findViewById(R.id.progress)

        sliderNoiseScale = findViewById(R.id.slider_noise_scale)
        sliderLengthScale = findViewById(R.id.slider_length_scale)
        sliderNoiseW = findViewById(R.id.slider_noise_w)
        labelNoiseScale = findViewById(R.id.label_noise_scale)
        labelLengthScale = findViewById(R.id.label_length_scale)
        labelNoiseW = findViewById(R.id.label_noise_w)

        // Disable speak/save buttons until initialized
        btnSpeak.isEnabled = false
        btnSaveWav.isEnabled = false
    }

    private fun setupListeners() {
        btnInitialize.setOnClickListener {
            initializeEngine()
        }

        btnSpeak.setOnClickListener {
            val text = inputText.text.toString().trim()
            if (text.isEmpty()) {
                status.text = "Please enter text to speak"
                return@setOnClickListener
            }
            speakText(text)
        }

        btnSaveWav.setOnClickListener {
            val text = inputText.text.toString().trim()
            if (text.isEmpty()) {
                status.text = "Please enter text to save"
                return@setOnClickListener
            }
            saveWav(text)
        }

        btnStop.setOnClickListener {
            stopSynthesis()
        }

        // Slider listeners
        sliderNoiseScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                noiseScale = progress / 100f
                labelNoiseScale.text = "Noise Scale (variability): ${String.format("%.3f", noiseScale)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderLengthScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lengthScale = progress / 100f
                labelLengthScale.text = "Length Scale (speed): ${String.format("%.3f", lengthScale)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sliderNoiseW.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                noiseW = progress / 100f
                labelNoiseW.text = "Noise W (prosody): ${String.format("%.3f", noiseW)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initializeEngine() {
        currentJob?.cancel()
        currentJob = lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            status.text = "Initializing TTS engine..."
            btnInitialize.isEnabled = false

            val success = withContext(Dispatchers.IO) {
                ttsEngine.initialize()
            }

            progress.visibility = View.GONE

            if (success) {
                val config = ttsEngine.getConfig()
                status.text = "Engine initialized successfully!"
                details.text = """
                    Sample Rate: ${config.sampleRate} Hz
                    Voice: ${config.voice}
                    Phoneme Type: ${config.phonemeType}
                    Phoneme Map Size: ${config.phonemeIdMap.size}
                    Default Scales: noise=${config.noiseScale}, length=${config.lengthScale}, noiseW=${config.noiseW}
                """.trimIndent()

                btnSpeak.isEnabled = true
                btnSaveWav.isEnabled = true
                btnInitialize.text = "Re-initialize"
            } else {
                status.text = "Failed to initialize engine. Check logs."
                details.text = "Error: Could not load ONNX model or phonemizer."
            }

            btnInitialize.isEnabled = true
        }
    }

    private fun speakText(text: String) {
        currentJob?.cancel()
        currentJob = lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            status.text = "Synthesizing speech..."
            btnSpeak.isEnabled = false
            btnSaveWav.isEnabled = false

            val result = ttsEngine.synthesizeAndPlay(text, noiseScale, lengthScale, noiseW)

            progress.visibility = View.GONE
            btnSpeak.isEnabled = true
            btnSaveWav.isEnabled = true

            if (result.success) {
                status.text = "✓ Speech synthesis completed"
                details.text = """
                    Phonemes: ${result.phonemes.take(100)}${if (result.phonemes.length > 100) "..." else ""}
                    Phoneme length: ${result.phonemeCount}
                    Token count: ${result.tokenCount}
                    Audio duration: ${String.format("%.2f", result.durationSec)}s
                    Inference time: ${result.inferenceTimeMs}ms
                    Waveform samples: ${result.waveform?.size ?: 0}
                """.trimIndent()
            } else {
                status.text = "✗ Synthesis failed: ${result.errorMessage}"
                details.text = if (result.phonemes.isNotEmpty()) {
                    "Phonemes generated: ${result.phonemes}\nTokens: ${result.tokenCount}"
                } else {
                    "No phonemes generated"
                }
            }
        }
    }

    private fun saveWav(text: String) {
        currentJob?.cancel()
        currentJob = lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            status.text = "Synthesizing and saving WAV..."
            btnSpeak.isEnabled = false
            btnSaveWav.isEnabled = false

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "TTS")
            outputDir.mkdirs()
            val outputFile = File(outputDir, "tts_${timestamp}.wav")

            val result = withContext(Dispatchers.IO) {
                ttsEngine.synthesizeAndSave(text, outputFile, noiseScale, lengthScale, noiseW)
            }

            progress.visibility = View.GONE
            btnSpeak.isEnabled = true
            btnSaveWav.isEnabled = true

            if (result.success) {
                status.text = "✓ WAV saved: ${outputFile.name}"
                details.text = """
                    File: ${outputFile.absolutePath}
                    Size: ${outputFile.length() / 1024} KB
                    Duration: ${String.format("%.2f", result.durationSec)}s
                    Sample rate: ${ttsEngine.getConfig().sampleRate} Hz
                    Inference time: ${result.inferenceTimeMs}ms
                """.trimIndent()

                Log.i(TAG, "WAV file saved: ${outputFile.absolutePath}")
            } else {
                status.text = "✗ Failed to save WAV: ${result.errorMessage}"
            }
        }
    }

    private fun stopSynthesis() {
        currentJob?.cancel()
        ttsEngine.stopPlayback()
        progress.visibility = View.GONE
        status.text = "Stopped"
        btnSpeak.isEnabled = true
        btnSaveWav.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        currentJob?.cancel()
        ttsEngine.cleanup()
    }
}