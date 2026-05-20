package com.matuncnn.app.util

import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private val entries = mutableStateListOf<String>()
    val logs: List<String> get() = entries

    fun log(tag: String, msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        entries.add("[$ts][$tag] $msg")
        if (entries.size > 500) entries.removeAt(0)
    }

    fun dump() = entries.joinToString("\n")

    fun saveTo(baseDir: File): File? {
        val dir = File(baseDir, "MatUnCNN")
        dir.mkdirs()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "debug_$ts.txt")
        return try {
            file.writeText(dump())
            file
        } catch (_: Exception) { null }
    }
}
