package com.medvoice.core.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build

object HardwareCapabilities {

    /**
     * Determines if the device has sufficient RAM and processing capability 
     * to run the 4B parameter MedGemma INT4 model on-device.
     */
    fun isLocalSlmCapable(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        
        // 4B models typically require at least ~5.5GB of total system RAM to run comfortably
        // without constant OOM crashing the OS.
        if (totalRamGb < 5.5) return false

        // Check for specific known low-end SoCs or specific constraints if needed
        val hardware = Build.HARDWARE.lowercase()
        val socModel = Build.SOC_MODEL.lowercase()

        // Exclude older budget chipsets explicitly if known to fail 
        // e.g. very old Exynos or MediaTek Helio G series
        if (hardware.contains("mt6769") || socModel.contains("mt6769")) return false

        return true
    }
}
