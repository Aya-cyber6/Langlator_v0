package com.android.llama

import android.content.Context
import com.android.llama.internal.InferenceEngineImpl

/**
 * Main entry point for Arm's AI Chat library.
 */
object LlamaEngine {
    /**
     * Get the inference engine single instance.
     */
    fun getInferenceEngine(context: Context) = InferenceEngineImpl.getInstance(context)
}
