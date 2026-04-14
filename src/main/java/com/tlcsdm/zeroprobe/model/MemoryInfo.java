package com.tlcsdm.zeroprobe.model;

/**
 * Memory information parsed from /proc/meminfo.
 */
public class MemoryInfo {

    private final long timestamp;
    private final long totalKb;
    private final long freeKb;
    private final long availableKb;
    private final long buffersKb;
    private final long cachedKb;
    private final double usagePercent;

    public MemoryInfo(long timestamp, long totalKb, long freeKb, long availableKb,
                      long buffersKb, long cachedKb, double usagePercent) {
        this.timestamp = timestamp;
        this.totalKb = totalKb;
        this.freeKb = freeKb;
        this.availableKb = availableKb;
        this.buffersKb = buffersKb;
        this.cachedKb = cachedKb;
        this.usagePercent = usagePercent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getTotalKb() {
        return totalKb;
    }

    public long getFreeKb() {
        return freeKb;
    }

    public long getAvailableKb() {
        return availableKb;
    }

    public long getBuffersKb() {
        return buffersKb;
    }

    public long getCachedKb() {
        return cachedKb;
    }

    public double getUsagePercent() {
        return usagePercent;
    }

    @Override
    public String toString() {
        return String.format("MemoryInfo[usage=%.1f%%, total=%dkB, free=%dkB, available=%dkB, buffers=%dkB, cached=%dkB]",
            usagePercent, totalKb, freeKb, availableKb, buffersKb, cachedKb);
    }
}
