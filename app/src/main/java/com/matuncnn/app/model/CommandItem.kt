package com.matuncnn.app.model

data class CommandItem(
    val command: String,
    val defaultLabel: String,
    val customLabel: String = "",
    val programType: String = ""
) {
    val displayLabel: String
        get() = if (customLabel.isNotBlank()) customLabel else defaultLabel

    val fingerprint: String
        get() = command.replace(Regex("\\s+-i\\s+\\S+\\s+-o\\s+\\S+"), "").trim()
}

data class LabelItem(
    val command: String,
    val fingerprint: String,
    val defaultLabel: String,
    val customLabel: String = ""
)

enum class ProgramType(val id: String) {
    REALSR("realsr"),
    SRMD("srmd"),
    WAIFU2X("waifu2x"),
    REALCUGAN("realcugan"),
    MNNSR("mnnsr"),
    RESIZE("resize"),
    MAGICK("magick"),
    ANIME4K("anime4k");

    companion object {
        fun fromCommand(command: String): ProgramType? {
            val cmd = command.trim().lowercase()
            return when {
                cmd.startsWith("./realsr-ncnn") -> REALSR
                cmd.startsWith("./srmd-ncnn") -> SRMD
                cmd.startsWith("./waifu2x-ncnn") -> WAIFU2X
                cmd.startsWith("./realcugan-ncnn") -> REALCUGAN
                cmd.startsWith("./mnnsr-ncnn") -> MNNSR
                cmd.startsWith("./resize-ncnn") -> RESIZE
                cmd.startsWith("./magick ") -> MAGICK
                cmd.startsWith("./anime4k") || cmd.startsWith("anime4k") -> ANIME4K
                else -> null
            }
        }
    }
}

data class ProcessingResult(
    val fullLog: String,
    val success: Boolean,
    val elapsedSeconds: Float = 0f
)
