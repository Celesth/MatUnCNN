package com.matuncnn.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

data class DownloadProgress(
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = -1,
    val message: String = "",
    val isFinished: Boolean = false,
    val error: String = ""
)

object AssetsDownloader {

    private const val RELEASE_URL = "https://github.com/Celesth/MatUnCNN/releases/download/v1.0.0-beta/realsr-assets-v1.0.0-beta.zip"

    suspend fun needsDownload(workDir: String): Boolean {
        return withContext(Dispatchers.IO) {
            val dir = File(workDir)
            if (!dir.exists() || !dir.isDirectory) return@withContext true
            val contents = dir.listFiles()
            if (contents.isNullOrEmpty()) return@withContext true
            contents.any { it.isFile && !it.name.endsWith(".param") && !it.name.endsWith(".xml") }.not()
        }
    }

    suspend fun downloadAndExtract(
        workDir: String,
        onProgress: (DownloadProgress) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(DownloadProgress(message = "Connecting..."))

            val url = URL(RELEASE_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.connect()

            val totalBytes = conn.contentLengthLong
            val zipFile = File(workDir, "download.zip")
            zipFile.parentFile?.mkdirs()

            val input = conn.inputStream
            val output = FileOutputStream(zipFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L
            var lastProgress = 0L

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (totalRead - lastProgress > 65536 || totalRead == totalBytes) {
                    lastProgress = totalRead
                    val pct = if (totalBytes > 0) (totalRead * 100 / totalBytes).toInt() else 0
                    onProgress(DownloadProgress(
                        bytesDownloaded = totalRead,
                        totalBytes = totalBytes,
                        message = "Downloading ($pct%)..."
                    ))
                }
            }
            output.close()
            input.close()
            conn.disconnect()

            // Count entries before extracting
            var entryCount = 0
            ZipInputStream(zipFile.inputStream()).use { zis ->
                while (zis.nextEntry != null) {
                    if (!zis.name.endsWith('/')) entryCount++
                    zis.closeEntry()
                }
            }

            var extracted = 0
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    extracted++
                    onProgress(DownloadProgress(
                        message = "Extracting ($extracted/$entryCount)..."
                    ))
                    val target = File(File(workDir), entry.name)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            zipFile.delete()
            setExecutableRecursive(File(workDir))

            onProgress(DownloadProgress(isFinished = true, message = "Done!"))

            Result.success(Unit)
        } catch (e: Exception) {
            onProgress(DownloadProgress(error = e.message ?: "Download failed"))
            Result.failure(e)
        }
    }

    private fun setExecutableRecursive(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                setExecutableRecursive(file)
            } else {
                file.setExecutable(true)
            }
        }
    }
}
