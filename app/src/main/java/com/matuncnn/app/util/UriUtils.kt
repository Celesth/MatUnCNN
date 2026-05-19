package com.matuncnn.app.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.lang.reflect.InvocationTargetException

object UriUtils {
    private const val TAG = "UriUtils"

    fun getFileName(uri: Uri, context: Context): String? {
        val path = getPathFromUri(uri, context)
        if (path != null) {
            Log.w(TAG, "uri-$uri\npath-$path")
            return File(path).name
        }
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file name", e)
        }
        return null
    }

    fun getPathFromUri(uri: Uri, context: Context): String? {
        if (uri == null) return null

        if (DocumentsContract.isDocumentUri(context, uri)) {
            Log.w(TAG, "documentUri: $uri")
            val authority = uri.authority ?: return null

            return when {
                "com.android.externalstorage.documents" == authority -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    if (split[0] == "primary") {
                        "${android.os.Environment.getExternalStorageDirectory()}/${split[1]}"
                    } else null
                }
                "com.android.providers.downloads.documents" == authority -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    if (docId.startsWith("raw:")) {
                        docId.removePrefix("raw:")
                    } else {
                        val downloadUri = Uri.parse("content://downloads/public_downloads")
                        val realId = docId.replaceFirst("^(msf):".toRegex(), "")
                        ContentUrisCompat.withAppendedId(downloadUri, realId.toLong())?.let {
                            queryAbsolutePath(context, it)
                        }
                    }
                }
                "com.android.providers.media.documents" == authority -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    val mediaUri = when (type) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> return null
                    }
                    val idUri = ContentUrisCompat.withAppendedId(mediaUri, split[1].toLong())
                    idUri?.let { queryAbsolutePath(context, it) }
                }
                else -> null
            }
        }

        return when (uri.scheme) {
            "content" -> {
                var path = queryAbsolutePath(context, uri)
                if (path == null) {
                    path = getFPUriToPath(uri, context)
                }
                path
            }
            "file" -> uri.path
            else -> null
        }
    }

    private fun queryAbsolutePath(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    return cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "queryAbsolutePath failed", e)
        }
        return null
    }

    private fun getFPUriToPath(uri: Uri, context: Context): String? {
        return try {
            val packs = context.packageManager.getInstalledPackages(android.content.pm.PackageManager.GET_PROVIDERS)
            val fpClassName = FileProvider::class.java.name
            for (pack in packs) {
                val providers = pack.providers ?: continue
                for (provider in providers) {
                    if (uri.authority == provider.authority && provider.name.equals(fpClassName, ignoreCase = true)) {
                        val fpClass = FileProvider::class.java
                        try {
                            val getPathStrategy = fpClass.getDeclaredMethod("getPathStrategy", Context::class.java, String::class.java)
                            getPathStrategy.isAccessible = true
                            val strategy = getPathStrategy.invoke(null, context, uri.authority)
                            if (strategy != null) {
                                val strategyClass = Class.forName("${FileProvider::class.java.name}\$PathStrategy")
                                val getFileForUri = strategyClass.getDeclaredMethod("getFileForUri", Uri::class.java)
                                getFileForUri.isAccessible = true
                                val file = getFileForUri.invoke(strategy, uri)
                                if (file is File) {
                                    return file.absolutePath
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to get FP Uri path", e)
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "getFPUriToPath failed", e)
            null
        }
    }

    private object ContentUrisCompat {
        fun withAppendedId(baseUri: Uri, id: Long): Uri? {
            return try {
                android.content.ContentUris.withAppendedId(baseUri, id)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getVideoInfo(uri: Uri, context: Context): Map<String, Any> {
        val info = mutableMapOf<String, Any>()
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val durationIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                    val widthIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.WIDTH)
                    val heightIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.HEIGHT)
                    val sizeIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE)

                    if (durationIndex >= 0) info["duration"] = cursor.getLong(durationIndex)
                    if (widthIndex >= 0) info["width"] = cursor.getInt(widthIndex)
                    if (heightIndex >= 0) info["height"] = cursor.getInt(heightIndex)
                    if (sizeIndex >= 0) info["size"] = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video info", e)
        }
        return info
    }
}
