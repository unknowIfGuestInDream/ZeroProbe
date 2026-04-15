package com.tlcsdm.zeroprobe.parser;

import com.tlcsdm.zeroprobe.model.EnvironmentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser for environment information collected from a remote Linux device.
 */
public class EnvironmentParser {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentParser.class);

    /**
     * Parse hostname output.
     */
    public String parseHostname(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        return output.trim();
    }

    /**
     * Parse kernel version from {@code uname -r} output.
     */
    public String parseKernelVersion(String output) {
        return extractFirstToken(output);
    }

    /**
     * Parse OS name from {@code cat /etc/os-release} output.
     * Extracts the PRETTY_NAME field.
     */
    public String parseOsRelease(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("PRETTY_NAME=")) {
                String value = trimmed.substring("PRETTY_NAME=".length());
                // Remove surrounding quotes if present
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        // Fallback: return first non-empty line
        for (String line : output.split("\n")) {
            if (!line.isBlank()) {
                return line.trim();
            }
        }
        return "";
    }

    /**
     * Parse architecture from {@code uname -m} output.
     */
    public String parseArchitecture(String output) {
        return extractFirstToken(output);
    }

    private String extractFirstToken(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                int firstSpace = trimmed.indexOf(' ');
                return firstSpace > 0 ? trimmed.substring(0, firstSpace) : trimmed;
            }
        }
        return "";
    }

    /**
     * Parse uptime from {@code cat /proc/uptime} output.
     * Format: "seconds.fraction idle_seconds.fraction"
     * Returns a human-readable string.
     */
    public String parseUptime(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        try {
            String[] parts = output.trim().split("\\s+");
            if (parts.length == 0) {
                return output.trim();
            }
            double totalSeconds = Double.parseDouble(parts[0]);
            long days = (long) (totalSeconds / 86400);
            long hours = (long) ((totalSeconds % 86400) / 3600);
            long minutes = (long) ((totalSeconds % 3600) / 60);
            long seconds = (long) (totalSeconds % 60);

            StringBuilder sb = new StringBuilder();
            if (days > 0) {
                sb.append(days).append("d ");
            }
            if (hours > 0 || days > 0) {
                sb.append(hours).append("h ");
            }
            if (minutes > 0 || hours > 0 || days > 0) {
                sb.append(minutes).append("m ");
            }
            sb.append(seconds).append("s");
            return sb.toString();
        } catch (NumberFormatException e) {
            log.warn("Failed to parse uptime: {}", output, e);
            return output.trim();
        }
    }

    /**
     * Parse environment variables from {@code env} command output.
     * Each line is expected to be KEY=VALUE format.
     */
    public Map<String, String> parseEnvironmentVariables(String output) {
        Map<String, String> envVars = new LinkedHashMap<>();
        if (output == null || output.isBlank()) {
            return envVars;
        }
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eqIdx = trimmed.indexOf('=');
            if (eqIdx > 0) {
                String key = trimmed.substring(0, eqIdx);
                String value = trimmed.substring(eqIdx + 1);
                envVars.put(key, value);
            }
        }
        return envVars;
    }

    /**
     * Build a complete EnvironmentInfo from individual command outputs.
     */
    public EnvironmentInfo buildEnvironmentInfo(String hostnameOutput, String kernelOutput,
                                                String osReleaseOutput, String archOutput,
                                                String uptimeOutput, String envOutput) {
        return new EnvironmentInfo(
            parseHostname(hostnameOutput),
            parseKernelVersion(kernelOutput),
            parseOsRelease(osReleaseOutput),
            parseArchitecture(archOutput),
            parseUptime(uptimeOutput),
            parseEnvironmentVariables(envOutput)
        );
    }
}
