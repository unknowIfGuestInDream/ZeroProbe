package com.tlcsdm.zeroprobe.parser;

import com.tlcsdm.zeroprobe.model.CpuInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for CPU information from /proc/stat.
 * Tracks previous readings to calculate delta-based CPU usage percentage.
 */
public class CpuParser {

    private static final Logger log = LoggerFactory.getLogger(CpuParser.class);

    private long prevUser, prevNice, prevSystem, prevIdle, prevIowait, prevIrq, prevSoftirq;
    private boolean hasPrevious = false;

    /**
     * Parse /proc/stat output and calculate CPU usage.
     *
     * @param procStatOutput the full output of cat /proc/stat
     * @return CpuInfo with calculated usage, or null if parsing fails
     */
    public CpuInfo parse(String procStatOutput) {
        if (procStatOutput == null || procStatOutput.isBlank()) {
            log.warn("Empty /proc/stat output");
            return null;
        }

        // Find the first line starting with "cpu "
        String cpuLine = null;
        for (String line : procStatOutput.split("\n")) {
            if (line.startsWith("cpu ")) {
                cpuLine = line;
                break;
            }
        }

        if (cpuLine == null) {
            log.warn("No 'cpu ' line found in /proc/stat output");
            return null;
        }

        // Parse: cpu  user nice system idle iowait irq softirq [steal [guest [guest_nice]]]
        String[] parts = cpuLine.trim().split("\\s+");
        if (parts.length < 8) {
            log.warn("Unexpected /proc/stat cpu line format: {}", cpuLine);
            return null;
        }

        try {
            long user = Long.parseLong(parts[1]);
            long nice = Long.parseLong(parts[2]);
            long system = Long.parseLong(parts[3]);
            long idle = Long.parseLong(parts[4]);
            long iowait = Long.parseLong(parts[5]);
            long irq = Long.parseLong(parts[6]);
            long softirq = Long.parseLong(parts[7]);

            double usagePercent = 0.0;

            if (hasPrevious) {
                long dUser = user - prevUser;
                long dNice = nice - prevNice;
                long dSystem = system - prevSystem;
                long dIdle = idle - prevIdle;
                long dIowait = iowait - prevIowait;
                long dIrq = irq - prevIrq;
                long dSoftirq = softirq - prevSoftirq;

                long totalDelta = dUser + dNice + dSystem + dIdle + dIowait + dIrq + dSoftirq;
                long idleDelta = dIdle + dIowait;

                if (totalDelta > 0) {
                    usagePercent = (double) (totalDelta - idleDelta) / totalDelta * 100.0;
                }
            }

            prevUser = user;
            prevNice = nice;
            prevSystem = system;
            prevIdle = idle;
            prevIowait = iowait;
            prevIrq = irq;
            prevSoftirq = softirq;
            hasPrevious = true;

            return new CpuInfo(System.currentTimeMillis(), usagePercent, user, nice,
                system, idle, iowait, irq, softirq);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse /proc/stat values: {}", cpuLine, e);
            return null;
        }
    }

    /**
     * Reset the parser state (used when reconnecting).
     */
    public void reset() {
        hasPrevious = false;
    }
}
