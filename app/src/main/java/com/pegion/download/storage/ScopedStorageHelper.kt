package com.pegion.download.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.pegion.R
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

/**
 * ScopedStorageHelper provides standardized, safe access to Android storage,
 * MediaStore indexing, Scoped Storage compatibility (Android 10+ / API 29+),
 * and intent dispatching for viewing and sharing downloaded files.
 */
object ScopedStorageHelper {

    /**
     * Resolves the most accurate MIME type for a file, leveraging extension mappings,
     * header-based MIME types, and system fallbacks.
     */
    fun resolveMimeType(file: File, serverMime: String? = null): String {
        // If server provided a specific MIME type (not generic octet-stream), prefer it
        if (!serverMime.isNullOrBlank() &&
            serverMime != "*/*" &&
            serverMime != "application/octet-stream" &&
            serverMime != "binary/octet-stream"
        ) {
            return serverMime
        }

        // Try file extension lookup
        val extension = file.extension.lowercase()
        if (extension.isNotBlank()) {
            val mimeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (!mimeFromExt.isNullOrBlank()) {
                return mimeFromExt
            }

            // Common types that may be absent from some device MimeTypeMaps
            when (extension) {
                "apk" -> return "application/vnd.android.package-archive"
                "mkv" -> return "video/x-matroska"
                "epub" -> return "application/epub+zip"
                "7z" -> return "application/x-7z-compressed"
                "rar" -> return "application/x-rar-compressed"
                "tar" -> return "application/x-tar"
                "gz" -> return "application/gzip"
                "flac" -> return "audio/flac"
                "opus" -> return "audio/opus"
                "webp" -> return "image/webp"
                "svg" -> return "image/svg+xml"
                "json" -> return "application/json"
                "md" -> return "text/markdown"
                "pdf" -> return "application/pdf"
            }
        }

        // Try URLConnection content guessing
        val guessed = try {
            URLConnection.guessContentTypeFromName(file.name)
        } catch (_: Exception) {
            null
        }

        return guessed ?: serverMime ?: "*/*"
    }

    /**
     * Obtains a secure content URI using FileProvider for inter-app sharing.
     */
    fun getSafeContentUri(context: Context, file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }
    }

    /**
     * Indexes a completed download into the Android system MediaStore via MediaScannerConnection.
     * This immediately exposes the file to the system Downloads app, galleries, and third-party file pickers.
     */
    fun scanCompletedFile(
        context: Context,
        file: File,
        mimeType: String? = null,
        onScanned: ((Uri?) -> Unit)? = null
    ) {
        if (!file.exists()) return

        val resolvedMime = resolveMimeType(file, mimeType)
        try {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                arrayOf(resolvedMime)
            ) { _, uri ->
                onScanned?.invoke(uri)
            }
        } catch (_: Exception) {
            onScanned?.invoke(null)
        }
    }

    /**
     * On Android 10+ (API 29+), copies a downloaded file into the public MediaStore.Downloads collection
     * using the IS_PENDING scoped storage flag for atomic publishing.
     */
    fun publishToDownloadsMediaStore(
        context: Context,
        sourceFile: File,
        displayName: String = sourceFile.name,
        mimeType: String? = null
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !sourceFile.exists()) {
            return null
        }

        val resolver = context.contentResolver
        val resolvedMime = resolveMimeType(sourceFile, mimeType)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, resolvedMime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Pegion")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collectionUri, values) ?: return null

        return try {
            resolver.openOutputStream(itemUri)?.use { outStream ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            // Clear IS_PENDING flag to make visible across the system
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            itemUri
        } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            null
        }
    }

    /**
     * Builds an Intent to view/open the downloaded file with an appropriate viewer.
     */
    fun buildOpenFileIntent(context: Context, file: File, mimeType: String? = null): Intent {
        val uri = getSafeContentUri(context, file)
        val resolvedMime = resolveMimeType(file, mimeType)

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, resolvedMime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Opens a file safely, displaying a user-friendly Toast if no suitable viewer is installed.
     */
    fun openFile(context: Context, filePath: String, mimeType: String? = null) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = buildOpenFileIntent(context, file, mimeType)
            val chooser = Intent.createChooser(intent, "Open file with…").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, R.string.no_app_to_open, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Builds an Intent to share the file with other applications via standard Android Sharesheet.
     */
    fun buildShareFileIntent(context: Context, file: File, mimeType: String? = null): Intent {
        val uri = getSafeContentUri(context, file)
        val resolvedMime = resolveMimeType(file, mimeType)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = resolvedMime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share file via…").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Shares a file safely using system sharesheet.
     */
    fun shareFile(context: Context, filePath: String, mimeType: String? = null) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val chooser = buildShareFileIntent(context, file, mimeType)
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot share file: No suitable app found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Safely deletes a file from disk and cleans up any MediaStore registry entries.
     */
    fun deleteFileSafely(context: Context, file: File): Boolean {
        var deleted = false
        try {
            if (file.exists()) {
                deleted = file.delete()
            }
        } catch (_: Exception) {}

        // Also clean up MediaStore entry if present
        try {
            val resolver = context.contentResolver
            val uri = MediaStore.Files.getContentUri("external")
            resolver.delete(uri, "${MediaStore.MediaColumns.DATA}=?", arrayOf(file.absolutePath))
        } catch (_: Exception) {}

        return deleted
    }
}
