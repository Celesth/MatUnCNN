package com.matuncnn.app.util

import android.content.pm.PackageManager

object VulkanHelper {
    fun hasVulkan(pm: PackageManager): Boolean {
        return pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
    }
}
