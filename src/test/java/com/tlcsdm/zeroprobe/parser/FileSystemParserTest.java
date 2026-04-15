package com.tlcsdm.zeroprobe.parser;

import com.tlcsdm.zeroprobe.model.FileEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileSystemParser.
 */
class FileSystemParserTest {

    private final FileSystemParser parser = new FileSystemParser();

    @Test
    void testParseLsOutput() {
        String output = """
            total 68
            drwxr-xr-x  20 root root  4096 Jan  5 12:00 .
            drwxr-xr-x  20 root root  4096 Jan  5 12:00 ..
            drwxr-xr-x   2 root root  4096 Jan  3 10:30 bin
            drwxr-xr-x   4 root root  4096 Jan  3 10:30 boot
            -rw-r--r--   1 root root  1234 Jan  4 08:15 test.txt
            lrwxrwxrwx   1 root root     7 Jan  3 10:30 lib -> usr/lib
            """;

        List<FileEntry> entries = parser.parseLsOutput(output, "/");

        assertEquals(4, entries.size());

        // bin directory
        FileEntry bin = entries.get(0);
        assertEquals("bin", bin.getName());
        assertEquals("/bin", bin.getPath());
        assertTrue(bin.isDirectory());
        assertEquals(4096, bin.getSize());
        assertEquals("drwxr-xr-x", bin.getPermissions());
        assertEquals("root", bin.getOwner());
        assertEquals("root", bin.getGroup());

        // test.txt file
        FileEntry testFile = entries.get(2);
        assertEquals("test.txt", testFile.getName());
        assertEquals("/test.txt", testFile.getPath());
        assertFalse(testFile.isDirectory());
        assertEquals(1234, testFile.getSize());
        assertEquals("-rw-r--r--", testFile.getPermissions());

        // symlink: name should strip the " -> target" part
        FileEntry lib = entries.get(3);
        assertEquals("lib", lib.getName());
    }

    @Test
    void testParseLsOutputSubdirectory() {
        String output = """
            total 8
            drwxr-xr-x  2 root root 4096 Jan  3 10:30 subdir
            -rw-r--r--  1 root root  256 Jan  4 08:15 readme.md
            """;

        List<FileEntry> entries = parser.parseLsOutput(output, "/home/user");

        assertEquals(2, entries.size());
        assertEquals("/home/user/subdir", entries.get(0).getPath());
        assertEquals("/home/user/readme.md", entries.get(1).getPath());
    }

    @Test
    void testParseLsOutputEmpty() {
        List<FileEntry> entries = parser.parseLsOutput("", "/");
        assertTrue(entries.isEmpty());

        entries = parser.parseLsOutput(null, "/");
        assertTrue(entries.isEmpty());
    }

    @Test
    void testParseLsOutputOnlyTotal() {
        List<FileEntry> entries = parser.parseLsOutput("total 0\n", "/");
        assertTrue(entries.isEmpty());
    }

    @Test
    void testParseLsOutputTrailingSlash() {
        String output = "-rw-r--r--  1 user group 100 Mar 10 2024 data.csv\n";
        List<FileEntry> entries = parser.parseLsOutput(output, "/tmp/");
        assertEquals(1, entries.size());
        assertEquals("/tmp/data.csv", entries.get(0).getPath());
    }

    @Test
    void testParseLineInvalid() {
        assertNull(parser.parseLine("short line", "/"));
        assertNull(parser.parseLine("", "/"));
    }
}
