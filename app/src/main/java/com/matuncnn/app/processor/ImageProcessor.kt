package com.matuncnn.app.processor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ImageProcessor {
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var currentProcess: Process? = null
    private var currentJob: Job? = null

    interface ProcessCallback {
        fun onProgress(line: String)
        fun onCompleted(result: String, success: Boolean)
        fun onError(error: String)
    }

    suspend fun executeCommand(
        command: String,
        workingDir: String,
        extraSetup: String = "",
        callback: ProcessCallback
    ) = withContext(Dispatchers.IO) {
        cancelCurrentTask()

        val resultBuilder = StringBuilder()
        var success = false

        try {
            Log.d("ImageProcessor", "Executing command: $command")
            Log.d("ImageProcessor", "Working dir: $workingDir")

            // Verify working dir exists and has binaries
            val wd = File(workingDir)
            if (!wd.exists()) {
                callback.onError("Working directory does not exist: $workingDir")
                callback.onCompleted("", false)
                return@withContext
            }
            val files = wd.listFiles()
            Log.d("ImageProcessor", "Files in work dir: ${files?.map { it.name }?.joinToString(", ") ?: "empty"}")

            val processBuilder = ProcessBuilder("sh")
            processBuilder.directory(wd)
            processBuilder.redirectErrorStream(true)

            currentProcess = processBuilder.start()

            // Make all files in the work dir executable using Java API
            wd.listFiles()?.forEach { file ->
                if (file.isFile) file.setExecutable(true)
            }

            val os: OutputStream = currentProcess!!.outputStream
            val setupCmd = buildString {
                appendLine("export LD_LIBRARY_PATH=$workingDir")
                appendLine("cd $workingDir")
                appendLine("ls -la . 2>&1")
            }
            os.write(setupCmd.toByteArray())
            if (extraSetup.isNotBlank()) {
                os.write((extraSetup + "\n").toByteArray())
            }
            os.write((command + "\n").toByteArray())
            os.write("exit\n".toByteArray())
            os.flush()
            os.close()

            val reader = BufferedReader(InputStreamReader(currentProcess!!.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (!isActive) {
                    throw InterruptedException("Task interrupted")
                }
                val l = line ?: continue
                if (l.contains("unused DT entry")) continue
                if (l.startsWith("CPU Group:")) continue
                if (l.startsWith("(last_midr")) continue
                if (l.startsWith("Error tunning info")) continue

                Log.d("ImageProcessor", l)
                callback.onProgress(l)
                resultBuilder.appendLine(l)
            }

            val exitCode = currentProcess!!.waitFor()
            success = exitCode == 0
            Log.d("ImageProcessor", "Process finished with exit code: $exitCode")
        } catch (e: InterruptedException) {
            Log.w("ImageProcessor", "Process interrupted")
            callback.onError("Process interrupted")
        } catch (e: Exception) {
            Log.e("ImageProcessor", "Error executing process", e)
            callback.onError(e.message ?: "Unknown error")
        } finally {
            currentProcess?.destroy()
            currentProcess = null
            callback.onCompleted(resultBuilder.toString(), success)
        }
    }

    fun cancelCurrentTask() {
        currentJob?.cancel()
        currentProcess?.destroy()
        currentProcess = null
    }

    fun shutdown() {
        executorService.shutdownNow()
    }
}
