package com.example.lenote

import com.example.lenote.ink.InkPerformanceMonitor
import org.junit.Assert.assertEquals
import org.junit.Test

class InkPerformanceMonitorTest {
    @Test
    fun keepsOnlyBoundedRecentSamplesAndReportsPercentiles() {
        val monitor = InkPerformanceMonitor(capacity = 3, frameBudgetNanos = 16_000_000L)
        monitor.recordInput(1_000_000L, 10L, 2)
        monitor.recordInput(2_000_000L, 20L, 1)
        monitor.recordInput(3_000_000L, 30L, 0)
        monitor.recordInput(4_000_000L, 40L, 4)
        monitor.recordFrame(10_000_000L)
        monitor.recordFrame(20_000_000L)

        val snapshot = monitor.snapshot()

        assertEquals(4L, snapshot.inputSamples)
        assertEquals(3.0, snapshot.inputP50Millis, 0.0)
        assertEquals(4.0, snapshot.inputP95Millis, 0.0)
        assertEquals(30.0, snapshot.eventIntervalP50Millis, 0.0)
        assertEquals(7L, snapshot.historicalSamples)
        assertEquals(2L, snapshot.frameSamples)
        assertEquals(1L, snapshot.framesOverBudget)
    }
}
