package com.android.llanglator.whisper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.*
import com.android.llanglator.MainViewModel
import com.android.llanglator.screens.WhisperScreen

class WhisperActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WhisperScreen(viewModel)
            }
        }
    }
}
