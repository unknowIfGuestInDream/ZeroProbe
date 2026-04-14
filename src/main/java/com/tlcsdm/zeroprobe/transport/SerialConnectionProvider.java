package com.tlcsdm.zeroprobe.transport;

import com.fazecast.jSerialComm.SerialPort;
import com.tlcsdm.zeroprobe.model.ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serial port connection provider using jSerialComm.
 * Uses boundary markers to cleanly extract command output from noisy serial lines.
 */
public class SerialConnectionProvider implements ConnectionProvider {

    private static final Logger log = LoggerFactory.getLogger(SerialConnectionProvider.class);

    private static final String BEGIN_MARKER = "[ZP_BEGIN]";
    private static final String END_MARKER = "[ZP_END]";
    private static final int READ_TIMEOUT_MS = 5000;

    private SerialPort port;

    @Override
    public synchronized void connect(ConnectionConfig config) throws Exception {
        disconnect();

        log.info("Opening serial port {} at {} baud", config.getSerialPort(), config.getBaudRate());

        port = SerialPort.getCommPort(config.getSerialPort());
        port.setBaudRate(config.getBaudRate());
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);

        if (!port.openPort()) {
            port = null;
            throw new Exception("Failed to open serial port: " + config.getSerialPort());
        }

        log.info("Serial port {} opened", config.getSerialPort());
    }

    @Override
    public synchronized String executeCommand(String command) throws Exception {
        if (port == null || !port.isOpen()) {
            throw new IllegalStateException("Not connected");
        }

        // Escape single quotes in the command to prevent injection
        String safeCommand = command.replace("'", "'\\''");
        String wrappedCommand = "echo '" + BEGIN_MARKER + "'; " + safeCommand + "; echo '" + END_MARKER + "'\n";

        OutputStream out = port.getOutputStream();
        out.write(wrappedCommand.getBytes(StandardCharsets.UTF_8));
        out.flush();

        InputStream in = port.getInputStream();
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1024];
        long deadline = System.currentTimeMillis() + READ_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            int available = in.available();
            if (available > 0) {
                int len = in.read(buf, 0, Math.min(available, buf.length));
                if (len > 0) {
                    sb.append(new String(buf, 0, len, StandardCharsets.UTF_8));
                    if (sb.toString().contains(END_MARKER)) {
                        break;
                    }
                }
            } else {
                Thread.sleep(50);
            }
        }

        String raw = sb.toString();
        int beginIdx = raw.indexOf(BEGIN_MARKER);
        int endIdx = raw.indexOf(END_MARKER);
        if (beginIdx >= 0 && endIdx > beginIdx) {
            return raw.substring(beginIdx + BEGIN_MARKER.length(), endIdx).trim();
        }

        log.warn("Could not find boundary markers in serial output for command: {}", command);
        return raw.trim();
    }

    @Override
    public synchronized boolean isConnected() {
        return port != null && port.isOpen();
    }

    @Override
    public synchronized void disconnect() {
        if (port != null) {
            log.info("Closing serial port");
            port.closePort();
            port = null;
        }
    }
}
