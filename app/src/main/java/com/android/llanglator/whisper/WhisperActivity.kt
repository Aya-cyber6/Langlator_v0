package com.android.llanglator.whisper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.*
class WhisperActivity : ComponentActivity() {

    private val viewModel: WhisperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WhisperScreen(viewModel)
            }
        }
    }
}
