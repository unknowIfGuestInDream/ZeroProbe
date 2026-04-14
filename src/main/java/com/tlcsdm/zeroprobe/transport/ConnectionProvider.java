package com.tlcsdm.zeroprobe.transport;

import com.tlcsdm.zeroprobe.model.ConnectionConfig;

/**
 * Abstract transport interface for executing commands on a remote device.
 */
public interface ConnectionProvider extends AutoCloseable {

    /**
     * Connect to the device.
     */
    void connect(ConnectionConfig config) throws Exception;

    /**
     * Execute a command and return standard output.
     */
    String executeCommand(String command) throws Exception;

    /**
     * Check if currently connected.
     */
    boolean isConnected();

    /**
     * Disconnect from the device.
     */
    void disconnect();

    @Override
    default void close() {
        disconnect();
    }
}
