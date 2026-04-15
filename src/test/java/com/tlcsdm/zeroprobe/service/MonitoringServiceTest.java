package com.tlcsdm.zeroprobe.service;

import com.tlcsdm.zeroprobe.model.ConnectionConfig;
import com.tlcsdm.zeroprobe.transport.ConnectionProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for MonitoringService.
 */
class MonitoringServiceTest {

    @Test
    void testStopDoesNotReportErrorsDuringDisconnect() throws Exception {
        FakeConnectionProvider provider = new FakeConnectionProvider();
        MonitoringService service = new MonitoringService(provider);
        List<String> errors = new CopyOnWriteArrayList<>();

        try {
            service.setMonitoredPid(11);
            service.setOnError(errors::add);

            service.start(1000);
            assertTrue(provider.awaitProcessListStarted(), "Process list collection should start");

            service.stop();
            provider.disconnect();

            TimeUnit.MILLISECONDS.sleep(200);

            assertTrue(errors.isEmpty(), "Expected no error callback during shutdown");
            assertFalse(provider.processDetailCommandCalled.get(),
                "Process detail collection should stop after interruption");
        } finally {
            service.shutdown();
        }
    }

    private static final class FakeConnectionProvider implements ConnectionProvider {
        private final CountDownLatch processListStarted = new CountDownLatch(1);
        private final AtomicBoolean connected = new AtomicBoolean(true);
        private final AtomicBoolean processDetailCommandCalled = new AtomicBoolean(false);

        @Override
        public void connect(ConnectionConfig config) {
            connected.set(true);
        }

        @Override
        public String executeCommand(String command) throws Exception {
            if (!connected.get()) {
                throw new IllegalStateException("Not connected");
            }
            if ("cat /proc/stat".equals(command)) {
                return "cpu  1 2 3 4 5 6 7 0 0 0\n";
            }
            if ("cat /proc/meminfo".equals(command)) {
                return """
                    MemTotal:       1000 kB
                    MemFree:         500 kB
                    MemAvailable:    600 kB
                    Buffers:          20 kB
                    Cached:           30 kB
                    """;
            }
            if (command.startsWith("for d in /proc/[0-9]*")) {
                processListStarted.countDown();
                Thread.sleep(5000);
                return "";
            }
            if (command.startsWith("cat /proc/11/")) {
                processDetailCommandCalled.set(true);
                return "";
            }
            return "";
        }

        @Override
        public boolean isConnected() {
            return connected.get();
        }

        @Override
        public void disconnect() {
            connected.set(false);
        }

        private boolean awaitProcessListStarted() throws InterruptedException {
            return processListStarted.await(2, TimeUnit.SECONDS);
        }
    }
}
