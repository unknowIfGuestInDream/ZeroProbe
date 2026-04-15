package com.tlcsdm.zeroprobe.model;

import java.util.Collections;
import java.util.Map;

/**
 * Environment information collected from a remote embedded Linux device.
 */
public class EnvironmentInfo {

    private final String hostname;
    private final String kernelVersion;
    private final String osName;
    private final String architecture;
    private final String uptime;
    private final Map<String, String> environmentVariables;

    public EnvironmentInfo(String hostname, String kernelVersion, String osName,
                           String architecture, String uptime,
                           Map<String, String> environmentVariables) {
        this.hostname = hostname;
        this.kernelVersion = kernelVersion;
        this.osName = osName;
        this.architecture = architecture;
        this.uptime = uptime;
        this.environmentVariables = environmentVariables != null
            ? Collections.unmodifiableMap(environmentVariables) : Collections.emptyMap();
    }

    public String getHostname() {
        return hostname;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public String getOsName() {
        return osName;
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getUptime() {
        return uptime;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    @Override
    public String toString() {
        return String.format("EnvironmentInfo[hostname=%s, kernel=%s, os=%s, arch=%s, uptime=%s, envVars=%d]",
            hostname, kernelVersion, osName, architecture, uptime, environmentVariables.size());
    }
}
