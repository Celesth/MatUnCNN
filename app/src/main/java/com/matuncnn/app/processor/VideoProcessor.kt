package com.matuncnn.app.processor

import android.content.Context
import android.net.Uri
import android.util.Log
import com.matuncnn.app.model.ProcessingState
import com.matuncnn.app.model.VideoProcessingProgress
import com.matuncnn.app.util.UriUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class VideoProcessor(private val context: Context) {

    private var currentJob: Job? = null
    private var _progress: VideoProcessingProgress = VideoProcessingProgress()
    val progress: VideoProcessingProgress get() = _progress

    suspend fun processVideo(
        inputUri: Uri,
        inputPath: String,
        modelCommand: String,
        workingDir: String,
        scaleFactor: Int = 4,
        onProgress: (VideoProcessingProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        cancelProcessing()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val framesDir = File(context.cacheDir, "video_frames_$timestamp")
        val outputFramesDir = File(context.cacheDir, "video_output_$timestamp")

        val audioFile = File(context.cacheDir, "audio_${timestamp}.aac")

        try {
            framesDir.mkdirs()
            outputFramesDir.mkdirs()

            val inputFileName = File(inputPath).nameWithoutExtension
            val outputFileName = "${inputFileName}_upscaled_${timestamp}.mp4"
            val outputDir = File(context.getExternalFilesDir(null), "MatUnCNN")
            outputDir.mkdirs()
            val outputPath = File(outputDir, outputFileName).absolutePath

            val ffmpegPath = "$workingDir/ffmpeg"

            _progress = _progress.copy(
                state = ProcessingState.EXTRACTING_FRAMES,
                message = "Analyzing video..."
            )
            onProgress(_progress)

            val fps = getVideoFps(ffmpegPath, inputPath)
            val totalFrames = getVideoFrameCount(ffmpegPath, inputPath)
            val hasAudio = hasAudioTrack(ffmpegPath, inputPath)

            if (hasAudio) {
                _progress = _progress.copy(message = "Extracting audio...")
                onProgress(_progress)
                runFfmpeg(ffmpegPath, listOf(
                    "-i", inputPath, "-vn", "-acodec", "copy",
                    "-y", audioFile.absolutePath
                ), workingDir)
            }

            _progress = _progress.copy(
                totalFrames = totalFrames,
                message = "Extracting frames (0/$totalFrames)..."
            )
            onProgress(_progress)

            val framePattern = "${framesDir.absolutePath}/frame_%05d.png"
            runFfmpeg(ffmpegPath, listOf(
                "-i", inputPath,
                "-vf", "fps=$fps",
                "-y", framePattern
            ), workingDir)

            val actualFrames = framesDir.listFiles()
                ?.filter { it.name.startsWith("frame_") }
                ?.sortedBy { it.name } ?: emptyList()
            val total = if (totalFrames > 0) totalFrames else actualFrames.size

            if (actualFrames.isEmpty()) {
                _progress = _progress.copy(state = ProcessingState.FAILED, message = "No frames extracted")
                onProgress(_progress)
                return@withContext
            }

            _progress = _progress.copy(
                state = ProcessingState.UPSCALING_FRAMES,
                totalFrames = total,
                currentFrame = 0,
                message = "Upscaling frames (0/$total)..."
            )
            onProgress(_progress)

            var processedCount = 0
            for (frame in actualFrames) {
                if (!isActive) {
                    onProgress(_progress.copy(state = ProcessingState.CANCELLED, message = "Cancelled"))
                    cleanup(framesDir, outputFramesDir, audioFile)
                    return@withContext
                }

                val outputFrame = File(outputFramesDir, frame.name)
                val cmd = modelCommand
                    .replace("input.png", frame.absolutePath)
                    .replace("output.png", outputFrame.absolutePath)

                try {
                    runShellCommand(cmd, workingDir)
                } catch (e: Exception) {
                    Log.w("VideoProcessor", "Frame upscale failed: ${frame.name}", e)
                }

                processedCount++
                _progress = _progress.copy(
                    progress = processedCount.toFloat() / total,
                    currentFrame = processedCount,
                    message = "Upscaling frames ($processedCount/$total)..."
                )
                onProgress(_progress)
            }

            _progress = _progress.copy(
                state = ProcessingState.REASSEMBLING_VIDEO,
                progress = 0.85f,
                message = "Reassembling video..."
            )
            onProgress(_progress)

            val outPattern = "${outputFramesDir.absolutePath}/frame_%05d.png"
            runFfmpeg(ffmpegPath, listOf(
                "-framerate", fps.toString(),
                "-i", outPattern,
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-preset", "medium",
                "-crf", "18",
                "-y", outputPath
            ), workingDir)

            if (hasAudio && audioFile.exists()) {
                _progress = _progress.copy(message = "Adding audio...")
                onProgress(_progress)
                val withAudio = outputPath.replace(".mp4", "_audio.mp4")
                runFfmpeg(ffmpegPath, listOf(
                    "-i", outputPath, "-i", audioFile.absolutePath,
                    "-c:v", "copy", "-c:a", "aac",
                    "-map", "0:v:0", "-map", "1:a:0",
                    "-shortest", "-y", withAudio
                ), workingDir)
                File(outputPath).delete()
                File(withAudio).renameTo(File(outputPath))
            }

            _progress = _progress.copy(
                state = ProcessingState.COMPLETED,
                progress = 1f,
                message = "Video upscaling complete!",
                outputPath = outputPath
            )
            onProgress(_progress)

            cleanup(framesDir, outputFramesDir, audioFile)

        } catch (e: Exception) {
            Log.e("VideoProcessor", "Failed", e)
            _progress = _progress.copy(state = ProcessingState.FAILED, message = e.message ?: "Error")
            onProgress(_progress)
            cleanup(framesDir, outputFramesDir, audioFile)
        }
    }

    private fun getVideoFps(ffmpegPath: String, inputPath: String): Double {
        val output = runFfmpegCapture(ffmpegPath, listOf("-i", inputPath), null)
        Regex("(\\d+\\.?\\d*)\\s*fps").find(output)?.let {
            return it.groupValues[1].toDoubleOrNull() ?: 30.0
        }
        return 30.0
    }

    private fun getVideoFrameCount(ffmpegPath: String, inputPath: String): Int {
        val output = runFfmpegCapture(ffmpegPath, listOf("-i", inputPath), null)
        Regex("(\\d+)\\s*frames?").find(output)?.let {
            return it.groupValues[1].toIntOrNull() ?: 0
        }
        return 0
    }

    private fun hasAudioTrack(ffmpegPath: String, inputPath: String): Boolean {
        val output = runFfmpegCapture(ffmpegPath, listOf("-i", inputPath), null)
        return output.contains("Audio:") || output.contains("aac") || output.contains("mp3")
    }

    private fun runFfmpeg(ffmpegPath: String, args: List<String>, workingDir: String): Boolean {
        return try {
            val cmd = listOf(ffmpegPath) + args
            val pb = ProcessBuilder(cmd)
            pb.directory(File(workingDir))
            pb.redirectErrorStream(true)
            val process = pb.start()
            process.inputStream.bufferedReader().use { it.readLines() }
            process.waitFor() == 0
        } catch (e: Exception) {
            Log.e("VideoProcessor", "ffmpeg failed", e)
            false
        }
    }

    private fun runFfmpegCapture(ffmpegPath: String, args: List<String>, workingDir: String?): String {
        return try {
            val cmd = listOf(ffmpegPath) + args
            val pb = ProcessBuilder(cmd)
            if (workingDir != null) pb.directory(File(workingDir))
            pb.redirectErrorStream(true)
            val process = pb.start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            ""
        }
    }

    private fun runShellCommand(command: String, workingDir: String): Boolean {
        val pb = ProcessBuilder("sh")
        pb.directory(File(workingDir))
        pb.redirectErrorStream(true)
        val process = pb.start()
        val os = process.outputStream
        os.write(("cd $workingDir\n").toByteArray())
        os.write(("export LD_LIBRARY_PATH=$workingDir\n").toByteArray())
        os.write(("chmod -R 777 . 2>/dev/null\n").toByteArray())
        os.write(("$command\n").toByteArray())
        os.write("exit\n".toByteArray())
        os.flush()
        os.close()
        process.inputStream.bufferedReader().use { it.readLines() }
        val exit = process.waitFor()
        process.destroy()
        return exit == 0
    }

    fun cancelProcessing() {
        currentJob?.cancel()
    }

    private fun cleanup(vararg dirs: File) {
        for (dir in dirs) {
            if (dir.isDirectory) dir.listFiles()?.forEach { it.delete() }
            dir.delete()
        }
    }
}
