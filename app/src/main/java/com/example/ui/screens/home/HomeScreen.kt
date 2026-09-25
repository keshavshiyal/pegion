package com.example.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DownloadEntity
import com.example.download.model.DownloadFilter
import com.example.download.model.DownloadPriority
import com.example.download.model.DownloadSortOrder
import com.example.download.model.DownloadStatus
import com.example.ui.components.AddDownloadDialog
import com.example.ui.components.BatchDownloadDialog
import com.example.ui.components.ClipboardBanner
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DownloadCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PegionLogo
import com.example.ui.components.RenameDialog
import com.example.ui.components.formatBytes
import com.example.ui.components.formatSpeed
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PegionTheme
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
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Pulsing animation for FAB when active downloads exist
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val fabPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (summary.activeCount > 0) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_scale"
    )

    // Handle shared URL from other apps
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            addDialogInitialUrl = sharedUrl
            showAddDialog = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Pegion",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Always delivers.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.testTag("top_app_bar_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options and Sort",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ViewList,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                text = { Text("Batch Download…") },
                                onClick = {
                                    showOptionsMenu = false
                                    showBatchDialog = true
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "SORT BY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = null
                                    )
                                },
                                text = { Text("Date Added (Newest)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.DATE_ADDED_DESC)
                                    showOptionsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Added (Oldest)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.DATE_ADDED_ASC)
                                    showOptionsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("File Name (A - Z)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.NAME_ASC)
                                    showOptionsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("File Size (Largest)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.SIZE_DESC)
                                    showOptionsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Progress (%)") },
                                onClick = {
                                    viewModel.onSortOrderSelected(DownloadSortOrder.PROGRESS_DESC)
                                    showOptionsMenu = false
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
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Download"
                    )
                },
                text = {
                    Text(
                        text = "New Download",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = fabPulseScale
                        scaleY = fabPulseScale
                    }
                    .testTag("fab_add_download")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
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

            // Hero Global Speed Card
            item {
                GlobalSpeedHeroCard(
                    summary = summary,
                    onPauseAll = { viewModel.pauseAll() },
                    onResumeAll = { viewModel.resumeAll() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Docked Search Bar
            item {
                DockedSearchBarView(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Filter Tabs Row
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                shape = RoundedCornerShape(12.dp),
                                label = {
                                    Text(
                                        text = "${filter.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedFilter == filter,
                                    borderColor = Color.Transparent,
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            // Download items or empty state
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

/**
 * Hero Card with solid primaryContainer background, circular indicator, and high contrast.
 */
@Composable
fun GlobalSpeedHeroCard(
    summary: HomeSummaryMetrics,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Large Circular Progress Indicator showing speed activity with Pegion mascot
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (summary.globalSpeed > 0) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                            strokeWidth = 5.dp,
                            strokeCap = StrokeCap.Round
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { 0f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                            strokeWidth = 5.dp,
                            strokeCap = StrokeCap.Round
                        )
                    }

                    PegionLogo(
                        size = 38.dp,
                        tint = MaterialTheme.colorScheme.primary,
                        accentTint = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Right: Speed, stats, and description in onPrimaryContainer
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Global Speed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatSpeed(summary.globalSpeed),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.25).sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${summary.activeCount} Active",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${formatBytes(summary.totalBytesDownloaded)} Delivered",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Actions row: Pause All / Resume All inside the card
            if (summary.activeCount > 0 || summary.pausedCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (summary.activeCount > 0) {
                        FilledTonalButton(
                            onClick = onPauseAll,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Pause All",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (summary.pausedCount > 0) {
                        if (summary.activeCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        FilledTonalButton(
                            onClick = onResumeAll,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Resume All",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Docked Search Bar with rounded corners and subtle surfaceVariant background.
 */
@Composable
fun DockedSearchBarView(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search downloads or URLs…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_input")
    )
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

@Preview(name = "Global Speed Hero Light", showBackground = true)
@Composable
private fun GlobalSpeedHeroLightPreview() {
    PegionTheme(dynamicColor = false) {
        GlobalSpeedHeroCard(
            summary = HomeSummaryMetrics(
                activeCount = 2,
                completedCount = 8,
                pausedCount = 1,
                failedCount = 0,
                totalBytesDownloaded = 4800000000L,
                globalSpeed = 3450000L
            ),
            onPauseAll = {},
            onResumeAll = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Global Speed Hero Dark", showBackground = true)
@Composable
private fun GlobalSpeedHeroDarkPreview() {
    PegionTheme(themeMode = AppThemeMode.DARK, dynamicColor = false) {
        GlobalSpeedHeroCard(
            summary = HomeSummaryMetrics(
                activeCount = 1,
                completedCount = 12,
                pausedCount = 0,
                failedCount = 0,
                totalBytesDownloaded = 12500000000L,
                globalSpeed = 6200000L
            ),
            onPauseAll = {},
            onResumeAll = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
