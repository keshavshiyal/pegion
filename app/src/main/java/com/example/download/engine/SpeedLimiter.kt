package com.example.download.engine

import kotlinx.coroutines.delay

class SpeedLimiter(var bytesPerSecond: Long = 0L) {

    private var bytesTransferredThisSecond: Long = 0L
    private var lastResetTime: Long = System.currentTimeMillis()

    suspend fun throttle(bytesCount: Int) {
        if (bytesPerSecond <= 0) return

        val now = System.currentTimeMillis()
        val elapsed = now - lastResetTime

        if (elapsed >= 1000) {
            bytesTransferredThisSecond = 0
            lastResetTime = now
        }

        bytesTransferredThisSecond += bytesCount

        if (bytesTransferredThisSecond >= bytesPerSecond) {
            val sleepTime = 1000 - elapsed
            if (sleepTime > 0) {
                delay(sleepTime)
            }
            bytesTransferredThisSecond = 0
            lastResetTime = System.currentTimeMillis()
        }
    }
}
