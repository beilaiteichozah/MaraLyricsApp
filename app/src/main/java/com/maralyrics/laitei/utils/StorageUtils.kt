package com.maralyrics.laitei.utils

import android.os.Environment
import android.os.StatFs
import kotlin.math.ceil

/**
 * Utility class for storage-related operations.
 */
object StorageUtils {
    private const val MB = 1024L * 1024L
    
    /** Minimum free space to maintain after download/extraction (500MB). */
    const val MIN_FREE_SPACE = 500 * MB
    
    /** Threshold for unknown download sizes (1GB). */
    const val UNKNOWN_SIZE_THRESHOLD = 1024 * MB
    
    /** Percentage threshold to trigger a storage warning (90%). */
    const val STORAGE_WARNING_PERCENT = 90

    /**
     * Data class containing storage information for validation.
     */
    data class SpaceInfo(
        val availableBytes: Long,
        val requiredBytes: Long,
        val usedPercentage: Int,
        val isLowStorageWarning: Boolean,
        val isEnoughSpace: Boolean
    ) {
        /**
         * The amount of additional space needed, if any.
         */
        val additionalNeededBytes: Long = maxOf(requiredBytes - availableBytes, 0L)
    }

    /**
     * Enum representing the stage of storage validation.
     */
    enum class StorageValidationStage {
        PRE_DOWNLOAD,
        PRE_EXTRACTION
    }

    /**
     * Calculates the minimum free space required before downloading.
     *
     * Includes:
     * - download payload
     * - extracted database size
     * - 25% working margin
     * - 500 MB reserved free space
     */
    fun calculateRequiredSpace(downloadSize: Long, extractionSize: Long): Long {
        if (downloadSize <= 0 && extractionSize <= 0) return 0L
        
        val base = downloadSize + extractionSize
        val safetyMargin = ceil(base * 0.25).toLong().coerceAtLeast(100 * MB)
        return base + safetyMargin + MIN_FREE_SPACE
    }

    /**
     * Returns the current storage information.
     * @param requiredBytes The amount of space required in bytes.
     * @return A [SpaceInfo] object containing the current storage status.
     */
    fun getSpaceInfo(requiredBytes: Long): SpaceInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalSpace = totalBlocks * blockSize
            val availableSpace = availableBlocks * blockSize
            
            val usedSpace = totalSpace - availableSpace
            val usedPercentage = if (totalSpace > 0) {
                ((usedSpace.toDouble() / totalSpace.toDouble()) * 100).toInt()
            } else {
                0
            }
            
            val isLowStorageWarning = usedPercentage >= STORAGE_WARNING_PERCENT
            val isEnoughSpace = availableSpace >= requiredBytes
            
            SpaceInfo(
                availableBytes = availableSpace,
                requiredBytes = requiredBytes,
                usedPercentage = usedPercentage,
                isLowStorageWarning = isLowStorageWarning,
                isEnoughSpace = isEnoughSpace
            )
        } catch (e: Exception) {
            SpaceInfo(0, requiredBytes, 0, false, false)
        }
    }

    /**
     * Checks if the device storage is critically low based on a percentage threshold.
     * Deprecated: Use [getSpaceInfo] for more accurate validation based on actual space needs.
     */
    @Deprecated("Use getSpaceInfo for more accurate validation", ReplaceWith("getSpaceInfo(requiredBytes).isLowStorageWarning"))
    fun isStorageLow(threshold: Float = 0.9f): Boolean {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val totalSpace = stat.blockCountLong * stat.blockSizeLong
            val availableSpace = stat.availableBlocksLong * stat.blockSizeLong
            
            if (totalSpace <= 0) return false
            
            val usedSpace = totalSpace - availableSpace
            val usedRatio = usedSpace.toFloat() / totalSpace.toFloat()
            
            usedRatio >= threshold
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns the used storage percentage.
     */
    fun getUsedStoragePercentage(): Int {
        return getSpaceInfo(0).usedPercentage
    }

    /**
     * Returns the available storage in bytes.
     */
    fun getAvailableSpaceBytes(): Long {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Formats bytes to a human-readable string (GB/MB).
     */
    fun formatSize(bytes: Long): String {
        val kb = 1024L
        val mb = kb * 1024L
        val gb = mb * 1024L

        return when {
            bytes >= gb -> String.format(java.util.Locale.US, "%.2f GB", bytes.toDouble() / gb)
            bytes >= mb -> String.format(java.util.Locale.US, "%.2f MB", bytes.toDouble() / mb)
            bytes >= kb -> String.format(java.util.Locale.US, "%.2f KB", bytes.toDouble() / kb)
            else -> "$bytes Bytes"
        }
    }
}
