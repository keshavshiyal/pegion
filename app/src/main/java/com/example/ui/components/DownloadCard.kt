package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DownloadEntity
import com.example.download.model.DownloadPriority
import com.example.download.model.DownloadStatus
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusDownloading
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPaused
import com.example.ui.theme.StatusQueued

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
        label = "progress"
    )

    val (statusColor, statusLabel) = when (download.status) {
        DownloadStatus.PENDING -> StatusQueued to "Queued"
        DownloadStatus.DOWNLOADING -> StatusDownloading to "Downloading"
        DownloadStatus.PAUSED -> StatusPaused to "Paused"
        DownloadStatus.COMPLETED -> StatusCompleted to "Completed"
        DownloadStatus.FAILED -> StatusFailed to "Failed"
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline to "Cancelled"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("download_card_${download.id}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: File icon, Name, Priority chip, Status chip, More menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type Icon Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getFileIcon(download.fileName),
                            contentDescription = "File Type",
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.fileName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Priority indicator if not Normal
                        if (download.priority != DownloadPriority.NORMAL) {
                            PriorityBadge(download.priority)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        StatusBadge(label = statusLabel, color = statusColor)
                    }
                }

                // Dropdown Menu Button
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

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar (when downloading, queued, or paused)
            if (download.status != DownloadStatus.COMPLETED) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Metrics & Status Information Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Size & percentage
                val sizeText = formatBytes(download.downloadedBytes) +
                        if (download.fileSize > 0) " / " + formatBytes(download.fileSize) else ""

                val progressText = if (download.fileSize > 0) {
                    "%.1f%%".format(download.progress)
                } else if (download.status == DownloadStatus.COMPLETED) {
                    "100%"
                } else {
                    "—"
                }

                Column {
                    Text(
                        text = "$sizeText  ($progressText)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (download.status == DownloadStatus.DOWNLOADING && download.speed > 0) {
                        Text(
                            text = "${formatSpeed(download.speed)} • ETA ${formatEta(download.eta)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = statusColor
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

                // Action Buttons based on status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (download.status) {
                        DownloadStatus.DOWNLOADING -> {
                            FilledTonalIconButton(
                                onClick = onPause,
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = StatusPaused.copy(alpha = 0.2f),
                                    contentColor = StatusPaused
                                )
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalIconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                            }
                        }
                        DownloadStatus.PAUSED, DownloadStatus.PENDING -> {
                            FilledTonalIconButton(
                                onClick = onResume,
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = statusColor.copy(alpha = 0.2f),
                                    contentColor = statusColor
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalIconButton(
                                onClick = onCancel,
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                            }
                        }
                        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                            FilledTonalIconButton(
                                onClick = onRetry,
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalIconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            FilledTonalIconButton(
                                onClick = onOpen,
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = StatusCompleted.copy(alpha = 0.2f),
                                    contentColor = StatusCompleted
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Open", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun PriorityBadge(priority: DownloadPriority) {
    val (color, text) = when (priority) {
        DownloadPriority.HIGH -> MaterialTheme.colorScheme.error to "High"
        DownloadPriority.LOW -> MaterialTheme.colorScheme.outline to "Low"
        DownloadPriority.NORMAL -> MaterialTheme.colorScheme.secondary to "Normal"
    }
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
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
