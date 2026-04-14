package com.tlcsdm.fxtemplate.config;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the I18N utility class.
 */
class I18NTest {

    @Test
    void testGetBundle() {
        ResourceBundle bundle = I18N.getBundle();
        assertNotNull(bundle, "Resource bundle should not be null");
    }

    @Test
    void testGetExistingKey() {
        String value = I18N.get("app.title");
        assertNotNull(value, "Value for existing key should not be null");
        assertFalse(value.isEmpty(), "Value for existing key should not be empty");
    }

    @Test
    void testGetMissingKey() {
        String key = "non.existent.key";
        String value = I18N.get(key);
        assertEquals(key, value, "Missing key should return the key itself");
    }

    @Test
    void testGetSupportedLocales() {
        Locale[] locales = I18N.getSupportedLocales();
        assertNotNull(locales, "Supported locales should not be null");
        assertEquals(3, locales.length, "Should support 3 locales");
    }

    @Test
    void testGetCurrentLocale() {
        Locale locale = I18N.getCurrentLocale();
        assertNotNull(locale, "Current locale should not be null");
    }

    @Test
    void testGetDisplayName() {
        String name = I18N.getDisplayName(Locale.ENGLISH);
        assertNotNull(name, "Display name should not be null");
        assertFalse(name.isEmpty(), "Display name should not be empty");
    }

    @Test
    void testGetWindowControlI18nKeys() {
        assertFalse(I18N.get("window.minimize").isEmpty(), "window.minimize should not be empty");
        assertFalse(I18N.get("window.maximize").isEmpty(), "window.maximize should not be empty");
        assertFalse(I18N.get("window.close").isEmpty(), "window.close should not be empty");
    }
}
