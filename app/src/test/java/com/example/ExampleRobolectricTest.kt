package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.download.engine.ChecksumVerifier
import com.example.download.engine.DownloadEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pegion", appName)
  }

  @Test
  fun `resolve filename from URL correctly`() {
    val resolved = DownloadEngine.resolveFileName("https://example.com/archive.zip", null)
    assertEquals("archive.zip", resolved)

    val custom = DownloadEngine.resolveFileName("https://example.com/file", "custom_name.tar.gz")
    assertEquals("custom_name.tar.gz", custom)
  }

  @Test
  fun `checksum calculation test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testFile = File(context.cacheDir, "test_checksum.txt")
    testFile.writeText("Pegion always delivers.")

    val md5 = ChecksumVerifier.calculateChecksum(testFile, "MD5")
    assertNotNull(md5)
    assertEquals(32, md5.length)

    val sha256 = ChecksumVerifier.calculateChecksum(testFile, "SHA-256")
    assertNotNull(sha256)
    assertEquals(64, sha256.length)

    testFile.delete()
  }
}
