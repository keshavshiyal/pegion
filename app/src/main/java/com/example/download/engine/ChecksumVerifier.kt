package com.example.download.engine

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ChecksumVerifier {

    fun calculateChecksum(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verify(file: File, type: String?, expected: String?): Boolean {
        if (type.isNullOrBlank() || expected.isNullOrBlank()) return true
        if (!file.exists()) return false

        val algorithm = when (type.uppercase()) {
            "MD5" -> "MD5"
            "SHA-256", "SHA256" -> "SHA-256"
            "SHA-1", "SHA1" -> "SHA-1"
            else -> return true
        }

        return try {
            val actual = calculateChecksum(file, algorithm)
            actual.equals(expected.trim(), ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
