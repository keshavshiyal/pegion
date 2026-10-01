package com.pegion.download.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe, high-precision Token Bucket rate limiter for parallel multi-segment downloads.
 * Distributes bandwidth smoothly across concurrent workers without starvation, jitter, or burst stutter.
 */
class SpeedLimiter(var bytesPerSecond: Long = 0L) {

    private val mutex = Mutex()
    private var availableTokens: Double = 0.0
    private var lastTokenTime: Long = System.currentTimeMillis()

    suspend fun throttle(bytesCount: Int) {
        val limit = bytesPerSecond
        if (limit <= 0) return

        var delayMs = 0L

        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsedMs = (now - lastTokenTime).coerceAtLeast(0L)
            lastTokenTime = now

            // Replenish tokens based on elapsed time (max burst = 1 second worth of tokens)
            val newTokens = (elapsedMs * limit.toDouble()) / 1000.0
            availableTokens = (availableTokens + newTokens).coerceAtMost(limit.toDouble())

            // Deduct tokens
            availableTokens -= bytesCount

            if (availableTokens < 0) {
                // Compute required wait time to replenish deficit
                val deficit = -availableTokens
                delayMs = ((deficit * 1000.0) / limit.toDouble()).toLong().coerceIn(1L, 2000L)
            }
        }

        if (delayMs > 0) {
            delay(delayMs)
        }
    }
}
