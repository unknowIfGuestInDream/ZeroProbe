package com.tlcsdm.zeroprobe.model;

/**
 * CPU usage information parsed from /proc/stat.
 */
public class CpuInfo {

    private final long timestamp;
    private final double usagePercent;
    private final long user;
    private final long nice;
    private final long system;
    private final long idle;
    private final long iowait;
    private final long irq;
    private final long softirq;

    public CpuInfo(long timestamp, double usagePercent, long user, long nice,
                   long system, long idle, long iowait, long irq, long softirq) {
        this.timestamp = timestamp;
        this.usagePercent = usagePercent;
        this.user = user;
        this.nice = nice;
        this.system = system;
        this.idle = idle;
        this.iowait = iowait;
        this.irq = irq;
        this.softirq = softirq;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getUsagePercent() {
        return usagePercent;
    }

    public long getUser() {
        return user;
    }

    public long getNice() {
        return nice;
    }

    public long getSystem() {
        return system;
    }

    public long getIdle() {
        return idle;
    }

    public long getIowait() {
        return iowait;
    }

    public long getIrq() {
        return irq;
    }

    public long getSoftirq() {
        return softirq;
    }

    @Override
    public String toString() {
        return String.format("CpuInfo[usage=%.1f%%, user=%d, nice=%d, system=%d, idle=%d, iowait=%d, irq=%d, softirq=%d]",
            usagePercent, user, nice, system, idle, iowait, irq, softirq);
    }
}
