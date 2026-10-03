package com.ai.assistance.operit.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStorageUsageTest {

    @Test
    fun dirSizeSumsRecursively() {
        val root = createTempDir("storage").apply {
            File(this, "a.bin").writeBytes(ByteArray(100))
            File(this, "sub").mkdir()
            File(this, "sub/b.bin").writeBytes(ByteArray(50))
        }
        try {
            assertEquals(150L, AppStorageUsage.dirSize(root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun dirSizeOfMissingIsZero() {
        assertEquals(0L, AppStorageUsage.dirSize(null))
        assertEquals(0L, AppStorageUsage.dirSize(File("/no/such/dir/anywhere")))
    }

    @Test
    fun breakdownTotalsAddUp() {
        val b = AppStorageBreakdown(
            apkBytes = 300,
            appDataBytes = 100,
            sdCardBytes = 50,
            internalFreeBytes = 1000,
            sdCardFreeBytes = 2000,
            sdCardTotalBytes = 4000,
        )
        assertEquals(400L, b.internalBytes)
        assertEquals(450L, b.totalBytes)
    }
}
