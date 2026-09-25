package com.example.ui.screens.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DownloadEntity
import com.example.download.model.DownloadPriority
import com.example.download.model.DownloadStatus
import com.example.download.model.SpeedSample
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SpeedHistoryChart
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatBytes
import com.example.ui.components.formatEta
import com.example.ui.components.formatSpeed
import com.example.ui.components.getFileIcon
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PegionTheme
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusDownloading
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPaused
import com.example.ui.theme.StatusQueued
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadDetailsScreen(
    viewModel: DetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val download by viewModel.download.collectAsStateWithLifecycle()
    val speedHistory by viewModel.speedHistory.collectAsStateWithLifecycle()
    val manualChecksum by viewModel.manualChecksumResult.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }

    val item = download
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading download details…")
        }
        return
    }

    DownloadDetailsContent(
        item = item,
        speedHistory = speedHistory,
        manualChecksum = manualChecksum,
        onNavigateBack = onNavigateBack,
        onDeleteClick = { showDeleteDialog = true },
        onPause = { viewModel.pause() },
        onResume = { viewModel.resume() },
        onCancel = { viewModel.cancel() },
        onRetry = { viewModel.retry() },
        onComputeChecksum = { viewModel.computeChecksum(it) },
        modifier = modifier
    )

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            fileName = item.fileName,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { deleteFile ->
                viewModel.delete(deleteFile)
                showDeleteDialog = false
                onNavigateBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadDetailsContent(
    item: DownloadEntity,
    speedHistory: List<SpeedSample> = emptyList(),
    manualChecksum: String? = null,
    onNavigateBack: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
    onComputeChecksum: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (statusColor, statusLabel) = when (item.status) {
        DownloadStatus.PENDING -> StatusQueued to "Queued"
        DownloadStatus.DOWNLOADING -> StatusDownloading to "Downloading"
        DownloadStatus.PAUSED -> StatusPaused to "Paused"
        DownloadStatus.COMPLETED -> StatusCompleted to "Completed"
        DownloadStatus.FAILED -> StatusFailed to "Failed"
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline to "Cancelled"
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }

    // Resilient file size calculation checking Room database and filesystem fallback
    val actualDiskSize = remember(item.filePath, item.status) {
        val f = File(item.filePath)
        if (f.exists()) f.length() else 0L
    }
    val effectiveSize = when {
        item.fileSize > 0 -> item.fileSize
        item.downloadedBytes > 0 -> item.downloadedBytes
        actualDiskSize > 0 -> actualDiskSize
        else -> 0L
    }
    val effectiveDownloaded = when {
        item.downloadedBytes > 0 -> item.downloadedBytes
        actualDiskSize > 0 -> actualDiskSize
        item.fileSize > 0 -> item.fileSize
        else -> 0L
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item.fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = statusColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getFileIcon(item.fileName),
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(label = statusLabel, color = statusColor)
                                PriorityBadge(item.priority)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress section
                    LinearProgressIndicator(
                        progress = { (item.progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val sizeText = when {
                            item.status == DownloadStatus.COMPLETED -> {
                                if (effectiveSize > 0) "${formatBytes(effectiveSize)} (Delivered)" else formatBytes(effectiveDownloaded)
                            }
                            effectiveSize > 0 -> {
                                "${formatBytes(effectiveDownloaded)} of ${formatBytes(effectiveSize)}"
                            }
                            effectiveDownloaded > 0 -> {
                                "${formatBytes(effectiveDownloaded)} of unknown"
                            }
                            else -> "Calculating size…"
                        }
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (item.status == DownloadStatus.COMPLETED) "100%" else "%.1f%%".format(item.progress),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }

                    // Primary Action Buttons
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (item.status) {
                            DownloadStatus.DOWNLOADING -> {
                                Button(
                                    onClick = onPause,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusPaused)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause")
                                }
                                OutlinedButton(
                                    onClick = onCancel,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel")
                                }
                            }
                            DownloadStatus.PAUSED, DownloadStatus.PENDING -> {
                                Button(
                                    onClick = onResume,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume")
                                }
                                OutlinedButton(
                                    onClick = onCancel,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel")
                                }
                            }
                            DownloadStatus.COMPLETED -> {
                                Button(
                                    onClick = { openFile(context, item.filePath) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted)
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open File")
                                }
                                FilledTonalButton(
                                    onClick = { shareFile(context, item.filePath) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }
                            DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                                Button(
                                    onClick = onRetry,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }

            // Speed Telemetry Graph Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Speed Telemetry", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Text(
                            text = formatSpeed(item.speed),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                    SpeedHistoryChart(
                        samples = speedHistory,
                        lineColor = statusColor
                    )
                }
            }

            // Checksum Verification Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Integrity & Checksum", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!item.checksumType.isNullOrBlank()) {
                        DetailRow("Type", item.checksumType)
                        DetailRow("Expected", item.checksumValue ?: "None")
                        DetailRow("Actual", item.actualChecksum ?: "Pending completion")

                        val matches = item.actualChecksum != null &&
                                item.actualChecksum.equals(item.checksumValue, ignoreCase = true)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (matches) StatusCompleted.copy(alpha = 0.15f) else StatusPaused.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (matches) "Checksum Verified (Valid)" else "Pending or Unverified",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (matches) StatusCompleted else StatusPaused,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "No expected checksum was provided when adding this download.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { onComputeChecksum("SHA-256") }) {
                                Text("Compute SHA-256", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(onClick = { onComputeChecksum("MD5") }) {
                                Text("Compute MD5", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    manualChecksum?.let { res ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = res,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Detailed Properties Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Details & Metadata", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                    DetailRowWithCopy("URL", item.url, context)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailRowWithCopy("Saved Path", item.filePath, context)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailRow("File Size", if (effectiveSize > 0) formatBytes(effectiveSize) else if (item.status == DownloadStatus.COMPLETED) formatBytes(effectiveDownloaded) else "Unknown")
                    DetailRow("Downloaded", formatBytes(effectiveDownloaded))
                    DetailRow("ETA", formatEta(item.eta))
                    DetailRow("Date Added", dateFormat.format(Date(item.createdAt)))
                    item.completedAt?.let {
                        DetailRow("Date Completed", dateFormat.format(Date(it)))
                    }
                    DetailRow("Engine", "Pegion Turbo Range Engine")
                    if (item.errorMessage != null) {
                        DetailRow("Error", item.errorMessage)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DetailRowWithCopy(label: String, value: String, context: Context) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(
                onClick = {
                    val clip = ClipData.newPlainText(label, value)
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun openFile(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Open file with…"))
    } catch (_: Exception) {
        Toast.makeText(context, "Cannot open file: No suitable app found", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFile(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Share file via…"))
    } catch (_: Exception) {
        Toast.makeText(context, "Could not share file", Toast.LENGTH_SHORT).show()
    }
}

@Preview(name = "Download Details Completed Light", showBackground = true)
@Composable
private fun DownloadDetailsCompletedLightPreview() {
    PegionTheme(dynamicColor = false) {
        DownloadDetailsContent(
            item = DownloadEntity(
                id = 101L,
                url = "https://releases.ubuntu.com/24.04/ubuntu-24.04-desktop-amd64.iso",
                fileName = "ubuntu-24.04-desktop-amd64.iso",
                filePath = "/storage/emulated/0/Download/ubuntu-24.04.iso",
                fileSize = 2576980377L,
                downloadedBytes = 2576980377L,
                status = DownloadStatus.COMPLETED,
                progress = 100.0f,
                speed = 0L,
                eta = 0L,
                priority = DownloadPriority.HIGH,
                checksumType = "SHA-256",
                checksumValue = "8897598801d6abf39a7b9736e67613b5",
                actualChecksum = "8897598801d6abf39a7b9736e67613b5",
                createdAt = System.currentTimeMillis() - 3600000L,
                completedAt = System.currentTimeMillis() - 60000L
            ),
            speedHistory = listOf(
                SpeedSample(1L, 1200000L),
                SpeedSample(2L, 2500000L),
                SpeedSample(3L, 4800000L),
                SpeedSample(4L, 6200000L),
                SpeedSample(5L, 0L)
            ),
            manualChecksum = null
        )
    }
}

@Preview(name = "Download Details Active Dark", showBackground = true)
@Composable
private fun DownloadDetailsActiveDarkPreview() {
    PegionTheme(themeMode = AppThemeMode.DARK, dynamicColor = false) {
        DownloadDetailsContent(
            item = DownloadEntity(
                id = 102L,
                url = "https://example.com/large_dataset.tar.gz",
                fileName = "scientific_dataset_2026.tar.gz",
                filePath = "/storage/emulated/0/Download/dataset.tar.gz",
                fileSize = 5000000000L,
                downloadedBytes = 3200000000L,
                status = DownloadStatus.DOWNLOADING,
                progress = 64.0f,
                speed = 4200000L,
                eta = 428L,
                priority = DownloadPriority.NORMAL,
                createdAt = System.currentTimeMillis() - 1800000L
            ),
            speedHistory = listOf(
                SpeedSample(1L, 1500000L),
                SpeedSample(2L, 3000000L),
                SpeedSample(3L, 3800000L),
                SpeedSample(4L, 4200000L),
                SpeedSample(5L, 4100000L)
            ),
            manualChecksum = null
        )
    }
}
