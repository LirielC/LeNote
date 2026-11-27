package com.example.lenote.ink

import kotlin.math.ceil

/** Bounded, allocation-free sample collection for input and frame diagnostics. */
class InkPerformanceMonitor(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val frameBudgetNanos: Long = DEFAULT_FRAME_BUDGET_NANOS
) {
    private val inputNanos = LongArray(capacity)
    private val inputIntervalsMillis = LongArray(capacity)
    private val frameNanos = LongArray(capacity)
    private var inputIndex = 0
    private var inputSize = 0
    private var frameIndex = 0
    private var frameSize = 0
    private var inputCount = 0L
    private var frameCount = 0L
    private var framesOverBudget = 0L
    private var historicalSamples = 0L

    init {
        require(capacity > 0)
    }

    @Synchronized
    fun recordInput(durationNanos: Long, intervalMillis: Long, historicalSampleCount: Int) {
        inputNanos[inputIndex] = durationNanos.coerceAtLeast(0L)
        inputIntervalsMillis[inputIndex] = intervalMillis.coerceAtLeast(0L) * 1_000_000L
        inputIndex = (inputIndex + 1) % capacity
        inputSize = minOf(inputSize + 1, capacity)
        inputCount++
        historicalSamples += historicalSampleCount.coerceAtLeast(0)
    }

    @Synchronized
    fun recordFrame(durationNanos: Long) {
        val duration = durationNanos.coerceAtLeast(0L)
        frameNanos[frameIndex] = duration
        frameIndex = (frameIndex + 1) % capacity
        frameSize = minOf(frameSize + 1, capacity)
        frameCount++
        if (duration > frameBudgetNanos) framesOverBudget++
    }

    @Synchronized
    fun snapshot(): InkPerformanceSnapshot = InkPerformanceSnapshot(
        inputSamples = inputCount,
        inputP50Millis = percentile(inputNanos, inputSize, 0.50),
        inputP95Millis = percentile(inputNanos, inputSize, 0.95),
        inputP99Millis = percentile(inputNanos, inputSize, 0.99),
        eventIntervalP50Millis = percentile(inputIntervalsMillis, inputSize, 0.50),
        historicalSamples = historicalSamples,
        frameSamples = frameCount,
        frameP50Millis = percentile(frameNanos, frameSize, 0.50),
        frameP95Millis = percentile(frameNanos, frameSize, 0.95),
        frameP99Millis = percentile(frameNanos, frameSize, 0.99),
        framesOverBudget = framesOverBudget
    )

    private fun percentile(source: LongArray, size: Int, percentile: Double): Double {
        if (size == 0) return 0.0
        val sorted = source.copyOf(size)
        sorted.sort()
        val index = (ceil(size * percentile).toInt() - 1).coerceIn(0, size - 1)
        return sorted[index] / 1_000_000.0
    }

    companion object {
        const val DEFAULT_CAPACITY = 512
        const val DEFAULT_FRAME_BUDGET_NANOS = 16_670_000L
    }
}

data class InkPerformanceSnapshot(
    val inputSamples: Long,
    val inputP50Millis: Double,
    val inputP95Millis: Double,
    val inputP99Millis: Double,
    val eventIntervalP50Millis: Double,
    val historicalSamples: Long,
    val frameSamples: Long,
    val frameP50Millis: Double,
    val frameP95Millis: Double,
    val frameP99Millis: Double,
    val framesOverBudget: Long
)
