package com.matuncnn.app.util

import java.io.File
import java.io.FileInputStream

object PreprocessToPng {
    private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val JPG = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    private val WEBP = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val BMP = byteArrayOf(0x42, 0x4D)
    private val HEIF = byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63, 0x00)
    private val GIF = byteArrayOf(0x47, 0x49, 0x46, 0x38)
    private val AVIF = byteArrayOf(0x61, 0x76, 0x69, 0x66)

    val suffix = arrayOf("png", "heif")

    fun isAVIF(i: Int) = i == 3
    fun isHeif(i: Int) = i == 1
    fun isGIF(i: Int) = i == 2

    fun match(filehead: ByteArray): Int {
        if (filehead.size < 10) return -1

        if (matchesMagic(filehead, PNG)) return -1
        if (matchesMagic(filehead, JPG)) return -1
        if (matchesMagic(filehead, WEBP)) return -1
        if (matchesMagic(filehead, BMP)) return -1
        if (matchesMagic(filehead, HEIF)) return 1
        if (matchesMagic(filehead, GIF)) return 2

        val avifHeader = filehead.drop(8).toByteArray()
        if (avifHeader.size >= AVIF.size && matchesMagic(avifHeader, AVIF)) return 3

        return 0
    }

    private fun matchesMagic(data: ByteArray, magic: ByteArray): Boolean {
        if (data.size < magic.size) return false
        for (i in magic.indices) {
            if (data[i] != magic[i]) return false
        }
        return true
    }

    fun needsConversion(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return true
            val fis = FileInputStream(file)
            val header = ByteArray(16)
            val read = fis.read(header)
            fis.close()
            if (read < 10) return true
            val result = match(header)
            result == 0 || result == 1 || result == 2 || result == 3
        } catch (e: Exception) {
            true
        }
    }
}
