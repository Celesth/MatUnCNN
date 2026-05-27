package com.matuncnn.app.util

import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray

object VulkanHelper {

    private var _isAdrenoChecked = false
    private var _isAdreno = false
    private var _nativeAvailable = false

    init {
        try {
            System.loadLibrary("matuncnn-native")
            _nativeAvailable = true
        } catch (_: UnsatisfiedLinkError) {
            _nativeAvailable = false
        }
    }

    /** JNI entry point — returns JSON array of GPU info */
    private external fun nativeProbeVulkan(): String

    fun hasVulkan(pm: PackageManager): Boolean {
        return pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
    }

    fun isAdrenoGpu(): Boolean {
        if (_isAdrenoChecked) return _isAdreno
        _isAdreno = if (_nativeAvailable) {
            probeWithNative()
        } else {
            detectAdrenoFallback()
        }
        _isAdrenoChecked = true
        return _isAdreno
    }

    private fun probeWithNative(): Boolean {
        return try {
            val json = nativeProbeVulkan()
            if (json == "[]") return detectAdrenoFallback()
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                // 0x5143 = Qualcomm/Adreno
                if (obj.optInt("vendorID", 0) == 0x5143) {
                    val name = obj.optString("name", "")
                    DebugLog.log("Vulkan", "Native probe: $name")
                    return true
                }
            }
            false
        } catch (e: Exception) {
            DebugLog.log("Vulkan", "Native probe error: ${e.message}")
            detectAdrenoFallback()
        }
    }

    private fun detectAdrenoFallback(): Boolean {
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
