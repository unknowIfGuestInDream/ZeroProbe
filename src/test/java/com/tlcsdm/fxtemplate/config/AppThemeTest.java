package com.tlcsdm.fxtemplate.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the AppTheme enum.
 */
class AppThemeTest {

    @Test
    void testGetSavedThemeDefault() {
        AppTheme theme = AppTheme.getSavedTheme();
        assertNotNull(theme, "Saved theme should not be null");
    }

    @Test
    void testGetDisplayNameKey() {
        AppTheme theme = AppTheme.ATLANTAFX_PRIMER_LIGHT;
        String key = theme.getDisplayNameKey();
        assertNotNull(key, "Display name key should not be null");
        assertEquals("settings.theme.atlantafx.primerLight", key);
    }

    @Test
    void testGetDisplayName() {
        AppTheme theme = AppTheme.ATLANTAFX_PRIMER_LIGHT;
        String name = theme.getDisplayName();
        assertNotNull(name, "Display name should not be null");
        assertFalse(name.isEmpty(), "Display name should not be empty");
    }

    @Test
    void testAllThemesHaveDisplayNameKey() {
        for (AppTheme theme : AppTheme.values()) {
            assertNotNull(theme.getDisplayNameKey(), "Theme " + theme.name() + " should have a display name key");
            assertFalse(theme.getDisplayNameKey().isEmpty(), "Theme " + theme.name() + " display name key should not be empty");
        }
    }

    @Test
    void testToString() {
        for (AppTheme theme : AppTheme.values()) {
            String str = theme.toString();
            assertNotNull(str, "toString() should not return null for " + theme.name());
            assertFalse(str.isEmpty(), "toString() should not return empty for " + theme.name());
        }
    }
}
