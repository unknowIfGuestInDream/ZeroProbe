package com.tlcsdm.zeroprobe.parser;

import com.tlcsdm.zeroprobe.model.ProcessListInfo;
import com.tlcsdm.zeroprobe.model.ProcessListInfo.ProcessEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProcessListParser.
 */
class ProcessListParserTest {

    private final ProcessListParser parser = new ProcessListParser();

    @Test
    void testParseValidOutput() {
        String output = """
            1 init
            42 sshd
            100 bash
            """;
        ProcessListInfo info = parser.parse(output);
        assertNotNull(info);
        assertEquals(3, info.getProcessCount());
        assertEquals(1, info.getProcesses().get(0).pid());
        assertEquals("init", info.getProcesses().get(0).name());
        assertEquals(42, info.getProcesses().get(1).pid());
        assertEquals("sshd", info.getProcesses().get(1).name());
        assertEquals(100, info.getProcesses().get(2).pid());
        assertEquals("bash", info.getProcesses().get(2).name());
    }

    @Test
    void testParseWithHeaderLine() {
        String output = """
            PID COMMAND
            1 init
            42 sshd
            """;
        ProcessListInfo info = parser.parse(output);
        assertNotNull(info);
        assertEquals(2, info.getProcessCount());
    }

    @Test
    void testParseNullInput() {
        assertNull(parser.parse(null));
    }

    @Test
    void testParseEmptyInput() {
        assertNull(parser.parse(""));
        assertNull(parser.parse("   "));
    }

    @Test
    void testParseWithExtraWhitespace() {
        String output = "  1  init  \n  42  sshd  \n";
        ProcessListInfo info = parser.parse(output);
        assertNotNull(info);
        assertEquals(2, info.getProcessCount());
        assertEquals(1, info.getProcesses().get(0).pid());
        assertEquals("init", info.getProcesses().get(0).name());
    }

    @Test
    void testParseWithBlankLines() {
        String output = "1 init\n\n42 sshd\n\n";
        ProcessListInfo info = parser.parse(output);
        assertNotNull(info);
        assertEquals(2, info.getProcessCount());
    }

    @Test
    void testProcessEntryToString() {
        ProcessEntry entry = new ProcessEntry(42, "sshd");
        assertEquals("42 - sshd", entry.toString());
    }

    @Test
    void testProcessEntryComparable() {
        ProcessEntry a = new ProcessEntry(1, "init");
        ProcessEntry b = new ProcessEntry(42, "sshd");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(new ProcessEntry(1, "other")));
    }

    @Test
    void testTimestampIsSet() {
        String output = "1 init\n";
        ProcessListInfo info = parser.parse(output);
        assertNotNull(info);
        assertTrue(info.getTimestamp() > 0);
    }

    @Test
    void testProcessListIsImmutable() {
        String output = "1 init\n42 sshd\n";
        ProcessListInfo info = parser.parse(output);
        assertNotNull(info);
        assertThrows(UnsupportedOperationException.class, () -> info.getProcesses().add(new ProcessEntry(99, "test")));
    }

    @Test
    void testParseProcFilesystemOutput() {
        // Output from: for d in /proc/[0-9]*; do pid=$(basename $d); [ -f $d/comm ] && echo "$pid $(cat $d/comm)"; done
        String output = """
            1 systemd
            2 kthreadd
            42 sshd
            100 bash
            1234 python3
            """;
        ProcessListInfo info = parser.parse(output);
        assertNotNull(info);
        assertEquals(5, info.getProcessCount());
        assertEquals(1, info.getProcesses().get(0).pid());
        assertEquals("systemd", info.getProcesses().get(0).name());
        assertEquals(1234, info.getProcesses().get(4).pid());
        assertEquals("python3", info.getProcesses().get(4).name());
    }
}
