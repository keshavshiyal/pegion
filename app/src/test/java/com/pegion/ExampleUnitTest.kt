package com.pegion

import com.pegion.download.engine.ChecksumVerifier
import com.pegion.download.engine.SpeedLimiter
import com.pegion.download.model.DownloadSegment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun segment_totalBytes_and_progress_isCorrect() {
        val segment = DownloadSegment(
            segmentIndex = 0,
            startByte = 0L,
            endByte = 999L,
            downloadedBytes = 500L,
            isFinished = false
        )
        assertEquals(1000L, segment.totalBytes)
        assertEquals(50.0f, segment.progress, 0.01f)
    }

    @Test
    fun segment_partition_math_covers_entire_file() {
        val totalFileSize = 10_000_000L
        val segmentCount = 4
        val segLength = totalFileSize / segmentCount

        val segments = (0 until segmentCount).map { i ->
            val start = i * segLength
            val end = if (i == segmentCount - 1) totalFileSize - 1 else (i + 1) * segLength - 1
            DownloadSegment(
                segmentIndex = i,
                startByte = start,
                endByte = end,
                downloadedBytes = 0L,
                isFinished = false
            )
        }

        assertEquals(4, segments.size)
        assertEquals(0L, segments.first().startByte)
        assertEquals(totalFileSize - 1, segments.last().endByte)

        // Ensure no overlapping and no gaps
        for (i in 0 until segments.size - 1) {
            assertEquals(segments[i].endByte + 1, segments[i + 1].startByte)
        }

        val totalCovered = segments.sumOf { it.totalBytes }
        assertEquals(totalFileSize, totalCovered)
    }

    @Test
    fun segment_partition_math_with_uneven_sizes_covers_exact_bytes() {
        // Test arbitrary uneven file size across 6 segments
        val totalFileSize = 10_000_007L
        val segmentCount = 6
        val segLength = totalFileSize / segmentCount

        val segments = (0 until segmentCount).map { i ->
            val start = i * segLength
            val end = if (i == segmentCount - 1) totalFileSize - 1 else (i + 1) * segLength - 1
            DownloadSegment(
                segmentIndex = i,
                startByte = start,
                endByte = end,
                downloadedBytes = 0L,
                isFinished = false
            )
        }

        assertEquals(6, segments.size)
        assertEquals(0L, segments.first().startByte)
        assertEquals(totalFileSize - 1, segments.last().endByte)

        for (i in 0 until segments.size - 1) {
            assertEquals(segments[i].endByte + 1, segments[i + 1].startByte)
        }

        val totalCovered = segments.sumOf { it.totalBytes }
        assertEquals(totalFileSize, totalCovered)
    }

    @Test
    fun checksum_algorithm_normalization_isCorrect() {
        assertEquals("SHA-256", ChecksumVerifier.normalizeAlgorithm("sha256"))
        assertEquals("SHA-256", ChecksumVerifier.normalizeAlgorithm("sha-256"))
        assertEquals("SHA-256", ChecksumVerifier.normalizeAlgorithm(" SHA256 "))
        assertEquals("MD5", ChecksumVerifier.normalizeAlgorithm("md5"))
        assertEquals("MD5", ChecksumVerifier.normalizeAlgorithm("Md5"))
        assertEquals("SHA-1", ChecksumVerifier.normalizeAlgorithm("sha1"))
        assertEquals("SHA-512", ChecksumVerifier.normalizeAlgorithm("sha512"))
    }

    @Test
    fun speed_limiter_does_not_block_when_disabled() = runBlocking {
        val limiter = SpeedLimiter(bytesPerSecond = 0L)
        val start = System.currentTimeMillis()
        limiter.throttle(1024 * 1024)
        val elapsed = System.currentTimeMillis() - start
        assertTrue(elapsed < 200)
    }
}
