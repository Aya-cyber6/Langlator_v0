package com.android.llanglator

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.llama.LlamaEngine
import com.android.llama.InferenceEngine
import com.android.llanglator.whisper.media.decodeWaveFile
import com.android.llanglator.whisper.recorder.Recorder
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val LOG_TAG = "MainViewModel"


class MainViewModel(
    private val app: Application
) : AndroidViewModel(app) {

    // Internal mutable state
    private val _log = MutableStateFlow("")

    // Public read-only state
    val log: StateFlow<String> = _log.asStateFlow()

    var canTranscribe by mutableStateOf(true)
        private set
    var isRecording by mutableStateOf(false)
        private set
    var downloadProgress by mutableStateOf(0)
        private set
    lateinit var engine: InferenceEngine
        private set

    private var isEngineReady = false
    private val recorder = Recorder()
    private var whisperContext: WhisperContext? = null
    private var recordedFile: File? = null

    // -----------------------
    // LOG HELPER
    // -----------------------
    private fun appendLog(text: String) {
        _log.value += if (_log.value.isEmpty()) text else "\n$text"
    }

    // -----------------------
    // INITIALIZATION
    // -----------------------
    init {
        viewModelScope.launch {

            copySampleIfNeeded()

            appendLog(WhisperContext.Companion.getSystemInfo())
            loadModel()
            canTranscribe = true
        }
    }

    suspend fun initEngine() {
        try {
            engine = withContext(Dispatchers.Default) {
                LlamaEngine.getInferenceEngine(getApplication<Application>().applicationContext)
            }

            // Optional: download model
            val modelFile = withContext(Dispatchers.IO) {
                Downloader.downloadIfNeeded(
                    getApplication(),
                    "llm",
                    "https://huggingface.co/Kam1k4dze/Llama-3.2-3B-Q4_K_M-GGUF/resolve/main/llama-3.2-3b-q4_k_m.gguf",
                    "llama-3.2-3b-q4_k_m.gguf",
                    100L * 1024 * 1024
                ) { percent ->
                    downloadProgress = percent
                }
            }

            // Load model
            withContext(Dispatchers.Default) {
                engine.loadModel(modelFile.path)
            }

            isEngineReady = true
            appendLog("Engine and model loaded ✅")
        } catch (e: Exception) {
            appendLog("Engine load failed: ${e.message}")
        }
    }

    private fun copySampleIfNeeded() {
        val outFile = File(app.filesDir, "Shahadah.wav")
        if (outFile.exists()) return

        app.assets.open("Shahadah.wav").use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
            Log.d("Whisper", "Shahadah.wav size = ${outFile.length()}")

        }
    }

    private suspend fun loadModel() = withContext(Dispatchers.IO) {
        whisperContext = WhisperContext.Companion.createContextFromAsset(
            app.assets,
            "models/ggml-base.bin"
        )
    }

    fun transcribeSample() {
        viewModelScope.launch {
            val wav = File(app.filesDir, "Shahadah.wav")
            val audio = decodeWaveFile(wav)

            // Update log in state so Compose re-renders
            val transcript = whisperContext?.transcribeData(audio) ?: ""
            translateText(transcript)
        }
    }

    fun toggleRecord() = viewModelScope.launch {
        if (isRecording) {
            recorder.stopRecording()
            isRecording = false
            recordedFile?.let {
                Log.d(LOG_TAG, "Audio data stopped Recording")

                val audio = decodeWaveFile(it)
                Log.d(LOG_TAG, "Decoded Audio data: " + audio.size)

                val transcript = whisperContext?.transcribeData(audio) ?: ""
                translateText(transcript)
                Log.d(LOG_TAG, "Audio data Trasncript: ${transcript}")

            }
        } else {
            val file = File.createTempFile("recording", ".wav", app.cacheDir)
            recorder.startRecording(file) { e ->
                appendLog(e.message ?: "Unknown error")
            }
            recordedFile = file
            isRecording = true
        }
    }

    override fun onCleared() {
        viewModelScope.launch {
            whisperContext?.release()
        }
    }

    fun translateText(userText: String) {
        if (!isEngineReady) {
            appendLog("Engine is not ready yet!")
            return
        }

        viewModelScope.launch {
            _log.value = "" // clear previous translation

            val prompt = "Translate the following into English:\n\n$userText\n\nTranslation:"

            try {
                engine.send(prompt, formatChat = false).collect { token ->
                    if (token.isNotEmpty()) appendLog(token)
                }
            } catch (e: Exception) {
                appendLog("Translation failed: ${e.message}")
            }
        }
    }


}