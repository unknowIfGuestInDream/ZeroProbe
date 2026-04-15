package com.tlcsdm.zeroprobe.export;

import com.tlcsdm.zeroprobe.model.CpuInfo;
import com.tlcsdm.zeroprobe.model.MemoryInfo;
import com.tlcsdm.zeroprobe.model.ProcessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Exports monitoring data to CSV files.
 * Creates separate CSV files for CPU, memory, and process data in the destination directory.
 */
public class CsvDataExporter implements DataExporter {

    private static final Logger log = LoggerFactory.getLogger(CsvDataExporter.class);

    private static final String CPU_HEADER = "timestamp,usage_percent,user,nice,system,idle,iowait,irq,softirq";
    private static final String MEMORY_HEADER = "timestamp,total_kb,free_kb,available_kb,buffers_kb,cached_kb,usage_percent";
    private static final String PROCESS_HEADER = "timestamp,pid,name,state,ppid,uid,threads,vmrss_kb,vmsize_kb,vmpeak_kb,utime,stime,voluntary_ctxt_switches,nonvoluntary_ctxt_switches";

    private static final DateTimeFormatter TIMESTAMP_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private BufferedWriter cpuWriter;
    private BufferedWriter memoryWriter;
    private BufferedWriter processWriter;
    private boolean open;

    @Override
    public synchronized void open(String destination) throws Exception {
        Path dir = Path.of(destination);
        Files.createDirectories(dir);

        cpuWriter = createWriter(dir.resolve("cpu.csv"), CPU_HEADER);
        memoryWriter = createWriter(dir.resolve("memory.csv"), MEMORY_HEADER);
        processWriter = createWriter(dir.resolve("process.csv"), PROCESS_HEADER);
        open = true;

        log.info("CSV export opened in {}", destination);
    }

    private BufferedWriter createWriter(Path file, String header) throws IOException {
        boolean writeHeader = !Files.exists(file) || Files.size(file) == 0;
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (writeHeader) {
            writer.write(header);
            writer.newLine();
        }
        return writer;
    }

    @Override
    public synchronized void exportCpuInfo(CpuInfo cpuInfo) throws Exception {
        checkOpen();
        String ts = TIMESTAMP_FMT.format(Instant.ofEpochMilli(cpuInfo.getTimestamp()));
        cpuWriter.write(String.format("%s,%.2f,%d,%d,%d,%d,%d,%d,%d",
            ts, cpuInfo.getUsagePercent(), cpuInfo.getUser(), cpuInfo.getNice(),
            cpuInfo.getSystem(), cpuInfo.getIdle(), cpuInfo.getIowait(),
            cpuInfo.getIrq(), cpuInfo.getSoftirq()));
        cpuWriter.newLine();
    }

    @Override
    public synchronized void exportMemoryInfo(MemoryInfo memInfo) throws Exception {
        checkOpen();
        String ts = TIMESTAMP_FMT.format(Instant.ofEpochMilli(memInfo.getTimestamp()));
        memoryWriter.write(String.format("%s,%d,%d,%d,%d,%d,%.2f",
            ts, memInfo.getTotalKb(), memInfo.getFreeKb(), memInfo.getAvailableKb(),
            memInfo.getBuffersKb(), memInfo.getCachedKb(), memInfo.getUsagePercent()));
        memoryWriter.newLine();
    }

    @Override
    public synchronized void exportProcessInfo(ProcessInfo procInfo) throws Exception {
        checkOpen();
        String ts = TIMESTAMP_FMT.format(Instant.ofEpochMilli(procInfo.getTimestamp()));
        String safeName = escapeCsv(procInfo.getName());
        processWriter.write(String.format("%s,%d,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d",
            ts, procInfo.getPid(), safeName, procInfo.getState(),
            procInfo.getPpid(), procInfo.getUid(),
            procInfo.getThreads(), procInfo.getVmRssKb(),
            procInfo.getVmSizeKb(), procInfo.getVmPeakKb(),
            procInfo.getUtime(), procInfo.getStime(),
            procInfo.getVoluntaryCtxtSwitches(), procInfo.getNonvoluntaryCtxtSwitches()));
        processWriter.newLine();
    }

    @Override
    public synchronized void flush() throws Exception {
        if (cpuWriter != null) cpuWriter.flush();
        if (memoryWriter != null) memoryWriter.flush();
        if (processWriter != null) processWriter.flush();
    }

    @Override
    public synchronized boolean isOpen() {
        return open;
    }

    @Override
    public synchronized void close() {
        open = false;
        closeQuietly(cpuWriter);
        closeQuietly(memoryWriter);
        closeQuietly(processWriter);
        cpuWriter = null;
        memoryWriter = null;
        processWriter = null;
        log.info("CSV export closed");
    }

    private void checkOpen() {
        if (!open) {
            throw new IllegalStateException("Exporter is not open");
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void closeQuietly(BufferedWriter writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                log.warn("Error closing CSV writer", e);
            }
        }
    }
}
