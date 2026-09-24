package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DownloadEntity
import com.example.download.model.ChecksumType
import com.example.download.model.DownloadPriority
import com.example.download.model.DownloadStatus
import com.example.ui.theme.PegionTheme
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusCompletedContainer
import com.example.ui.theme.StatusDownloading
import com.example.ui.theme.StatusDownloadingContainer
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusFailedContainer
import com.example.ui.theme.StatusPaused
import com.example.ui.theme.StatusPausedContainer
import com.example.ui.theme.StatusQueued
import com.example.ui.theme.StatusQueuedContainer
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadCard(
    download: DownloadEntity,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCopyUrl: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = (download.progress / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350),
        label = "progress"
    )

    // Semantic status styling with WCAG AA compliance
    val (statusFgColor, statusBgColor, statusLabel) = when (download.status) {
        DownloadStatus.PENDING -> Triple(StatusQueued, StatusQueuedContainer, "Queued")
        DownloadStatus.DOWNLOADING -> Triple(StatusDownloading, StatusDownloadingContainer, "Downloading")
        DownloadStatus.PAUSED -> Triple(StatusPaused, StatusPausedContainer, "Paused")
        DownloadStatus.COMPLETED -> Triple(StatusCompleted, StatusCompletedContainer, "Completed")
        DownloadStatus.FAILED -> Triple(StatusFailed, StatusFailedContainer, "Failed")
        DownloadStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant,
            "Cancelled"
        )
    }

    // Elevated Card with subtle elevation and clean surface container
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("download_card_${download.id}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: File icon, Name, Status Chip, More menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type Icon Surface
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBgColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getFileIcon(download.fileName),
                            contentDescription = "File Type",
                            tint = statusFgColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.fileName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.1.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Status Chip using secondaryContainer style
                        StatusChip(
                            label = statusLabel,
                            fgColor = statusFgColor,
                            bgColor = statusBgColor
                        )

                        // Priority chip if non-normal
                        if (download.priority != DownloadPriority.NORMAL) {
                            PriorityChip(priority = download.priority)
                        }
                    }
                }

                // Dropdown Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("menu_btn_${download.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (download.status == DownloadStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = { Text("Open File") },
                                onClick = { menuExpanded = false; onOpen() }
                            )
                            DropdownMenuItem(
                                text = { Text("Share File") },
                                onClick = { menuExpanded = false; onShare() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Details & Speed Graph") },
                            onClick = { menuExpanded = false; onClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Download URL") },
                            onClick = { menuExpanded = false; onCopyUrl() }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Thicker, expressive LinearProgressIndicator with StrokeCap.Round
            if (download.status != DownloadStatus.COMPLETED) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusFgColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Info Row: Left (Downloaded / Total (Percentage)), Right (Speed • ETA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sizeText = formatBytes(download.downloadedBytes) +
                        if (download.fileSize > 0) " / " + formatBytes(download.fileSize) else ""

                val progressText = if (download.fileSize > 0) {
                    "%.1f%%".format(download.progress)
                } else if (download.status == DownloadStatus.COMPLETED) {
                    "100%"
                } else {
                    "—"
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "$sizeText ($progressText)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (download.status == DownloadStatus.DOWNLOADING && download.speed > 0) {
                        Text(
                            text = "${formatSpeed(download.speed)} • ETA ${formatEta(download.eta)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = statusFgColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (download.errorMessage != null && download.status == DownloadStatus.FAILED) {
                        Text(
                            text = download.errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Actions: FilledTonalIconButton with >= 48dp touch bounds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (download.status) {
                        DownloadStatus.DOWNLOADING -> {
                            FilledTonalIconButton(
                                onClick = onPause,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DownloadStatus.PAUSED, DownloadStatus.PENDING -> {
                            FilledTonalIconButton(
                                onClick = onResume,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                            FilledTonalIconButton(
                                onClick = onRetry,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            FilledTonalIconButton(
                                onClick = onOpen,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = StatusCompletedContainer,
                                    contentColor = StatusCompleted
                                )
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Open File",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    fgColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        contentColor = fgColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusBadge(label: String, color: Color, modifier: Modifier = Modifier) {
    StatusChip(
        label = label,
        fgColor = color,
        bgColor = color.copy(alpha = 0.15f),
        modifier = modifier
    )
}

@Composable
fun PriorityBadge(priority: DownloadPriority, modifier: Modifier = Modifier) {
    PriorityChip(priority = priority, modifier = modifier)
}

@Composable
fun PriorityChip(
    priority: DownloadPriority,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor, text) = when (priority) {
        DownloadPriority.HIGH -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "High"
        )
        DownloadPriority.LOW -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Low"
        )
        DownloadPriority.NORMAL -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Normal"
        )
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        contentColor = fgColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun getFileIcon(fileName: String): ImageVector {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "webp", "gif", "svg" -> Icons.Default.Image
        "mp4", "mkv", "avi", "mov", "webm" -> Icons.Default.VideoFile
        "mp3", "flac", "wav", "m4a", "ogg" -> Icons.Default.AudioFile
        "zip", "tar", "gz", "7z", "rar" -> Icons.Default.FolderZip
        "pdf", "doc", "docx", "txt", "epub" -> Icons.Default.Description
        else -> Icons.Default.Description
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.0f KB".format(kb)
        else -> "$bytes B"
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 B/s"
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> "%.1f MB/s".format(mb)
        kb >= 1.0 -> "%.0f KB/s".format(kb)
        else -> "$bytesPerSec B/s"
    }
}

fun formatEta(seconds: Long): String {
    if (seconds < 0) return "--"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

@Preview(name = "Download Card Light", showBackground = true)
@Composable
private fun DownloadCardLightPreview() {
    PegionTheme(dynamicColor = false) {
        DownloadCard(
            download = DownloadEntity(
                id = 1L,
                url = "https://example.com/ubuntu-desktop.iso",
                fileName = "ubuntu-24.04-desktop-amd64.iso",
                filePath = "/storage/downloads/ubuntu.iso",
                fileSize = 4800000000L,
                downloadedBytes = 1200000000L,
                status = DownloadStatus.DOWNLOADING,
                progress = 25.0f,
                speed = 2450000L,
                eta = 1469L
            ),
            onClick = {},
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
            onDelete = {},
            onRename = {},
            onOpen = {},
            onShare = {},
            onCopyUrl = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Download Card Dark", showBackground = true)
@Composable
private fun DownloadCardDarkPreview() {
    PegionTheme(themeMode = com.example.ui.theme.AppThemeMode.DARK, dynamicColor = false) {
        DownloadCard(
            download = DownloadEntity(
                id = 2L,
                url = "https://example.com/archive.zip",
                fileName = "project_assets_highres.zip",
                filePath = "/storage/downloads/project.zip",
                fileSize = 250000000L,
                downloadedBytes = 250000000L,
                status = DownloadStatus.COMPLETED,
                progress = 100.0f,
                speed = 0L,
                eta = 0L
            ),
            onClick = {},
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
            onDelete = {},
            onRename = {},
            onOpen = {},
            onShare = {},
            onCopyUrl = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
