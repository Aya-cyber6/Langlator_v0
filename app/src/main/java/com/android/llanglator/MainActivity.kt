package com.android.llanglator

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.aichat.AiChat
import com.android.aichat.InferenceEngine
import com.android.aichat.gguf.GgufMetadata
import com.android.aichat.gguf.GgufMetadataReader
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import android.content.Intent
import android.widget.Button
class MainActivity : AppCompatActivity() {

    // Android views
    private lateinit var ggufTv: TextView
    private lateinit var messagesRv: RecyclerView
    private lateinit var userInputEt: EditText
    private lateinit var userActionFab: FloatingActionButton

    // Arm AI Chat inference engine
    private lateinit var engine: InferenceEngine
    private var generationJob: Job? = null

    // Conversation states
    private var isModelReady = false
    private val messages = mutableListOf<Message>()
    private val lastAssistantMsg = StringBuilder()
    private val messageAdapter = MessageAdapter(messages)
    private lateinit var modelFile: File
    private val systemPrompt = """
        "Translate the following into English.\n" +
                "Only output the translation.\n\n" +
                userMessage + "\n\n" +
                "Translation:";
""".trimIndent()

    val MODEL_URL: String =
        "https://huggingface.co/Kam1k4dze/Llama-3.2-3B-Q4_K_M-GGUF/resolve/main/llama-3.2-3b-q4_k_m.gguf"
    val LLM_MODEL_NAME: String = "llama-3.2-3b-q4_k_m.gguf"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnOpenWhisper).setOnClickListener {
            startActivity(Intent(this, com.android.llanglator.whisper.WhisperActivity::class.java))
        }
        // View model boilerplate and state management is out of this basic sample's scope
        onBackPressedDispatcher.addCallback { Log.w(TAG, "Ignore back press for simplicity") }

        // Find views
        ggufTv = findViewById(R.id.gguf)
        messagesRv = findViewById(R.id.messages)
        messagesRv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        messagesRv.adapter = messageAdapter
        userInputEt = findViewById(R.id.user_input)
        userActionFab = findViewById(R.id.fab)

        // Arm AI Chat initialization
        lifecycleScope.launch(Dispatchers.Default) {
            // 1️⃣ Initialize engine FIRST
            engine = AiChat.getInferenceEngine(applicationContext)

            // 2️⃣ Download model (IO thread)
            modelFile = withContext(Dispatchers.IO) {
                Downloader.downloadIfNeeded(
                    applicationContext,
                    "llm",
                    MODEL_URL,
                    LLM_MODEL_NAME,
                    100L * 1024 * 1024
                ) { p ->
                    runOnUiThread {
                        ggufTv.text = "Downloading LLM… $p%"
                    }
                }
            }

        }

        // Upon CTA button tapped
        userActionFab.setOnClickListener {
            if (isModelReady) {
                // If model is ready, validate input and send to engine
                handleUserInput()
            } else {
                val modelFile = File(
                    filesDir,
                    "llm/llama-3.2-3b-q4_k_m.gguf"
                )
                Log.i(TAG, "1. Loading model file: $modelFile")
                handleSelectedModelFile(modelFile)
            }
        }
    }
    private fun handleSelectedModelFile(modelFile: File) {
        if (!modelFile.exists()) {
            Log.e(TAG, "Model file not found")
            return
        }

        userActionFab.isEnabled = false
        userInputEt.hint = "Parsing GGUF..."
        ggufTv.text = "Parsing metadata from:\n${modelFile.absolutePath}"

        lifecycleScope.launch(Dispatchers.IO) {
            FileInputStream(modelFile).use { input ->
                val metadata = GgufMetadataReader.create()
                    .readStructuredMetadata(input)


                withContext(Dispatchers.Main) {
                    ggufTv.text = metadata.toString()
                    userInputEt.hint = "Type and send a message!"
                    userInputEt.isEnabled = true
                    userActionFab.setImageResource(R.drawable.outline_send_24)
                    userActionFab.isEnabled = true
                }


                loadModel(modelFile.name, modelFile)

            }
        }
    }

    /**
     * Prepare the model file within app's private storage
     */
    private suspend fun ensureModelFile(modelName: String, input: InputStream) =
        withContext(Dispatchers.IO) {
            File(ensureModelsDirectory(), modelName).also { file ->
                // Copy the file into local storage if not yet done
                if (!file.exists()) {
                    Log.i(TAG, "Start copying file to $modelName")
                    withContext(Dispatchers.Main) {
                        userInputEt.hint = "Copying file..."
                    }

                    FileOutputStream(file).use { input.copyTo(it) }
                    Log.i(TAG, "Finished copying file to $modelName")
                } else {
                    Log.i(TAG, "File already exists $modelName")
                }
            }
        }

    /**
     * Load the model file from the app private storage
     */
    private suspend fun loadModel(modelName: String, modelFile: File) =
        withContext(Dispatchers.IO) {

            Log.i(TAG, "⏳ START loadModel()")
            Log.i(TAG, "Path: ${modelFile.absolutePath}")
            Log.i(TAG, "Size: ${modelFile.length() / (1024 * 1024)} MB")

            withContext(Dispatchers.Main) {
                userInputEt.hint = "Loading model..."
            }

            try {
                engine.loadModel(modelFile.path)

                Log.i(TAG, "✅ loadModel() RETURNED")

                withContext(Dispatchers.Main) {
                    isModelReady = true
                    Toast.makeText(
                        this@MainActivity,
                        "Model loaded successfully ✅",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ loadModel() FAILED", e)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Model load failed: ${e::class.simpleName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    /**
     * Validate and send the user message into [InferenceEngine]
     */
    private fun handleUserInput() {
        val userMsg = userInputEt.text.toString().trim()
        val prompt =
            "Translate the following into English.\n" +
                    "Only output the translation.\n\n" +
                    userMsg + "\n\nTranslation:"

        if (userMsg.isEmpty()) {
            Toast.makeText(this, "Input message is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable input
        userInputEt.text = null
        userInputEt.isEnabled = false
        userActionFab.isEnabled = false

        // Add user message
        messages.add(
            Message(UUID.randomUUID().toString(), userMsg, true)
        )

        // Prepare assistant message
        lastAssistantMsg.clear()
        messages.add(
            Message(UUID.randomUUID().toString(), "", false)
        )

        messageAdapter.notifyDataSetChanged()

        // Cancel previous generation if any
        generationJob?.cancel()

        generationJob = lifecycleScope.launch(Dispatchers.Default) {
            try {
                engine.send(prompt, formatChat = false)
                    .collect { token ->
                        if (token.isEmpty()) return@collect
                        if (token.contains(userMsg)) return@collect // stop repeating

                        withContext(Dispatchers.Main) {
                            lastAssistantMsg.append(token)

                            val lastIndex = messages.lastIndex
                            messages[lastIndex] = messages[lastIndex].copy(
                                content = lastAssistantMsg.toString()
                            )

                            messageAdapter.notifyItemChanged(lastIndex)
                        }
                    }
            } catch (e: CancellationException) {
                // Generation cancelled — ignore
            } finally {
                withContext(Dispatchers.Main) {
                    userInputEt.isEnabled = true
                    userActionFab.isEnabled = true
                }
            }
        }
    }

    /**
     * Run a benchmark with the model file
     */
    @Deprecated("This benchmark doesn't accurately indicate GUI performance expected by app developers")
    private suspend fun runBenchmark(modelName: String, modelFile: File) =
        withContext(Dispatchers.Default) {
            Log.i(TAG, "Starts benchmarking $modelName")
            withContext(Dispatchers.Main) {
                userInputEt.hint = "Running benchmark..."
            }
            engine.bench(
                pp=BENCH_PROMPT_PROCESSING_TOKENS,
                tg=BENCH_TOKEN_GENERATION_TOKENS,
                pl=BENCH_SEQUENCE,
                nr=BENCH_REPETITION
            ).let { result ->
                messages.add(Message(UUID.randomUUID().toString(), result, false))
                withContext(Dispatchers.Main) {
                    messageAdapter.notifyItemChanged(messages.size - 1)
                }
            }
        }

    /**
     * Create the `models` directory if not exist.
     */
    private fun ensureModelsDirectory() =
        File(filesDir, DIRECTORY_MODELS).also {
            if (it.exists() && !it.isDirectory) { it.delete() }
            if (!it.exists()) { it.mkdir() }
        }

    override fun onStop() {
        generationJob?.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        engine.destroy()
        super.onDestroy()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private const val DIRECTORY_MODELS = "models"
        private const val FILE_EXTENSION_GGUF = ".gguf"

        private const val BENCH_PROMPT_PROCESSING_TOKENS = 512
        private const val BENCH_TOKEN_GENERATION_TOKENS = 128
        private const val BENCH_SEQUENCE = 1
        private const val BENCH_REPETITION = 3
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun GgufMetadata.filename() = when {
    basic.name != null -> {
        basic.name?.let { name ->
            basic.sizeLabel?.let { size ->
                "$name-$size"
            } ?: name
        }
    }
    architecture?.architecture != null -> {
        architecture?.architecture?.let { arch ->
            basic.uuid?.let { uuid ->
                "$arch-$uuid"
            } ?: "$arch-${System.currentTimeMillis()}"
        }
    }
    else -> {
        "model-${System.currentTimeMillis().toHexString()}"
    }
}
