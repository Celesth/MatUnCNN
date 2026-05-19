package com.matuncnn.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.text.format.Formatter

object DeviceInfo {
    fun getInfo(context: Context): String {
        val mActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        mActivityManager.getMemoryInfo(memoryInfo)
        val memSize = memoryInfo.totalMem
        val availMemStr = Formatter.formatFileSize(context, memSize)

        return "Model:\t${Build.MODEL}, " +
                "System:\t${Build.VERSION.RELEASE}, " +
                "CPU:\t${Build.HARDWARE}, " +
                "RAM:\t$availMemStr"
    }

    fun getConfigStr(cpu: Boolean, tile: Int): String {
        val str = if (cpu) "CPU" else "GPU"
        return if (tile > 0) "$str, tile=$tile" else str
    }
}
