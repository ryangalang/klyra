package com.music.bitchord.auth

import org.junit.Assert.*
import org.junit.Test

class SessionPreferencesTest {
    @Test fun ephemeralEditsPreservePreferencesSemantics() {
        val prefs = SessionPreferences()
        val original = mutableSetOf("one")
        prefs.edit().putString("cookie", "fixture").putStringSet("set", original).commit()
        original += "two"
        assertEquals(setOf("one"), prefs.getStringSet("set", null))
        prefs.getStringSet("set", null)!!.add("three")
        assertEquals(setOf("one"), prefs.getStringSet("set", null))
        prefs.edit().clear().putLong("position", 42).apply()
        assertFalse(prefs.contains("cookie"))
        assertEquals(42L, prefs.getLong("position", 0))
        prefs.edit().remove("position").commit()
        assertEquals(0L, prefs.getLong("position", 0))
        assertNull(SessionPreferences().getString("cookie", null))
    }
}
