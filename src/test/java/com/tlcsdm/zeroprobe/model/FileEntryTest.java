package com.tlcsdm.zeroprobe.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileEntry.
 */
class FileEntryTest {

    @Test
    void testGetFormattedSizeBytes() {
        FileEntry entry = new FileEntry("f.txt", "/f.txt", false, 512, "-rw-r--r--", "root", "root", "Jan 1 00:00");
        assertEquals("512 B", entry.getFormattedSize());
    }

    @Test
    void testGetFormattedSizeKilobytes() {
        FileEntry entry = new FileEntry("f.txt", "/f.txt", false, 2048, "-rw-r--r--", "root", "root", "Jan 1 00:00");
        assertEquals("2.0 KB", entry.getFormattedSize());
    }

    @Test
    void testGetFormattedSizeMegabytes() {
        FileEntry entry = new FileEntry("f.bin", "/f.bin", false, 5 * 1024 * 1024, "-rw-r--r--", "root", "root", "Jan 1 00:00");
        assertEquals("5.0 MB", entry.getFormattedSize());
    }

    @Test
    void testGetFormattedSizeGigabytes() {
        FileEntry entry = new FileEntry("f.img", "/f.img", false, 2L * 1024 * 1024 * 1024, "-rw-r--r--", "root", "root", "Jan 1 00:00");
        assertEquals("2.0 GB", entry.getFormattedSize());
    }

    @Test
    void testGetFormattedSizeDirectory() {
        FileEntry entry = new FileEntry("dir", "/dir", true, 4096, "drwxr-xr-x", "root", "root", "Jan 1 00:00");
        assertEquals("-", entry.getFormattedSize());
    }

    @Test
    void testDirectoryFlag() {
        FileEntry dir = new FileEntry("bin", "/bin", true, 4096, "drwxr-xr-x", "root", "root", "Jan 1 00:00");
        assertTrue(dir.isDirectory());

        FileEntry file = new FileEntry("a.txt", "/a.txt", false, 100, "-rw-r--r--", "user", "user", "Jan 2 10:00");
        assertFalse(file.isDirectory());
    }

    @Test
    void testToString() {
        FileEntry entry = new FileEntry("myfile.txt", "/myfile.txt", false, 100, "-rw-r--r--", "root", "root", "Jan 1 00:00");
        assertEquals("myfile.txt", entry.toString());
    }
}
