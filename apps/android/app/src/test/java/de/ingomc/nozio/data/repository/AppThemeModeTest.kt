package de.ingomc.nozio.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeModeTest {

    @Test
    fun `fromStorageValue returns stored mode`() {
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStorageValue("light"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromStorageValue("dark"))
    }

    @Test
    fun `fromStorageValue falls back to system for unknown values`() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStorageValue(null))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStorageValue("something-else"))
    }
}
