package com.matuncnn.app.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matuncnn.app.MatUnCnnApp
import com.matuncnn.app.data.AppSettings
import com.matuncnn.app.data.SettingsRepository
import com.matuncnn.app.model.ProcessingState
import com.matuncnn.app.model.VideoInfo
import com.matuncnn.app.model.VideoProcessingProgress
import com.matuncnn.app.processor.CommandListManager
import com.matuncnn.app.processor.VideoProcessor
import com.matuncnn.app.util.AssetsCopyer
import com.matuncnn.app.util.UriUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VideoUiState(
    val selectedVideoUri: Uri? = null,
    val selectedVideoPath: String? = null,
    val videoInfo: VideoInfo? = null,
    val commandManager: CommandListManager? = null,
    val selectedCommandIndex: Int = 0,
    val settings: AppSettings = AppSettings(),
    val processingProgress: VideoProcessingProgress = VideoProcessingProgress(),
    val isProcessing: Boolean = false,
    val statusMessage: String = ""
)

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val videoProcessor = VideoProcessor(application)

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        val context = getApplication<Application>()
        val settings = settingsRepo.settingsFlow.first()

        val manager = withContext(Dispatchers.Default) {
            val classicalFilters = settings.classicalFilters.split(",")
                .map { it.trim() }.filter { it.isNotBlank() }.toTypedArray()
            val magickFilters = settings.magickFilters.split(",")
                .map { it.trim() }.filter { it.isNotBlank() }.toTypedArray()
            val labels = context.resources.getStringArray(com.matuncnn.app.R.array.style_array)

            CommandListManager(
                presetLabels = labels,
                extraPath = settings.extraPath,
                extraCommand = settings.extraCommand,
                classicalFilters = classicalFilters,
                magickFilters = magickFilters
            ).also { it.loadCustomLabels(settings.customLabelsJson) }
        }

        _uiState.update {
            it.copy(settings = settings, commandManager = manager)
        }
    }

    fun selectVideo(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val path = UriUtils.getPathFromUri(uri, context)
            val fileName = UriUtils.getFileName(uri, context) ?: "video.mp4"

            // Get video metadata from MediaStore
            val videoInfo = getVideoMetadata(uri, path, fileName, context)

            _uiState.update {
                it.copy(
                    selectedVideoUri = uri,
                    selectedVideoPath = path,
                    videoInfo = videoInfo,
                    statusMessage = ""
                )
            }
        }
    }

    private fun getVideoMetadata(uri: Uri, path: String?, fileName: String, context: Application): VideoInfo {
        val info = VideoInfo(uri = uri, fileName = fileName, filePath = path ?: uri.toString())
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val durationIdx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                    val widthIdx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.WIDTH)
                    val heightIdx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.HEIGHT)
                    val sizeIdx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE)

                    return info.copy(
                        durationMs = if (durationIdx >= 0) cursor.getLong(durationIdx) else 0,
                        width = if (widthIdx >= 0) cursor.getInt(widthIdx) else 0,
                        height = if (heightIdx >= 0) cursor.getInt(heightIdx) else 0,
                        fileSize = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return info
    }

    fun selectCommand(index: Int) {
        _uiState.update { it.copy(selectedCommandIndex = index) }
    }

    fun startProcessing() {
        val state = _uiState.value
        val inputPath = state.selectedVideoPath ?: return
        val inputUri = state.selectedVideoUri ?: return
        val cmd = state.commandManager?.getCommandAt(state.selectedCommandIndex) ?: return
        if (cmd.isBlank()) return

        val app = getApplication<MatUnCnnApp>()

        viewModelScope.launch {
            app.ensureWorkDir()
            _uiState.update {
                it.copy(isProcessing = true, statusMessage = "Starting video processing...")
            }

            videoProcessor.processVideo(
                inputUri = inputUri,
                inputPath = inputPath,
                modelCommand = cmd,
                workingDir = app.workDir,
                scaleFactor = 4,
                onProgress = { progress ->
                    _uiState.update {
                        it.copy(
                            processingProgress = progress,
                            statusMessage = progress.message,
                            isProcessing = progress.state != ProcessingState.COMPLETED &&
                                    progress.state != ProcessingState.FAILED &&
                                    progress.state != ProcessingState.CANCELLED
                        )
                    }
                }
            )
        }
    }

    fun cancelProcessing() {
        videoProcessor.cancelProcessing()
        _uiState.update {
            it.copy(
                isProcessing = false,
                statusMessage = "Processing cancelled"
            )
        }
    }

    fun clearVideo() {
        _uiState.update {
            VideoUiState(
                commandManager = it.commandManager,
                settings = it.settings
            )
        }
    }
}
