package com.android.llanglator.whisper

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WhisperScreen(vm: WhisperViewModel) {

    val isRecording = vm.isRecording
    val canTranscribe = vm.canTranscribe
    val log = vm.log
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // 🎙️ Record button
            Button(
                onClick = { vm.toggleRecord() },
                enabled = canTranscribe
            ) {
                Text(if (isRecording) "Stop Recording" else "Start Recording")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🧪 Transcribe test WAV button
            Button(
                onClick = { vm.transcribeSample() },
                enabled = canTranscribe && !isRecording
            ) {
                Text("Transcribe audio test")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 📝 Output
            Text(
                text = log,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}
