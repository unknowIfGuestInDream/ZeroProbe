package com.tlcsdm.zeroprobe.service;

import com.tlcsdm.zeroprobe.model.CpuInfo;
import com.tlcsdm.zeroprobe.model.MemoryInfo;
import com.tlcsdm.zeroprobe.model.ProcessInfo;
import com.tlcsdm.zeroprobe.model.ProcessListInfo;
import com.tlcsdm.zeroprobe.parser.CpuParser;
import com.tlcsdm.zeroprobe.parser.MemoryParser;
import com.tlcsdm.zeroprobe.parser.ProcessListParser;
import com.tlcsdm.zeroprobe.parser.ProcessParser;
import com.tlcsdm.zeroprobe.transport.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service that periodically collects monitoring data from a remote device.
 * Each collection step is independent — a failure in one does not stop others.
 */
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private static final int DEFAULT_INTERVAL_MS = 1000;

    private final ConnectionProvider connectionProvider;
    private final CpuParser cpuParser = new CpuParser();
    private final MemoryParser memoryParser = new MemoryParser();
    private final ProcessParser processParser = new ProcessParser();
    private final ProcessListParser processListParser = new ProcessListParser();

    private ScheduledExecutorService scheduler;
    private volatile int monitoredPid = -1;

    private Consumer<CpuInfo> onCpuUpdate;
    private Consumer<MemoryInfo> onMemoryUpdate;
    private Consumer<ProcessInfo> onProcessUpdate;
    private Consumer<ProcessListInfo> onProcessListUpdate;
    private Consumer<String> onError;

    public MonitoringService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Start periodic data collection.
     *
     * @param intervalMs collection interval in milliseconds
     */
    public void start(int intervalMs) {
        stop();
        cpuParser.reset();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "zeroprobe-monitor");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) ->
                log.error("Uncaught exception in monitoring thread", ex));
            return t;
        });

        int interval = intervalMs > 0 ? intervalMs : DEFAULT_INTERVAL_MS;
        scheduler.scheduleAtFixedRate(this::collectData, 0, interval, TimeUnit.MILLISECONDS);
        log.info("Monitoring started with {}ms interval", interval);
    }

    /**
     * Start periodic data collection with default interval.
     */
    public void start() {
        start(DEFAULT_INTERVAL_MS);
    }

    /**
     * Stop periodic data collection.
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
            log.info("Monitoring stopped");
        }
    }

    /**
     * Perform a single data collection cycle.
     */
    private void collectData() {
        collectCpu();
        collectMemory();
        collectProcessList();
        collectProcess();
    }

    private void collectCpu() {
        try {
            String output = connectionProvider.executeCommand("cat /proc/stat");
            CpuInfo cpuInfo = cpuParser.parse(output);
            if (cpuInfo != null && onCpuUpdate != null) {
                onCpuUpdate.accept(cpuInfo);
            }
        } catch (Exception e) {
            log.error("CPU collection failed", e);
            notifyError("CPU collection failed: " + e.getMessage());
        }
    }

    private void collectMemory() {
        try {
            String output = connectionProvider.executeCommand("cat /proc/meminfo");
            MemoryInfo memInfo = memoryParser.parse(output);
            if (memInfo != null && onMemoryUpdate != null) {
                onMemoryUpdate.accept(memInfo);
            }
        } catch (Exception e) {
            log.error("Memory collection failed", e);
            notifyError("Memory collection failed: " + e.getMessage());
        }
    }

    private void collectProcess() {
        int pid = monitoredPid;
        if (pid <= 0) {
            return;
        }

        try {
            String statOutput = connectionProvider.executeCommand("cat /proc/" + pid + "/stat");
            String statusOutput = connectionProvider.executeCommand("cat /proc/" + pid + "/status");
            ProcessInfo procInfo = processParser.parse(pid, statOutput, statusOutput);
            if (procInfo != null && onProcessUpdate != null) {
                onProcessUpdate.accept(procInfo);
            }
        } catch (Exception e) {
            log.error("Process collection failed for PID {}", pid, e);
            notifyError("Process collection failed for PID " + pid + ": " + e.getMessage());
        }
    }

    private void collectProcessList() {
        try {
            String output = connectionProvider.executeCommand(
                "for d in /proc/[0-9]*; do pid=$(basename $d); "
                    + "[ -f $d/comm ] && echo \"$pid $(cat $d/comm 2>/dev/null)\"; done");
            ProcessListInfo listInfo = processListParser.parse(output);
            if (listInfo != null && onProcessListUpdate != null) {
                onProcessListUpdate.accept(listInfo);
            }
        } catch (Exception e) {
            log.error("Process list collection failed", e);
            notifyError("Process list collection failed: " + e.getMessage());
        }
    }

    private void notifyError(String message) {
        if (onError != null) {
            onError.accept(message);
        }
    }

    /**
     * Shut down the service and release resources.
     */
    public void shutdown() {
        stop();
        cpuParser.reset();
    }

    public void setMonitoredPid(int pid) {
        this.monitoredPid = pid;
    }

    public int getMonitoredPid() {
        return monitoredPid;
    }

    public void setOnCpuUpdate(Consumer<CpuInfo> onCpuUpdate) {
        this.onCpuUpdate = onCpuUpdate;
    }

    public void setOnMemoryUpdate(Consumer<MemoryInfo> onMemoryUpdate) {
        this.onMemoryUpdate = onMemoryUpdate;
    }

    public void setOnProcessUpdate(Consumer<ProcessInfo> onProcessUpdate) {
        this.onProcessUpdate = onProcessUpdate;
    }

    public void setOnProcessListUpdate(Consumer<ProcessListInfo> onProcessListUpdate) {
        this.onProcessListUpdate = onProcessListUpdate;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }
}
