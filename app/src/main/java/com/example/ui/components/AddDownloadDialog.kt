package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.download.model.ChecksumType
import com.example.download.model.DownloadPriority

@Composable
fun AddDownloadDialog(
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onConfirm: (
        url: String,
        fileName: String?,
        priority: DownloadPriority,
        checksumType: String?,
        checksumValue: String?,
        speedLimit: Long
    ) -> Unit,
    checkDuplicate: suspend (String) -> Boolean
) {
    var url by remember { mutableStateOf(initialUrl) }
    var fileName by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(DownloadPriority.NORMAL) }
    var checksumType by remember { mutableStateOf(ChecksumType.NONE) }
    var checksumValue by remember { mutableStateOf("") }
    var speedLimitSliderValue by remember { mutableFloatStateOf(0f) } // 0 = unlimited, 1=512KB, 2=1MB, 3=2MB, 4=5MB
    var isDuplicate by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            isDuplicate = checkDuplicate(url)
        } else {
            isDuplicate = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add New Download", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // URL input with Paste button
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Download URL (HTTP/HTTPS)") },
                    placeholder = { Text("https://example.com/file.zip") },
                    singleLine = false,
                    maxLines = 3,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    url = clip
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_url_input")
                )

                // Duplicate Warning
                AnimatedVisibility(visible = isDuplicate) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Duplicate Warning",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This URL has already been downloaded or queued.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // File name (Optional)
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name (Optional)") },
                    placeholder = { Text("Leaves default if empty") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_filename_input")
                )

                // Priority Selection
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DownloadPriority.values().forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                // Checksum verification options
                Text("Checksum Verification (Optional)", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChecksumType.values().forEach { c ->
                        FilterChip(
                            selected = checksumType == c,
                            onClick = { checksumType = c },
                            label = { Text(if (c == ChecksumType.NONE) "None" else c.name.replace("_", "-")) }
                        )
                    }
                }

                if (checksumType != ChecksumType.NONE) {
                    OutlinedTextField(
                        value = checksumValue,
                        onValueChange = { checksumValue = it },
                        label = { Text("Expected ${checksumType.name.replace("_", "-")} Hash") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Per-Download Speed Limit
                val (limitLabel, limitBytes) = when (speedLimitSliderValue.toInt()) {
                    0 -> "Unlimited" to 0L
                    1 -> "512 KB/s" to 512L * 1024
                    2 -> "1 MB/s" to 1024L * 1024
                    3 -> "2 MB/s" to 2048L * 1024
                    4 -> "5 MB/s" to 5120L * 1024
                    else -> "Unlimited" to 0L
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speed Limit", style = MaterialTheme.typography.labelMedium)
                        Text(limitLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = speedLimitSliderValue,
                        onValueChange = { speedLimitSliderValue = it },
                        valueRange = 0f..4f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        val selectedChecksum = if (checksumType == ChecksumType.NONE) null else checksumType.name.replace("_", "-")
                        val speedBytes = when (speedLimitSliderValue.toInt()) {
                            1 -> 512L * 1024
                            2 -> 1024L * 1024
                            3 -> 2048L * 1024
                            4 -> 5120L * 1024
                            else -> 0L
                        }
                        onConfirm(
                            url.trim(),
                            fileName.ifBlank { null },
                            priority,
                            selectedChecksum,
                            checksumValue.ifBlank { null },
                            speedBytes
                        )
                    }
                },
                enabled = url.isNotBlank() && (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)),
                modifier = Modifier.testTag("dialog_confirm_btn")
            ) {
                Text("Start Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
