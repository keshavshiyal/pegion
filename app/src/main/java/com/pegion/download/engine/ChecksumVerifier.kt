package com.pegion.download.engine

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.security.MessageDigest

object ChecksumVerifier {

    fun normalizeAlgorithm(name: String): String {
        return when (name.trim().uppercase().replace("-", "").replace(" ", "")) {
            "MD5" -> "MD5"
            "SHA256" -> "SHA-256"
            "SHA1" -> "SHA-1"
            "SHA512" -> "SHA-512"
            else -> name.trim()
        }
    }

    fun calculateChecksum(file: File, rawAlgorithm: String): String {
        if (!file.exists() || !file.canRead()) {
            throw FileNotFoundException("File not accessible: ${file.absolutePath}")
        }
        val algorithm = normalizeAlgorithm(rawAlgorithm)
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(65536) // 64KB buffer for efficient disk I/O
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

        return try {
            val actual = calculateChecksum(file, type)
            actual.equals(expected.trim(), ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
