package com.tlcsdm.zeroprobe.config;

import com.tlcsdm.zeroprobe.model.ConnectionConfig;

import java.util.prefs.Preferences;

/**
 * User preference persistence for window state and connection settings.
 */
public final class UserPreferences {

    private static final Preferences PREFS = Preferences.userNodeForPackage(UserPreferences.class);

    private static final String KEY_WINDOW_X = "window.x";
    private static final String KEY_WINDOW_Y = "window.y";
    private static final String KEY_WINDOW_WIDTH = "window.width";
    private static final String KEY_WINDOW_HEIGHT = "window.height";
    private static final String KEY_WINDOW_MAXIMIZED = "window.maximized";

    private static final String KEY_CONNECTION_TYPE = "connection.type";
    private static final String KEY_CONNECTION_HOST = "connection.host";
    private static final String KEY_CONNECTION_PORT = "connection.port";
    private static final String KEY_CONNECTION_USERNAME = "connection.username";
    private static final String KEY_CONNECTION_SERIAL_PORT = "connection.serialPort";
    private static final String KEY_CONNECTION_BAUD_RATE = "connection.baudRate";

    private UserPreferences() {
    }

    public static WindowState loadWindowState(double defaultWidth, double defaultHeight) {
        return new WindowState(
            PREFS.getDouble(KEY_WINDOW_X, Double.NaN),
            PREFS.getDouble(KEY_WINDOW_Y, Double.NaN),
            PREFS.getDouble(KEY_WINDOW_WIDTH, defaultWidth),
            PREFS.getDouble(KEY_WINDOW_HEIGHT, defaultHeight),
            PREFS.getBoolean(KEY_WINDOW_MAXIMIZED, false)
        );
    }

    public static void saveWindowState(WindowState state) {
        PREFS.putDouble(KEY_WINDOW_X, state.x());
        PREFS.putDouble(KEY_WINDOW_Y, state.y());
        PREFS.putDouble(KEY_WINDOW_WIDTH, state.width());
        PREFS.putDouble(KEY_WINDOW_HEIGHT, state.height());
        PREFS.putBoolean(KEY_WINDOW_MAXIMIZED, state.maximized());
    }

    public static ConnectionState loadConnectionState() {
        ConnectionConfig defaults = new ConnectionConfig();
        ConnectionConfig.ConnectionType type = defaults.getType();
        String savedType = PREFS.get(KEY_CONNECTION_TYPE, type.name());
        try {
            type = ConnectionConfig.ConnectionType.valueOf(savedType);
        } catch (IllegalArgumentException ignored) {
            // keep defaults
        }

        return new ConnectionState(
            type,
            PREFS.get(KEY_CONNECTION_HOST, defaults.getHost()),
            PREFS.getInt(KEY_CONNECTION_PORT, defaults.getPort()),
            PREFS.get(KEY_CONNECTION_USERNAME, defaults.getUsername()),
            PREFS.get(KEY_CONNECTION_SERIAL_PORT, defaults.getSerialPort()),
            PREFS.getInt(KEY_CONNECTION_BAUD_RATE, defaults.getBaudRate())
        );
    }

    public static void saveConnectionState(ConnectionState state) {
        PREFS.put(KEY_CONNECTION_TYPE, state.type().name());
        PREFS.put(KEY_CONNECTION_HOST, state.host() == null ? "" : state.host());
        PREFS.putInt(KEY_CONNECTION_PORT, state.port());
        PREFS.put(KEY_CONNECTION_USERNAME, state.username() == null ? "" : state.username());
        PREFS.put(KEY_CONNECTION_SERIAL_PORT, state.serialPort() == null ? "" : state.serialPort());
        PREFS.putInt(KEY_CONNECTION_BAUD_RATE, state.baudRate());
    }

    public record WindowState(double x, double y, double width, double height, boolean maximized) {
    }

    public record ConnectionState(ConnectionConfig.ConnectionType type, String host, int port, String username,
                                  String serialPort, int baudRate) {
    }
}
