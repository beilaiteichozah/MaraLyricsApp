package com.maralyrics.laitei.utils

import android.os.Environment
import android.os.StatFs

object StorageUtils {
    /**
     * Checks if the device storage is critically low.
     * @param threshold The threshold of used space ratio (default 0.9 for 90%)
     * @return True if used space is above the threshold
     */
    fun isStorageLow(threshold: Float = 0.9f): Boolean {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalSpace = totalBlocks * blockSize
            val availableSpace = availableBlocks * blockSize
            
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
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalSpace = totalBlocks * blockSize
            val availableSpace = availableBlocks * blockSize
            
            if (totalSpace <= 0) return 0
            
            val usedSpace = totalSpace - availableSpace
            ((usedSpace.toFloat() / totalSpace.toFloat()) * 100).toInt()
        } catch (e: Exception) {
            0
        }
    }
}
