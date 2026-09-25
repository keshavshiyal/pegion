package com.example.ui.screens.downloads

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DownloadEntity
import com.example.download.model.DownloadFilter
import com.example.download.model.DownloadStatus
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DownloadCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RenameDialog
import com.example.ui.screens.home.HomeSummaryMetrics
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PegionTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsListScreen(
    viewModel: HomeViewModel,
    onNavigateToDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val summary by viewModel.summaryMetrics.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Active", "Completed", "Paused", "Failed")

    var itemToDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    var itemToRename by remember { mutableStateOf<DownloadEntity?>(null) }

    // Sync selected filter with tab
    val currentFilter = when (selectedTabIndex) {
        0 -> DownloadFilter.ALL
        1 -> DownloadFilter.ACTIVE
        2 -> DownloadFilter.COMPLETED
        3 -> DownloadFilter.PAUSED
        4 -> DownloadFilter.FAILED
        else -> DownloadFilter.ALL
    }

    DownloadsListContent(
        downloads = downloads,
        summary = summary,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { index ->
            selectedTabIndex = index
            viewModel.onFilterSelected(
                when (index) {
                    0 -> DownloadFilter.ALL
                    1 -> DownloadFilter.ACTIVE
                    2 -> DownloadFilter.COMPLETED
                    3 -> DownloadFilter.PAUSED
                    else -> DownloadFilter.FAILED
                }
            )
        },
        onNavigateToDetails = onNavigateToDetails,
        onPauseAll = { viewModel.pauseAll() },
        onResumeAll = { viewModel.resumeAll() },
        onClearCompleted = { viewModel.clearCompleted() },
        onPauseDownload = { viewModel.pauseDownload(it) },
        onResumeDownload = { viewModel.resumeDownload(it) },
        onCancelDownload = { viewModel.cancelDownload(it) },
        onRetryDownload = { viewModel.retryDownload(it) },
        onDeleteRequest = { itemToDelete = it },
        onRenameRequest = { itemToRename = it },
        modifier = modifier
    )

    itemToRename?.let { download ->
        RenameDialog(
            currentName = download.fileName,
            onDismiss = { itemToRename = null },
            onConfirm = { newName ->
                viewModel.renameDownload(download.id, newName)
                itemToRename = null
            }
        )
    }

    itemToDelete?.let { download ->
        DeleteConfirmDialog(
            fileName = download.fileName,
            onDismiss = { itemToDelete = null },
            onConfirm = { deleteFile ->
                viewModel.deleteDownload(download.id, deleteFile)
                itemToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsListContent(
    downloads: List<DownloadEntity>,
    summary: HomeSummaryMetrics,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToDetails: (Long) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onClearCompleted: () -> Unit,
    onPauseDownload: (Long) -> Unit,
    onResumeDownload: (Long) -> Unit,
    onCancelDownload: (Long) -> Unit,
    onRetryDownload: (Long) -> Unit,
    onDeleteRequest: (DownloadEntity) -> Unit,
    onRenameRequest: (DownloadEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs = listOf("All", "Active", "Completed", "Paused", "Failed")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Download Queue",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    if (summary.completedCount > 0) {
                        IconButton(onClick = onClearCompleted) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Completed")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                },
                divider = {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { onTabSelected(index) },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            // Quick Batch Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${downloads.size} items in queue",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    if (summary.activeCount > 0) {
                        TextButton(onClick = onPauseAll) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause All")
                        }
                    }
                    if (summary.pausedCount > 0) {
                        TextButton(onClick = onResumeAll) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume All")
                        }
                    }
                }
            }

            if (downloads.isEmpty()) {
                EmptyStateView(
                    isFiltered = selectedTabIndex != 0,
                    onAddClick = {}
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(downloads, key = { it.id }) { download ->
                        DownloadCard(
                            download = download,
                            onClick = { onNavigateToDetails(download.id) },
                            onPause = { onPauseDownload(download.id) },
                            onResume = { onResumeDownload(download.id) },
                            onCancel = { onCancelDownload(download.id) },
                            onRetry = { onRetryDownload(download.id) },
                            onDelete = { onDeleteRequest(download) },
                            onRename = { onRenameRequest(download) },
                            onOpen = { openFile(context, download.filePath) },
                            onShare = { shareFile(context, download.filePath) },
                            onCopyUrl = {
                                val clip = ClipData.newPlainText("URL", download.url)
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
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

@Preview(name = "Queue Screen Light", showBackground = true)
@Composable
private fun DownloadsListLightPreview() {
    PegionTheme(dynamicColor = false) {
        DownloadsListContent(
            downloads = listOf(
                DownloadEntity(
                    id = 1L,
                    url = "https://example.com/archlinux-x86_64.iso",
                    fileName = "archlinux-2026.09-x86_64.iso",
                    filePath = "/storage/downloads/archlinux.iso",
                    fileSize = 1200000000L,
                    downloadedBytes = 720000000L,
                    status = DownloadStatus.DOWNLOADING,
                    progress = 60.0f,
                    speed = 4500000L,
                    eta = 106L
                ),
                DownloadEntity(
                    id = 2L,
                    url = "https://example.com/flutter_sdk.zip",
                    fileName = "flutter_linux_3.24.0-stable.tar.xz",
                    filePath = "/storage/downloads/flutter_sdk.zip",
                    fileSize = 850000000L,
                    downloadedBytes = 850000000L,
                    status = DownloadStatus.COMPLETED,
                    progress = 100.0f,
                    speed = 0L,
                    eta = 0L
                )
            ),
            summary = HomeSummaryMetrics(
                activeCount = 1,
                completedCount = 1,
                pausedCount = 0,
                failedCount = 0,
                totalBytesDownloaded = 1570000000L,
                globalSpeed = 4500000L
            ),
            selectedTabIndex = 0,
            onTabSelected = {},
            onNavigateToDetails = {},
            onPauseAll = {},
            onResumeAll = {},
            onClearCompleted = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancelDownload = {},
            onRetryDownload = {},
            onDeleteRequest = {},
            onRenameRequest = {}
        )
    }
}

@Preview(name = "Queue Screen Dark", showBackground = true)
@Composable
private fun DownloadsListDarkPreview() {
    PegionTheme(themeMode = AppThemeMode.DARK, dynamicColor = false) {
        DownloadsListContent(
            downloads = listOf(
                DownloadEntity(
                    id = 2L,
                    url = "https://example.com/flutter_sdk.zip",
                    fileName = "flutter_linux_3.24.0-stable.tar.xz",
                    filePath = "/storage/downloads/flutter_sdk.zip",
                    fileSize = 850000000L,
                    downloadedBytes = 850000000L,
                    status = DownloadStatus.COMPLETED,
                    progress = 100.0f,
                    speed = 0L,
                    eta = 0L
                )
            ),
            summary = HomeSummaryMetrics(
                activeCount = 0,
                completedCount = 1,
                pausedCount = 0,
                failedCount = 0,
                totalBytesDownloaded = 850000000L,
                globalSpeed = 0L
            ),
            selectedTabIndex = 2,
            onTabSelected = {},
            onNavigateToDetails = {},
            onPauseAll = {},
            onResumeAll = {},
            onClearCompleted = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancelDownload = {},
            onRetryDownload = {},
            onDeleteRequest = {},
            onRenameRequest = {}
        )
    }
}
