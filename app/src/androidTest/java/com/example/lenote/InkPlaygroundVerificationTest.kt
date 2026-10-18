package com.example.lenote

import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.ink.authoring.InProgressStrokesView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lenote.ink.FinishedStrokesView
import com.example.lenote.ink.GestureInputState
import com.example.lenote.ink.InkingTool
import com.example.lenote.ink.JetpackInkEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InkPlaygroundVerificationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private fun obtainSinglePointerEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        pointerId: Int,
        toolType: Int,
        x: Float,
        y: Float,
        pressure: Float = 0.6f,
        flags: Int = 0
    ): MotionEvent {
        val props = arrayOf(
            MotionEvent.PointerProperties().apply {
                id = pointerId
                this.toolType = toolType
            }
        )
        val coords = arrayOf(
            MotionEvent.PointerCoords().apply {
                this.x = x
                this.y = y
                this.pressure = pressure
            }
        )
        return MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            1,
            props,
            coords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            0,
            flags
        )
    }

    private fun drawSyntheticStylusStroke(
        engine: JetpackInkEngine,
        inProgressView: InProgressStrokesView,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val initialCount = engine.state.value.strokeCount

        instrumentation.runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            val downEvent = obtainSinglePointerEvent(
                downTime = downTime,
                eventTime = downTime,
                action = MotionEvent.ACTION_DOWN,
                pointerId = 0,
                toolType = MotionEvent.TOOL_TYPE_STYLUS,
                x = startX,
                y = startY,
                pressure = 0.7f
            )
            engine.onTouchEvent(downEvent)
            downEvent.recycle()

            val steps = 5
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val x = startX + (endX - startX) * t
                val y = startY + (endY - startY) * t
                val moveTime = downTime + i * 10L
                val moveEvent = obtainSinglePointerEvent(
                    downTime = downTime,
                    eventTime = moveTime,
                    action = MotionEvent.ACTION_MOVE,
                    pointerId = 0,
                    toolType = MotionEvent.TOOL_TYPE_STYLUS,
                    x = x,
                    y = y,
                    pressure = 0.75f
                )
                engine.onTouchEvent(moveEvent)
                moveEvent.recycle()
            }

            val upTime = downTime + (steps + 1) * 10L
            val upEvent = obtainSinglePointerEvent(
                downTime = downTime,
                eventTime = upTime,
                action = MotionEvent.ACTION_UP,
                pointerId = 0,
                toolType = MotionEvent.TOOL_TYPE_STYLUS,
                x = endX,
                y = endY,
                pressure = 0.1f
            )
            engine.onTouchEvent(upEvent)
            upEvent.recycle()

            inProgressView.requestHandoff()
        }

        val deadline = SystemClock.uptimeMillis() + 3000L
        while (engine.state.value.strokeCount == initialCount && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(15)
        }
        instrumentation.waitForIdleSync()
    }

    private fun performStylusEraserSwipe(
        engine: JetpackInkEngine,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        eraserSize: com.example.lenote.ink.EraserSize = com.example.lenote.ink.EraserSize.MEDIUM,
        steps: Int = 8
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val prevTool = engine.state.value.currentTool
            engine.setTool(InkingTool.Eraser(eraserSize))

            val downTime = SystemClock.uptimeMillis()
            val downEvent = obtainSinglePointerEvent(
                downTime, downTime, MotionEvent.ACTION_DOWN, 0, MotionEvent.TOOL_TYPE_STYLUS, startX, startY
            )
            engine.onTouchEvent(downEvent)
            downEvent.recycle()

            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val x = startX + (endX - startX) * t
                val y = startY + (endY - startY) * t
                val moveEvent = obtainSinglePointerEvent(
                    downTime, downTime + i * 10L, MotionEvent.ACTION_MOVE, 0, MotionEvent.TOOL_TYPE_STYLUS, x, y
                )
                engine.onTouchEvent(moveEvent)
                moveEvent.recycle()
            }

            val upEvent = obtainSinglePointerEvent(
                downTime, downTime + (steps + 2) * 10L, MotionEvent.ACTION_UP, 0, MotionEvent.TOOL_TYPE_STYLUS, endX, endY
            )
            engine.onTouchEvent(upEvent)
            upEvent.recycle()

            engine.setTool(prevTool)
        }
    }

    @Test
    fun verify25Strokes_colorSwitching_highlighter_eraser_undoRedo_and_noDuplication() {
        val engine = JetpackInkEngine()
        lateinit var inProgressView: InProgressStrokesView
        lateinit var finishedView: FinishedStrokesView

        activityRule.scenario.onActivity { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
            val container = FrameLayout(activity)
            finishedView = FinishedStrokesView(activity)
            inProgressView = InProgressStrokesView(activity)
            container.addView(finishedView, FrameLayout.LayoutParams(1000, 1000))
            container.addView(inProgressView, FrameLayout.LayoutParams(1000, 1000))
            activity.setContentView(container)

            inProgressView.eagerInit()
            engine.attachView(inProgressView)
            engine.attachFinishedView(finishedView)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(300)

        val colors = listOf(
            0xFF000000.toInt(),
            0xFF2196F3.toInt(),
            0xFFE91E63.toInt(),
            0xFF4CAF50.toInt()
        )

        val totalStrokes = 25
        for (i in 0 until totalStrokes) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                if (i % 5 == 4) {
                    engine.setTool(InkingTool.Highlighter())
                } else {
                    engine.setTool(InkingTool.Pen(colorInt = colors[i % colors.size]))
                }
            }
            val y = 50f + i * 30f
            drawSyntheticStylusStroke(engine, inProgressView, 100f, y, 400f, y)

            assertEquals("Contagem de strokes após traço ${i + 1}", i + 1, engine.state.value.strokeCount)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertTrue(
                    "InProgressStrokesView não deve reter strokes finalizados",
                    inProgressView.getFinishedStrokes().isEmpty()
                )
            }
        }

        for (remaining in (totalStrokes - 1) downTo 0) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertTrue(engine.state.value.canUndo)
                engine.undo()
            }
            assertEquals("Contagem após Undo para $remaining", remaining, engine.state.value.strokeCount)
        }
        assertFalse(engine.state.value.canUndo)
        assertTrue(engine.state.value.canRedo)

        for (restored in 1..totalStrokes) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertTrue(engine.state.value.canRedo)
                engine.redo()
            }
            assertEquals("Contagem após Redo para $restored", restored, engine.state.value.strokeCount)
        }
        assertFalse(engine.state.value.canRedo)
        assertTrue(engine.state.value.canUndo)

        // Apaga o primeiro stroke por completo (x = 90..410 em y = 50 com borracha SMALL para não atingir y = 80)
        performStylusEraserSwipe(
            engine,
            90f,
            50f,
            410f,
            50f,
            eraserSize = com.example.lenote.ink.EraserSize.SMALL,
            steps = 20
        )
        assertEquals("Eraser deve remover o primeiro stroke", totalStrokes - 1, engine.state.value.strokeCount)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            engine.undo()
        }
        assertEquals("Undo após Eraser deve restaurar o stroke", totalStrokes, engine.state.value.strokeCount)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            engine.redo()
        }
        assertEquals("Redo após Undo do Eraser deve remover novamente", totalStrokes - 1, engine.state.value.strokeCount)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            engine.undo()
        }
        assertTrue(engine.state.value.canRedo)
        drawSyntheticStylusStroke(engine, inProgressView, 500f, 500f, 700f, 500f)
        assertFalse("Novo stroke após Undo deve limpar pilha de Redo", engine.state.value.canRedo)
        assertEquals(totalStrokes + 1, engine.state.value.strokeCount)
    }

    @Test
    fun verifyPalmAndFingerRejection_stylusPlusPalm_stylusPlusFinger_and_twoFingerPanZoom() {
        val engine = JetpackInkEngine()
        lateinit var inProgressView: InProgressStrokesView
        lateinit var finishedView: FinishedStrokesView

        activityRule.scenario.onActivity { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
            val container = FrameLayout(activity)
            finishedView = FinishedStrokesView(activity)
            inProgressView = InProgressStrokesView(activity)
            container.addView(finishedView, FrameLayout.LayoutParams(1000, 1000))
            container.addView(inProgressView, FrameLayout.LayoutParams(1000, 1000))
            activity.setContentView(container)

            inProgressView.eagerInit()
            engine.attachView(inProgressView)
            engine.attachFinishedView(finishedView)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(300)

        // CENÁRIO 1: Dedo único isolado NUNCA deve criar stroke
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            val evDown = obtainSinglePointerEvent(downTime, downTime, MotionEvent.ACTION_DOWN, 0, MotionEvent.TOOL_TYPE_FINGER, 100f, 100f)
            val evMove = obtainSinglePointerEvent(downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 0, MotionEvent.TOOL_TYPE_FINGER, 300f, 100f)
            val evUp = obtainSinglePointerEvent(downTime, downTime + 40L, MotionEvent.ACTION_UP, 0, MotionEvent.TOOL_TYPE_FINGER, 300f, 100f)

            assertTrue(
                engine.state.value.gestureState == GestureInputState.SINGLE_FINGER_PAN ||
                engine.state.value.gestureState == GestureInputState.SINGLE_FINGER_IGNORED
            )

            evDown.recycle(); evMove.recycle(); evUp.recycle()
            inProgressView.requestHandoff()
        }
        Thread.sleep(100)
        assertEquals("Toque de dedo isolado nunca deve criar traço", 0, engine.state.value.strokeCount)
        assertFalse("InProgressStrokesView não deve ter traços em progresso de dedo", inProgressView.hasUnfinishedStrokes())

        // CENÁRIO 2: STYLUS + PALM (Palma apoiada ANTES da caneta tocar a tela, seguida de escrita com Precision Pen 2)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            // 2a. Palma pousa na tela como pointerId=0 (TOOL_TYPE_FINGER)
            val palmDown = obtainSinglePointerEvent(downTime, downTime, MotionEvent.ACTION_DOWN, 0, MotionEvent.TOOL_TYPE_FINGER, 600f, 600f)
            engine.onTouchEvent(palmDown)
            palmDown.recycle()

            // 2b. Stylus pousa na tela enquanto a palma está apoiada (ACTION_POINTER_DOWN no índice 1, pointerId=1, TOOL_TYPE_STYLUS)
            val propsTwo = arrayOf(
                MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER },
                MotionEvent.PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_STYLUS }
            )
            val coordsStart = arrayOf(
                MotionEvent.PointerCoords().apply { x = 600f; y = 600f; pressure = 0.4f },
                MotionEvent.PointerCoords().apply { x = 150f; y = 200f; pressure = 0.8f }
            )
            val stylusPointerDown = MotionEvent.obtain(
                downTime,
                downTime + 15L,
                MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                2,
                propsTwo,
                coordsStart,
                0, 0, 1f, 1f, 0, 0, 0, 0
            )
            engine.onTouchEvent(stylusPointerDown)
            stylusPointerDown.recycle()
            assertEquals(GestureInputState.STYLUS_DRAWING, engine.state.value.gestureState)

            // 2c. Palma e Stylus se movem juntas na tela (ACTION_MOVE com 2 ponteiros: 0=FINGER, 1=STYLUS)
            for (step in 1..4) {
                val coordsMove = arrayOf(
                    MotionEvent.PointerCoords().apply { x = 600f + step * 5f; y = 600f + step * 5f; pressure = 0.4f },
                    MotionEvent.PointerCoords().apply { x = 150f + step * 50f; y = 200f; pressure = 0.85f }
                )
                val moveEv = MotionEvent.obtain(
                    downTime,
                    downTime + 15L + step * 10L,
                    MotionEvent.ACTION_MOVE,
                    2,
                    propsTwo,
                    coordsMove,
                    0, 0, 1f, 1f, 0, 0, 0, 0
                )
                engine.onTouchEvent(moveEv)
                moveEv.recycle()
            }

            // 2d. Sistema Android cancela o ponteiro da palma com FLAG_CANCELED em ACTION_POINTER_UP (index 0)
            val coordsEnd = arrayOf(
                MotionEvent.PointerCoords().apply { x = 620f; y = 620f; pressure = 0.4f },
                MotionEvent.PointerCoords().apply { x = 350f; y = 200f; pressure = 0.85f }
            )
            val palmPointerCancel = MotionEvent.obtain(
                downTime,
                downTime + 70L,
                MotionEvent.ACTION_POINTER_UP or (0 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                2,
                propsTwo,
                coordsEnd,
                0, 0, 1f, 1f, 0, 0, 0, MotionEvent.FLAG_CANCELED
            )
            engine.onTouchEvent(palmPointerCancel)
            palmPointerCancel.recycle()

            // 2e. Stylus levanta normalmente (ACTION_UP, pointerId=1)
            val stylusUp = obtainSinglePointerEvent(
                downTime, downTime + 85L, MotionEvent.ACTION_UP, 1, MotionEvent.TOOL_TYPE_STYLUS, 350f, 200f, 0.1f
            )
            engine.onTouchEvent(stylusUp)
            stylusUp.recycle()

            inProgressView.requestHandoff()
        }

        val deadline1 = SystemClock.uptimeMillis() + 3000L
        while (engine.state.value.strokeCount < 1 && SystemClock.uptimeMillis() < deadline1) {
            Thread.sleep(15)
        }
        assertEquals("Somente 1 stroke legítimo da Stylus deve ser criado (nenhum da palma) e sem ser cancelado!", 1, engine.state.value.strokeCount)
        assertEquals("Pan X não deve ter sido alterado pela palma + stylus", 0f, engine.state.value.panOffsetX, 0.01f)
        assertEquals("Pan Y não deve ter sido alterado pela palma + stylus", 0f, engine.state.value.panOffsetY, 0.01f)

        // CENÁRIO 3: STYLUS + FINGER (Caneta já escrevendo, um dedo toca na tela e se move enquanto a caneta continua escrevendo)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            // 3a. Stylus inicia escrita (pointerId=0)
            val stylusDown = obtainSinglePointerEvent(downTime, downTime, MotionEvent.ACTION_DOWN, 0, MotionEvent.TOOL_TYPE_STYLUS, 100f, 400f, 0.7f)
            engine.onTouchEvent(stylusDown)
            stylusDown.recycle()

            // 3b. Dedo toca a tela durante a escrita (pointerId=1, TOOL_TYPE_FINGER)
            val propsStylusAndFinger = arrayOf(
                MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_STYLUS },
                MotionEvent.PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_FINGER }
            )
            val coordsBoth = arrayOf(
                MotionEvent.PointerCoords().apply { x = 150f; y = 400f; pressure = 0.7f },
                MotionEvent.PointerCoords().apply { x = 500f; y = 500f; pressure = 0.5f }
            )
            val fingerPointerDown = MotionEvent.obtain(
                downTime,
                downTime + 15L,
                MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                2,
                propsStylusAndFinger,
                coordsBoth,
                0, 0, 1f, 1f, 0, 0, 0, 0
            )
            engine.onTouchEvent(fingerPointerDown)
            fingerPointerDown.recycle()

            // 3c. Stylus e Dedo se movem; o stroke da Stylus NÃO pode ser cancelado e o dedo NÃO pode gerar stroke nem Pan!
            for (step in 1..4) {
                val coordsMove = arrayOf(
                    MotionEvent.PointerCoords().apply { x = 150f + step * 50f; y = 400f; pressure = 0.75f },
                    MotionEvent.PointerCoords().apply { x = 500f + step * 30f; y = 500f + step * 30f; pressure = 0.5f }
                )
                val moveEv = MotionEvent.obtain(
                    downTime,
                    downTime + 15L + step * 10L,
                    MotionEvent.ACTION_MOVE,
                    2,
                    propsStylusAndFinger,
                    coordsMove,
                    0, 0, 1f, 1f, 0, 0, 0, 0
                )
                engine.onTouchEvent(moveEv)
                moveEv.recycle()
            }

            // 3d. Dedo levanta
            val fingerUp = MotionEvent.obtain(
                downTime,
                downTime + 70L,
                MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                2,
                propsStylusAndFinger,
                coordsBoth,
                0, 0, 1f, 1f, 0, 0, 0, 0
            )
            engine.onTouchEvent(fingerUp)
            fingerUp.recycle()

            // 3e. Stylus termina de escrever
            val stylusUp = obtainSinglePointerEvent(downTime, downTime + 85L, MotionEvent.ACTION_UP, 0, MotionEvent.TOOL_TYPE_STYLUS, 400f, 400f, 0.1f)
            engine.onTouchEvent(stylusUp)
            stylusUp.recycle()

            inProgressView.requestHandoff()
        }

        val deadline2 = SystemClock.uptimeMillis() + 3000L
        while (engine.state.value.strokeCount < 2 && SystemClock.uptimeMillis() < deadline2) {
            Thread.sleep(15)
        }
        assertEquals("O stroke da Stylus deve terminar normalmente e o dedo não deve criar stroke extra", 2, engine.state.value.strokeCount)
        assertEquals("Pan X não deve mudar quando dedo toca durante escrita da Stylus", 0f, engine.state.value.panOffsetX, 0.01f)

        // CENÁRIO 4: Dois dedos (TOOL_TYPE_FINGER) realizam Pan/Zoom sem criar traços
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            val propsTwoFingers = arrayOf(
                MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER },
                MotionEvent.PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_FINGER }
            )
            val coordsStart = arrayOf(
                MotionEvent.PointerCoords().apply { x = 200f; y = 200f },
                MotionEvent.PointerCoords().apply { x = 300f; y = 200f }
            )
            val coordsMove = arrayOf(
                MotionEvent.PointerCoords().apply { x = 320f; y = 350f },
                MotionEvent.PointerCoords().apply { x = 420f; y = 350f }
            )
            val ev1 = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 1, propsTwoFingers, coordsStart, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev2 = MotionEvent.obtain(downTime, downTime + 10L, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, propsTwoFingers, coordsStart, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev3 = MotionEvent.obtain(downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 2, propsTwoFingers, coordsMove, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev4 = MotionEvent.obtain(downTime, downTime + 30L, MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, propsTwoFingers, coordsMove, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev5 = MotionEvent.obtain(downTime, downTime + 40L, MotionEvent.ACTION_UP, 1, propsTwoFingers, coordsMove, 0, 0, 1f, 1f, 0, 0, 0, 0)

            engine.onTouchEvent(ev1)
            engine.onTouchEvent(ev2)
            engine.onTouchEvent(ev3)
            assertEquals(GestureInputState.TWO_FINGER_PAN_ZOOM, engine.state.value.gestureState)
            engine.onTouchEvent(ev4)
            engine.onTouchEvent(ev5)

            ev1.recycle(); ev2.recycle(); ev3.recycle(); ev4.recycle(); ev5.recycle()
        }
        assertEquals("Dois dedos devem aplicar Pan X (+120)", 120f, engine.state.value.panOffsetX, 1f)
        assertEquals("Dois dedos devem aplicar Pan Y (+150)", 150f, engine.state.value.panOffsetY, 1f)
        assertEquals("Dois dedos nunca devem criar strokes de tinta", 2, engine.state.value.strokeCount)
    }

    @Test
    fun verifyRotation_20Strokes_rotate_20Strokes_rotate_undoRedo_preservesAllState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        fun findViewByTagRecursively(root: android.view.View, targetTag: Any): android.view.View? {
            if (root.tag == targetTag) return root
            if (root is android.view.ViewGroup) {
                for (i in 0 until root.childCount) {
                    val found = findViewByTagRecursively(root.getChildAt(i), targetTag)
                    if (found != null) return found
                }
            }
            return null
        }

        fun prepareActivityAndGetViews(): Triple<JetpackInkEngine, InProgressStrokesView, FinishedStrokesView> {
            var result: Triple<JetpackInkEngine, InProgressStrokesView, FinishedStrokesView>? = null
            activityRule.scenario.onActivity { activity ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    activity.setShowWhenLocked(true)
                    activity.setTurnScreenOn(true)
                }
                @Suppress("DEPRECATION")
                activity.window.addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                )
                val decor = activity.window.decorView
                val inProgress = findViewByTagRecursively(decor, "InProgressStrokesView") as? InProgressStrokesView
                val finished = findViewByTagRecursively(decor, "FinishedStrokesView") as? FinishedStrokesView
                if (inProgress != null && finished != null) {
                    result = Triple(activity.playgroundViewModel.engine, inProgress, finished)
                }
            }
            return checkNotNull(result) { "Views do InkCanvas não encontradas na hierarquia da Activity" }
        }

        instrumentation.waitForIdleSync()
        Thread.sleep(300)

        val (initialEngine, initialInProgressView, initialFinishedView) = prepareActivityAndGetViews()

        // Limpa estado caso haja algo residual
        instrumentation.runOnMainSync {
            initialEngine.clear()
        }

        val colors = listOf(
            0xFF000000.toInt(),
            0xFF2196F3.toInt(),
            0xFFE91E63.toInt(),
            0xFF4CAF50.toInt()
        )

        // 1. Desenhar 20 strokes em Portrait (alternando cores e marca-texto)
        for (i in 0 until 20) {
            instrumentation.runOnMainSync {
                if (i % 5 == 4) {
                    initialEngine.setTool(InkingTool.Highlighter())
                } else {
                    initialEngine.setTool(InkingTool.Pen(colorInt = colors[i % colors.size], size = 6f))
                }
            }
            val y = 60f + i * 18f
            drawSyntheticStylusStroke(initialEngine, initialInProgressView, 80f, y, 360f, y)
            assertEquals("Contagem após traço ${i + 1} antes da 1ª rotação", i + 1, initialEngine.state.value.strokeCount)
        }

        assertEquals(20, initialFinishedView.getRenderedStrokeCount())

        // 2. Girar tablet (Recriação completa da Activity — Portrait -> Landscape)
        activityRule.scenario.recreate()
        instrumentation.waitForIdleSync()
        Thread.sleep(300)

        // 3. Verificar que TODOS os 20 strokes, cores e o ViewModel/Engine permanecem intactos na nova View
        val (rotatedEngine1, rotatedInProgressView1, rotatedFinishedView1) = prepareActivityAndGetViews()
        assertTrue("A instância do JetpackInkEngine deve sobreviver à rotação via ViewModel", initialEngine === rotatedEngine1)
        assertTrue("FinishedStrokesView deve ser uma nova instância reconstruída após rotação", initialFinishedView !== rotatedFinishedView1)
        assertEquals("Todos os 20 strokes devem continuar no estado do engine após 1ª rotação", 20, rotatedEngine1.state.value.strokeCount)
        assertEquals("FinishedStrokesView reconstruído deve conter os 20 strokes após 1ª rotação", 20, rotatedFinishedView1.getRenderedStrokeCount())

        // 4. Desenhar mais 20 strokes após a primeira rotação (total = 40)
        for (i in 20 until 40) {
            instrumentation.runOnMainSync {
                val color = colors[i % colors.size]
                rotatedEngine1.setTool(InkingTool.Pen(colorInt = color, size = 7f))
                rotatedEngine1.setSmoothingLevel(com.example.lenote.ink.SmoothingLevel.MEDIUM)
            }
            val y = 60f + (i - 20) * 18f
            drawSyntheticStylusStroke(rotatedEngine1, rotatedInProgressView1, 420f, y, 700f, y)
            assertEquals("Contagem após traço ${i + 1}", i + 1, rotatedEngine1.state.value.strokeCount)
        }

        assertEquals(40, rotatedFinishedView1.getRenderedStrokeCount())

        // 5. Girar novamente (Landscape -> Portrait)
        activityRule.scenario.recreate()
        instrumentation.waitForIdleSync()
        Thread.sleep(300)

        // 6. Verificar todos os 40 strokes, cores, ferramenta e suavização após a 2ª rotação
        val (rotatedEngine2, _, rotatedFinishedView2) = prepareActivityAndGetViews()
        assertEquals("Todos os 40 strokes devem continuar presentes após 2ª rotação", 40, rotatedEngine2.state.value.strokeCount)
        assertEquals("FinishedStrokesView deve renderizar todos os 40 strokes após 2ª rotação", 40, rotatedFinishedView2.getRenderedStrokeCount())
        assertEquals(com.example.lenote.ink.SmoothingLevel.MEDIUM, rotatedEngine2.state.value.smoothingLevel)

        val snapshot40 = rotatedEngine2.getFinishedStrokesSnapshot()
        assertEquals(40, snapshot40.size)
        // Confere cores preservadas do primeiro lote (traço 0 = preto, traço 1 = azul)
        assertEquals(colors[0], snapshot40[0].brush.colorIntArgb)
        assertEquals(colors[1], snapshot40[1].brush.colorIntArgb)

        // 7. Executar Undo (desfaz 10 traços, incluindo traços anteriores e posteriores à rotação)
        for (expectedRemaining in 39 downTo 30) {
            instrumentation.runOnMainSync {
                assertTrue(rotatedEngine2.state.value.canUndo)
                rotatedEngine2.undo()
            }
            assertEquals(expectedRemaining, rotatedEngine2.state.value.strokeCount)
            assertEquals(expectedRemaining, rotatedFinishedView2.getRenderedStrokeCount())
        }

        // 8. Executar Redo (restaura todos os 10 traços até voltar aos 40)
        for (expectedRestored in 31..40) {
            instrumentation.runOnMainSync {
                assertTrue(rotatedEngine2.state.value.canRedo)
                rotatedEngine2.redo()
            }
            assertEquals(expectedRestored, rotatedEngine2.state.value.strokeCount)
            assertEquals(expectedRestored, rotatedFinishedView2.getRenderedStrokeCount())
        }
    }

    @Test
    fun verifySmoothingLevels_offLowMedium_reducesJitterAndPreservesPressureAndEndpoints() {
        val brush = androidx.ink.brush.Brush.createWithColorIntArgb(
            androidx.ink.brush.StockBrushes.pressurePen(),
            0xFF000000.toInt(),
            5f,
            0.1f
        )

        // Cria um batch com jitter artificial (ziguezague perpendicular de +/- 1.5px em Y)
        val jitterBatch = androidx.ink.strokes.MutableStrokeInputBatch()
        val pointCount = 15
        for (i in 0 until pointCount) {
            val x = 100f + i * 20f
            val jitterY = when {
                i == 0 || i == pointCount - 1 -> 200f
                i % 2 == 0 -> 201.5f
                else -> 198.5f
            }
            val pressure = 0.3f + (i.toFloat() / pointCount) * 0.5f
            jitterBatch.add(
                androidx.ink.strokes.StrokeInput.create(
                    x = x,
                    y = jitterY,
                    elapsedTimeMillis = i * 8L,
                    toolType = androidx.ink.brush.InputToolType.STYLUS,
                    strokeUnitLengthCm = 0.1f,
                    pressure = pressure
                )
            )
        }

        val rawStroke = androidx.ink.strokes.Stroke(brush, jitterBatch.toImmutable())
        val offStroke = com.example.lenote.ink.StrokeSmoother.smoothStroke(rawStroke, com.example.lenote.ink.SmoothingLevel.OFF)
        val lowStroke = com.example.lenote.ink.StrokeSmoother.smoothStroke(rawStroke, com.example.lenote.ink.SmoothingLevel.LOW)
        val medStroke = com.example.lenote.ink.StrokeSmoother.smoothStroke(rawStroke, com.example.lenote.ink.SmoothingLevel.MEDIUM)

        // 1. OFF mantém exatamente a instância original
        assertTrue("Smoothing OFF deve retornar o stroke original inalterado", rawStroke === offStroke)

        // 2. Preservação de quantidade de pontos, extremidades, pressão e tempo
        assertEquals(pointCount, lowStroke.inputs.size)
        assertEquals(pointCount, medStroke.inputs.size)

        val firstRaw = rawStroke.inputs.get(0)
        val lastRaw = rawStroke.inputs.get(pointCount - 1)
        val firstMed = medStroke.inputs.get(0)
        val lastMed = medStroke.inputs.get(pointCount - 1)

        assertEquals(firstRaw.x, firstMed.x, 0.001f)
        assertEquals(firstRaw.y, firstMed.y, 0.001f)
        assertEquals(lastRaw.x, lastMed.x, 0.001f)
        assertEquals(lastRaw.y, lastMed.y, 0.001f)

        // 3. Verifica que a oscilação média em relação à linha central (Y=200) diminui: RAW > LOW > MEDIUM
        fun computeMeanDeviationFrom200(stroke: androidx.ink.strokes.Stroke): Float {
            var sum = 0f
            for (i in 1 until pointCount - 1) {
                val pt = stroke.inputs.get(i)
                sum += kotlin.math.abs(pt.y - 200f)
                // Verifica que a pressão e o tempo foram 100% preservados em cada ponto
                val origPt = rawStroke.inputs.get(i)
                assertEquals("Pressão deve ser preservada no ponto $i", origPt.pressure, pt.pressure, 0.0001f)
                assertEquals("Tempo deve ser preservado no ponto $i", origPt.elapsedTimeMillis, pt.elapsedTimeMillis)
            }
            return sum / (pointCount - 2)
        }

        val rawDev = computeMeanDeviationFrom200(rawStroke)
        val lowDev = computeMeanDeviationFrom200(lowStroke)
        val medDev = computeMeanDeviationFrom200(medStroke)

        assertTrue("LOW ($lowDev) deve suavizar a micro-oscilação em relação ao RAW ($rawDev)", lowDev < rawDev)
        assertTrue("MEDIUM ($medDev) deve suavizar mais que LOW ($lowDev)", medDev < lowDev)
    }

    @Test
    fun verifyPageBackground_and_EraserIndicator_withZoomPanAndRotation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        fun findViewByTagRecursively(root: android.view.View, targetTag: Any): android.view.View? {
            if (root.tag == targetTag) return root
            if (root is android.view.ViewGroup) {
                for (i in 0 until root.childCount) {
                    val found = findViewByTagRecursively(root.getChildAt(i), targetTag)
                    if (found != null) return found
                }
            }
            return null
        }

        fun prepareActivityAndGetViews(): Triple<JetpackInkEngine, InProgressStrokesView, FinishedStrokesView> {
            var result: Triple<JetpackInkEngine, InProgressStrokesView, FinishedStrokesView>? = null
            activityRule.scenario.onActivity { activity ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    activity.setShowWhenLocked(true)
                    activity.setTurnScreenOn(true)
                }
                @Suppress("DEPRECATION")
                activity.window.addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                )
                val decor = activity.window.decorView
                val inProgress = findViewByTagRecursively(decor, "InProgressStrokesView") as? InProgressStrokesView
                val finished = findViewByTagRecursively(decor, "FinishedStrokesView") as? FinishedStrokesView
                if (inProgress != null && finished != null) {
                    result = Triple(activity.playgroundViewModel.engine, inProgress, finished)
                }
            }
            return checkNotNull(result) { "Views do InkCanvas não encontradas" }
        }

        instrumentation.waitForIdleSync()
        Thread.sleep(300)

        val (engine, inProgressView, finishedView) = prepareActivityAndGetViews()
        instrumentation.runOnMainSync {
            engine.clear()
            engine.setPageBackground(com.example.lenote.ink.PageBackground.BLANK)
            engine.setTool(InkingTool.Pen())
        }

        // 1. Escrever vários strokes (5 strokes em posições conhecidas)
        for (i in 0 until 5) {
            val y = 100f + i * 50f
            drawSyntheticStylusStroke(engine, inProgressView, 120f, y, 420f, y)
        }
        assertEquals(5, engine.state.value.strokeCount)
        val initialStrokeSnapshot = engine.getFinishedStrokesSnapshot()

        // 2 & 3. Trocar folha entre todos os tipos e verificar que nenhum stroke mudou
        for (bg in com.example.lenote.ink.PageBackground.entries) {
            instrumentation.runOnMainSync {
                engine.setPageBackground(bg)
            }
            assertEquals(bg, engine.state.value.pageBackground)
            assertEquals(bg, finishedView.getCurrentPageBackground())
            assertEquals("Trocar folha para $bg não deve alterar contagem de strokes", 5, engine.state.value.strokeCount)
            val afterBgSnapshot = engine.getFinishedStrokesSnapshot()
            for (idx in initialStrokeSnapshot.indices) {
                assertTrue(
                    "Trocar folha não deve recriar ou alterar instâncias de Stroke existentes",
                    initialStrokeSnapshot[idx] === afterBgSnapshot[idx]
                )
            }
        }

        // 4 & 5. Aplicar Zoom e Pan com 2 dedos
        instrumentation.runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            val propsTwoFingers = arrayOf(
                MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER },
                MotionEvent.PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_FINGER }
            )
            // Distância inicial = 100px (200..300) -> Distância final = 200px (150..350) => Zoom 2.0x + Pan
            val coordsStart = arrayOf(
                MotionEvent.PointerCoords().apply { x = 200f; y = 200f },
                MotionEvent.PointerCoords().apply { x = 300f; y = 200f }
            )
            val coordsMove = arrayOf(
                MotionEvent.PointerCoords().apply { x = 180f; y = 240f },
                MotionEvent.PointerCoords().apply { x = 380f; y = 240f }
            )
            val ev1 = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 1, propsTwoFingers, coordsStart, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev2 = MotionEvent.obtain(downTime, downTime + 10L, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, propsTwoFingers, coordsStart, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev3 = MotionEvent.obtain(downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 2, propsTwoFingers, coordsMove, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev4 = MotionEvent.obtain(downTime, downTime + 30L, MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, propsTwoFingers, coordsMove, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev5 = MotionEvent.obtain(downTime, downTime + 40L, MotionEvent.ACTION_UP, 1, propsTwoFingers, coordsMove, 0, 0, 1f, 1f, 0, 0, 0, 0)

            engine.onTouchEvent(ev1)
            engine.onTouchEvent(ev2)
            engine.onTouchEvent(ev3)
            engine.onTouchEvent(ev4)
            engine.onTouchEvent(ev5)
            ev1.recycle(); ev2.recycle(); ev3.recycle(); ev4.recycle(); ev5.recycle()
        }
        val currentZoom = engine.state.value.zoomLevel
        assertTrue("Zoom deve ter aumentado (zoom=$currentZoom)", currentZoom > 1.5f)

        // 6. Trocar folha novamente com Zoom/Pan ativo (ex: MATHEMATICS e RULED)
        instrumentation.runOnMainSync {
            engine.setPageBackground(com.example.lenote.ink.PageBackground.MATHEMATICS)
            finishedView.invalidate()
        }
        assertEquals(com.example.lenote.ink.PageBackground.MATHEMATICS, finishedView.getCurrentPageBackground())

        // 7, 8, 9 & 10. Selecionar Eraser, trocar tamanhos (SMALL, MEDIUM, LARGE) e verificar indicador + apagamento sem apagar background
        instrumentation.runOnMainSync {
            engine.setTool(InkingTool.Eraser(com.example.lenote.ink.EraserSize.SMALL))
            assertEquals(com.example.lenote.ink.EraserSize.SMALL, engine.state.value.eraserSize)

            // Simula Stylus HOVER_ENTER e HOVER_MOVE para verificar que o indicador acompanha a stylus
            val hoverTime = SystemClock.uptimeMillis()
            val hoverEnter = obtainSinglePointerEvent(
                hoverTime, hoverTime, MotionEvent.ACTION_HOVER_ENTER, 0, MotionEvent.TOOL_TYPE_STYLUS, 250f, 180f
            )
            engine.onHoverEvent(hoverEnter)
            hoverEnter.recycle()

            assertTrue("Indicador da borracha deve ficar visível no HOVER_ENTER", engine.state.value.isEraserIndicatorVisible)
            assertTrue("FinishedStrokesView deve exibir indicador", finishedView.isEraserIndicatorShowing())
            assertEquals(250f, finishedView.getEraserIndicatorScreenPosition().first, 0.01f)
            assertEquals(180f, finishedView.getEraserIndicatorScreenPosition().second, 0.01f)
            assertEquals(
                com.example.lenote.ink.EraserSize.SMALL.worldRadius * currentZoom,
                finishedView.getEraserIndicatorScreenRadius(),
                0.05f
            )

            // 9. Trocar tamanho da borracha para MEDIUM e LARGE e verificar atualização imediata do raio
            engine.setEraserSize(com.example.lenote.ink.EraserSize.MEDIUM)
            assertEquals(
                com.example.lenote.ink.EraserSize.MEDIUM.worldRadius * currentZoom,
                finishedView.getEraserIndicatorScreenRadius(),
                0.05f
            )

            engine.setEraserSize(com.example.lenote.ink.EraserSize.LARGE)
            assertEquals(
                com.example.lenote.ink.EraserSize.LARGE.worldRadius * currentZoom,
                finishedView.getEraserIndicatorScreenRadius(),
                0.05f
            )

            // 10. Movimento durante toque da borracha acompanha a stylus e esconde no ACTION_UP
            val downEvent = obtainSinglePointerEvent(
                hoverTime + 10L, hoverTime + 10L, MotionEvent.ACTION_DOWN, 0, MotionEvent.TOOL_TYPE_STYLUS, 300f, 220f
            )
            engine.onTouchEvent(downEvent)
            downEvent.recycle()
            assertTrue(engine.state.value.isEraserIndicatorVisible)
            assertEquals(300f, finishedView.getEraserIndicatorScreenPosition().first, 0.01f)
            assertEquals(220f, finishedView.getEraserIndicatorScreenPosition().second, 0.01f)

            val moveEvent = obtainSinglePointerEvent(
                hoverTime + 10L, hoverTime + 20L, MotionEvent.ACTION_MOVE, 0, MotionEvent.TOOL_TYPE_STYLUS, 340f, 260f
            )
            engine.onTouchEvent(moveEvent)
            moveEvent.recycle()
            assertTrue(engine.state.value.isEraserIndicatorVisible)
            assertEquals(340f, finishedView.getEraserIndicatorScreenPosition().first, 0.01f)
            assertEquals(260f, finishedView.getEraserIndicatorScreenPosition().second, 0.01f)

            // Verificar que o background continua sendo MATHEMATICS e não foi afetado pela borracha
            assertEquals(com.example.lenote.ink.PageBackground.MATHEMATICS, engine.state.value.pageBackground)
            assertEquals(com.example.lenote.ink.PageBackground.MATHEMATICS, finishedView.getCurrentPageBackground())

            // 11 & 12. Trocar para Pen enquanto indicador estava ativo -> indicador desaparece imediatamente
            engine.setTool(InkingTool.Pen())
            assertFalse("Indicador deve desaparecer imediatamente ao trocar para Pen", engine.state.value.isEraserIndicatorVisible)
            assertFalse("FinishedStrokesView deve ocultar indicador ao trocar para Pen", finishedView.isEraserIndicatorShowing())

            // Limpa toque residual
            val upEvent = obtainSinglePointerEvent(
                hoverTime + 10L, hoverTime + 30L, MotionEvent.ACTION_UP, 0, MotionEvent.TOOL_TYPE_STYLUS, 340f, 260f
            )
            engine.onTouchEvent(upEvent)
            upEvent.recycle()
        }

        val strokesBeforeRotation = engine.state.value.strokeCount
        assertTrue("Deve restar pelo menos 1 stroke para validar rotação com background", strokesBeforeRotation > 0)

        // 13 & 14. Girar a tela e verificar que PageBackground + Strokes + Zoom/Pan continuam alinhados e preservados
        activityRule.scenario.recreate()
        instrumentation.waitForIdleSync()
        Thread.sleep(300)

        val (rotatedEngine, _, rotatedFinishedView) = prepareActivityAndGetViews()
        assertEquals(
            "PageBackground deve sobreviver à rotação",
            com.example.lenote.ink.PageBackground.MATHEMATICS,
            rotatedEngine.state.value.pageBackground
        )
        assertEquals(
            "FinishedStrokesView reconstruído deve receber o PageBackground preservado",
            com.example.lenote.ink.PageBackground.MATHEMATICS,
            rotatedFinishedView.getCurrentPageBackground()
        )
        assertEquals(
            "Strokes devem permanecer intactos após rotação com background",
            strokesBeforeRotation,
            rotatedFinishedView.getRenderedStrokeCount()
        )
        assertEquals(
            "Zoom deve permanecer intacto após rotação",
            currentZoom,
            rotatedEngine.state.value.zoomLevel,
            0.01f
        )
    }

    @Test
    fun verifyPartialEraser_all19Scenarios_letterA_split_sizes_zoomPan_undoRedo_penAndHighlighter() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val engine = JetpackInkEngine()
        lateinit var inProgressView: InProgressStrokesView
        lateinit var finishedView: FinishedStrokesView

        activityRule.scenario.onActivity { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
            val container = FrameLayout(activity)
            finishedView = FinishedStrokesView(activity)
            inProgressView = InProgressStrokesView(activity)
            container.addView(finishedView, FrameLayout.LayoutParams(1000, 1000))
            container.addView(inProgressView, FrameLayout.LayoutParams(1000, 1000))
            activity.setContentView(container)

            inProgressView.eagerInit()
            engine.attachView(inProgressView)
            engine.attachFinishedView(finishedView)
        }
        instrumentation.waitForIdleSync()
        Thread.sleep(300)

        fun drawMultiPointStylusStroke(
            points: List<Pair<Float, Float>>,
            tool: InkingTool
        ) {
            val initialCount = engine.state.value.strokeCount
            instrumentation.runOnMainSync {
                engine.setTool(tool)
                val downTime = SystemClock.uptimeMillis()
                val first = points.first()
                val downEv = obtainSinglePointerEvent(
                    downTime, downTime, MotionEvent.ACTION_DOWN, 0, MotionEvent.TOOL_TYPE_STYLUS, first.first, first.second, 0.7f
                )
                engine.onTouchEvent(downEv)
                downEv.recycle()

                var stepIdx = 1
                for (seg in 0 until points.size - 1) {
                    val p0 = points[seg]
                    val p1 = points[seg + 1]
                    val subSteps = 8
                    for (s in 1..subSteps) {
                        val t = s.toFloat() / subSteps
                        val x = p0.first + (p1.first - p0.first) * t
                        val y = p0.second + (p1.second - p0.second) * t
                        val moveEv = obtainSinglePointerEvent(
                            downTime,
                            downTime + stepIdx * 8L,
                            MotionEvent.ACTION_MOVE,
                            0,
                            MotionEvent.TOOL_TYPE_STYLUS,
                            x,
                            y,
                            0.75f
                        )
                        engine.onTouchEvent(moveEv)
                        moveEv.recycle()
                        stepIdx++
                    }
                }

                val last = points.last()
                val upEv = obtainSinglePointerEvent(
                    downTime,
                    downTime + (stepIdx + 1) * 8L,
                    MotionEvent.ACTION_UP,
                    0,
                    MotionEvent.TOOL_TYPE_STYLUS,
                    last.first,
                    last.second,
                    0.2f
                )
                engine.onTouchEvent(upEv)
                upEv.recycle()
                inProgressView.requestHandoff()
            }
            val deadline = SystemClock.uptimeMillis() + 3000L
            while (engine.state.value.strokeCount == initialCount && SystemClock.uptimeMillis() < deadline) {
                Thread.sleep(15)
            }
            instrumentation.waitForIdleSync()
        }

        // =========================================================================
        // TESTES 1 A 5: Letra "A" desenhada com UM único stroke contínuo:
        // Base esquerda (100, 300) -> Topo (200, 100) -> Base direita (300, 300)
        // -> Subida parcial (260, 220) -> Barra horizontal até (140, 220)
        // =========================================================================
        val bluePen = InkingTool.Pen(colorInt = 0xFF2196F3.toInt(), size = 6f)
        drawMultiPointStylusStroke(
            points = listOf(
                100f to 300f,
                200f to 100f,
                300f to 300f,
                260f to 220f,
                140f to 220f
            ),
            tool = bluePen
        )
        assertEquals("A letra 'A' contínua deve começar como 1 stroke único", 1, engine.state.value.strokeCount)
        val originalLetterAStroke = engine.getFinishedStrokesSnapshot().first()

        // 2 & 3: Apagar uma pequena parte da perna esquerda do "A" perto de (130, 240) com SMALL (radius=12)
        // Isso divide a perna esquerda sem tocar no topo (200,100), na perna direita ou na barra (y=220..140,220)
        performStylusEraserSwipe(
            engine,
            startX = 110f,
            startY = 250f,
            endX = 140f,
            endY = 250f,
            eraserSize = com.example.lenote.ink.EraserSize.SMALL,
            steps = 6
        )
        assertEquals(
            "Apagar um pequeno trecho da perna esquerda do 'A' deve dividir o stroke em 2 partes restantes (não remover o 'A' inteiro!)",
            2,
            engine.state.value.strokeCount
        )
        assertEquals(2, finishedView.getRenderedStrokeCount())
        for (fragment in engine.getFinishedStrokesSnapshot()) {
            assertEquals("Cor do Brush deve ser preservada após split", 0xFF2196F3.toInt(), fragment.brush.colorIntArgb)
            assertEquals("Espessura do Brush deve ser preservada após split", 6f, fragment.brush.size, 0.01f)
        }

        // 4 & 5: Apagar outra parte da letra "A" (meio da barra horizontal em x=200, y=205..235)
        performStylusEraserSwipe(
            engine,
            startX = 200f,
            startY = 205f,
            endX = 200f,
            endY = 235f,
            eraserSize = com.example.lenote.ink.EraserSize.SMALL,
            steps = 6
        )
        assertEquals(
            "Apagar outra parte na barra horizontal do 'A' deve dividir em 3 sub-strokes mantendo o topo e as pernas intactos",
            3,
            engine.state.value.strokeCount
        )

        // =========================================================================
        // TESTES 14 E 15: Undo e Redo do apagamento parcial
        // =========================================================================
        instrumentation.runOnMainSync { engine.undo() }
        assertEquals("1º Undo deve restaurar o estado com 2 sub-strokes", 2, engine.state.value.strokeCount)

        instrumentation.runOnMainSync { engine.undo() }
        assertEquals("2º Undo deve restaurar a letra 'A' original inteira (1 stroke)", 1, engine.state.value.strokeCount)
        assertTrue(
            "Undo deve restaurar exatamente a instância original do Stroke da letra 'A'",
            engine.getFinishedStrokesSnapshot().first() === originalLetterAStroke
        )

        instrumentation.runOnMainSync { engine.redo() }
        assertEquals("1º Redo deve reaplicar o primeiro corte parcial (2 sub-strokes)", 2, engine.state.value.strokeCount)

        instrumentation.runOnMainSync { engine.redo() }
        assertEquals("2º Redo deve reaplicar o segundo corte parcial (3 sub-strokes)", 3, engine.state.value.strokeCount)

        // =========================================================================
        // TESTES 6, 7 E 8: Dois strokes próximos — apagar parcialmente apenas um
        // e passar ENTRE dois strokes sem tocar em nenhum
        // =========================================================================
        instrumentation.runOnMainSync { engine.clear() }
        drawSyntheticStylusStroke(engine, inProgressView, 100f, 100f, 500f, 100f) // Stroke superior (y=100)
        drawSyntheticStylusStroke(engine, inProgressView, 100f, 160f, 500f, 160f) // Stroke inferior (y=160)
        assertEquals(2, engine.state.value.strokeCount)
        val untouchedBottomStroke = engine.getFinishedStrokesSnapshot()[1]

        // Caso C: Borracha SMALL (r=12) passa horizontalmente ENTRE os dois strokes (em y=130) -> nenhum tocado!
        performStylusEraserSwipe(
            engine,
            startX = 100f,
            startY = 130f,
            endX = 500f,
            endY = 130f,
            eraserSize = com.example.lenote.ink.EraserSize.SMALL,
            steps = 10
        )
        assertEquals("Borracha passando entre dois strokes sem tocá-los não deve alterar nenhum", 2, engine.state.value.strokeCount)

        // 7 & 8: Apagar parcialmente apenas o meio do stroke superior (x=300, y=90..110 com SMALL r=12)
        performStylusEraserSwipe(
            engine,
            startX = 300f,
            startY = 90f,
            endX = 300f,
            endY = 110f,
            eraserSize = com.example.lenote.ink.EraserSize.SMALL,
            steps = 5
        )
        assertEquals("Stroke superior deve ser dividido em 2 enquanto o inferior permanece intacto (total=3)", 3, engine.state.value.strokeCount)
        assertTrue(
            "O segundo stroke próximo não tocado deve permanecer exatamente a mesma instância",
            engine.getFinishedStrokesSnapshot().last() === untouchedBottomStroke
        )

        // =========================================================================
        // TESTES 9, 10 E 11: Testar borracha SMALL, MEDIUM e LARGE (área geométrica real)
        // =========================================================================
        instrumentation.runOnMainSync { engine.clear() }
        // Desenha 3 linhas horizontais idênticas (x = 100..600) em y = 100, 250, 400
        drawSyntheticStylusStroke(engine, inProgressView, 100f, 100f, 600f, 100f)
        drawSyntheticStylusStroke(engine, inProgressView, 100f, 250f, 600f, 250f)
        drawSyntheticStylusStroke(engine, inProgressView, 100f, 400f, 600f, 400f)

        // Corta cada linha no centro (x = 350) com SMALL, MEDIUM e LARGE respectivamente
        performStylusEraserSwipe(engine, 350f, 95f, 350f, 105f, com.example.lenote.ink.EraserSize.SMALL, steps = 4)
        performStylusEraserSwipe(engine, 350f, 245f, 350f, 255f, com.example.lenote.ink.EraserSize.MEDIUM, steps = 4)
        performStylusEraserSwipe(engine, 350f, 395f, 350f, 405f, com.example.lenote.ink.EraserSize.LARGE, steps = 4)

        val splitStrokes = engine.getFinishedStrokesSnapshot()
        assertEquals("Cada uma das 3 linhas deve ter sido dividida em 2 sub-strokes (total=6)", 6, splitStrokes.size)

        fun computeGapBetweenPair(leftStroke: androidx.ink.strokes.Stroke, rightStroke: androidx.ink.strokes.Stroke): Float {
            val leftEnd = leftStroke.inputs.get(leftStroke.inputs.size - 1).x
            val rightStart = rightStroke.inputs.get(0).x
            return rightStart - leftEnd
        }

        val gapSmall = computeGapBetweenPair(splitStrokes[0], splitStrokes[1])
        val gapMedium = computeGapBetweenPair(splitStrokes[2], splitStrokes[3])
        val gapLarge = computeGapBetweenPair(splitStrokes[4], splitStrokes[5])

        assertTrue("Gap SMALL ($gapSmall) deve ser aproximadamente 2 * 12 = 24px", gapSmall in 18f..32f)
        assertTrue("Gap MEDIUM ($gapMedium) deve ser maior que SMALL ($gapSmall)", gapMedium > gapSmall + 15f)
        assertTrue("Gap LARGE ($gapLarge) deve ser maior que MEDIUM ($gapMedium)", gapLarge > gapMedium + 25f)

        // =========================================================================
        // TESTES 12 E 13: Testar apagamento parcial com Zoom e Pan ativos
        // =========================================================================
        instrumentation.runOnMainSync {
            engine.clear()
            // Aplica Pan (+100, +80) e Zoom (2.0x) usando gesto de 2 dedos
            val downTime = SystemClock.uptimeMillis()
            val propsTwoFingers = arrayOf(
                MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER },
                MotionEvent.PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_FINGER }
            )
            // Centro inicial = (200, 200), distância = 100
            val coordsStart = arrayOf(
                MotionEvent.PointerCoords().apply { x = 150f; y = 200f },
                MotionEvent.PointerCoords().apply { x = 250f; y = 200f }
            )
            // Centro final = (300, 280), distância = 200 (zoom 2x + pan)
            val coordsMove = arrayOf(
                MotionEvent.PointerCoords().apply { x = 200f; y = 280f },
                MotionEvent.PointerCoords().apply { x = 400f; y = 280f }
            )
            val ev1 = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 1, propsTwoFingers, coordsStart, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev2 = MotionEvent.obtain(downTime, downTime + 10L, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, propsTwoFingers, coordsStart, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev3 = MotionEvent.obtain(downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 2, propsTwoFingers, coordsMove, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev4 = MotionEvent.obtain(downTime, downTime + 30L, MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, propsTwoFingers, coordsMove, 0, 0, 1f, 1f, 0, 0, 0, 0)
            val ev5 = MotionEvent.obtain(downTime, downTime + 40L, MotionEvent.ACTION_UP, 1, propsTwoFingers, coordsMove, 0, 0, 1f, 1f, 0, 0, 0, 0)

            engine.onTouchEvent(ev1); engine.onTouchEvent(ev2); engine.onTouchEvent(ev3); engine.onTouchEvent(ev4); engine.onTouchEvent(ev5)
            ev1.recycle(); ev2.recycle(); ev3.recycle(); ev4.recycle(); ev5.recycle()
        }
        assertTrue("Zoom deve estar ativo (> 1.5x)", engine.state.value.zoomLevel > 1.5f)
        // Desenha um stroke na posição de tela x=150..550, y=300 (com Zoom e Pan ativos)
        drawSyntheticStylusStroke(engine, inProgressView, 150f, 300f, 550f, 300f)
        assertEquals(1, engine.state.value.strokeCount)
        // Corta no meio em coordenadas de tela (x=350, y=280..320)
        performStylusEraserSwipe(engine, 350f, 280f, 350f, 320f, com.example.lenote.ink.EraserSize.MEDIUM, steps = 6)
        assertEquals("Apagamento parcial com Zoom e Pan ativos deve dividir o stroke em 2", 2, engine.state.value.strokeCount)

        // =========================================================================
        // TESTES 16, 17, 18 E 19:
        // - 16: Apagar um stroke inteiro
        // - 17: Múltiplos strokes formando uma letra (ex: "H" com 3 strokes)
        // - 18 & 19: Testar Pen e Highlighter
        // =========================================================================
        instrumentation.runOnMainSync {
            engine.clear()
        }

        // 17: Letra "H" formada por 3 strokes (haste esquerda, haste direita, barra central)
        drawSyntheticStylusStroke(engine, inProgressView, 100f, 100f, 100f, 300f) // Haste esq (x=100)
        drawSyntheticStylusStroke(engine, inProgressView, 300f, 100f, 300f, 300f) // Haste dir (x=300)
        drawSyntheticStylusStroke(engine, inProgressView, 100f, 200f, 300f, 200f) // Barra central (x=100..300)
        assertEquals(3, engine.state.value.strokeCount)

        // Apaga parcialmente apenas o meio da barra central (x=200, y=185..215) com SMALL (r=12)
        // A barra é dividida em [100..188] e [212..300]
        performStylusEraserSwipe(engine, 200f, 185f, 200f, 215f, com.example.lenote.ink.EraserSize.SMALL, steps = 5)
        assertEquals("Apagar o centro da barra do 'H' deve manter as 2 hastes intactas e dividir a barra em 2 (total=4)", 4, engine.state.value.strokeCount)

        // 16: Apagar um stroke separado inteiro em (x=420, y=100..300) para validar remoção 100% completa
        drawSyntheticStylusStroke(engine, inProgressView, 420f, 100f, 420f, 300f)
        assertEquals(5, engine.state.value.strokeCount)
        performStylusEraserSwipe(engine, 420f, 80f, 420f, 320f, com.example.lenote.ink.EraserSize.MEDIUM, steps = 15)
        assertEquals("Apagar ao longo de todo o stroke em x=420 deve removê-lo completamente (total=4)", 4, engine.state.value.strokeCount)

        // 18 & 19: Testar Highlighter (marca-texto) sendo parcialmente apagado
        instrumentation.runOnMainSync {
            engine.setTool(InkingTool.Highlighter(colorInt = 0x80FFEB3B.toInt(), size = 24f))
        }
        drawSyntheticStylusStroke(engine, inProgressView, 100f, 450f, 500f, 450f)
        assertEquals(5, engine.state.value.strokeCount)

        performStylusEraserSwipe(engine, 300f, 420f, 300f, 480f, com.example.lenote.ink.EraserSize.MEDIUM, steps = 6)
        assertEquals("Traço de Highlighter também deve ser dividido parcialmente em 2 sub-strokes (total=6)", 6, engine.state.value.strokeCount)
    }

    @Test
    fun verifyPartialEraser_performanceAndGeometryRegression_manyStrokesAndHistoricalPoints() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val engine = JetpackInkEngine()
        lateinit var inProgressView: InProgressStrokesView
        lateinit var finishedView: FinishedStrokesView

        activityRule.scenario.onActivity { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
            val container = FrameLayout(activity)
            finishedView = FinishedStrokesView(activity)
            inProgressView = InProgressStrokesView(activity)
            container.addView(finishedView, FrameLayout.LayoutParams(1000, 1000))
            container.addView(inProgressView, FrameLayout.LayoutParams(1000, 1000))
            activity.setContentView(container)

            inProgressView.eagerInit()
            engine.attachView(inProgressView)
            engine.attachFinishedView(finishedView)
        }
        instrumentation.waitForIdleSync()
        Thread.sleep(200)

        // 1. Cria 60 strokes na página (distribuídos em grade: 3 colunas x 20 linhas)
        // para simular uma página densa de anotações no Lenovo Xiaoxin Pad 2022.
        val totalInitialStrokes = 60
        for (i in 0 until totalInitialStrokes) {
            val col = i / 20
            val row = i % 20
            val startX = 60f + col * 300f
            val endX = startX + 220f
            val y = 60f + row * 40f
            drawSyntheticStylusStroke(engine, inProgressView, startX, y, endX, y)
        }
        assertEquals(totalInitialStrokes, engine.state.value.strokeCount)
        val initialStrokesSnapshot = engine.getFinishedStrokesSnapshot()

        // 2. Simula um gesto rápido de borracha SMALL (r=12) cortando verticalmente APENAS
        // os 5 primeiros strokes da 1ª coluna (x=170, y=40 -> 235) em 20 eventos ACTION_MOVE
        // a 240Hz. Os outros 55 strokes estão longe da área da borracha.
        var gestureElapsedMs = 0L
        instrumentation.runOnMainSync {
            engine.setTool(InkingTool.Eraser(com.example.lenote.ink.EraserSize.SMALL))
            val downTime = SystemClock.uptimeMillis()
            val t0 = System.nanoTime()

            val downEv = obtainSinglePointerEvent(
                downTime, downTime, MotionEvent.ACTION_DOWN, 0, MotionEvent.TOOL_TYPE_STYLUS, 170f, 40f
            )
            engine.onTouchEvent(downEv)
            downEv.recycle()

            val moveSteps = 20
            for (step in 1..moveSteps) {
                val t = step.toFloat() / moveSteps
                val curY = 40f + (235f - 40f) * t
                val moveEv = obtainSinglePointerEvent(
                    downTime, downTime + step * 4L, MotionEvent.ACTION_MOVE, 0, MotionEvent.TOOL_TYPE_STYLUS, 170f, curY
                )
                engine.onTouchEvent(moveEv)
                moveEv.recycle()
            }

            val upEv = obtainSinglePointerEvent(
                downTime, downTime + 90L, MotionEvent.ACTION_UP, 0, MotionEvent.TOOL_TYPE_STYLUS, 170f, 235f
            )
            engine.onTouchEvent(upEv)
            upEv.recycle()

            gestureElapsedMs = (System.nanoTime() - t0) / 1_000_000L
        }

        // 3. Verifica que APENAS os 5 primeiros strokes foram divididos em 2 (60 + 5 = 65 strokes)
        // e que os outros 55 strokes permaneceram exatamente as mesmas instâncias.
        assertEquals(
            "Os 5 primeiros strokes devem ser divididos em 2 cada (total = 65)",
            totalInitialStrokes + 5,
            engine.state.value.strokeCount
        )

        val stats = engine.lastEraserProfilingStats
        val avgMoveTimeMicros = (stats.totalHitTestNanos / 1000L) / stats.moveEventsProcessed.coerceAtLeast(1)

        // Verifica eficiência do filtro espacial (Bounding Box pre-filtering):
        // de ~1260 verificações totais (21 eventos * ~60 strokes), apenas uma pequena fração
        // passa pelo filtro AABB para o cálculo geométrico detalhado!
        assertTrue(
            "Filtro espacial AABB deve descartar >90% dos strokes distantes (examinados=${stats.totalStrokesExamined}, candidatos=${stats.candidateStrokesPassedBBox})",
            stats.candidateStrokesPassedBBox < stats.totalStrokesExamined / 8
        )
        assertTrue(
            "Reconstruções de Stroke devem ocorrer apenas nos 5 traços atingidos (modificados=${stats.strokesActuallyModified} de ${stats.totalStrokesExamined} examinados)",
            stats.strokesActuallyModified in 5..15
        )
        assertTrue(
            "Tempo médio por ACTION_MOVE ($avgMoveTimeMicros us, total=${gestureElapsedMs}ms) deve ser < 2000 us (< 2ms) para fluidez a 60/120Hz",
            avgMoveTimeMicros < 2000L
        )

        // 4. Verifica que Undo restaura os 60 strokes originais em 1 única operação atômica
        instrumentation.runOnMainSync { engine.undo() }
        assertEquals(totalInitialStrokes, engine.state.value.strokeCount)
        val restoredSnapshot = engine.getFinishedStrokesSnapshot()
        for (i in 0 until totalInitialStrokes) {
            assertTrue(
                "Undo deve restaurar a instância exata do stroke $i",
                initialStrokesSnapshot[i] === restoredSnapshot[i]
            )
        }

        // 5. Verifica que Redo reaplica exatamente as mesmas 5 divisões parciais (65 strokes)
        instrumentation.runOnMainSync { engine.redo() }
        assertEquals(totalInitialStrokes + 5, engine.state.value.strokeCount)

        // 6. Verifica o tempo real de onDraw(canvas) no tablet (Lenovo_TB128FU) com Static Layer Bitmap Cache
        // sobre fundo MATHEMATICS + 65 strokes durante 30 frames de movimento da borracha
        val frameBitmap = android.graphics.Bitmap.createBitmap(1000, 1000, android.graphics.Bitmap.Config.ARGB_8888)
        val frameCanvas = android.graphics.Canvas(frameBitmap)
        var avgDrawFrameMicros = 0L
        var fastBlitsDelta = 0

        instrumentation.runOnMainSync {
            engine.setPageBackground(com.example.lenote.ink.PageBackground.MATHEMATICS)
            // Primeiro frame aquece o staticCacheBitmap com os 65 strokes + fundo MATHEMATICS
            finishedView.draw(frameCanvas)
            val blitsBefore = finishedView.fastBitmapBlitFrameCount
            val rebuildsBefore = finishedView.staticCacheRebuildCount

            val hoverTime = SystemClock.uptimeMillis()
            val tDrawStart = System.nanoTime()
            val frames = 30
            for (f in 1..frames) {
                val hx = 150f + f * 15f
                val hy = 150f + f * 10f
                val hoverMove = obtainSinglePointerEvent(
                    hoverTime, hoverTime + f * 8L, MotionEvent.ACTION_HOVER_MOVE, 0, MotionEvent.TOOL_TYPE_STYLUS, hx, hy
                )
                engine.onHoverEvent(hoverMove)
                hoverMove.recycle()
                finishedView.draw(frameCanvas)
            }
            val totalDrawNanos = System.nanoTime() - tDrawStart
            avgDrawFrameMicros = (totalDrawNanos / 1000L) / frames
            fastBlitsDelta = finishedView.fastBitmapBlitFrameCount - blitsBefore
            val rebuildsDelta = finishedView.staticCacheRebuildCount - rebuildsBefore
            assertEquals("Cache estático de strokes não deve ser reconstruído ao mover o cursor da borracha", 0, rebuildsDelta)
            // 6b. Agora simula um gesto ATIVO de corte (ACTION_DOWN + 20 ACTION_MOVE cortando a 2ª coluna)
            // renderizando finishedView.draw(frameCanvas) a cada frame (60fps) e garantindo ZERO rebuilds completos!
            val rebuildsBeforeActiveCut = finishedView.staticCacheRebuildCount
            val patchesBeforeActiveCut = finishedView.staticCachePartialPatchCount
            val cutDownTime = SystemClock.uptimeMillis()
            val cutDown = obtainSinglePointerEvent(
                cutDownTime, cutDownTime, MotionEvent.ACTION_DOWN, 0, MotionEvent.TOOL_TYPE_STYLUS, 470f, 40f
            )
            engine.onTouchEvent(cutDown)
            cutDown.recycle()
            finishedView.draw(frameCanvas)

            for (step in 1..20) {
                val curY = 40f + (235f - 40f) * (step.toFloat() / 20f)
                val cutMove = obtainSinglePointerEvent(
                    cutDownTime, cutDownTime + step * 16L, MotionEvent.ACTION_MOVE, 0, MotionEvent.TOOL_TYPE_STYLUS, 470f, curY
                )
                engine.onTouchEvent(cutMove)
                cutMove.recycle()
                finishedView.draw(frameCanvas)
            }
            val rebuildsDuringActiveCut = finishedView.staticCacheRebuildCount - rebuildsBeforeActiveCut
            val patchesDuringActiveCut = finishedView.staticCachePartialPatchCount - patchesBeforeActiveCut
            assertEquals(
                "Durante o gesto ativo de corte (ACTION_MOVE), NUNCA deve haver rebuild completo do staticCacheBitmap",
                0,
                rebuildsDuringActiveCut
            )
            assertTrue(
                "Os strokes atingidos na 2ª coluna devem atualizar apenas suas sub-regiões via Dirty-Rect Patch",
                patchesDuringActiveCut >= 1
            )

            val cutUp = obtainSinglePointerEvent(
                cutDownTime, cutDownTime + 350L, MotionEvent.ACTION_UP, 0, MotionEvent.TOOL_TYPE_STYLUS, 470f, 235f
            )
            engine.onTouchEvent(cutUp)
            cutUp.recycle()
        }
        frameBitmap.recycle()

        assertEquals("Todos os 30 frames devem usar o caminho rápido de Bitmap Blit", 30, fastBlitsDelta)
        assertTrue(
            "Tempo médio de onDraw por frame ($avgDrawFrameMicros us) deve ser < 3500 us (< 3.5ms) para 60fps constantes no tablet real",
            avgDrawFrameMicros < 3500L
        )
    }

    @Test
    fun verifyPhase2PersistenceAndNotebookPageSwitching() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var vm: com.example.lenote.ui.InkPlaygroundViewModel
        lateinit var inkEngine: JetpackInkEngine
        lateinit var strokeView: InProgressStrokesView
        lateinit var finishedView: FinishedStrokesView

        activityRule.scenario.onActivity { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
            vm = activity.playgroundViewModel
            inkEngine = vm.engine
            val container = FrameLayout(activity)
            finishedView = FinishedStrokesView(activity)
            strokeView = InProgressStrokesView(activity)
            container.addView(finishedView, FrameLayout.LayoutParams(1000, 1000))
            container.addView(strokeView, FrameLayout.LayoutParams(1000, 1000))
            activity.setContentView(container)

            strokeView.eagerInit()
            inkEngine.attachView(strokeView)
            inkEngine.attachFinishedView(finishedView)
        }
        instrumentation.waitForIdleSync()
        Thread.sleep(300)

        instrumentation.runOnMainSync {
            inkEngine.clear()
            inkEngine.setTool(InkingTool.Pen(colorInt = 0xFF000000.toInt()))
        }

        val pageA = requireNotNull(vm.currentPage.value)
        drawSyntheticStylusStroke(inkEngine, strokeView, 220f, 320f, 520f, 320f)
        drawSyntheticStylusStroke(inkEngine, strokeView, 220f, 380f, 520f, 380f)
        assertEquals(2, inkEngine.state.value.strokeCount)

        // Cria nova página (auto-salva a página A e abre a página B vazia)
        instrumentation.runOnMainSync {
            vm.createNewPage("Página Teste B")
        }
        val pageB = requireNotNull(vm.currentPage.value)
        assertFalse(pageA.id == pageB.id)
        assertEquals(0, inkEngine.state.value.strokeCount)

        drawSyntheticStylusStroke(inkEngine, strokeView, 300f, 400f, 600f, 450f)
        assertEquals(1, inkEngine.state.value.strokeCount)

        // Volta para a página A e verifica que os 2 traços foram restaurados do arquivo .ink
        instrumentation.runOnMainSync {
            vm.selectPage(pageA)
        }
        assertEquals(pageA.id, vm.currentPage.value?.id)
        assertEquals(2, inkEngine.state.value.strokeCount)
    }

    @Test
    fun verifyPhase5LassoToolStylusButtonEraserAndExport() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val inkEngine = JetpackInkEngine()
        lateinit var strokeView: InProgressStrokesView
        lateinit var finishedView: FinishedStrokesView
        lateinit var ctx: android.content.Context

        activityRule.scenario.onActivity { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
            ctx = activity.applicationContext
            val container = FrameLayout(activity)
            finishedView = FinishedStrokesView(activity)
            strokeView = InProgressStrokesView(activity)
            container.addView(finishedView, FrameLayout.LayoutParams(1000, 1000))
            container.addView(strokeView, FrameLayout.LayoutParams(1000, 1000))
            activity.setContentView(container)

            strokeView.eagerInit()
            inkEngine.attachView(strokeView)
            inkEngine.attachFinishedView(finishedView)
        }
        instrumentation.waitForIdleSync()
        Thread.sleep(300)

        instrumentation.runOnMainSync {
            inkEngine.clear()
            inkEngine.setCameraTransform(1.0f, 0f, 0f)
            inkEngine.setTool(InkingTool.Pen(colorInt = 0xFF000000.toInt()))
        }

        // 1. Desenha um traço horizontal em y = 300, x = 200..400
        drawSyntheticStylusStroke(inkEngine, strokeView, 200f, 300f, 400f, 300f)
        assertEquals(1, inkEngine.state.value.strokeCount)

        // 2. Seleciona a ferramenta Laço e circula a região (150, 250) -> (450, 350)
        instrumentation.runOnMainSync {
            inkEngine.setTool(InkingTool.Lasso)
            val downTime = SystemClock.uptimeMillis()
            val lassoPts = listOf(
                Pair(150f, 250f),
                Pair(450f, 250f),
                Pair(450f, 350f),
                Pair(150f, 350f),
                Pair(150f, 250f)
            )
            val downEv = obtainSinglePointerEvent(
                downTime, downTime, MotionEvent.ACTION_DOWN, 0, MotionEvent.TOOL_TYPE_STYLUS, lassoPts[0].first, lassoPts[0].second
            )
            inkEngine.onTouchEvent(downEv)
            downEv.recycle()

            for (i in 1 until lassoPts.size) {
                val moveEv = obtainSinglePointerEvent(
                    downTime, downTime + i * 10L, MotionEvent.ACTION_MOVE, 0, MotionEvent.TOOL_TYPE_STYLUS, lassoPts[i].first, lassoPts[i].second
                )
                inkEngine.onTouchEvent(moveEv)
                moveEv.recycle()
            }

            val upEv = obtainSinglePointerEvent(
                downTime, downTime + 100L, MotionEvent.ACTION_UP, 0, MotionEvent.TOOL_TYPE_STYLUS, lassoPts.last().first, lassoPts.last().second
            )
            inkEngine.onTouchEvent(upEv)
            upEv.recycle()
        }

        assertEquals("O Laço deve selecionar o traço dentro do polígono", 1, inkEngine.state.value.selectedStrokeCount)

        // 3. Duplica a seleção (+1 traço -> total 2) e testa Undo/Redo
        instrumentation.runOnMainSync {
            inkEngine.duplicateSelectedStrokesByOffset(40f, 40f)
        }
        assertEquals(2, inkEngine.state.value.strokeCount)

        instrumentation.runOnMainSync { inkEngine.undo() }
        assertEquals(1, inkEngine.state.value.strokeCount)

        instrumentation.runOnMainSync { inkEngine.redo() }
        assertEquals(2, inkEngine.state.value.strokeCount)

        // 4. Testa Exportação PDF e PNG reais
        val strokesSnapshot = inkEngine.getFinishedStrokesSnapshot()
        val pngFile = com.example.lenote.export.PageExporter.exportPageToPng(
            ctx, "TesteExport", com.example.lenote.ink.PageBackground.GRID, strokesSnapshot, 600, 800
        )
        val pdfFile = com.example.lenote.export.PageExporter.exportPageToPdf(
            ctx, "TesteExport", com.example.lenote.ink.PageBackground.GRID, strokesSnapshot, 600, 800
        )
        assertTrue(pngFile.exists() && pngFile.length() > 100L)
        assertTrue(pdfFile.exists() && pdfFile.length() > 100L)
        pngFile.delete()
        pdfFile.delete()
    }
}

