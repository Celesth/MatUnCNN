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
    val hasVulkan: Boolean = true,
    val scaleText: String = "",
    val useCpuFallback: Boolean = false
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

        val isAdreno = VulkanHelper.isAdrenoGpu()
        DebugLog.log("Init", "GPU check: ${if (isAdreno) "Adreno detected, will force CPU" else "non-Adreno"}")
        if (isAdreno) {
            _uiState.update { it.copy(useCpuFallback = true) }
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
            if (state.inputUri != null) {
                // Copy content URI to temp file
                val tempInput = File(app.workDir, "input.png")
                context.contentResolver.openInputStream(state.inputUri)?.use { input ->
                    FileOutputStream(tempInput).use { output ->
                        input.copyTo(output)
                    }
                }
                inputForCommand = tempInput.absolutePath
                DebugLog.log("Input", "Copied content:// to $inputForCommand")
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

            // Parse scale from command
            val scaleMatch = Regex("-s\\s+(\\d+)").find(cmd)
            val scaleText = if (scaleMatch != null) "x${scaleMatch.groupValues[1]}" else ""
            _uiState.update { it.copy(scaleText = scaleText) }

            // Inject -g -1 for CPU mode on Adreno
            val cpuForced = state.useCpuFallback && isNcnnCommand(cmd)
            val finalCmd = if (cpuForced) {
                val base = cmd.replace("input.png", inputForCommand)
                    .replace("output.png", outputPath)
                "$base -g -1"
            } else {
                cmd.replace("input.png", inputForCommand)
                    .replace("output.png", outputPath)
            }

            if (cpuForced) {
                DebugLog.log("Upscale", "Adreno detected — forcing CPU mode (-g -1)")
            }

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

<<<<<<< HEAD
                    override fun onError(error: String) {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                statusMessage = "Error: $error"
                            )
                        }
=======
                if (settings.showFinalCommand) {
                    DebugLog.log("Cmd", finalCmd)
                    progressLogHelper.appendLine("> $finalCmd")
                }

                val cacheKey = UpscaleCache.buildKey(
                    job.uri?.toString() ?: job.path ?: job.baseName,
                    template,
                    4
                )
                val cached = UpscaleCache.get(cacheKey)
                if (cached != null && File(cached).exists()) {
                    DebugLog.log("Cache", "HIT: $cacheKey -> $cached")
                    _uiState.update {
                        it.copy(
                            outputFilePath = cached,
                            outputImageExists = true,
                            scaleText = scaleText,
                            batchIndex = i + 1,
                            inputUri = job.uri ?: it.inputUri,
                            inputFilePath = job.path ?: it.inputFilePath,
                            inputFileName = job.baseName.ifBlank { it.inputFileName }
                        )
>>>>>>> f7dabff (i forgot, what this was about)
                    }
                }
<<<<<<< HEAD
            )
=======

                tasks.add(
                    RunTask(
                        id = i,
                        command = finalCmd,
                        workingDir = app.workDir,
                        outputPath = outputPath,
                        cacheKey = cacheKey,
                        scaleText = scaleText,
                        inputUri = job.uri,
                        inputFilePath = job.path,
                        inputFileName = job.baseName
                    )
                )
            }

            if (tasks.isEmpty()) {
                _uiState.update {
                    it.copy(isProcessing = false, statusMessage = "All inputs already cached or invalid")
                }
                return@launch
            }

            _uiState.update { it.copy(batchTotal = tasks.size) }
            pendingStart = PreparedRun(tasks)

            val intent = Intent(context, ProcessingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            val bound = try {
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                DebugLog.log("Service", "Bind failed: ${e.message}")
                false
            }

            if (!bound) {
                context.stopService(intent)
                pendingStart = null
                runInline(tasks)
            }
>>>>>>> f7dabff (i forgot, what this was about)
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

    fun saveOutputToGallery() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val path = _uiState.value.outputFilePath ?: return@launch
            val file = File(path)
            if (!file.exists()) return@launch

            try {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    }
                    _uiState.update { it.copy(statusMessage = "Saved to Gallery!") }
                }
            } catch (e: Exception) {
                DebugLog.log("Error", "saveOutputToGallery: ${e.message}")
                _uiState.update { it.copy(statusMessage = "Failed to save to gallery") }
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

<<<<<<< HEAD
=======
    // ─── Processing helpers ─────────────────────────────────────

    private fun buildJobs(state: MainUiState, context: Context): List<JobInput> {
        if (state.isMultipleFiles && state.selectedUris.isNotEmpty()) {
            return state.selectedUris.map { uri ->
                JobInput(
                    uri = uri,
                    path = UriUtils.getPathFromUri(uri, context),
                    baseName = UriUtils.getFileName(uri, context) ?: "image"
                )
            }
        }
        if (state.inputUri == null && state.inputFilePath == null) return emptyList()
        return listOf(
            JobInput(
                uri = state.inputUri,
                path = state.inputFilePath,
                baseName = state.inputFileName.ifBlank {
                    state.inputUri?.let { UriUtils.getFileName(it, context) } ?: "image"
                }
            )
        )
    }

    private suspend fun copyInputTo(job: JobInput, context: Context, dest: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                when {
                    job.uri != null -> context.contentResolver.openInputStream(job.uri)?.use { input ->
                        dest.outputStream().use { out -> input.copyTo(out) }
                    } ?: return@withContext false
                    job.path != null && File(job.path).exists() -> File(job.path).inputStream().use { input ->
                        dest.outputStream().use { out -> input.copyTo(out) }
                    }
                    else -> return@withContext false
                }
                true
            } catch (e: Exception) {
                DebugLog.log("Input", "copy failed: ${e.message}")
                false
            }
        }
    }

    private suspend fun ensurePng(file: File): Boolean {
        if (!PreprocessToPng.needsConversion(file.absolutePath)) return true
        return withContext(Dispatchers.IO) {
            try {
                val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
                val ok = file.outputStream().use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bmp.recycle()
                ok
            } catch (e: Exception) {
                DebugLog.log("Preprocess", "PNG conversion failed: ${e.message}")
                false
            }
        }
    }

    private fun buildOutputPath(settings: AppSettings, inputBaseName: String, index: Int): String {
        val defaultSaveDir = Environment.getExternalStorageDirectory()
            .absolutePath + File.separator + Environment.DIRECTORY_DCIM + File.separator + "MatUnCNN"
        val saveDir = if (settings.savePath.isNotBlank()) settings.savePath else defaultSaveDir
        File(saveDir).mkdirs()

        val ext = when (settings.format) {
            1 -> "jpg"
            2 -> "webp"
            3 -> "bmp"
            else -> "png"
        }
        val base = when (settings.name) {
            0 -> inputBaseName.substringBeforeLast('.').take(60) + "_upscaled"
            1 -> "upscaled_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            else -> {
                val max = File(saveDir).listFiles()
                    ?.filter { it.isFile && it.name.startsWith("upscaled_") }
                    ?.mapNotNull { it.name.removePrefix("upscaled_").substringBefore('.').toIntOrNull() }
                    ?.maxOrNull() ?: 0
                "upscaled_%03d".format(max + 1 + index)
            }
        }

        var candidate = "$saveDir/$base.$ext"
        var n = 1
        while (File(candidate).exists()) {
            candidate = "$saveDir/${base}_$n.$ext"
            n++
        }
        return candidate
    }

    private fun buildFinalCommand(
        template: String,
        inputPath: String,
        outputPath: String,
        settings: AppSettings,
        cpuForced: Boolean
    ): String {
        var cmd = template
            .replace("input.png", inputPath)
            .replace("output.png", outputPath)
        if (isNcnnCommand(cmd)) {
            if (cpuForced) cmd = "$cmd -g -1"
            if (settings.tileSize > 0) cmd = "$cmd -t ${settings.tileSize}"
            if (settings.threadCount.isNotBlank()) cmd = "$cmd -j ${settings.threadCount}"
        }
        return cmd
    }

    private fun parseScale(cmd: String): String {
        val m = Regex("-s\\s+(\\d+)").find(cmd)
        return if (m != null) "x${m.groupValues[1]}" else ""
    }

    private fun createTaskCallback(): TaskCallback = object : TaskCallback {
        override fun onProgress(line: String) {
            progressLogHelper.appendLine(line)
            val elapsed = progressLogHelper.elapsedTimeSeconds
            val pct = parsePercent(progressLogHelper.progressText)
            val eta = if (pct > 0) ((elapsed * (100 - pct) / pct).toLong()) else 0L
            _uiState.update {
                it.copy(
                    logText = progressLogHelper.displayText,
                    progressText = progressLogHelper.progressText,
                    elapsedSeconds = elapsed,
                    etaSeconds = eta
                )
            }
        }

        override fun onTaskCompleted(task: RunTask, success: Boolean) {
            if (success && task.outputPath != null) {
                val outFile = File(task.outputPath)
                if (outFile.exists() && outFile.length() > 0) {
                    task.cacheKey?.let { UpscaleCache.put(it, task.outputPath) }
                    _uiState.update {
                        it.copy(
                            outputFilePath = task.outputPath,
                            outputImageExists = true,
                            scaleText = task.scaleText,
                            batchIndex = task.id + 1,
                            inputUri = task.inputUri ?: it.inputUri,
                            inputFilePath = task.inputFilePath ?: it.inputFilePath,
                            inputFileName = task.inputFileName.ifBlank { it.inputFileName }
                        )
                    }
                    if (_uiState.value.settings.autoSave) {
                        saveFileToGallery(outFile)
                    }
                } else {
                    DebugLog.log("Output", "File missing or empty: ${task.outputPath}")
                }
            } else {
                _uiState.update { it.copy(batchIndex = task.id + 1) }
            }
        }

        override fun onAllCompleted(allSuccess: Boolean) {
            finishProcessing(allSuccess)
        }

        override fun onError(error: String) {
            DebugLog.log("Error", error)
            _uiState.update { it.copy(statusMessage = "Error: $error") }
        }
    }

    private suspend fun runInline(tasks: List<RunTask>) {
        val callback = createTaskCallback()
        var allOk = true
        for (task in tasks) {
            if (userCancelled) break
            var taskOk = false
            imageProcessor.executeCommand(
                command = task.command,
                workingDir = task.workingDir,
                extraSetup = task.extraSetup,
                outputFile = task.outputPath?.let { File(it) },
                callback = object : ImageProcessor.ProcessCallback {
                    override fun onProgress(line: String) { callback.onProgress(line) }
                    override fun onCompleted(result: String, success: Boolean) { taskOk = success }
                    override fun onError(error: String) { callback.onError(error) }
                }
            )
            callback.onTaskCompleted(task, taskOk)
            if (!taskOk) allOk = false
        }
        callback.onAllCompleted(allOk)
    }

    private fun finishProcessing(allSuccess: Boolean) {
        val summary = progressLogHelper.getCompletionSummary(allSuccess, isNcnnCommand = true)
        _uiState.update {
            it.copy(
                isProcessing = false,
                logText = progressLogHelper.displayText + summary,
                statusMessage = when {
                    userCancelled -> "Cancelled"
                    !allSuccess -> "Failed!"
                    !it.outputImageExists -> "Failed: output file is empty"
                    else -> "Complete!"
                }
            )
        }
        boundService = null
        val context = getApplication<Application>()
        mainHandler.post {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {
            }
            context.stopService(Intent(context, ProcessingService::class.java))
        }
    }

    private fun parsePercent(text: String): Float {
        val v = text.trim().removeSuffix("%").toFloatOrNull() ?: return 0f
        return v.coerceIn(0f, 100f)
    }

    private fun saveFileToGallery(file: File): Boolean {
        val context = getApplication<Application>()
        return try {
            val mime = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                else -> "image/png"
            }
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, mime)
            }
            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            true
        } catch (e: Exception) {
            DebugLog.log("Save", "gallery save failed: ${e.message}")
            false
        }
    }

>>>>>>> f7dabff (i forgot, what this was about)
    override fun onCleared() {
        super.onCleared()
        imageProcessor.shutdown()
    }

    companion object {
        private val NCNN_BINARIES = setOf(
            "realsr-ncnn", "realesrgan-ncnn", "reaalsr-ncnn",
            "srmd-ncnn", "waifu2x-ncnn", "realcugan-ncnn",
            "mnnsr-ncnn", "resize-ncnn", "anime4k"
        )

        private fun isNcnnCommand(cmd: String): Boolean {
            val name = cmd.trim().substringAfter("./").substringBefore(" ")
            return name in NCNN_BINARIES
        }
    }
}
