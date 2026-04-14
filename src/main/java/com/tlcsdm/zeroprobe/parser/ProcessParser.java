package com.tlcsdm.zeroprobe.parser;

import com.tlcsdm.zeroprobe.model.ProcessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for process information from /proc/[pid]/stat and /proc/[pid]/status.
 */
public class ProcessParser {

    private static final Logger log = LoggerFactory.getLogger(ProcessParser.class);

    /**
     * Parse combined output of /proc/[pid]/stat and /proc/[pid]/status.
     *
     * @param pid              the process ID
     * @param procStatOutput   output of cat /proc/[pid]/stat
     * @param procStatusOutput output of cat /proc/[pid]/status
     * @return ProcessInfo, or null if parsing fails
     */
    public ProcessInfo parse(int pid, String procStatOutput, String procStatusOutput) {
        if (procStatOutput == null || procStatOutput.isBlank()) {
            log.warn("Empty /proc/{}/stat output", pid);
            return null;
        }

        // /proc/[pid]/stat: pid (comm) state ... fields
        // comm may contain spaces and parentheses, so find the last ')' to delimit it
        String line = procStatOutput.trim().split("\n")[0];
        int openParen = line.indexOf('(');
        int closeParen = line.lastIndexOf(')');
        if (openParen < 0 || closeParen < 0 || closeParen <= openParen) {
            log.warn("Cannot parse /proc/{}/stat format: {}", pid, line);
            return null;
        }

        String name = line.substring(openParen + 1, closeParen);
        String afterComm = line.substring(closeParen + 2).trim();
        String[] fields = afterComm.split("\\s+");

        // fields[0] = state (field 3 in original)
        // fields[11] = utime (field 14 in original)
        // fields[12] = stime (field 15 in original)
        if (fields.length < 13) {
            log.warn("Not enough fields in /proc/{}/stat after comm: {}", pid, fields.length);
            return null;
        }

        String state = fields[0];
        long utime;
        long stime;
        try {
            utime = Long.parseLong(fields[11]);
            stime = Long.parseLong(fields[12]);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse utime/stime from /proc/{}/stat", pid, e);
            return null;
        }

        // Parse /proc/[pid]/status for Threads and VmRSS
        int threads = 1;
        long vmRssKb = 0;

        if (procStatusOutput != null && !procStatusOutput.isBlank()) {
            for (String sLine : procStatusOutput.split("\n")) {
                String trimmed = sLine.trim();
                if (trimmed.startsWith("Threads:")) {
                    threads = parseIntField(trimmed, threads);
                } else if (trimmed.startsWith("VmRSS:")) {
                    vmRssKb = parseLongField(trimmed, vmRssKb);
                }
            }
        }

        return new ProcessInfo(System.currentTimeMillis(), pid, name, state,
            threads, vmRssKb, utime, stime);
    }

    private int parseIntField(String line, int defaultValue) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse int from: {}", line);
            }
        }
        return defaultValue;
    }

    private long parseLongField(String line, long defaultValue) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse long from: {}", line);
            }
        }
        return defaultValue;
    }
}
