package com.android.llanglator.whisper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.llanglator.whisper.media.decodeWaveFile
import com.android.llanglator.whisper.recorder.Recorder
import kotlinx.coroutines.launch
import java.io.File
import com.whispercpp.whisper.WhisperContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WhisperViewModel(
    private val app: Application
) : AndroidViewModel(app) {

    var log by mutableStateOf("")  // ← use Compose state
        private set
    var canTranscribe by mutableStateOf(true)
        private set
    var isRecording by mutableStateOf(false)
        private set

    private val recorder = Recorder()
    private var whisperContext: WhisperContext? = null
    private var recordedFile: File? = null

    init {
        viewModelScope.launch {

            copySampleIfNeeded()

            log += WhisperContext.getSystemInfo() + "\n"
            loadModel()
            canTranscribe = true
        }
    }

    private fun copySampleIfNeeded() {
        val outFile = File(app.filesDir, "sample.wav")
        if (outFile.exists()) return

        app.assets.open("sample.wav").use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private suspend fun loadModel() = withContext(Dispatchers.IO) {
        whisperContext = WhisperContext.createContextFromAsset(
            app.assets,
            "models/ggml-base.en.bin"
        )
    }

    fun appendLog(text: String) {
        log += "\n$text"
    }
    fun transcribeSample() {
        viewModelScope.launch {
            val wav = File(app.filesDir, "sample.wav")
            val audio = decodeWaveFile(wav)

            // Update log in state so Compose re-renders
            val transcript = whisperContext?.transcribeData(audio) ?: ""
            appendLog(transcript)
        }
    }

    fun toggleRecord() = viewModelScope.launch {
        if (isRecording) {
            recorder.stopRecording()
            isRecording = false
            recordedFile?.let {
                val audio = decodeWaveFile(it)
                log += whisperContext?.transcribeData(audio) ?: ""
            }
        } else {
            val file = File.createTempFile("recording", ".wav", app.cacheDir)
            recorder.startRecording(file) { e ->
                log += e.message + "\n"
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
}
