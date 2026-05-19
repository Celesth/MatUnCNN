package com.matuncnn.app.model

import android.net.Uri

data class VideoInfo(
    val uri: Uri,
    val fileName: String,
    val filePath: String,
    val durationMs: Long = 0,
    val frameCount: Int = 0,
    val fps: Float = 0f,
    val width: Int = 0,
    val height: Int = 0,
    val hasAudio: Boolean = false,
    val fileSize: Long = 0
)

enum class ProcessingState {
    IDLE,
    EXTRACTING_FRAMES,
    UPSCALING_FRAMES,
    REASSEMBLING_VIDEO,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class VideoProcessingProgress(
    val state: ProcessingState = ProcessingState.IDLE,
    val progress: Float = 0f,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val message: String = "",
    val outputPath: String = ""
)
