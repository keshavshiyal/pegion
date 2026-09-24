package com.example.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DownloadEntity
import com.example.download.model.DownloadFilter
import com.example.download.model.DownloadSortOrder
import com.example.download.model.DownloadStatus
import com.example.ui.components.AddDownloadDialog
import com.example.ui.components.BatchDownloadDialog
import com.example.ui.components.ClipboardBanner
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DownloadCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RenameDialog
import com.example.ui.components.formatBytes
import com.example.ui.components.formatSpeed
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetails: (Long) -> Unit,
    sharedUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val summary by viewModel.summaryMetrics.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val detectedClipboardUrl by viewModel.detectedClipboardUrl.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var addDialogInitialUrl by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    var itemToRename by remember { mutableStateOf<DownloadEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Handle shared URL from other apps
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            addDialogInitialUrl = sharedUrl
            showAddDialog = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pegion",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Always delivers",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showBatchDialog = true }) {
                        Icon(
                            Icons.Default.ViewList,
                            contentDescription = "Batch Download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Sort downloads",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date Added (Newest)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.DATE_ADDED_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Added (Oldest)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.DATE_ADDED_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("File Name (A - Z)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.NAME_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("File Size (Largest)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.SIZE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Progress (%)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.PROGRESS_DESC)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    addDialogInitialUrl = ""
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Download") },
                text = { Text("New Download") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_download")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Clipboard banner
            item {
                ClipboardBanner(
                    detectedUrl = detectedClipboardUrl,
                    onDownloadNow = { url ->
                        addDialogInitialUrl = url
                        showAddDialog = true
                    },
                    onDismiss = { viewModel.dismissClipboardBanner() }
                )
            }

            // Summary Telemetry Card
            item {
                SummaryHeaderCard(
                    summary = summary,
                    onPauseAll = { viewModel.pauseAll() },
                    onResumeAll = { viewModel.resumeAll() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search downloads or URLs…") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_input")
                )
            }

            // Filter Tabs Row
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DownloadFilter.values()) { filter ->
                        val count = when (filter) {
                            DownloadFilter.ALL -> downloads.size
                            DownloadFilter.ACTIVE -> summary.activeCount
                            DownloadFilter.COMPLETED -> summary.completedCount
                            DownloadFilter.PAUSED -> summary.pausedCount
                            DownloadFilter.FAILED -> summary.failedCount
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { viewModel.onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = "${filter.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Download items
            if (downloads.isEmpty()) {
                item {
                    EmptyStateView(
                        isFiltered = searchQuery.isNotBlank() || selectedFilter != DownloadFilter.ALL,
                        onAddClick = {
                            addDialogInitialUrl = ""
                            showAddDialog = true
                        }
                    )
                }
            } else {
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
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Add Single Download Dialog
    if (showAddDialog) {
        AddDownloadDialog(
            initialUrl = addDialogInitialUrl,
            onDismiss = { showAddDialog = false },
            onConfirm = { url, fileName, priority, checksumType, checksumValue, speedLimit ->
                viewModel.addDownload(url, fileName, priority, checksumType, checksumValue, speedLimit)
                showAddDialog = false
            },
            checkDuplicate = { url -> viewModel.isDuplicate(url) }
        )
    }

    // Add Batch Dialog
    if (showBatchDialog) {
        BatchDownloadDialog(
            onDismiss = { showBatchDialog = false },
            onConfirm = { urls, priority ->
                viewModel.addBatchDownloads(urls, priority)
                showBatchDialog = false
            }
        )
    }

    // Rename Dialog
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

    // Delete Confirmation Dialog
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

@Composable
fun SummaryHeaderCard(
    summary: HomeSummaryMetrics,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                // Speed indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = formatSpeed(summary.globalSpeed),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Global Speed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Batch controls
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (summary.activeCount > 0) {
                        TextButton(onClick = onPauseAll) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause All", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (summary.pausedCount > 0) {
                        TextButton(onClick = onResumeAll) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume All", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 statistics counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "Active", value = "${summary.activeCount}", color = MaterialTheme.colorScheme.primary)
                StatItem(label = "Completed", value = "${summary.completedCount}", color = Color(0xFF10B981))
                StatItem(label = "Total Delivered", value = formatBytes(summary.totalBytesDownloaded), color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
