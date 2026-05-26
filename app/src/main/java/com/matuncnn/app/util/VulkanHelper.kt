package com.matuncnn.app.util

import android.content.pm.PackageManager
import android.os.Build

object VulkanHelper {

    private var _isAdrenoChecked = false
    private var _isAdreno = false

    fun hasVulkan(pm: PackageManager): Boolean {
        return pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
    }

    fun isAdrenoGpu(): Boolean {
        if (_isAdrenoChecked) return _isAdreno

        _isAdreno = detectAdreno()
        _isAdrenoChecked = true
        return _isAdreno
    }

    private fun detectAdreno(): Boolean {
        if (Build.VERSION.SDK_INT >= 31) {
            val soc = Build.SOC_MODEL
            if (soc != null && soc.contains("Snapdragon", ignoreCase = true)) return true
            if (soc != null && soc.contains("Qualcomm", ignoreCase = true)) return true
        }
        val hardware = Build.HARDWARE
        if (hardware.contains("qcom", ignoreCase = true)) return true
        if (hardware.contains("qualcomm", ignoreCase = true)) return true
        val board = Build.BOARD
        if (board.contains("qcom", ignoreCase = true)) return true
        return false
    }

    fun resetCache() {
        _isAdrenoChecked = false
        _isAdreno = false
    }
}
