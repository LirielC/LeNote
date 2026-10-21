package com.example.lenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.lenote.ui.HomeScreen
import com.example.lenote.ui.InkPlaygroundScreen
import com.example.lenote.ui.InkPlaygroundViewModel

/**
 * Destinos de navegação do aplicativo.
 */
enum class AppDestination {
    HOME,
    CANVAS
}

class MainActivity : ComponentActivity() {

    val playgroundViewModel: InkPlaygroundViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playgroundViewModel.initializeIfNeeded(applicationContext)
        enableEdgeToEdge()
        setContent {
            var currentScreen by remember { mutableStateOf(AppDestination.HOME) }

            // Tratamento do botão Voltar do sistema Android quando estiver no canvas
            BackHandler(enabled = currentScreen == AppDestination.CANVAS) {
                playgroundViewModel.flushCurrentPageSave()
                currentScreen = AppDestination.HOME
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == AppDestination.CANVAS) {
                                (slideInHorizontally { width -> width / 3 } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> -width / 3 } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut())
                            }
                        },
                        label = "AppScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppDestination.HOME -> {
                                HomeScreen(
                                    viewModel = playgroundViewModel,
                                    onOpenNotebook = {
                                        currentScreen = AppDestination.CANVAS
                                    },
                                    onOpenPage = {
                                        currentScreen = AppDestination.CANVAS
                                    }
                                )
                            }
                            AppDestination.CANVAS -> {
                                InkPlaygroundScreen(
                                    viewModel = playgroundViewModel,
                                    onNavigateBack = {
                                        playgroundViewModel.flushCurrentPageSave()
                                        currentScreen = AppDestination.HOME
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        playgroundViewModel.flushCurrentPageSave()
        super.onStop()
    }
}
