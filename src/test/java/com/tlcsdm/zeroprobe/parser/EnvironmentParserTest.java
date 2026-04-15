package com.tlcsdm.zeroprobe.parser;

import com.tlcsdm.zeroprobe.model.EnvironmentInfo;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EnvironmentParser.
 */
class EnvironmentParserTest {

    private final EnvironmentParser parser = new EnvironmentParser();

    @Test
    void testParseHostname() {
        assertEquals("mydevice", parser.parseHostname("mydevice\n"));
        assertEquals("mydevice", parser.parseHostname("  mydevice  "));
        assertEquals("", parser.parseHostname(null));
        assertEquals("", parser.parseHostname(""));
        assertEquals("", parser.parseHostname("   "));
    }

    @Test
    void testParseKernelVersion() {
        assertEquals("5.10.0-arm64", parser.parseKernelVersion("5.10.0-arm64\n"));
        assertEquals("", parser.parseKernelVersion(null));
        assertEquals("", parser.parseKernelVersion(""));
    }

    @Test
    void testParseOsRelease() {
        String output = """
            NAME="Ubuntu"
            VERSION="22.04 LTS"
            PRETTY_NAME="Ubuntu 22.04 LTS"
            ID=ubuntu
            """;
        assertEquals("Ubuntu 22.04 LTS", parser.parseOsRelease(output));
    }

    @Test
    void testParseOsReleaseWithoutQuotes() {
        String output = "PRETTY_NAME=Buildroot 2023.02\nID=buildroot\n";
        assertEquals("Buildroot 2023.02", parser.parseOsRelease(output));
    }

    @Test
    void testParseOsReleaseFallback() {
        assertEquals("Some OS Info", parser.parseOsRelease("Some OS Info\n"));
    }

    @Test
    void testParseOsReleaseNull() {
        assertEquals("", parser.parseOsRelease(null));
        assertEquals("", parser.parseOsRelease(""));
    }

    @Test
    void testParseArchitecture() {
        assertEquals("aarch64", parser.parseArchitecture("aarch64\n"));
        assertEquals("armv7l", parser.parseArchitecture("armv7l"));
        assertEquals("", parser.parseArchitecture(null));
    }

    @Test
    void testParseUptime() {
        // 90061.5 seconds = 1d 1h 1m 1s
        assertEquals("1d 1h 1m 1s", parser.parseUptime("90061.50 45000.00"));
    }

    @Test
    void testParseUptimeShort() {
        // 65 seconds = 1m 5s
        assertEquals("1m 5s", parser.parseUptime("65.00 30.00"));
    }

    @Test
    void testParseUptimeSeconds() {
        assertEquals("45s", parser.parseUptime("45.00 20.00"));
    }

    @Test
    void testParseUptimeNull() {
        assertEquals("", parser.parseUptime(null));
        assertEquals("", parser.parseUptime(""));
    }

    @Test
    void testParseEnvironmentVariables() {
        String output = """
            HOME=/root
            PATH=/usr/bin:/bin
            LANG=en_US.UTF-8
            """;
        Map<String, String> envVars = parser.parseEnvironmentVariables(output);
        assertEquals(3, envVars.size());
        assertEquals("/root", envVars.get("HOME"));
        assertEquals("/usr/bin:/bin", envVars.get("PATH"));
        assertEquals("en_US.UTF-8", envVars.get("LANG"));
    }

    @Test
    void testParseEnvironmentVariablesWithEqualsInValue() {
        String output = "MY_VAR=key=value\n";
        Map<String, String> envVars = parser.parseEnvironmentVariables(output);
        assertEquals(1, envVars.size());
        assertEquals("key=value", envVars.get("MY_VAR"));
    }

    @Test
    void testParseEnvironmentVariablesEmpty() {
        assertTrue(parser.parseEnvironmentVariables(null).isEmpty());
        assertTrue(parser.parseEnvironmentVariables("").isEmpty());
    }

    @Test
    void testParseEnvironmentVariablesSkipsBadLines() {
        String output = "GOOD=value\nbadline\n=nokey\nALSO_GOOD=val2\n";
        Map<String, String> envVars = parser.parseEnvironmentVariables(output);
        assertEquals(2, envVars.size());
        assertEquals("value", envVars.get("GOOD"));
        assertEquals("val2", envVars.get("ALSO_GOOD"));
    }

    @Test
    void testBuildEnvironmentInfo() {
        EnvironmentInfo info = parser.buildEnvironmentInfo(
            "mydevice",
            "5.10.0",
            "PRETTY_NAME=\"Ubuntu 22.04\"",
            "aarch64",
            "3661.00 1000.00",
            "HOME=/root\nPATH=/usr/bin"
        );
        assertNotNull(info);
        assertEquals("mydevice", info.getHostname());
        assertEquals("5.10.0", info.getKernelVersion());
        assertEquals("Ubuntu 22.04", info.getOsName());
        assertEquals("aarch64", info.getArchitecture());
        assertEquals("1h 1m 1s", info.getUptime());
        assertEquals(2, info.getEnvironmentVariables().size());
    }

    @Test
    void testEnvironmentInfoImmutableMap() {
        EnvironmentInfo info = parser.buildEnvironmentInfo(
            "host", "5.10", "PRETTY_NAME=\"OS\"", "arm", "100.00 50.00", "A=1"
        );
        assertThrows(UnsupportedOperationException.class,
            () -> info.getEnvironmentVariables().put("NEW", "val"));
    }
}
