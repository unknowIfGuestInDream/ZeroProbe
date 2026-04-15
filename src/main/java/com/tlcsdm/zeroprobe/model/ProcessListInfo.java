package com.tlcsdm.zeroprobe.model;

import java.util.List;

/**
 * Information about the process list collected from a remote device.
 * Contains a snapshot of all running processes (PID + name) and the total count.
 */
public class ProcessListInfo {

    private final long timestamp;
    private final List<ProcessEntry> processes;

    public ProcessListInfo(long timestamp, List<ProcessEntry> processes) {
        this.timestamp = timestamp;
        this.processes = List.copyOf(processes);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<ProcessEntry> getProcesses() {
        return processes;
    }

    public int getProcessCount() {
        return processes.size();
    }

    /**
     * A single process entry with PID and command name.
     */
    public record ProcessEntry(int pid, String name) implements Comparable<ProcessEntry> {

        @Override
        public int compareTo(ProcessEntry other) {
            return Integer.compare(this.pid, other.pid);
        }

        @Override
        public String toString() {
            return pid + " - " + name;
        }
    }
}
