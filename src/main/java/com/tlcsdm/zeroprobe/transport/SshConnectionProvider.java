package com.tlcsdm.zeroprobe.transport;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.tlcsdm.zeroprobe.model.ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * SSH-based connection provider using JSch.
 */
public class SshConnectionProvider implements ConnectionProvider {

    private static final Logger log = LoggerFactory.getLogger(SshConnectionProvider.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int COMMAND_TIMEOUT_MS = 15_000;
    private static final int BUFFER_SIZE = 4096;
    private static final int SERVER_ALIVE_INTERVAL_MS = 30_000;
    private static final int SERVER_ALIVE_COUNT_MAX = 3;

    private final JSch jsch = new JSch();
    private Session session;

    @Override
    public synchronized void connect(ConnectionConfig config) throws Exception {
        disconnect();

        log.info("Connecting via SSH to {}:{}", config.getHost(), config.getPort());

        session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
        session.setPassword(config.getPassword());
        session.setConfig("StrictHostKeyChecking", "no");
        session.setServerAliveInterval(SERVER_ALIVE_INTERVAL_MS);
        session.setServerAliveCountMax(SERVER_ALIVE_COUNT_MAX);
        session.setTimeout(CONNECT_TIMEOUT_MS);
        session.connect(CONNECT_TIMEOUT_MS);

        log.info("SSH connection established to {}", config.getHost());
    }

    @Override
    public synchronized String executeCommand(String command) throws Exception {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("Not connected");
        }

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        try {
            channel.setCommand(command);
            channel.setInputStream(null);
            ByteArrayOutputStream errStream = new ByteArrayOutputStream();
            channel.setErrStream(errStream);

            InputStream in = channel.getInputStream();
            channel.connect(COMMAND_TIMEOUT_MS);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buf = new byte[BUFFER_SIZE];
            long deadline = System.currentTimeMillis() + COMMAND_TIMEOUT_MS;
            int len;
            while (true) {
                // Use available() to avoid blocking on read(), allowing timeout checks
                while (in.available() > 0 && (len = in.read(buf)) > 0) {
                    output.write(buf, 0, len);
                }
                if (channel.isClosed()) {
                    // Drain remaining buffered data after channel closure;
                    // available() prevents blocking on a closed stream with no data
                    while (in.available() > 0 && (len = in.read(buf)) > 0) {
                        output.write(buf, 0, len);
                    }
                    break;
                }
                if (System.currentTimeMillis() > deadline) {
                    log.warn("Command timed out: {}", command);
                    break;
                }
                Thread.sleep(50);
            }

            int exitStatus = channel.getExitStatus();
            String result = output.toString(StandardCharsets.UTF_8);
            String stderr = errStream.toString(StandardCharsets.UTF_8);

            if (!stderr.isBlank()) {
                log.warn("Command stderr [{}]: {}", command, stderr.trim());
            }
            // exitStatus -1 means the server did not send an exit status (e.g. channel not yet closed)
            if (exitStatus != 0 && exitStatus != -1) {
                log.warn("Command [{}] exited with status {}", command, exitStatus);
            }

            return result;
        } finally {
            channel.disconnect();
        }
    }

    @Override
    public synchronized boolean isConnected() {
        return session != null && session.isConnected();
    }

    @Override
    public synchronized void disconnect() {
        if (session != null) {
            log.info("Disconnecting SSH session");
            session.disconnect();
            session = null;
        }
    }
}
