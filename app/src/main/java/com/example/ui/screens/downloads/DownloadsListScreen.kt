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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.ui.screens.home.HomeViewModel
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
                        IconButton(onClick = { viewModel.clearCompleted() }) {
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
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
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
                        text = {
                            Text(title, style = MaterialTheme.typography.labelMedium)
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
                        TextButton(onClick = { viewModel.pauseAll() }) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause All")
                        }
                    }
                    if (summary.pausedCount > 0) {
                        TextButton(onClick = { viewModel.resumeAll() }) {
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
                            onPause = { viewModel.pauseDownload(download.id) },
                            onResume = { viewModel.resumeDownload(download.id) },
                            onCancel = { viewModel.cancelDownload(download.id) },
                            onRetry = { viewModel.retryDownload(download.id) },
                            onDelete = { itemToDelete = download },
                            onRename = { itemToRename = download },
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
