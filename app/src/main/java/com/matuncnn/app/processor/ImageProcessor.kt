package com.matuncnn.app.processor

import android.util.Log
import com.matuncnn.app.util.ExecHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class ImageProcessor {
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

            val wd = File(workingDir)
            if (!wd.exists()) {
                callback.onError("Working directory does not exist: $workingDir")
                callback.onCompleted("", false)
                return@withContext
            }

            val parts = command.trim().split("\\s+".toRegex())
            if (parts.isEmpty()) {
                callback.onError("Empty command")
                callback.onCompleted("", false)
                return@withContext
            }

            val binaryPath = parts[0]
            val binaryFile = if (binaryPath.startsWith("./")) {
                File(workingDir, binaryPath.removePrefix("./"))
            } else {
                File(binaryPath)
            }

            if (!binaryFile.exists()) {
                callback.onError("Binary not found: ${binaryFile.absolutePath}")
                callback.onCompleted("", false)
                return@withContext
            }

            val args = parts.drop(1)
            val env = mutableMapOf<String, String>()
            if (extraSetup.isNotBlank()) {
                extraSetup.lineSequence().forEach { line ->
                    val eq = line.indexOf('=')
                    if (eq > 0) env[line.substring(0, eq)] = line.substring(eq + 1)
                }
            }

            currentProcess = ExecHelper.exec(binaryFile, args, workingDir, env)

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
        cancelCurrentTask()
    }
}
