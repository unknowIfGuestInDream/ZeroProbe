package com.tlcsdm.fxtemplate.model;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DisplayLocale class.
 */
class DisplayLocaleTest {

    @Test
    void testConstructor() {
        DisplayLocale dl = new DisplayLocale(Locale.ENGLISH);
        assertNotNull(dl.getLocale());
        assertEquals(Locale.ENGLISH, dl.getLocale());
    }

    @Test
    void testConstructorNullThrows() {
        assertThrows(NullPointerException.class, () -> new DisplayLocale(null));
    }

    @Test
    void testToStringEnglish() {
        DisplayLocale dl = new DisplayLocale(Locale.ENGLISH);
        assertEquals("English", dl.toString());
    }

    @Test
    void testToStringChinese() {
        DisplayLocale dl = new DisplayLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("\u4e2d\u6587", dl.toString());
    }

    @Test
    void testToStringJapanese() {
        DisplayLocale dl = new DisplayLocale(Locale.JAPANESE);
        assertEquals("\u65e5\u672c\u8a9e", dl.toString());
    }

    @Test
    void testEqualsSameLanguage() {
        DisplayLocale dl1 = new DisplayLocale(Locale.ENGLISH);
        DisplayLocale dl2 = new DisplayLocale(Locale.ENGLISH);
        assertEquals(dl1, dl2);
    }

    @Test
    void testEqualsDifferentLanguage() {
        DisplayLocale dl1 = new DisplayLocale(Locale.ENGLISH);
        DisplayLocale dl2 = new DisplayLocale(Locale.JAPANESE);
        assertNotEquals(dl1, dl2);
    }

    @Test
    void testHashCodeConsistency() {
        DisplayLocale dl1 = new DisplayLocale(Locale.ENGLISH);
        DisplayLocale dl2 = new DisplayLocale(Locale.ENGLISH);
        assertEquals(dl1.hashCode(), dl2.hashCode());
    }
}
