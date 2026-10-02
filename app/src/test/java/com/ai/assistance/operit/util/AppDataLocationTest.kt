package com.ai.assistance.operit.util

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The rule that decides what "the memory card" is. Getting it wrong either
 * offers internal storage under the card's name or hides a mounted card, and
 * both fail silently on the phone, so the rule is pinned here instead.
 */
class AppDataLocationTest {

    private val removable = { file: File -> file.name.startsWith("sd") }
    private fun dir(name: String): File = Files.createTempDirectory(name).toFile().also {
        it.deleteOnExit()
    }

    @Test
    fun noVolumesMeansNoCard() {
        assertNull(AppDataLocation.pickRemovable(null, removable))
        assertNull(AppDataLocation.pickRemovable(emptyArray(), removable))
    }

    @Test
    fun primaryVolumeAloneMeansNoCard() {
        assertNull(AppDataLocation.pickRemovable(arrayOf(dir("emulated")), removable))
    }

    @Test
    fun secondRemovableVolumeIsTheCard() {
        val primary = dir("emulated")
        val card = dir("sdcard1")
        assertEquals(card, AppDataLocation.pickRemovable(arrayOf(primary, card), removable))
    }

    @Test
    fun primaryIsNeverTheCardEvenWhenRemovable() {
        // Some devices report the primary volume removable; it still must not
        // be offered as the memory card.
        val primary = dir("sdprimary")
        assertNull(AppDataLocation.pickRemovable(arrayOf(primary), removable))
    }

    @Test
    fun nonRemovableSecondVolumeIsSkipped() {
        val primary = dir("emulated")
        val other = dir("usb")
        assertNull(AppDataLocation.pickRemovable(arrayOf(primary, other), removable))
    }

    @Test
    fun laterRemovableVolumeIsFound() {
        val primary = dir("emulated")
        val other = dir("usb")
        val card = dir("sdcard1")
        assertEquals(
            card,
            AppDataLocation.pickRemovable(arrayOf(primary, other, card), removable),
        )
    }

    @Test
    fun nullAndMissingEntriesAreSkipped() {
        val primary = dir("emulated")
        val card = dir("sdcard1")
        assertEquals(
            card,
            AppDataLocation.pickRemovable(
                arrayOf(primary, null, File("/nonexistent/operit-test"), card),
                removable,
            ),
        )
    }

    @Test
    fun throwingProbeDoesNotKillTheSearch() {
        val primary = dir("emulated")
        val card = dir("sdcard1")
        val flaky: (File) -> Boolean = { throw RuntimeException("no permission") }
        // Nothing selectable, but no exception either.
        assertNull(AppDataLocation.pickRemovable(arrayOf(primary, card), flaky))
    }
}
