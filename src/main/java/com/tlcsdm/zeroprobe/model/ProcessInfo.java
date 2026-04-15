package com.tlcsdm.zeroprobe.model;

/**
 * Process information parsed from /proc/[pid]/stat and /proc/[pid]/status.
 */
public class ProcessInfo {

    private final long timestamp;
    private final int pid;
    private final String name;
    private final String state;
    private final int ppid;
    private final int uid;
    private final int threads;
    private final long vmRssKb;
    private final long vmSizeKb;
    private final long vmPeakKb;
    private final long utime;
    private final long stime;
    private final long voluntaryCtxtSwitches;
    private final long nonvoluntaryCtxtSwitches;

    public ProcessInfo(long timestamp, int pid, String name, String state,
                       int ppid, int uid, int threads, long vmRssKb,
                       long vmSizeKb, long vmPeakKb, long utime, long stime,
                       long voluntaryCtxtSwitches, long nonvoluntaryCtxtSwitches) {
        this.timestamp = timestamp;
        this.pid = pid;
        this.name = name;
        this.state = state;
        this.ppid = ppid;
        this.uid = uid;
        this.threads = threads;
        this.vmRssKb = vmRssKb;
        this.vmSizeKb = vmSizeKb;
        this.vmPeakKb = vmPeakKb;
        this.utime = utime;
        this.stime = stime;
        this.voluntaryCtxtSwitches = voluntaryCtxtSwitches;
        this.nonvoluntaryCtxtSwitches = nonvoluntaryCtxtSwitches;
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

    public int getPpid() {
        return ppid;
    }

    public int getUid() {
        return uid;
    }

    public int getThreads() {
        return threads;
    }

    public long getVmRssKb() {
        return vmRssKb;
    }

    public long getVmSizeKb() {
        return vmSizeKb;
    }

    public long getVmPeakKb() {
        return vmPeakKb;
    }

    public long getUtime() {
        return utime;
    }

    public long getStime() {
        return stime;
    }

    public long getVoluntaryCtxtSwitches() {
        return voluntaryCtxtSwitches;
    }

    public long getNonvoluntaryCtxtSwitches() {
        return nonvoluntaryCtxtSwitches;
    }

    @Override
    public String toString() {
        return String.format("ProcessInfo[pid=%d, name=%s, state=%s, ppid=%d, uid=%d, threads=%d, vmRSS=%dkB, vmSize=%dkB, vmPeak=%dkB, utime=%d, stime=%d]",
            pid, name, state, ppid, uid, threads, vmRssKb, vmSizeKb, vmPeakKb, utime, stime);
    }
}
