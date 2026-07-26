package eu.kanade.tachiyomi.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderColorInversionModeTest {
    @Test
    fun `off never compensates pages`() {
        assertFalse(ReaderColorInversionMode.shouldCompensate(ReaderColorInversionMode.OFF, false))
        assertFalse(ReaderColorInversionMode.shouldCompensate(ReaderColorInversionMode.OFF, true))
    }

    @Test
    fun `automatic follows the system inversion state`() {
        assertFalse(ReaderColorInversionMode.shouldCompensate(ReaderColorInversionMode.AUTOMATIC, false))
        assertTrue(ReaderColorInversionMode.shouldCompensate(ReaderColorInversionMode.AUTOMATIC, true))
    }

    @Test
    fun `always compensates even when system detection is unavailable`() {
        assertTrue(ReaderColorInversionMode.shouldCompensate(ReaderColorInversionMode.ALWAYS, false))
        assertTrue(ReaderColorInversionMode.shouldCompensate(ReaderColorInversionMode.ALWAYS, true))
    }
}
