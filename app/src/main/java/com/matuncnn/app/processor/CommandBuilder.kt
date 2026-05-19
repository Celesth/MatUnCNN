package com.matuncnn.app.processor

class CommandBuilder {
    private val commandParts = mutableListOf<String>()

    fun append(part: String): CommandBuilder {
        if (part.isNotBlank()) {
            commandParts.add(part)
        }
        return this
    }

    fun append(key: String, value: String): CommandBuilder {
        if (key.isNotBlank() && value.isNotBlank()) {
            commandParts.add(key)
            commandParts.add(value)
        }
        return this
    }

    fun append(key: String, value: Int): CommandBuilder {
        if (key.isNotBlank()) {
            commandParts.add(key)
            commandParts.add(value.toString())
        }
        return this
    }

    fun appendIf(condition: Boolean, part: String): CommandBuilder {
        if (condition) append(part)
        return this
    }

    fun appendIf(condition: Boolean, key: String, value: String): CommandBuilder {
        if (condition) append(key, value)
        return this
    }

    fun build(): String {
        return commandParts.joinToString(" ")
    }

    fun buildArray(): Array<String> {
        return commandParts.toTypedArray()
    }

    fun clear() {
        commandParts.clear()
    }
}
