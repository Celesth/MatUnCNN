package com.matuncnn.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.matuncnn.app.BuildConfig
import com.matuncnn.app.data.AppSettings
import com.matuncnn.app.ui.theme.AppColors
import com.matuncnn.app.util.DebugLog
import com.matuncnn.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Debug")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tabIndex,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = tabIndex == i,
                    onClick = { tabIndex = i },
                    text = { Text(title, fontWeight = if (tabIndex == i) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        when (tabIndex) {
            0 -> GeneralTab(settings, viewModel)
            1 -> DebugTab()
        }
    }
}

// ─── General Tab ────────────────────────────────────────────────

@Composable
private fun GeneralTab(settings: AppSettings, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "tile_size") {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tile Size", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.tileSize.toString(),
                        onValueChange = { v ->
                            val n = v.toIntOrNull() ?: 0
                            viewModel.updateSettings(settings.copy(tileSize = n))
                        },
                        label = { Text("Tile size (0 = auto)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        item(key = "extra_args") {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Custom Arguments", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.extraCommand,
                        onValueChange = { viewModel.updateSettings(settings.copy(extraCommand = it)) },
                        label = { Text("Extra command args (one per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2, maxLines = 4
                    )
                }
            }
        }

        item(key = "toggles") {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Preferences", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    SettingToggle("Keep screen on", settings.keepScreen) {
                        viewModel.updateSettings(settings.copy(keepScreen = it))
                    }
                    SettingToggle("Preprocess to PNG", settings.prePng) {
                        viewModel.updateSettings(settings.copy(prePng = it))
                    }
                    SettingToggle("Auto-save to gallery", settings.autoSave) {
                        viewModel.updateSettings(settings.copy(autoSave = it))
                    }
                    SettingToggle("CPU mode (NCNN)", settings.useCPU) {
                        viewModel.updateSettings(settings.copy(useCPU = it))
                    }
                    SettingToggle("Show command input", settings.showSearchView) {
                        viewModel.updateSettings(settings.copy(showSearchView = it))
                    }
                    SettingToggle("Show final command", settings.showFinalCommand) {
                        viewModel.updateSettings(settings.copy(showFinalCommand = it))
                    }
                    SettingToggle("Use custom labels", settings.useCustomLabel) {
                        viewModel.updateSettings(settings.copy(useCustomLabel = it))
                    }
                }
            }
        }

        item(key = "processing") {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Processing", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.threadCount,
                        onValueChange = { viewModel.updateSettings(settings.copy(threadCount = it)) },
                        label = { Text("Thread count (load:proc:save)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Save format", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    SettingDropdown(
                        listOf("PNG", "JPG", "WEBP", "BMP"),
                        settings.format.coerceIn(0, 3)
                    ) { viewModel.updateSettings(settings.copy(format = it)) }
                    Spacer(Modifier.height(12.dp))
                    Text("Naming scheme", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    SettingDropdown(
                        listOf("Original", "Timestamp", "Sequential"),
                        settings.name.coerceIn(0, 2)
                    ) { viewModel.updateSettings(settings.copy(name = it)) }
                    Spacer(Modifier.height(12.dp))
                    Text("Notification mode", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    SettingDropdown(
                        listOf("Silent", "Result Only", "Detailed", "Auto Dismiss"),
                        settings.notify.coerceIn(0, 3)
                    ) { viewModel.updateSettings(settings.copy(notify = it)) }
                }
            }
        }

        item(key = "paths") {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paths", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.extraPath,
                        onValueChange = { viewModel.updateSettings(settings.copy(extraPath = it)) },
                        label = { Text("Custom model path") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.savePath,
                        onValueChange = { viewModel.updateSettings(settings.copy(savePath = it)) },
                        label = { Text("Save path") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        item(key = "commands") {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Commands", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.defaultCommand,
                        onValueChange = { viewModel.updateSettings(settings.copy(defaultCommand = it)) },
                        label = { Text("Default command") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.classicalFilters,
                        onValueChange = { viewModel.updateSettings(settings.copy(classicalFilters = it)) },
                        label = { Text("Classical interpolation filters") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.magickFilters,
                        onValueChange = { viewModel.updateSettings(settings.copy(magickFilters = it)) },
                        label = { Text("Magick filters") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        item(key = "footer") {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.padding(end = 6.dp))
                        Text("MatUnCNN", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("by Celesth", style = MaterialTheme.typography.bodyMedium, color = AppColors.textSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
                }
            }
        }

        item(key = "spacer") { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── Debug Tab ──────────────────────────────────────────────────

@Composable
private fun DebugTab() {
    val context = LocalContext.current
    val logs = DebugLog.logs

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val clip = ClipData.newPlainText("DebugLog", DebugLog.dump())
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.ContentCopy, null, Modifier.padding(end = 4.dp))
                Text("Copy")
            }
            OutlinedButton(
                onClick = { DebugLog.saveTo(context.getExternalFilesDir(null)!!) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Download, null, Modifier.padding(end = 4.dp))
                Text("Save")
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.card)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                if (logs.isEmpty()) {
                    Text("No log entries yet.", style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary)
                } else {
                    logs.forEach { entry ->
                        Text(entry, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = AppColors.textSecondary,
                            modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
        }
    }
}

// ─── Shared Components ──────────────────────────────────────────

@Composable
private fun SettingToggle(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingDropdown(options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(index); expanded = false }
                )
            }
        }
    }
}
