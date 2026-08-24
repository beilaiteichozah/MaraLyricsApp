package com.maralyrics.laitei.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageUtilsTest {

    private val MB = 1024L * 1024L
    private val GB = 1024 * MB

    @Test
    fun `calculateRequiredSpace with zero sizes returns zero`() {
        val result = StorageUtils.calculateRequiredSpace(0, 0)
        assertEquals(0L, result)
    }

    @Test
    fun `calculateRequiredSpace with small sizes uses minimum 100MB margin`() {
        // base = 10MB + 10MB = 20MB
        // 25% of 20MB = 5MB, which is less than 100MB
        // so margin = 100MB
        // expected = 20MB + 100MB + 500MB = 620MB
        val result = StorageUtils.calculateRequiredSpace(10 * MB, 10 * MB)
        assertEquals(620 * MB, result)
    }

    @Test
    fun `calculateRequiredSpace with large sizes uses 25 percent margin`() {
        // base = 400MB + 400MB = 800MB
        // 25% of 800MB = 200MB, which is more than 100MB
        // so margin = 200MB
        // expected = 800MB + 200MB + 500MB = 1500MB
        val result = StorageUtils.calculateRequiredSpace(400 * MB, 400 * MB)
        assertEquals(1500 * MB, result)
    }

    @Test
    fun `calculateRequiredSpace with very large sizes`() {
        // base = 4GB + 4GB = 8GB
        // 25% of 8GB = 2GB
        // expected = 8GB + 2GB + 500MB = 10.5GB
        val result = StorageUtils.calculateRequiredSpace(4 * GB, 4 * GB)
        val expected = (10 * GB) + (512 * MB) // 10.5 GB
        assertEquals(expected, result)
    }
}
