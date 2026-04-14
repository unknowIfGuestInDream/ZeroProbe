package com.tlcsdm.zeroprobe.model;

/**
 * Process information parsed from /proc/[pid]/stat and /proc/[pid]/status.
 */
public class ProcessInfo {

    private final long timestamp;
    private final int pid;
    private final String name;
    private final String state;
    private final int threads;
    private final long vmRssKb;
    private final long utime;
    private final long stime;

    public ProcessInfo(long timestamp, int pid, String name, String state,
                       int threads, long vmRssKb, long utime, long stime) {
        this.timestamp = timestamp;
        this.pid = pid;
        this.name = name;
        this.state = state;
        this.threads = threads;
        this.vmRssKb = vmRssKb;
        this.utime = utime;
        this.stime = stime;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public int getThreads() {
        return threads;
    }

    public long getVmRssKb() {
        return vmRssKb;
    }

    public long getUtime() {
        return utime;
    }

    public long getStime() {
        return stime;
    }

    @Override
    public String toString() {
        return String.format("ProcessInfo[pid=%d, name=%s, state=%s, threads=%d, vmRSS=%dkB, utime=%d, stime=%d]",
            pid, name, state, threads, vmRssKb, utime, stime);
    }
}
