// file: MainActivity.kt
package com.android.llanglator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.llanglator.screens.TextTranslateScreen
import com.android.llanglator.screens.WhisperScreen
import com.android.llanglator.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var currentPage by remember { mutableStateOf("textTranslate") }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top buttons to switch pages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { currentPage = "textTranslate" }) {
                            Text("Text → Text")
                        }
                        Button(onClick = { currentPage = "whisper" }) {
                            Text("Audio → Text")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.fillMaxWidth())

                    // Page content
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentPage) {
                            "textTranslate" -> TextTranslateScreen(vm)
                            "whisper" -> WhisperScreen(vm)
                        }
                    }
                }
            }
        }
    }
}
