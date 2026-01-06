package com.android.llanglator.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.llanglator.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun TextTranslateScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    val log by vm.log.collectAsState() // Assume vm.log is MutableStateFlow<String>
    var userInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.initEngine() // make sure the engine is initialized
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Input text field
        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Enter text to translate") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Translate button
        Button(
            onClick = {
                if (userInput.isNotBlank()) {
                    scope.launch {
                        vm.translateText(userInput)
                        userInput = ""
                    }
                }
            },
            enabled = userInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Translate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Output log / translation
        Text(
            text = log,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
