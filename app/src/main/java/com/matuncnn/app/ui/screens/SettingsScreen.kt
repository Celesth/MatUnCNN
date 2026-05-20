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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.matuncnn.app.data.AppSettings
import com.matuncnn.app.ui.components.DoubleOutlineCard
import com.matuncnn.app.ui.theme.statusError
import com.matuncnn.app.ui.theme.statusSuccess
import com.matuncnn.app.util.DebugLog
import com.matuncnn.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Theme", "Debug")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2B2B2B))) {
        ScrollableTabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color(0xFF2B2B2B),
            contentColor = Color.White,
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
            1 -> ThemeTab(settings, viewModel)
            2 -> DebugTab()
        }
    }
}

// ─── General Tab ────────────────────────────────────────────────

@Composable
private fun GeneralTab(settings: AppSettings, viewModel: MainViewModel) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? -> uri?.let { viewModel.saveLogTo(it) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "tile_size") {
            DoubleOutlineCard {
                Text("Tile Size", color = Color.White, style = MaterialTheme.typography.titleMedium)
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

        item(key = "extra_args") {
            DoubleOutlineCard {
                Text("Custom Arguments", color = Color.White, style = MaterialTheme.typography.titleMedium)
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

        item(key = "toggles") {
            DoubleOutlineCard {
                Text("Preferences", color = Color.White, style = MaterialTheme.typography.titleMedium)
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

        item(key = "processing") {
            DoubleOutlineCard {
                Text("Processing", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.threadCount,
                    onValueChange = { viewModel.updateSettings(settings.copy(threadCount = it)) },
                    label = { Text("Thread count (load:proc:save)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Text("Save format", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                ThemeDropdown(
                    options = listOf("PNG", "JPG", "WEBP", "BMP"),
                    selectedIndex = settings.format.coerceIn(0, 3),
                    onSelected = { viewModel.updateSettings(settings.copy(format = it)) }
                )
                Spacer(Modifier.height(12.dp))
                Text("Naming scheme", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                ThemeDropdown(
                    options = listOf("Original", "Timestamp", "Sequential"),
                    selectedIndex = settings.name.coerceIn(0, 2),
                    onSelected = { viewModel.updateSettings(settings.copy(name = it)) }
                )
                Spacer(Modifier.height(12.dp))
                Text("Notification mode", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                ThemeDropdown(
                    options = listOf("Silent", "Result Only", "Detailed", "Auto Dismiss"),
                    selectedIndex = settings.notify.coerceIn(0, 3),
                    onSelected = { viewModel.updateSettings(settings.copy(notify = it)) }
                )
            }
        }

        item(key = "paths") {
            DoubleOutlineCard {
                Text("Paths", color = Color.White, style = MaterialTheme.typography.titleMedium)
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

        item(key = "commands") {
            DoubleOutlineCard {
                Text("Commands", color = Color.White, style = MaterialTheme.typography.titleMedium)
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

        // Footer
        item(key = "footer") {
            DoubleOutlineCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Color(0xAAFFFFFF), modifier = Modifier.padding(end = 6.dp))
                        Text("MatUnCNN", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("by Celesth", color = Color(0xAAFFFFFF), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── Theme Tab ──────────────────────────────────────────────────

@Composable
private fun ThemeTab(settings: AppSettings, viewModel: MainViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DoubleOutlineCard {
            Text("Theme", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val themeOptions = listOf("System", "Light", "Dark", "AMOLED", "Hyprland")
            ThemeDropdown(
                options = themeOptions,
                selectedIndex = settings.themeIndex.coerceIn(0, 4),
                onSelected = { viewModel.updateSettings(settings.copy(themeIndex = it)) }
            )
        }

        // Theme preview cards
        val previews = listOf("System" to Color(0xFF1C1B1F), "Light" to Color(0xFFFFFBFE),
            "Dark" to Color(0xFF1C1B1F), "AMOLED" to Color.Black, "Hyprland" to Color(0xFF0D1117))
        previews.forEachIndexed { i, (name, bg) ->
            DoubleOutlineCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(24.dp).height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(bg)
                            .then(Modifier)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    if (i == settings.themeIndex) {
                        Text("ACTIVE", color = statusSuccess, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val clip = ClipData.newPlainText("DebugLog", DebugLog.dump())
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(clip)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.ContentCopy, null, Modifier.padding(end = 4.dp))
                Text("Copy")
            }
            OutlinedButton(
                onClick = {
                    DebugLog.saveTo(context.getExternalFilesDir(null)!!)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Download, null, Modifier.padding(end = 4.dp))
                Text("Save")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
                .padding(8.dp)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .horizontalScroll(rememberScrollState())
            ) {
                if (logs.isEmpty()) {
                    Text("No log entries yet.",
                        color = Color(0x88FFFFFF),
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    logs.forEach { entry ->
                        Text(entry,
                            color = Color(0xFFCCCCCC),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
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
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
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
