package com.tlcsdm.zeroprobe.model;

/**
 * Configuration for device connection.
 */
public class ConnectionConfig {

    public enum ConnectionType {SSH, SERIAL}

    private ConnectionType type = ConnectionType.SSH;

    // SSH fields
    private String host = "";
    private int port = 22;
    private String username = "root";
    private String password = "";

    // Serial fields
    private String serialPort = "";
    private int baudRate = 115200;

    public ConnectionType getType() {
        return type;
    }

    public void setType(ConnectionType type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSerialPort() {
        return serialPort;
    }

    public void setSerialPort(String serialPort) {
        this.serialPort = serialPort;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    @Override
    public String toString() {
        if (type == ConnectionType.SSH) {
            return "SSH " + username + "@" + host + ":" + port;
        }
        return "Serial " + serialPort + " @ " + baudRate;
    }
}
