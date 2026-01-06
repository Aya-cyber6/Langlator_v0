package com.android.llanglator.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.llanglator.MainViewModel

@Composable
fun WhisperScreen(vm: MainViewModel) {

    val isRecording = vm.isRecording
    val canTranscribe = vm.canTranscribe
    val log by vm.log.collectAsState() // << collect StateFlow as Compose state


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
                Text("Translate audio test")
            }

            Spacer(modifier = Modifier.height(16.dp))


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = log,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
}
