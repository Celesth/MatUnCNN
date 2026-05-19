package com.matuncnn.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.matuncnn.app.data.AppSettings
import com.matuncnn.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    val logSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? -> uri?.let { viewModel.saveLogTo(it) } }

    val outputSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? -> uri?.let { viewModel.saveOutputTo(it) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Theme section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Theme", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    val themeOptions = listOf("System", "Light", "Dark", "AMOLED", "Hyprland")
                    SettingsDropdown(
                        options = themeOptions,
                        selectedIndex = settings.themeIndex.coerceIn(0, 4),
                        onSelected = { viewModel.updateSettings(settings.copy(themeIndex = it)) }
                    )
                }
            }
        }

        // General section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("General", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsToggle(
                        label = "Keep screen on",
                        checked = settings.keepScreen,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(keepScreen = it))
                        }
                    )
                    SettingsToggle(
                        label = "Preprocess to PNG",
                        checked = settings.prePng,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(prePng = it))
                        }
                    )
                    SettingsToggle(
                        label = "Auto-save to gallery",
                        checked = settings.autoSave,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(autoSave = it))
                        }
                    )
                    SettingsToggle(
                        label = "CPU mode (NCNN)",
                        checked = settings.useCPU,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(useCPU = it))
                        }
                    )
                    SettingsToggle(
                        label = "Show command input",
                        checked = settings.showSearchView,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(showSearchView = it))
                        }
                    )
                    SettingsToggle(
                        label = "Show final command",
                        checked = settings.showFinalCommand,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(showFinalCommand = it))
                        }
                    )
                    SettingsToggle(
                        label = "Use custom labels",
                        checked = settings.useCustomLabel,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(useCustomLabel = it))
                        }
                    )
                }
            }
        }

        // Processing section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Processing", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = settings.tileSize.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull() ?: 0
                            viewModel.updateSettings(settings.copy(tileSize = v))
                        },
                        label = { Text("Tile size") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = settings.threadCount,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(threadCount = it))
                        },
                        label = { Text("Thread count (load:proc:save)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Notification mode", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    val notifyOptions = listOf("Silent", "Result Only", "Detailed", "Auto Dismiss")
                    SettingsDropdown(
                        options = notifyOptions,
                        selectedIndex = settings.notify.coerceIn(0, 3),
                        onSelected = {
                            viewModel.updateSettings(settings.copy(notify = it))
                        }
                    )
                }
            }
        }

        // Paths section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paths", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = settings.extraPath,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(extraPath = it))
                        },
                        label = { Text("Custom model path") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = settings.savePath,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(savePath = it))
                        },
                        label = { Text("Save path") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Commands section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Commands", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = settings.defaultCommand,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(defaultCommand = it))
                        },
                        label = { Text("Default command") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = settings.extraCommand,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(extraCommand = it))
                        },
                        label = { Text("Extra commands (one per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = settings.classicalFilters,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(classicalFilters = it))
                        },
                        label = { Text("Classical interpolation filters") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = settings.magickFilters,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(magickFilters = it))
                        },
                        label = { Text("Magick filters") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Output Format section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Output Format", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Save format", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsDropdown(
                        options = listOf("PNG", "JPG", "WEBP", "BMP"),
                        selectedIndex = settings.format.coerceIn(0, 3),
                        onSelected = { viewModel.updateSettings(settings.copy(format = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Naming scheme", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsDropdown(
                        options = listOf("Original", "Timestamp", "Sequential"),
                        selectedIndex = settings.name.coerceIn(0, 2),
                        onSelected = { viewModel.updateSettings(settings.copy(name = it)) }
                    )
                }
            }
        }

        // Debug / Logging section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Debug", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { logSaveLauncher.launch("matuncnn_log.txt") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Log")
                        }
                        OutlinedButton(
                            onClick = { outputSaveLauncher.launch("matuncnn_output.png") },
                            modifier = Modifier.weight(1f),
                            enabled = state.outputImageExists
                        ) {
                            Text("Save Output")
                        }
                    }

                    if (state.outputFilePath != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Output: ${state.outputFilePath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version: 1.0.0-beta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // About section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "MatUnCNN - RealSR NCNN Android GUI\n" +
                                "Upscale images using AI models.\n\n" +
                                "Built with ncnn and ImageMagick.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Bottom spacer
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" },
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
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
