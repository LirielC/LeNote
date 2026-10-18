package com.example.lenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.lenote.ui.InkPlaygroundScreen
import com.example.lenote.ui.InkPlaygroundViewModel

class MainActivity : ComponentActivity() {

    val playgroundViewModel: InkPlaygroundViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playgroundViewModel.initializeIfNeeded(applicationContext)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InkPlaygroundScreen(viewModel = playgroundViewModel)
                }
            }
        }
    }

    override fun onStop() {
        playgroundViewModel.flushCurrentPageSave()
        super.onStop()
    }
}