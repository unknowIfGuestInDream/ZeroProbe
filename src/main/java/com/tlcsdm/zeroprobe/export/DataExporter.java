package com.tlcsdm.zeroprobe.export;

import com.tlcsdm.zeroprobe.model.CpuInfo;
import com.tlcsdm.zeroprobe.model.MemoryInfo;
import com.tlcsdm.zeroprobe.model.ProcessInfo;

/**
 * Interface for exporting monitoring data.
 * Designed for extensibility (CSV now, SQLite later).
 */
public interface DataExporter extends AutoCloseable {

    void open(String destination) throws Exception;

    void exportCpuInfo(CpuInfo cpuInfo) throws Exception;

    void exportMemoryInfo(MemoryInfo memInfo) throws Exception;

    void exportProcessInfo(ProcessInfo procInfo) throws Exception;

    void flush() throws Exception;

    boolean isOpen();
}
