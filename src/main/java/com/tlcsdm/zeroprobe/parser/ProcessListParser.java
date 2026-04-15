package com.tlcsdm.zeroprobe.parser;

import com.tlcsdm.zeroprobe.model.ProcessListInfo;
import com.tlcsdm.zeroprobe.model.ProcessListInfo.ProcessEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for process list output from {@code ps -eo pid,comm --no-headers}.
 */
public class ProcessListParser {

    private static final Logger log = LoggerFactory.getLogger(ProcessListParser.class);

    /**
     * Parse the output of {@code ps -eo pid,comm --no-headers}.
     *
     * @param psOutput the raw output string
     * @return ProcessListInfo with parsed entries, or null if parsing fails
     */
    public ProcessListInfo parse(String psOutput) {
        if (psOutput == null || psOutput.isBlank()) {
            log.warn("Empty ps output");
            return null;
        }

        List<ProcessEntry> entries = new ArrayList<>();
        for (String line : psOutput.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // Expected format: "<PID> <COMMAND>"
            int spaceIdx = trimmed.indexOf(' ');
            if (spaceIdx <= 0) {
                continue;
            }
            try {
                int pid = Integer.parseInt(trimmed.substring(0, spaceIdx).trim());
                String name = trimmed.substring(spaceIdx).trim();
                if (!name.isEmpty()) {
                    entries.add(new ProcessEntry(pid, name));
                }
            } catch (NumberFormatException e) {
                // Skip header lines or malformed entries
                log.trace("Skipping non-numeric PID line: {}", trimmed);
            }
        }

        if (entries.isEmpty()) {
            log.warn("No process entries parsed from ps output");
            return null;
        }

        return new ProcessListInfo(System.currentTimeMillis(), entries);
    }
}
