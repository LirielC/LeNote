package com.example.lenote

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lenote.ink.JetpackInkEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationLockInstrumentedTest {

    @Test
    fun navigationLock_blocksFingerPanAndZoomCommands_untilUnlocked() {
        val engine = JetpackInkEngine()
        engine.setCameraTransform(2f, 50f, 60f)
        engine.setNavigationLocked(true)

        val downTime = SystemClock.uptimeMillis()
        engine.dispatch(fingerEvent(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f))
        engine.dispatch(fingerEvent(downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 220f, 260f))
        engine.dispatch(fingerEvent(downTime, downTime + 40L, MotionEvent.ACTION_UP, 220f, 260f))
        engine.zoomIn()
        engine.zoomOut()
        engine.resetZoom()
        engine.fitToPage()

        assertTrue(engine.state.value.isNavigationLocked)
        assertEquals(2f, engine.state.value.zoomLevel, 0.001f)
        assertEquals(50f, engine.state.value.panOffsetX, 0.001f)
        assertEquals(60f, engine.state.value.panOffsetY, 0.001f)

        engine.setNavigationLocked(false)
        engine.zoomIn()

        assertTrue(engine.state.value.zoomLevel > 2f)
    }

    private fun fingerEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float
    ): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val coordinates = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = 1f
            size = 1f
        }
        return MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            1,
            arrayOf(properties),
            arrayOf(coordinates),
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )
    }

    private fun JetpackInkEngine.dispatch(event: MotionEvent) {
        try {
            onTouchEvent(event)
        } finally {
            event.recycle()
        }
    }
}
