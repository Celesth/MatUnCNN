package com.matuncnn.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.matuncnn.app.model.CommandItem
import com.matuncnn.app.model.ProcessingState
import com.matuncnn.app.model.VideoInfo
import com.matuncnn.app.ui.theme.statusError
import com.matuncnn.app.ui.theme.statusProcessing
import com.matuncnn.app.ui.theme.statusSuccess
import com.matuncnn.app.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    viewModel: VideoViewModel,
    onLaunchVideoPicker: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Video selection
        item {
            VideoSelectionCard(
                videoInfo = state.videoInfo,
                onSelectVideo = onLaunchVideoPicker
            )
        }

        // Model selector
        item {
            VideoModelCard(
                commands = state.commandManager?.getCommandItems(state.settings.useCustomLabel) ?: emptyList(),
                selectedIndex = state.selectedCommandIndex,
                onCommandSelected = { viewModel.selectCommand(it) }
            )
        }

        // Processing
        item {
            VideoProcessingCard(
                progress = state.processingProgress,
                isProcessing = state.isProcessing,
                hasVideo = state.videoInfo != null,
                onStart = { viewModel.startProcessing() },
                onCancel = { viewModel.cancelProcessing() },
                onClear = { viewModel.clearVideo() }
            )
        }
    }
}

@Composable
fun VideoSelectionCard(
    videoInfo: VideoInfo?,
    onSelectVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Input Video", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (videoInfo == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .then(Modifier)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = onSelectVideo) {
                        Icon(Icons.Filled.VideoFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Video")
                    }
                }
            } else {
                Column {
                    Text(
                        text = videoInfo.fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val durationStr = if (videoInfo.durationMs > 0) {
                        val sec = videoInfo.durationMs / 1000
                        val min = sec / 60
                        val s = sec % 60
                        "${min}m ${s}s"
                    } else "Unknown"
                    val resolutionStr = if (videoInfo.width > 0) "${videoInfo.width}x${videoInfo.height}" else "Unknown"

                    Text(
                        text = "${videoInfo.fileName} | $resolutionStr | $durationStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoModelCard(
    commands: List<com.matuncnn.app.model.CommandItem>,
    selectedIndex: Int,
    onCommandSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Upscaling Model", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                val selectedLabel = if (selectedIndex in commands.indices) {
                    commands[selectedIndex].displayLabel
                } else "Select model"

                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    commands.forEachIndexed { index: Int, item: CommandItem ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(item.displayLabel, style = MaterialTheme.typography.bodyMedium)
                                    Text(item.command, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            },
                            onClick = {
                                onCommandSelected(index)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoProcessingCard(
    progress: com.matuncnn.app.model.VideoProcessingProgress,
    isProcessing: Boolean,
    hasVideo: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Processing", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (isProcessing) {
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = progress.message,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (progress.totalFrames > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${progress.currentFrame} / ${progress.totalFrames} frames",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            } else if (progress.state == ProcessingState.COMPLETED) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = statusSuccess,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Processing complete!",
                    style = MaterialTheme.typography.titleMedium,
                    color = statusSuccess
                )
                if (progress.outputPath.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = progress.outputPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClear) {
                    Text("Process Another")
                }
            } else if (progress.state == ProcessingState.FAILED) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = statusError,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = progress.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusError
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClear) {
                    Text("Try Again")
                }
            } else {
                Button(
                    onClick = onStart,
                    enabled = hasVideo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Video Upscaling")
                }
            }
        }
    }
}
