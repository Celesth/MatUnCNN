package com.matuncnn.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.matuncnn.app.model.CommandItem
import com.matuncnn.app.ui.theme.statusError
import com.matuncnn.app.ui.theme.statusProcessing
import com.matuncnn.app.ui.theme.statusSuccess
import com.matuncnn.app.viewmodel.MainUiState
import com.matuncnn.app.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onLaunchImagePicker: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val logSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> uri?.let { viewModel.saveLogTo(it) } }

    val outputSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri -> uri?.let { viewModel.saveOutputTo(it) } }

    if (!state.isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading assets...")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ImageSelectionCard(state, onLaunchImagePicker) }
        item { CommandSelectorCard(state, viewModel) }
        item { ProcessingControlsCard(state, viewModel) }

        if (state.outputImageExists) {
            item { OutputPreviewCard(state, viewModel, outputSaver) }
        }

        if (state.logText.isNotBlank()) {
            item { LogCard(state, viewModel, logSaver) }
        }
    }
}

@Composable
fun ImageSelectionCard(
    state: MainUiState,
    onSelectImage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Input Image", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))

            if (!state.inputImageExists) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelectImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Image, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to select image", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.inputUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(state.inputUri).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(state.inputFileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Ready", style = MaterialTheme.typography.bodySmall, color = statusSuccess)
                    }
                    OutlinedButton(onClick = onSelectImage) { Text("Change") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandSelectorCard(state: MainUiState, viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val commands = remember(state.commandManager, state.settings.useCustomLabel) {
        state.commandManager?.getCommandItems(state.settings.useCustomLabel) ?: emptyList()
    }
    val selectedLabel by remember(commands, state.selectedCommandIndex) {
        derivedStateOf {
            if (state.selectedCommandIndex in commands.indices)
                commands[state.selectedCommandIndex].displayLabel else "Select model"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Model", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { viewModel.toggleCommandInput() }) {
                    Text(if (state.showCommandInput) "Hide" else "Edit", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedLabel, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    commands.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(item.displayLabel, style = MaterialTheme.typography.bodyMedium)
                                    Text(item.command, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            },
                            onClick = { viewModel.selectCommand(index); expanded = false }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = state.showCommandInput) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.commandText, onValueChange = { viewModel.updateCommandText(it) },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, label = { Text("Command") }
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessingControlsCard(state: MainUiState, viewModel: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (state.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                Spacer(Modifier.height(6.dp))
            }

            if (!state.hasVulkan) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Vulkan is not available on this device. ncnn may not work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusError
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.startProcessing() },
                    enabled = !state.isProcessing && state.inputImageExists && state.commandText.isNotBlank() && state.hasVulkan,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Run")
                }

                if (state.isProcessing) {
                    Button(
                        onClick = { viewModel.cancelProcessing() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Filled.Stop, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }

            if (state.statusMessage.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    state.statusMessage, style = MaterialTheme.typography.bodySmall,
                    color = when {
                        state.statusMessage.contains("Error") || state.statusMessage.contains("Failed") -> statusError
                        state.statusMessage.contains("Complete") || state.statusMessage.contains("Done") -> statusSuccess
                        state.isProcessing -> statusProcessing
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun OutputPreviewCard(
    state: MainUiState,
    viewModel: MainViewModel,
    saver: androidx.activity.result.ActivityResultLauncher<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Output", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (state.outputFilePath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("file://${state.outputFilePath}").crossfade(true).build(),
                    contentDescription = "Output",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { saver.launch("upscaled.png") }) {
                        Icon(Icons.Filled.Save, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save As")
                    }
                    OutlinedButton(onClick = { viewModel.shareOutput() }) {
                        Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

@Composable
fun LogCard(
    state: MainUiState,
    viewModel: MainViewModel,
    saver: androidx.activity.result.ActivityResultLauncher<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.BugReport, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { saver.launch("matuncnn_log.txt") }) {
                    Icon(Icons.Filled.Download, "Save log", Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    state.logText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
