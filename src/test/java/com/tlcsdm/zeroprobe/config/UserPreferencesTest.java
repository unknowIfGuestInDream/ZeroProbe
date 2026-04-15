package com.tlcsdm.zeroprobe.config;

import com.tlcsdm.zeroprobe.model.ConnectionConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserPreferencesTest {

    @Test
    void testSaveAndLoadWindowState() {
        UserPreferences.WindowState expected = new UserPreferences.WindowState(111.0, 222.0, 933.0, 744.0, true);
        UserPreferences.saveWindowState(expected);

        UserPreferences.WindowState actual = UserPreferences.loadWindowState(900, 700);
        assertEquals(expected.x(), actual.x());
        assertEquals(expected.y(), actual.y());
        assertEquals(expected.width(), actual.width());
        assertEquals(expected.height(), actual.height());
        assertEquals(expected.maximized(), actual.maximized());
    }

    @Test
    void testSaveAndLoadConnectionState() {
        UserPreferences.ConnectionState expected = new UserPreferences.ConnectionState(
            ConnectionConfig.ConnectionType.SERIAL,
            "192.168.8.1",
            2222,
            "admin",
            "COM9",
            57600
        );
        UserPreferences.saveConnectionState(expected);

        UserPreferences.ConnectionState actual = UserPreferences.loadConnectionState();
        assertEquals(expected.type(), actual.type());
        assertEquals(expected.host(), actual.host());
        assertEquals(expected.port(), actual.port());
        assertEquals(expected.username(), actual.username());
        assertEquals(expected.serialPort(), actual.serialPort());
        assertEquals(expected.baudRate(), actual.baudRate());
    }
}
