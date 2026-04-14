package com.tlcsdm.zeroprobe.parser;

import com.tlcsdm.zeroprobe.model.MemoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for memory information from /proc/meminfo.
 */
public class MemoryParser {

    private static final Logger log = LoggerFactory.getLogger(MemoryParser.class);

    /**
     * Parse /proc/meminfo output.
     *
     * @param procMeminfoOutput the output of cat /proc/meminfo
     * @return MemoryInfo with parsed values, or null if parsing fails
     */
    public MemoryInfo parse(String procMeminfoOutput) {
        if (procMeminfoOutput == null || procMeminfoOutput.isBlank()) {
            log.warn("Empty /proc/meminfo output");
            return null;
        }

        long totalKb = -1;
        long freeKb = -1;
        long availableKb = -1;
        long buffersKb = 0;
        long cachedKb = 0;

        for (String line : procMeminfoOutput.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("MemTotal:")) {
                totalKb = parseKbValue(trimmed);
            } else if (trimmed.startsWith("MemFree:")) {
                freeKb = parseKbValue(trimmed);
            } else if (trimmed.startsWith("MemAvailable:")) {
                availableKb = parseKbValue(trimmed);
            } else if (trimmed.startsWith("Buffers:")) {
                buffersKb = parseKbValue(trimmed);
            } else if (trimmed.startsWith("Cached:")) {
                cachedKb = parseKbValue(trimmed);
            }
        }

        if (totalKb < 0 || freeKb < 0) {
            log.warn("Missing required fields in /proc/meminfo (MemTotal or MemFree)");
            return null;
        }

        // Some embedded systems lack MemAvailable; estimate it
        if (availableKb < 0) {
            availableKb = freeKb + buffersKb + cachedKb;
        }

        double usagePercent = 0.0;
        if (totalKb > 0) {
            usagePercent = (double) (totalKb - freeKb - buffersKb - cachedKb) / totalKb * 100.0;
            usagePercent = Math.max(0.0, Math.min(100.0, usagePercent));
        }

        return new MemoryInfo(System.currentTimeMillis(), totalKb, freeKb, availableKb,
            buffersKb, cachedKb, usagePercent);
    }

    /**
     * Parse a kB value from a /proc/meminfo line like "MemTotal:       1234567 kB".
     */
    private long parseKbValue(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse meminfo value: {}", line, e);
            }
        }
        return -1;
    }
}
