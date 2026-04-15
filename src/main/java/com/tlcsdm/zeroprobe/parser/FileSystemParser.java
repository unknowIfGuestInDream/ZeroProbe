package com.tlcsdm.zeroprobe.parser;

import com.tlcsdm.zeroprobe.model.FileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for file system listing output from a remote Linux device.
 * Parses the output of {@code ls -la} (with optional {@code --time-style}) to produce {@link FileEntry} objects.
 */
public class FileSystemParser {

    private static final Logger log = LoggerFactory.getLogger(FileSystemParser.class);

    /**
     * Parse {@code ls -la} output into a list of FileEntry objects.
     *
     * @param output     the raw command output
     * @param parentPath the parent directory path
     * @return list of parsed file entries (excludes "." and "..")
     */
    public List<FileEntry> parseLsOutput(String output, String parentPath) {
        List<FileEntry> entries = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return entries;
        }

        String normalizedParent = parentPath.endsWith("/") ? parentPath : parentPath + "/";

        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("total")) {
                continue;
            }

            FileEntry entry = parseLine(trimmed, normalizedParent);
            if (entry != null && !".".equals(entry.getName()) && !"..".equals(entry.getName())) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * Parse a single line of {@code ls -la} output.
     * Supports both standard format (month day time/year) and custom time-style (YYYY-MM-DD HH:MM:SS).
     */
    FileEntry parseLine(String line, String parentPath) {
        String[] parts = line.split("\\s+", 9);
        if (parts.length < 8) {
            log.debug("Skipping unparseable ls line: {}", line);
            return null;
        }

        try {
            String permissions = parts[0];
            // parts[1] = link count (unused)
            String owner = parts[2];
            String group = parts[3];
            long size = parseSizeSafe(parts[4]);

            String lastModified;
            String name;

            // Detect format: if parts[5] starts with a digit, it's YYYY-MM-DD date format (8 fields)
            if (parts[5].length() >= 4 && Character.isDigit(parts[5].charAt(0))) {
                // New format: permissions links owner group size date time name
                if (parts.length < 8) {
                    log.debug("Skipping unparseable ls line (new format): {}", line);
                    return null;
                }
                lastModified = parts[5] + " " + parts[6];
                // Re-split with limit 8 to keep name with spaces intact
                String[] reParts = line.split("\\s+", 8);
                name = reParts.length >= 8 ? reParts[7] : parts[7];
            } else {
                // Old format: permissions links owner group size month day time/year name
                if (parts.length < 9) {
                    log.debug("Skipping unparseable ls line (old format): {}", line);
                    return null;
                }
                lastModified = parts[5] + " " + parts[6] + " " + parts[7];
                name = parts[8];
            }

            // Handle symlinks: name -> target
            int arrowIdx = name.indexOf(" -> ");
            if (arrowIdx >= 0) {
                name = name.substring(0, arrowIdx);
            }

            boolean isDirectory = permissions.startsWith("d");
            String fullPath = parentPath + name;

            return new FileEntry(name, fullPath, isDirectory, size, permissions, owner, group, lastModified);
        } catch (Exception e) {
            log.debug("Failed to parse ls line: {}", line, e);
            return null;
        }
    }

    private long parseSizeSafe(String sizeStr) {
        try {
            return Long.parseLong(sizeStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
