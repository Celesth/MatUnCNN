package com.matuncnn.app.processor

import android.util.Log
import com.matuncnn.app.model.CommandItem
import com.matuncnn.app.model.ProgramType
import org.json.JSONObject
import java.io.File
import java.util.Locale

class CommandListManager(
    presetLabels: Array<String>,
    extraPath: String,
    extraCommand: String,
    classicalFilters: Array<String>,
    magickFilters: Array<String>
) {
    val commandList: Array<String>
    val defaultLabels: Array<String>
    private val customLabelMap = mutableMapOf<String, String>()

    init {
        val extraCmdList = mutableListOf<String>()
        val extraCmdLabels = buildExtraCommands(extraPath, extraCommand, classicalFilters, magickFilters, extraCmdList)

        val l = COMMAND_0.size
        commandList = Array(extraCmdList.size + l) { i ->
            if (i < l) COMMAND_0[i] else extraCmdList[i - l]
        }

        defaultLabels = Array(extraCmdLabels.size + l) { i ->
            when {
                i < presetLabels.size.coerceAtMost(l) -> presetLabels[i]
                i < l -> commandFingerprint(COMMAND_0[i])
                else -> extraCmdLabels[i - l]
            }
        }
    }

    val commandCount: Int get() = commandList.size

    fun getCommandAt(index: Int): String {
        return if (index in commandList.indices) commandList[index] else ""
    }

    fun getCommandItems(useCustomLabel: Boolean): List<CommandItem> {
        val labels = getDisplayLabels(useCustomLabel)
        return commandList.mapIndexed { i, cmd ->
            CommandItem(
                command = cmd,
                defaultLabel = defaultLabels.getOrElse(i) { "" },
                customLabel = if (useCustomLabel) {
                    customLabelMap[commandFingerprint(cmd)] ?: ""
                } else "",
                programType = ProgramType.fromCommand(cmd)?.id ?: ""
            )
        }
    }

    fun getDisplayLabels(useCustomLabel: Boolean): Array<String> {
        if (!useCustomLabel || customLabelMap.isEmpty()) {
            return defaultLabels.copyOf()
        }
        return Array(commandList.size) { i ->
            val fp = commandFingerprint(commandList[i])
            customLabelMap[fp]?.takeIf { it.isNotBlank() } ?: defaultLabels.getOrElse(i) { "" }
        }
    }

    fun loadCustomLabels(json: String?) {
        customLabelMap.clear()
        if (json.isNullOrBlank()) return
        try {
            val obj = JSONObject(json)
            for (key in obj.keys()) {
                customLabelMap[key] = obj.getString(key)
            }
        } catch (e: Exception) {
            Log.e("CommandListManager", "Failed to parse customLabels JSON", e)
            customLabelMap.clear()
        }
    }

    fun setCustomLabelMap(map: Map<String, String>?) {
        customLabelMap.clear()
        if (map != null) customLabelMap.putAll(map)
    }

    fun getCustomLabelMap(): Map<String, String> = customLabelMap.toMap()

    fun toCustomLabelJson(): String {
        if (customLabelMap.isEmpty()) return ""
        return JSONObject(customLabelMap.toMap()).toString()
    }

    fun exportAllText(): String {
        return buildString {
            for (cmd in commandList) {
                val fp = commandFingerprint(cmd)
                append(fp)
                customLabelMap[fp]?.takeIf { it.isNotBlank() }?.let { append('\t').append(it) }
                appendLine()
            }
        }
    }

    fun importFromText(text: String?): Int {
        if (text.isNullOrBlank()) return 0
        var count = 0
        val fpIndexMap = commandList.map { commandFingerprint(it) }.toSet()
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val parts = trimmed.split("\t", limit = 2)
            val fp = parts[0].trim()
            if (fp in fpIndexMap) {
                if (parts.size > 1 && parts[1].trim().isNotEmpty()) {
                    customLabelMap[fp] = parts[1].trim()
                } else {
                    customLabelMap.remove(fp)
                }
                count++
            }
        }
        return count
    }

    companion object {
        val ALL_PROGRAMS = arrayOf(
            "realsr", "srmd", "waifu2x", "realcugan",
            "mnnsr", "resize", "magick", "anime4k"
        )

        val COMMAND_0 = arrayOf(
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGAN-anime",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGAN",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv3-general -s 4",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv3-anime -s 2",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv3-anime -s 3",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv3-anime -s 4",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv2-anime -s 2",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv2-anime -s 4",
            "./mnnsr-ncnn -i input.png -o output.png  -m models-MNN/ESRGAN-MoeSR-jp_Illustration-x4.mnn -s 4",
            "./mnnsr-ncnn -i input.png -o output.png  -m models-MNN/ESRGAN-MoeSR-jp_Illustration-x4.mnn -d 0 -s 4",
            "./realsr-ncnn -i input.png -o output.png  -m models-ESRGAN-Nomos8kSC -s 4",
            "./mnnsr-ncnn -i input.png -o output.png  -m models-MNN/ESRGAN-Nomos8kSC-x4.mnn -s 4",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGAN-SourceBook -s 2",
            "./realcugan-ncnn -i input.png -o output.png  -m models-nose -s 2  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n -1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n 1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n 2",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n 3",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 4  -n -1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 4  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 4  -n 3",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 2  -n -1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 2  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 2  -n 3",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 3  -n -1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 3  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 3  -n 3",
            "./Anime4k -i input.png -o output.png -z 2 -A",
            "./Anime4k -i input.png -o output.png -z 2 -A -a -e 48",
            "./Anime4k -i input.png -o output.png -z 2 -A -b -r 48",
            "./Anime4k -i input.png -o output.png -z 2 -A -w",
            "./Anime4k -i input.png -o output.png -z 2 -A -w -H",
            "./Anime4k -i input.png -o output.png -z 4 -A ",
            "./Anime4k -i input.png -o output.png -z 4 -A -a -e 40",
            "./Anime4k -i input.png -o output.png -z 4 -A -b -r 40",
            "./Anime4k -i input.png -o output.png -z 4 -A -w",
            "./Anime4k -i input.png -o output.png -z 4 -A -w -H",
        )

        fun supportsDirectoryMode(command: String?): Boolean {
            if (command.isNullOrEmpty()) return false
            val cmd = command.trim().lowercase()
            return cmd.startsWith("./realsr-ncnn") ||
                    cmd.startsWith("./srmd-ncnn") ||
                    cmd.startsWith("./waifu2x-ncnn") ||
                    cmd.startsWith("./realcugan-ncnn") ||
                    cmd.startsWith("./mnnsr-ncnn") ||
                    cmd.startsWith("./resize-ncnn") ||
                    cmd.startsWith("./anime4k")
        }

        fun commandFingerprint(cmd: String?): String {
            if (cmd == null) return ""
            return cmd.replace(Regex("\\s+-i\\s+\\S+\\s+-o\\s+\\S+"), "").trim()
        }

        fun getNameFromModelPath(path: String, type: String): Pair<String, String> {
            val scaleMatcher = Regex("([xX]\\d+|\\d+[xX])")
            var s = ""
            var name = ""
            val splitPath = path.split("[/\\\\]+".toRegex())

            if (splitPath.size > 1) {
                val last = splitPath.last()
                if (last.matches(Regex("$scaleMatcher\\..+"))) {
                    s = last.replace(scaleMatcher, "$1")
                    name = splitPath[splitPath.size - 2]
                } else {
                    name = last.replaceFirst(Regex("\\.(.{1,4})$"), "")
                    val scaleMatch = scaleMatcher.find(name)
                    if (scaleMatch != null) {
                        s = scaleMatch.value
                    } else {
                        val m = Regex("[-_.\\s]+")
                        val tags = name.split(m)
                        for (tag in tags) {
                            val match = scaleMatcher.find(tag)
                            if (match != null) {
                                s = match.value
                                break
                            }
                        }
                    }
                }
            }

            var scale = s.replaceFirst(Regex("[xX]"), "").toIntOrNull() ?: 1
            if (scale < 1) scale = 1
            if (!name.contains(s)) {
                name = "$name-x$scale"
            }
            name = name.replaceFirst(Regex("(models-|model-)"), "")
            if (type.isNotEmpty()) {
                name = "$type-$name"
            }
            return Pair(name, scale.toString())
        }

        private fun buildExtraCommands(
            extraPath: String,
            extraCommand: String,
            classicalFilters: Array<String>,
            magickFilters: Array<String>,
            cmdList: MutableList<String>
        ): MutableList<String> {
            val cmdLabel = mutableListOf<String>()

            val classicalResize = arrayOf("2", "4", "10")
            for (f in classicalFilters) {
                for (s in classicalResize) {
                    cmdList.add("./resize-ncnn -i input.png -o output.png  -m $f -s $s")
                    cmdLabel.add("Classical-$f-x$s")
                }
            }

            val magickResize = arrayOf("200%", "400%", "1000%")
            for (f in magickFilters) {
                for (s in magickResize) {
                    cmdList.add("./magick input.png -filter $f -resize $s output.png ")
                    cmdLabel.add("Magick-${f}-x${s.replaceFirst(Regex("(\\d+)00%"), "$1")}")
                }
            }

            if (extraPath.isNotBlank()) {
                val folders = File(extraPath).listFiles()
                if (folders != null) {
                    folders.sortBy { it.name }
                    for (folder in folders) {
                        val name = folder.name
                        when {
                            name.endsWith(".mnn") || name.startsWith("models-MNN") -> {
                                if (folder.isDirectory) {
                                    val files = folder.listFiles()
                                    if (files != null) {
                                        files.sortBy { it.name }
                                        for (file in files) {
                                            if (file.name.endsWith(".mnn")) {
                                                val (n, scale) = getNameFromModelPath(file.absolutePath, "MNNSR")
                                                cmdList.add("./mnnsr-ncnn -i input.png -o output.png  -m ${file.absolutePath} -s $scale")
                                                cmdLabel.add(n)
                                            }
                                        }
                                    }
                                } else {
                                    val (n, scale) = getNameFromModelPath(folder.absolutePath, "MNNSR")
                                    cmdList.add("./mnnsr-ncnn -i input.png -o output.png  -m ${folder.absolutePath} -s $scale")
                                    cmdLabel.add(n)
                                }
                            }
                            folder.isDirectory && name.startsWith("models") -> {
                                var model = name.replace("models-", "")
                                var scaleMatcher = ".*x(\\d+).*"
                                var noiseMatcher = ""
                                var command = "./realsr-ncnn -i input.png -o output.png  -m ${folder.absolutePath} -s "

                                when {
                                    name.matches(Regex("models-(cugan|cunet|upconv).*")) -> {
                                        val newModel = name.replace("models-", "Waifu2x-")
                                        scaleMatcher = ".*scale(\\d+).*"
                                        command = "./waifu2x-ncnn -i input.png -o output.png  -m ${folder.absolutePath} -s "
                                        noiseMatcher = "noise(\\d+).*"
                                        cmdLabel.addAll(genCmdFromModel(folder, scaleMatcher, noiseMatcher).map { s ->
                                            "$newModel-x${s.replace(" -n ", "-noise")}"
                                        })
                                        cmdList.addAll(genCmdFromModel(folder, scaleMatcher, noiseMatcher).map { s ->
                                            command + s
                                        })
                                        continue
                                    }
                                    name.matches(Regex("models-srmd.*")) -> {
                                        val newModel = if (name == "models-srmd") "SRMD"
                                        else name.replace("models-srmd", "SRMD-")
                                        command = "./srmd-ncnn -i input.png -o output.png  -m ${folder.absolutePath} -s "
                                        cmdLabel.addAll(genCmdFromModel(folder, scaleMatcher, noiseMatcher).map { s ->
                                            "$newModel-x${s.replace(" -n ", "-noise")}"
                                        })
                                        cmdList.addAll(genCmdFromModel(folder, scaleMatcher, noiseMatcher).map { s ->
                                            command + s
                                        })
                                        continue
                                    }
                                    name.startsWith("models-DF2K") -> {
                                        model = name.replace("models-", "RealSR-")
                                    }
                                    name.startsWith("models-mnn") -> {
                                        continue
                                    }
                                }

                                cmdLabel.addAll(genCmdFromModel(folder, scaleMatcher, noiseMatcher).map { s ->
                                    "$model-x${s.replace(" -n ", "-noise")}"
                                })
                                cmdList.addAll(genCmdFromModel(folder, scaleMatcher, noiseMatcher).map { s ->
                                    command + s
                                })
                            }
                        }
                    }
                }
            }

            if (extraCommand.isNotBlank()) {
                val cmds = extraCommand.split("\n".toRegex()).filter { it.isNotBlank() }
                cmdList.addAll(cmds)
                cmdLabel.addAll(cmds)
            }

            return cmdLabel
        }

        private fun genCmdFromModel(folder: File, scaleMatcher: String, noiseMatcher: String): List<String> {
            val list = mutableListOf<String>()
            val files = folder.listFiles() ?: return list
            val names = files.filter { it.name.lowercase(Locale.ROOT).endsWith("bin") }.map { it.name }.sorted()

            for (name in names) {
                val s = if (name.matches(Regex(scaleMatcher))) {
                    name.replaceFirst(Regex(scaleMatcher), "$1")
                } else "1"

                val suffix = if (noiseMatcher.isNotEmpty()) {
                    val noise = name.replaceFirst(Regex(noiseMatcher), "$1")
                    if (noise.matches(Regex("\\d+"))) {
                        val n = noise.toIntOrNull() ?: 0
                        "$s -n $n"
                    } else s
                } else s

                if (!list.contains(suffix)) list.add(suffix)
            }
            return list
        }
    }
}
