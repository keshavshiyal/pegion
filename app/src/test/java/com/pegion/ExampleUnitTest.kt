package com.pegion

import com.pegion.download.model.DownloadSegment
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
}
