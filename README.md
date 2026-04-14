# ZeroProbe

A lightweight monitoring tool for embedded Linux devices. Connects via SSH or serial port to collect system metrics from the /proc filesystem — **zero agent required on device**.

## Features

- **SSH & Serial Connection**: Connect to devices via SSH (password auth) or serial port
- **CPU Monitoring**: Real-time CPU usage chart from /proc/stat
- **Memory Monitoring**: Real-time memory usage chart from /proc/meminfo
- **Process Monitoring**: Track specific processes via /proc/[pid]/stat and /proc/[pid]/status
- **Data Recording**: Export monitoring data to CSV files for offline analysis
- **Multi-language Support**: English, Chinese (Simplified), and Japanese
- **Theme Support**: AtlantaFX themes (Primer Light/Dark, Nord Light/Dark)
- **User Preferences**: Persistent settings for language, theme, and connection

## Design Principles

- **Zero-Agent**: Absolutely no software installation on the target device
- **/proc Only**: All data collected through /proc and /sys filesystem reads
- **BusyBox Compatible**: No dependency on `top`, `free`, `ps`, or other tools
- **Extensible Architecture**: Modular design with interfaces for transport, parsing, and export

## Requirements

- Java 21 or later
- Maven 3.9+

## Getting Started

### Build

```bash
mvn clean package
```

### Run

```bash
mvn javafx:run
```

### Test

```bash
mvn clean verify
```

## Architecture

```
┌─────────────────────────────────────────┐
│           JavaFX UI Layer               │
│  Connection │ Monitor │ Process │ Record│
├─────────────────────────────────────────┤
│         MonitoringService               │
│    (ScheduledExecutorService)           │
├──────────┬──────────┬───────────────────┤
│ CpuParser│MemParser │ ProcessParser     │
├──────────┴──────────┴───────────────────┤
│        ConnectionProvider               │
│   ┌──────────┐  ┌──────────────────┐    │
│   │   SSH    │  │     Serial       │    │
│   │  (JSch)  │  │  (jSerialComm)   │    │
│   └──────────┘  └──────────────────┘    │
├─────────────────────────────────────────┤
│         DataExporter                    │
│   ┌──────────┐  ┌──────────────────┐    │
│   │   CSV    │  │  SQLite (future) │    │
│   └──────────┘  └──────────────────┘    │
└─────────────────────────────────────────┘
```

## License

[MIT License](LICENSE)
