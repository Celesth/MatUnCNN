package com.matuncnn.app.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matuncnn.app.MatUnCnnApp
import com.matuncnn.app.data.AppSettings
import com.matuncnn.app.data.SettingsRepository
import com.matuncnn.app.processor.CommandListManager
import com.matuncnn.app.processor.ImageProcessor
import com.matuncnn.app.util.AssetsCopyer
import com.matuncnn.app.util.AssetsDownloader
import com.matuncnn.app.util.DebugLog
import com.matuncnn.app.util.DownloadProgress
import com.matuncnn.app.util.ProgressLogHelper
import com.matuncnn.app.util.UpscaleCache
import com.matuncnn.app.util.UriUtils
import com.matuncnn.app.util.VulkanHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class MainUiState(
    val isInitialized: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val commandManager: CommandListManager? = null,
    val selectedCommandIndex: Int = 0,
    val inputFilePath: String? = null,
    val inputUri: Uri? = null,
    val outputFilePath: String? = null,
    val isProcessing: Boolean = false,
    val logText: String = "",
    val progressText: String = "",
    val statusMessage: String = "",
    val inputImageExists: Boolean = false,
    val outputImageExists: Boolean = false,
    val inputFileName: String = "",
    val showCommandInput: Boolean = false,
    val commandText: String = "",
    val isMultipleFiles: Boolean = false,
    val selectedUris: List<Uri> = emptyList(),
    val downloadProgress: DownloadProgress? = null,
    val hasVulkan: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val imageProcessor = ImageProcessor()
    private val progressLogHelper = ProgressLogHelper()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val settingsFlow = settingsRepo.settingsFlow

    private val assetsReady = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            initializeApp()
        }
    }

    private suspend fun initializeApp() {
        if (assetsReady.get()) return

        val context = getApplication<Application>()
        val app = getApplication<MatUnCnnApp>()

        val ok = withContext(Dispatchers.IO) {
            app.ensureWorkDir()
            DebugLog.log("Init", "Work dir: ${app.workDir}")

            if (AssetsDownloader.needsDownload(app.workDir)) {
                DebugLog.log("Init", "Assets download needed, starting...")
                val result = AssetsDownloader.downloadAndExtract(app.workDir) { progress ->
                    _uiState.update { it.copy(downloadProgress = progress) }
                }
                _uiState.update { it.copy(downloadProgress = null) }
                if (result.isFailure) {
                    DebugLog.log("Init", "Asset download failed")
                    return@withContext false
                }
                DebugLog.log("Init", "Asset download complete")
            }

            DebugLog.log("Init", "Copying bundled assets...")
            AssetsCopyer.releaseAssets(context, "realsr", app.workDir, false)
            DebugLog.log("Init", "Assets ready")
            true
        }
        if (!ok) return

        val hasVulkan = VulkanHelper.hasVulkan(context.packageManager)
        DebugLog.log("Init", "Vulkan check: ${if (hasVulkan) "available" else "NOT available"}")
        if (!hasVulkan) {
            _uiState.update { it.copy(hasVulkan = false) }
        }

        val settings = settingsRepo.settingsFlow.first()
        val manager = withContext(Dispatchers.Default) {
            val classicalFilters = settings.classicalFilters.split(",")
                .map { it.trim() }.filter { it.isNotBlank() }.toTypedArray()
            val magickFilters = settings.magickFilters.split(",")
                .map { it.trim() }.filter { it.isNotBlank() }.toTypedArray()

            val labels = context.resources.getStringArray(
                com.matuncnn.app.R.array.style_array
            )

            CommandListManager(
                presetLabels = labels,
                extraPath = settings.extraPath,
                extraCommand = settings.extraCommand,
                classicalFilters = classicalFilters,
                magickFilters = magickFilters
            ).also { it.loadCustomLabels(settings.customLabelsJson) }
        }

        val defaultCommand = if (settings.defaultCommand.isNotBlank()) {
            settings.defaultCommand
        } else {
            manager.getCommandAt(settings.selectCommand)
        }

        assetsReady.set(true)

        _uiState.update {
            it.copy(
                isInitialized = true,
                settings = settings,
                commandManager = manager,
                selectedCommandIndex = settings.selectCommand,
                showCommandInput = settings.showSearchView,
                commandText = defaultCommand
            )
        }
    }

    fun handleSendIntent(intent: Intent?) {
        if (intent == null) return

        when {
            intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (imageUri != null) {
                    setInputImage(imageUri)
                }
            }
            intent.action == Intent.ACTION_SEND_MULTIPLE && intent.type?.startsWith("image/") == true -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (!uris.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(isMultipleFiles = true, selectedUris = uris)
                    }
                    setInputImage(uris.first())
                }
            }
        }
    }

    fun setInputImage(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val path = UriUtils.getPathFromUri(uri, context)
            val fileName = UriUtils.getFileName(uri, context) ?: "image"

            _uiState.update {
                it.copy(
                    inputUri = uri,
                    inputFilePath = path ?: uri.toString(),
                    inputFileName = fileName,
                    inputImageExists = true,
                    outputFilePath = null,
                    outputImageExists = false,
                    logText = "",
                    statusMessage = ""
                )
            }
        }
    }

    fun selectCommand(index: Int) {
        val manager = _uiState.value.commandManager ?: return
        val cmd = manager.getCommandAt(index)
        _uiState.update {
            it.copy(
                selectedCommandIndex = index,
                commandText = cmd
            )
        }
        viewModelScope.launch {
            settingsRepo.update { it.copy(selectCommand = index) }
        }
    }

    fun updateCommandText(text: String) {
        _uiState.update { it.copy(commandText = text) }
    }

    fun toggleCommandInput() {
        _uiState.update { it.copy(showCommandInput = !it.showCommandInput) }
    }

    fun startProcessing() {
        val state = _uiState.value
        val inputFile = state.inputFilePath ?: return
        val cmd = state.commandText
        if (cmd.isBlank()) return

        viewModelScope.launch {
            val app = getApplication<MatUnCnnApp>()
            val context = getApplication<Application>()
            app.ensureWorkDir()

            // Handle input file
            val inputForCommand: String
            if (state.inputUri != null && state.inputFilePath == null) {
                // Copy content URI to temp file
                val tempInput = File(app.workDir, "input.png")
                context.contentResolver.openInputStream(state.inputUri)?.use { input ->
                    FileOutputStream(tempInput).use { output ->
                        input.copyTo(output)
                    }
                }
                inputForCommand = tempInput.absolutePath
            } else {
                inputForCommand = inputFile
            }

            val defaultSaveDir = Environment.getExternalStorageDirectory()
                .absolutePath + File.separator + Environment.DIRECTORY_DCIM + File.separator + "MatUnCNN"
            val saveDir = if (state.settings.savePath.isNotBlank()) state.settings.savePath else defaultSaveDir
            File(saveDir).mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFileName = "upscaled_${timestamp}.png"
            val outputPath = "$saveDir/$outputFileName"

            val finalCmd = cmd
                .replace("input.png", inputForCommand)
                .replace("output.png", outputPath)

            // Check cache
            val cacheKey = UpscaleCache.buildKey(
                (state.inputUri?.toString() ?: inputFile),
                cmd,
                4
            )
            val cached = UpscaleCache.get(cacheKey)
            if (cached != null && File(cached).exists()) {
                DebugLog.log("Cache", "HIT: $cacheKey -> $cached")
                _uiState.update {
                    it.copy(
                        outputFilePath = cached,
                        outputImageExists = true,
                        statusMessage = "Complete! (cached)"
                    )
                }
                return@launch
            }
            DebugLog.log("Cache", "MISS: $cacheKey")

            progressLogHelper.reset()
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    logText = "",
                    progressText = "",
                    statusMessage = "Processing...",
                    outputFilePath = outputPath
                )
            }

            imageProcessor.executeCommand(
                command = finalCmd,
                workingDir = app.workDir,
                callback = object : ImageProcessor.ProcessCallback {
                    override fun onProgress(line: String) {
                        progressLogHelper.appendLine(line)
                        _uiState.update {
                            it.copy(
                                logText = progressLogHelper.displayText,
                                progressText = progressLogHelper.progressText
                            )
                        }
                    }

                    override fun onCompleted(result: String, success: Boolean) {
                        if (success) {
                            UpscaleCache.put(cacheKey, outputPath)
                            DebugLog.log("Cache", "Stored: $cacheKey -> $outputPath")
                        }
                        val outFile = File(outputPath)
                        val fileOk = outFile.exists() && outFile.length() > 0
                        if (!fileOk && success) {
                            DebugLog.log("Output", "File missing or empty: $outputPath")
                        } else if (fileOk) {
                            DebugLog.log("Output", "File OK: ${outFile.length()} bytes")
                        }
                        val summary = progressLogHelper.getCompletionSummary(
                            success,
                            isNcnnCommand = true
                        )
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                logText = progressLogHelper.displayText + summary,
                                statusMessage = if (!success) "Failed!"
                                    else if (!fileOk) "Failed: output file is empty"
                                    else "Complete!",
                                outputImageExists = fileOk
                            )
                        }
                    }

                    override fun onError(error: String) {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                statusMessage = "Error: $error"
                            )
                        }
                    }
                }
            )
        }
    }

    fun cancelProcessing() {
        imageProcessor.cancelCurrentTask()
        _uiState.update {
            it.copy(
                isProcessing = false,
                statusMessage = "Cancelled"
            )
        }
    }

    fun shareOutput() {
        val path = _uiState.value.outputFilePath ?: return
        val context = getApplication<Application>()
        val file = File(path)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share image"))
    }

    fun saveLogTo(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(_uiState.value.logText.toByteArray())
                }
            } catch (e: Exception) {
                DebugLog.log("Error", "saveLogTo: ${e.message}")
                _uiState.update { it.copy(statusMessage = "Failed to save log") }
            }
        }
    }

    fun saveOutputTo(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val path = _uiState.value.outputFilePath ?: return@launch
            try {
                val input = java.io.File(path).inputStream()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    input.copyTo(out)
                }
                input.close()
                _uiState.update { it.copy(statusMessage = "Saved!") }
            } catch (e: Exception) {
                DebugLog.log("Error", "saveOutputTo: ${e.message}")
                _uiState.update { it.copy(statusMessage = "Failed to save") }
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _uiState.update { it.copy(settings = newSettings) }
        viewModelScope.launch {
            settingsRepo.update { newSettings }
            // Rebuild command manager
            val context = getApplication<Application>()
            val classicalFilters = newSettings.classicalFilters.split(",")
                .map { it.trim() }.filter { it.isNotBlank() }.toTypedArray()
            val magickFilters = newSettings.magickFilters.split(",")
                .map { it.trim() }.filter { it.isNotBlank() }.toTypedArray()
            val labels = context.resources.getStringArray(com.matuncnn.app.R.array.style_array)

            val manager = CommandListManager(
                presetLabels = labels,
                extraPath = newSettings.extraPath,
                extraCommand = newSettings.extraCommand,
                classicalFilters = classicalFilters,
                magickFilters = magickFilters
            ).also { it.loadCustomLabels(newSettings.customLabelsJson) }

            _uiState.update { it.copy(commandManager = manager) }
        }
    }

    fun retryDownload() {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadProgress = DownloadProgress(message = "Retrying...")) }
            initializeApp()
        }
    }

    override fun onCleared() {
        super.onCleared()
        imageProcessor.shutdown()
    }
}
