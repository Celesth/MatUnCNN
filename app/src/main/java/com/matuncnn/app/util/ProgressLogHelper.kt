package com.matuncnn.app.util

class ProgressLogHelper {
    private val logBuilder = StringBuilder()
    private var lastProgressLine = ""
    private var startTime = 0L

    fun reset() {
        logBuilder.clear()
        lastProgressLine = ""
        startTime = System.currentTimeMillis()
    }

    fun appendLine(line: String?) {
        if (line.isNullOrEmpty()) return
        if (isProgressLine(line)) {
            lastProgressLine = line
        } else {
            logBuilder.appendLine(line)
            lastProgressLine = ""
        }
    }

    val displayText: String
        get() = if (lastProgressLine.isEmpty()) logBuilder.toString()
        else logBuilder.toString() + lastProgressLine

    val fullLog: String
        get() = logBuilder.toString()

    val progressText: String
        get() {
            if (lastProgressLine.isNotEmpty()) {
                return lastProgressLine.trim().split("\\s".toRegex()).firstOrNull() ?: ""
            }
            return ""
        }

    val hasProgress: Boolean
        get() = lastProgressLine.isNotEmpty()

    val elapsedTimeSeconds: Float
        get() = (System.currentTimeMillis() - startTime) / 1000f

    fun getCompletionSummary(success: Boolean, modelName: String? = null, isNcnnCommand: Boolean = false): String {
        val summary = StringBuilder()

        summary.append(
            if (!success) "\nfail, use ${elapsedTimeSeconds} second"
            else "\nfinish, use ${elapsedTimeSeconds} second"
        )

        if (isNcnnCommand && !modelName.isNullOrEmpty()) {
            summary.append(", $modelName")
        }

        summary.appendLine()
        return summary.toString()
    }

    companion object {
        private val PROGRESS_REGEX = Regex("\\s*\\d([0-9.]*)%(\\s.+)?")

        fun isProgressLine(line: String?): Boolean {
            return line != null && PROGRESS_REGEX.matches(line)
        }
    }
}
