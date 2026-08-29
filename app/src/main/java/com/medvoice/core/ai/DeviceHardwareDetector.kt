package com.medvoice.core.ai

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.util.Locale

enum class OnDeviceEligibilityStatus {
    FULLY_ELIGIBLE,
    WARNING_THERMAL_OR_BATTERY,
    INELIGIBLE_INSUFFICIENT_RAM,
    INELIGIBLE_32BIT_OR_LOW_CORE
}

data class HardwareEligibilityReport(
    val deviceModel: String,
    val manufacturer: String,
    val socHardware: String,
    val totalRamMb: Long,
    val totalRamGb: Double,
    val availableRamMb: Long,
    val availableRamGb: Double,
    val cpuCores: Int,
    val is64Bit: Boolean,
    val primaryAbi: String,
    val batteryPct: Int,
    val batteryTempCelsius: Float,
    val isRamEligible: Boolean,
    val isCpuEligible: Boolean,
    val isThermalSafe: Boolean,
    val isModelBinaryPresent: Boolean,
    val eligibilityStatus: OnDeviceEligibilityStatus,
    val summaryReason: String
)

object DeviceHardwareDetector {

    private const val MIN_TOTAL_RAM_MB = 5500L // 6.0 GB RAM threshold (~5.5GB reporting after OS reservation)
    private const val MIN_FREE_RAM_MB = 1000L  // Minimum 1.0 GB free memory required
    private const val MIN_CPU_CORES = 6        // Minimum 6 cores (Octa-core standard)
    private const val MAX_BATTERY_TEMP_C = 42.0f // 42°C max safe battery temp
    private const val MIN_BATTERY_PCT = 15      // 15% min battery for tensor workloads

    fun evaluateHardware(context: Context): HardwareEligibilityReport {
        // 1. RAM Specs via ActivityManager
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availableRamMb = memInfo.availMem / (1024 * 1024)
        val totalRamGb = String.format(Locale.US, "%.1f", totalRamMb / 1024.0).toDoubleOrNull() ?: (totalRamMb / 1024.0)
        val availableRamGb = String.format(Locale.US, "%.1f", availableRamMb / 1024.0).toDoubleOrNull() ?: (availableRamMb / 1024.0)

        // 2. CPU Architecture & Cores
        val is64Bit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Process.is64Bit()
        } else {
            Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        }
        val primaryAbi = if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0] else "unknown"
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // 3. Battery & Thermal Status
        val batteryStatus = getBatteryInfo(context)
        val batteryPct = batteryStatus.first
        val batteryTempCelsius = batteryStatus.second

        // 4. Model Binary Check
        val isModelBinaryPresent = checkModelBinaryPresence(context)

        // 5. Individual Gate Evaluations
        val isRamEligible = totalRamMb >= MIN_TOTAL_RAM_MB && availableRamMb >= MIN_FREE_RAM_MB
        val isCpuEligible = is64Bit && cpuCores >= MIN_CPU_CORES
        val isThermalSafe = batteryTempCelsius <= MAX_BATTERY_TEMP_C && batteryPct >= MIN_BATTERY_PCT

        // 6. Strict Eligibility Determination
        val (eligibilityStatus, summaryReason) = when {
            !is64Bit || cpuCores < MIN_CPU_CORES -> {
                Pair(
                    OnDeviceEligibilityStatus.INELIGIBLE_32BIT_OR_LOW_CORE,
                    "32-bit architecture or insufficient CPU cores (<$MIN_CPU_CORES cores). Cannot run tensor kernels."
                )
            }
            totalRamMb < MIN_TOTAL_RAM_MB -> {
                Pair(
                    OnDeviceEligibilityStatus.INELIGIBLE_INSUFFICIENT_RAM,
                    "Total RAM is ${totalRamGb}GB. Strict requirement is 6.0GB+ RAM to prevent system crashes."
                )
            }
            !isThermalSafe -> {
                Pair(
                    OnDeviceEligibilityStatus.WARNING_THERMAL_OR_BATTERY,
                    "Device thermal state is elevated (${batteryTempCelsius}°C) or battery is low ($batteryPct%)."
                )
            }
            else -> {
                Pair(
                    OnDeviceEligibilityStatus.FULLY_ELIGIBLE,
                    "Hardware meets strict criteria: 64-bit Octa-Core, ${totalRamGb}GB RAM, safe thermal headroom."
                )
            }
        }

        val deviceModel = Build.MODEL ?: "Android Device"
        val manufacturer = Build.MANUFACTURER ?: "Unknown"
        val socHardware = Build.HARDWARE ?: "ARM SoC"

        Log.d(
            "DeviceHardwareDetector",
            "Hardware Audit: model=$deviceModel, RAM=${totalRamGb}GB, free=${availableRamGb}GB, cores=$cpuCores, 64bit=$is64Bit, temp=${batteryTempCelsius}°C, status=$eligibilityStatus"
        )

        return HardwareEligibilityReport(
            deviceModel = deviceModel,
            manufacturer = manufacturer,
            socHardware = socHardware,
            totalRamMb = totalRamMb,
            totalRamGb = totalRamGb,
            availableRamMb = availableRamMb,
            availableRamGb = availableRamGb,
            cpuCores = cpuCores,
            is64Bit = is64Bit,
            primaryAbi = primaryAbi,
            batteryPct = batteryPct,
            batteryTempCelsius = batteryTempCelsius,
            isRamEligible = isRamEligible,
            isCpuEligible = isCpuEligible,
            isThermalSafe = isThermalSafe,
            isModelBinaryPresent = isModelBinaryPresent,
            eligibilityStatus = eligibilityStatus,
            summaryReason = summaryReason
        )
    }

    private fun getBatteryInfo(context: Context): Pair<Int, Float> {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = context.registerReceiver(null, filter)
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0

            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 80
            val tempC = if (temp > 0) temp / 10.0f else 32.0f
            Pair(pct, tempC)
        } catch (e: Exception) {
            Pair(80, 32.0f)
        }
    }

    private fun checkModelBinaryPresence(context: Context): Boolean {
        return try {
            val internalFile = File(context.filesDir, "models/qwen2.5_1.5b_int4.bin")
            if (internalFile.exists() && internalFile.length() > 10_000_000L) return true

            val assetList = context.assets.list("models") ?: emptyArray()
            assetList.any { it.endsWith(".bin") || it.endsWith(".tflite") || it.endsWith(".onnx") }
        } catch (e: Exception) {
            false
        }
    }
}
