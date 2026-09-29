package com.pegion.download.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe bandwidth throttle supporting concurrent multi-segment workers.
 */
class SpeedLimiter(var bytesPerSecond: Long = 0L) {

    private val mutex = Mutex()
    private var bytesTransferredThisSecond: Long = 0L
    private var lastResetTime: Long = System.currentTimeMillis()

    suspend fun throttle(bytesCount: Int) {
        if (bytesPerSecond <= 0) return

        var sleepTime = 0L
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastResetTime

            if (elapsed >= 1000) {
                bytesTransferredThisSecond = 0
                lastResetTime = now
            }

            bytesTransferredThisSecond += bytesCount

            if (bytesTransferredThisSecond >= bytesPerSecond) {
                sleepTime = 1000 - elapsed
                bytesTransferredThisSecond = 0
                lastResetTime = System.currentTimeMillis()
            }
        }

        if (sleepTime > 0) {
            delay(sleepTime)
        }
    }
}
