package com.matuncnn.app.util

import android.util.LruCache

object UpscaleCache {
    private const val MAX_ENTRIES = 20

    private val cache = object : LruCache<String, String>(MAX_ENTRIES) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: String,
            newValue: String?
        ) {
            // Remove cached output file when evicted
            if (evicted) {
                java.io.File(oldValue).delete()
            }
        }
    }

    fun buildKey(inputUri: String, command: String, scale: Int): String {
        return "${inputUri}|${command}|${scale}"
    }

    fun get(key: String): String? {
        val path = cache.get(key)
        if (path != null && java.io.File(path).exists()) {
            return path
        }
        if (path != null) {
            cache.remove(key)
        }
        return null
    }

    fun put(key: String, outputPath: String) {
        cache.put(key, outputPath)
    }

    fun clear() {
        cache.evictAll()
    }
}
