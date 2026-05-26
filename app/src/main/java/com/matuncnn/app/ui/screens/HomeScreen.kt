package com.matuncnn.app.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.matuncnn.app.model.CommandItem
import com.matuncnn.app.ui.theme.AppColors
import com.matuncnn.app.viewmodel.MainUiState
import com.matuncnn.app.viewmodel.MainViewModel
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onLaunchImagePicker: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val logSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> uri?.let { viewModel.saveLogTo(it) } }

    val outputSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri -> uri?.let { viewModel.saveOutputTo(it) } }

    // Show snackbar on completion
    val prevStatus = remember { mutableStateOf("") }
    if (state.statusMessage.isNotBlank() && state.statusMessage != prevStatus.value && !state.isProcessing) {
        prevStatus.value = state.statusMessage
        scope.launch {
            snackbarHostState.showSnackbar(state.statusMessage)
        }
    }

    if (!state.isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading assets...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Text("Upscale", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }

            item(key = "image_select") { ImageSelectionCard(state, onLaunchImagePicker) }

            item(key = "model_divider") { HorizontalDivider() }

            item(key = "model_select") { CommandSelectorCard(state, viewModel) }

            item(key = "run_button") {
                Button(
                    onClick = { viewModel.startProcessing() },
                    enabled = !state.isProcessing && state.inputImageExists && state.commandText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Run", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (state.isProcessing) {
                item(key = "progress") {
                    val progress = state.progressText.filter { it.isDigit() || it == '.' }.toFloatOrNull()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { if (progress != null) progress / 100f else 0f },
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                        )
                        if (state.progressText.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(state.progressText, style = MaterialTheme.typography.bodySmall,
                                color = AppColors.statusProcessing)
                        }
                    }
                }

                item(key = "stop_button") {
                    Button(
                        onClick = { viewModel.cancelProcessing() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Stop, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                }
            }

            if (!state.hasVulkan) {
                item(key = "vulkan_warning") {
                    Text("Vulkan is not available. ncnn may not work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }

            item(key = "output_header") {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("Output", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (state.outputImageExists && state.outputFilePath != null) {
                item(key = "output_preview") { OutputPreviewCard(state) }
                item(key = "output_actions") { OutputActionsRow(state, viewModel, outputSaver) }
            } else {
                item(key = "output_empty") { EmptyOutputPlaceholder() }
            }

            if (state.logText.isNotBlank()) {
                item(key = "log_divider") { HorizontalDivider() }
                item(key = "log_card") { LogCard(state, logSaver) }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

// ─── Image Selection ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSelectionCard(state: MainUiState, onSelectImage: () -> Unit) {
    if (!state.inputImageExists) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().clickable { onSelectImage() },
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ImageSearch, null, Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("Select an image to upscale",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } else {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().clickable { onSelectImage() },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.inputUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(state.inputUri).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(state.inputFileName, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Ready", style = MaterialTheme.typography.bodySmall,
                        color = AppColors.statusSuccess)
                }
                OutlinedButton(onClick = onSelectImage) { Text("Change") }
            }
        }
    }
}

// ─── Model Selector ──────────────────────────────────────────────

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

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedLabel, onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                commands.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(item.displayLabel, style = MaterialTheme.typography.bodyMedium)
                                Text(item.command, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        onClick = { viewModel.selectCommand(index); expanded = false }
                    )
                }
            }
        }

        AnimatedVisibility(visible = state.showCommandInput) {
            Column {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.commandText,
                    onValueChange = { viewModel.updateCommandText(it) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                    label = { Text("Custom command") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

// ─── Output Preview ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputPreviewCard(state: MainUiState) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = Offset(
            x = (offset.x + panChange.x).coerceIn(-500f, 500f),
            y = (offset.y + panChange.y).coerceIn(-500f, 500f)
        )
    }

    val ctx = LocalContext.current
    val file = state.outputFilePath?.let { File(it) }
    val metadata = file?.let { loadImageMetadata(it.absolutePath) }

    Column {
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (file != null) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(file).crossfade(true).build(),
                    contentDescription = "Output",
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                        .graphicsLayer(
                            scaleX = scale, scaleY = scale,
                            translationX = offset.x, translationY = offset.y
                        )
                        .transformable(state = transformState)
                        .pointerInput(Unit) { },
                    contentScale = ContentScale.Fit
                )
            }
        }

        if (metadata != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetadataRow("Name", metadata.fileName)
                    MetadataRow("Resolution", "${metadata.width} x ${metadata.height}")
                    MetadataRow("Size", metadata.fileSize)
                    MetadataRow("Format", metadata.format)
                    MetadataRow("Scale", state.scaleText.ifBlank { "-" })
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium)
    }
}

// ─── Output Actions ──────────────────────────────────────────────

@Composable
fun OutputActionsRow(
    state: MainUiState,
    viewModel: MainViewModel,
    saver: androidx.activity.result.ActivityResultLauncher<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { viewModel.saveOutputToGallery() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Save to Gallery")
        }
        OutlinedButton(
            onClick = { viewModel.shareOutput() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Share")
        }
        OutlinedButton(
            onClick = { saver.launch("upscaled.png") },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Save, null, Modifier.size(16.dp))
        }
    }
}

// ─── Empty Output ────────────────────────────────────────────────

@Composable
fun EmptyOutputPlaceholder() {
    Box(
        modifier = Modifier.fillMaxWidth().height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Image, null, Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            Text("Your upscaled image will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center)
        }
    }
}

// ─── Log Card ────────────────────────────────────────────────────

@Composable
fun LogCard(
    state: MainUiState,
    saver: androidx.activity.result.ActivityResultLauncher<String>
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BugReport, null, Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text("Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { saver.launch("matuncnn_log.txt") }) {
                Icon(Icons.Filled.Download, "Save log", Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.logText.isBlank()) {
                Text("No output yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    state.logText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Metadata Helper ─────────────────────────────────────────────

data class ImageMetadata(
    val fileName: String,
    val width: Int,
    val height: Int,
    val fileSize: String,
    val format: String
)

private fun loadImageMetadata(path: String): ImageMetadata? {
    return try {
        val file = File(path)
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        val w = opts.outWidth
        val h = opts.outHeight
        val fmt = opts.outMimeType?.substringAfterLast("/")?.uppercase() ?: "Unknown"
        val size = when {
            file.length() < 1024 -> "${file.length()} B"
            file.length() < 1024 * 1024 -> "${file.length() / 1024} KB"
            else -> "%.1f MB".format(file.length().toDouble() / (1024 * 1024))
        }
        ImageMetadata(file.name, w, h, size, fmt)
    } catch (_: Exception) { null }
}
