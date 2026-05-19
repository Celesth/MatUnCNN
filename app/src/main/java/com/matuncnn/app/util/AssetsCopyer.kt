package com.matuncnn.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object AssetsCopyer {
    private const val TAG = "AssetsCopyer"

    fun releaseAssets(
        context: Context,
        assetsDir: String,
        releaseDir: String,
        skipExistFile: Boolean = true
    ) {
        if (releaseDir.isBlank()) return

        val normReleaseDir = releaseDir.trimEnd('/')
        val normAssetsDir = if (assetsDir.isBlank() || assetsDir == "/") "" else assetsDir.trimEnd('/')

        val assetManager = context.assets
        try {
            val fileNames = assetManager.list(normAssetsDir)
            if (fileNames.isNullOrEmpty()) return

            if (fileNames.isNotEmpty()) {
                for (name in fileNames) {
                    val fullName = if (normAssetsDir.isNotEmpty()) "$normAssetsDir/$name" else name
                    val childNames = assetManager.list(fullName)
                    if (!childNames.isNullOrEmpty()) {
                        val subDir = File(normReleaseDir, name).absolutePath
                        File(subDir).mkdirs()
                        releaseAssets(context, fullName, subDir, skipExistFile)
                    } else {
                        val `is` = assetManager.open(fullName)
                        writeFile("$normReleaseDir/$name", `is`, skipExistFile)
                    }
                }
            } else {
                val `is` = assetManager.open(normAssetsDir)
                writeFile("$normReleaseDir/${normAssetsDir.substringAfterLast('/')}", `is`, skipExistFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release assets", e)
        }
    }

    private fun writeFile(fileName: String, `in`: InputStream, skipExistFile: Boolean): Boolean {
        return try {
            val file = File(fileName)
            if (file.exists()) {
                if (skipExistFile) {
                    Log.d(TAG, "skip file: $fileName")
                    `in`.close()
                    return true
                }
                file.delete()
            } else {
                file.parentFile?.mkdirs()
            }

            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4112)
                var read: Int
                while (`in`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
            }
            `in`.close()
            Log.d(TAG, "copied file: $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file: $fileName", e)
            false
        }
    }

    fun isAssetExists(context: Context, path: String): Boolean {
        return try {
            context.assets.list(path)?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }
}
