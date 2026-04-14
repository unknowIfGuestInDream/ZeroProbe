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

    private Session session;

    @Override
    public synchronized void connect(ConnectionConfig config) throws Exception {
        disconnect();

        log.info("Connecting via SSH to {}:{}", config.getHost(), config.getPort());

        JSch jsch = new JSch();
        session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
        session.setPassword(config.getPassword());
        session.setConfig("StrictHostKeyChecking", "no");
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
                while ((len = in.read(buf)) > 0) {
                    output.write(buf, 0, len);
                }
                if (channel.isClosed()) {
                    break;
                }
                if (System.currentTimeMillis() > deadline) {
                    log.warn("Command timed out: {}", command);
                    break;
                }
                Thread.sleep(50);
            }

            String result = output.toString(StandardCharsets.UTF_8);
            String stderr = errStream.toString(StandardCharsets.UTF_8);
            if (!stderr.isBlank()) {
                log.warn("Command stderr [{}]: {}", command, stderr.trim());
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
