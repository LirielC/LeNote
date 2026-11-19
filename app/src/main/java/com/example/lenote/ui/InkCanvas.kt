package com.example.lenote.ui

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokesView
import com.example.lenote.ink.FinishedStrokesView
import com.example.lenote.ink.JetpackInkEngine

/**
 * Área de visualização nativa de traços (Jetpack Ink).
 * Mantém as tags "FinishedStrokesView" e "InProgressStrokesView" para compatibilidade com testes instrumentados.
 */
@Composable
fun InkCanvas(
    engine: JetpackInkEngine,
    modifier: Modifier = Modifier
) {
    DisposableEffect(engine) {
        onDispose {
            engine.detachView()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val root = FrameLayout(context)

            val finishedView = FinishedStrokesView(context).apply {
                tag = "FinishedStrokesView"
            }
            val inProgressView = InProgressStrokesView(context).apply {
                tag = "InProgressStrokesView"
                eagerInit()
            }

            root.addView(
                finishedView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            root.addView(
                inProgressView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            engine.attachView(inProgressView)
            engine.attachFinishedView(finishedView)

            root.setOnTouchListener { _, event ->
                engine.onTouchEvent(event)
            }
            root.setOnHoverListener { _, event ->
                engine.onHoverEvent(event)
            }

            root
        }
    )
}
