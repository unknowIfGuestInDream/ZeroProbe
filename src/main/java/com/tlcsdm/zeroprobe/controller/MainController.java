package com.tlcsdm.zeroprobe.controller;

import com.tlcsdm.zeroprobe.ZeroProbeApplication;
import com.tlcsdm.zeroprobe.config.AppSettings;
import com.tlcsdm.zeroprobe.config.I18N;
import com.tlcsdm.zeroprobe.config.UserPreferences;
import com.tlcsdm.zeroprobe.export.CsvDataExporter;
import com.tlcsdm.zeroprobe.export.DataExporter;
import com.fazecast.jSerialComm.SerialPort;
import com.tlcsdm.zeroprobe.model.ConnectionConfig;
import com.tlcsdm.zeroprobe.model.CpuInfo;
import com.tlcsdm.zeroprobe.model.MemoryInfo;
import com.tlcsdm.zeroprobe.model.ProcessInfo;
import com.tlcsdm.zeroprobe.model.TimeRange;
import com.tlcsdm.zeroprobe.service.MonitoringService;
import com.tlcsdm.zeroprobe.transport.ConnectionProvider;
import com.tlcsdm.zeroprobe.transport.SerialConnectionProvider;
import com.tlcsdm.zeroprobe.transport.SshConnectionProvider;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main controller for the ZeroProbe application.
 */
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
    private static final TimeRange DEFAULT_TIME_RANGE = TimeRange.MIN_1;

    // Window controls
    @FXML
    private Label statusLabel;
    @FXML
    private Label windowTitleLabel;
    @FXML
    private Button maximizeButton;
    @FXML
    private Button closeButton;

    // Tab pane
    @FXML
    private TabPane mainTabPane;

    // Connection tab
    @FXML
    private ToggleGroup connectionTypeGroup;
    @FXML
    private RadioButton sshRadio;
    @FXML
    private RadioButton serialRadio;
    @FXML
    private VBox sshFieldsPane;
    @FXML
    private VBox serialFieldsPane;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> serialPortCombo;
    @FXML
    private ComboBox<String> baudRateCombo;
    @FXML
    private Button connectButton;
    @FXML
    private Button disconnectButton;
    @FXML
    private Label connectionStatusLabel;

    // Monitor tab
    @FXML
    private LineChart<Number, Number> cpuChart;
    @FXML
    private NumberAxis cpuTimeAxis;
    @FXML
    private NumberAxis cpuUsageAxis;
    @FXML
    private LineChart<Number, Number> memoryChart;
    @FXML
    private NumberAxis memoryTimeAxis;
    @FXML
    private NumberAxis memoryUsageAxis;
    @FXML
    private Label currentCpuLabel;
    @FXML
    private Label currentMemoryLabel;
    @FXML
    private ComboBox<TimeRange> timeRangeCombo;

    // Process tab
    @FXML
    private TextField pidField;
    @FXML
    private Button monitorProcessButton;
    @FXML
    private Label processNameLabel;
    @FXML
    private Label processStateLabel;
    @FXML
    private Label processThreadsLabel;
    @FXML
    private Label processVmRssLabel;
    @FXML
    private Label processCpuTimeLabel;

    // Recording tab
    @FXML
    private TextField recordingPathField;
    @FXML
    private Button startRecordingButton;
    @FXML
    private Button stopRecordingButton;
    @FXML
    private Label recordingStatusLabel;
    @FXML
    private Label sampleCountLabel;

    private Stage primaryStage;
    private double dragOffsetX;
    private double dragOffsetY;

    private static final String TITLE_BUTTON_HOVER_STYLE = "-fx-background-color: -color-bg-default;";
    private static final String TITLE_BUTTON_CLOSE_HOVER_STYLE =
        "-fx-background-color: -color-danger-emphasis; -fx-text-fill: -color-fg-emphasis;";
    private static final double RESIZE_MARGIN = 5;

    private ConnectionProvider connectionProvider;
    private MonitoringService monitoringService;
    private DataExporter dataExporter;
    private final AtomicInteger sampleCount = new AtomicInteger(0);
    private volatile boolean connected;
    private volatile boolean recording;

    private XYChart.Series<Number, Number> cpuSeries;
    private XYChart.Series<Number, Number> memorySeries;
    private int cpuDataIndex;
    private int memoryDataIndex;
    private volatile int maxDataPoints = DEFAULT_TIME_RANGE.getMaxDataPoints();

    private Cursor resizeCursor = Cursor.DEFAULT;
    private double resizeStartX;
    private double resizeStartY;
    private double resizeStartW;
    private double resizeStartH;
    private double resizeStartStageX;
    private double resizeStartStageY;

    @FXML
    public void initialize() {
        statusLabel.setText(I18N.get("status.ready"));

        // Connection type toggle
        connectionTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSsh = newVal == sshRadio;
            sshFieldsPane.setVisible(isSsh);
            sshFieldsPane.setManaged(isSsh);
            serialFieldsPane.setVisible(!isSsh);
            serialFieldsPane.setManaged(!isSsh);
        });

        // Populate baud rate combo
        baudRateCombo.setItems(FXCollections.observableArrayList(
            "9600", "19200", "38400", "57600", "115200"));
        baudRateCombo.setValue("115200");

        // Populate serial port combo
        refreshSerialPorts();
        serialPortCombo.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                refreshSerialPorts();
            }
        });
        serialPortCombo.setEditable(true);
        loadSavedConnectionSettings();

        // Initialize charts
        cpuSeries = new XYChart.Series<>();
        cpuSeries.setName(I18N.get("monitor.cpu"));
        cpuChart.getData().add(cpuSeries);
        cpuChart.setCreateSymbols(false);
        cpuChart.setAnimated(false);
        cpuChart.setLegendVisible(false);

        memorySeries = new XYChart.Series<>();
        memorySeries.setName(I18N.get("monitor.memory"));
        memoryChart.getData().add(memorySeries);
        memoryChart.setCreateSymbols(false);
        memoryChart.setAnimated(false);
        memoryChart.setLegendVisible(false);

        // Initialize time range combo
        timeRangeCombo.setItems(FXCollections.observableArrayList(TimeRange.values()));
        timeRangeCombo.setValue(DEFAULT_TIME_RANGE);
        timeRangeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                maxDataPoints = newVal.getMaxDataPoints();
                trimChartData(cpuSeries);
                trimChartData(memorySeries);
            }
        });
    }

    /**
     * Set the primary stage reference.
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.maximizedProperty().addListener((obs, oldVal, newVal) -> updateMaximizeButtonText());
        updateMaximizeButtonText();
        initResizeHandling();
    }

    private void initResizeHandling() {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            return;
        }

        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (primaryStage.isMaximized()) {
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            double x = event.getX();
            double y = event.getY();
            double w = scene.getWidth();
            double h = scene.getHeight();
            Cursor cursor = computeResizeCursor(x, y, w, h);
            scene.setCursor(cursor);
        });

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (primaryStage.isMaximized()) {
                return;
            }
            double x = event.getX();
            double y = event.getY();
            double w = scene.getWidth();
            double h = scene.getHeight();
            resizeCursor = computeResizeCursor(x, y, w, h);
            if (resizeCursor != Cursor.DEFAULT) {
                resizeStartX = event.getScreenX();
                resizeStartY = event.getScreenY();
                resizeStartW = primaryStage.getWidth();
                resizeStartH = primaryStage.getHeight();
                resizeStartStageX = primaryStage.getX();
                resizeStartStageY = primaryStage.getY();
                event.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (resizeCursor == Cursor.DEFAULT || primaryStage.isMaximized()) {
                return;
            }
            double dx = event.getScreenX() - resizeStartX;
            double dy = event.getScreenY() - resizeStartY;
            double minW = primaryStage.getMinWidth();
            double minH = primaryStage.getMinHeight();

            if (resizeCursor == Cursor.E_RESIZE || resizeCursor == Cursor.NE_RESIZE || resizeCursor == Cursor.SE_RESIZE) {
                primaryStage.setWidth(Math.max(minW, resizeStartW + dx));
            }
            if (resizeCursor == Cursor.S_RESIZE || resizeCursor == Cursor.SE_RESIZE || resizeCursor == Cursor.SW_RESIZE) {
                primaryStage.setHeight(Math.max(minH, resizeStartH + dy));
            }
            if (resizeCursor == Cursor.W_RESIZE || resizeCursor == Cursor.NW_RESIZE || resizeCursor == Cursor.SW_RESIZE) {
                double newW = Math.max(minW, resizeStartW - dx);
                primaryStage.setX(resizeStartStageX + resizeStartW - newW);
                primaryStage.setWidth(newW);
            }
            if (resizeCursor == Cursor.N_RESIZE || resizeCursor == Cursor.NW_RESIZE || resizeCursor == Cursor.NE_RESIZE) {
                double newH = Math.max(minH, resizeStartH - dy);
                primaryStage.setY(resizeStartStageY + resizeStartH - newH);
                primaryStage.setHeight(newH);
            }
            event.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (resizeCursor != Cursor.DEFAULT) {
                resizeCursor = Cursor.DEFAULT;
                event.consume();
            }
        });
    }

    private Cursor computeResizeCursor(double x, double y, double w, double h) {
        boolean left = x < RESIZE_MARGIN;
        boolean right = x > w - RESIZE_MARGIN;
        boolean top = y < RESIZE_MARGIN;
        boolean bottom = y > h - RESIZE_MARGIN;

        if (top && left) return Cursor.NW_RESIZE;
        if (top && right) return Cursor.NE_RESIZE;
        if (bottom && left) return Cursor.SW_RESIZE;
        if (bottom && right) return Cursor.SE_RESIZE;
        if (left) return Cursor.W_RESIZE;
        if (right) return Cursor.E_RESIZE;
        if (top) return Cursor.N_RESIZE;
        if (bottom) return Cursor.S_RESIZE;
        return Cursor.DEFAULT;
    }

    // ---- Connection handling ----

    @FXML
    public void onConnect() {
        try {
            ConnectionConfig config = buildConnectionConfig();
            if (sshRadio.isSelected()) {
                connectionProvider = new SshConnectionProvider();
            } else {
                connectionProvider = new SerialConnectionProvider();
            }

            statusLabel.setText(I18N.get("status.connecting"));
            connectButton.setDisable(true);

            Thread connectThread = new Thread(() -> {
                try {
                    connectionProvider.connect(config);
                    Platform.runLater(() -> {
                        connected = true;
                        connectButton.setDisable(true);
                        disconnectButton.setDisable(false);
                        connectionStatusLabel.setText(
                            MessageFormat.format(I18N.get("connection.status.connected"), config.toString()));
                        statusLabel.setText(I18N.get("status.connected"));
                        startMonitoring();
                    });
                } catch (Exception e) {
                    LOG.error("Connection failed", e);
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        connectionStatusLabel.setText(
                            MessageFormat.format(I18N.get("connection.status.error"), e.getMessage()));
                        statusLabel.setText(I18N.get("status.error"));
                        showErrorDialog(I18N.get("dialog.error.connection"), e.getMessage());
                    });
                }
            }, "zeroprobe-connect");
            connectThread.setDaemon(true);
            connectThread.start();
        } catch (Exception e) {
            LOG.error("Failed to create connection config", e);
            showErrorDialog(I18N.get("dialog.error.connection"), e.getMessage());
        }
    }

    @FXML
    public void onDisconnect() {
        stopMonitoring();
        if (connectionProvider != null) {
            connectionProvider.disconnect();
            connectionProvider = null;
        }
        connected = false;
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        connectionStatusLabel.setText(I18N.get("connection.status.disconnected"));
        statusLabel.setText(I18N.get("status.disconnected"));
    }

    private ConnectionConfig buildConnectionConfig() {
        ConnectionConfig config = new ConnectionConfig();
        if (sshRadio.isSelected()) {
            config.setType(ConnectionConfig.ConnectionType.SSH);
            config.setHost(hostField.getText().trim());
            config.setPort(Integer.parseInt(portField.getText().trim()));
            config.setUsername(usernameField.getText().trim());
            config.setPassword(passwordField.getText());
        } else {
            config.setType(ConnectionConfig.ConnectionType.SERIAL);
            config.setSerialPort(serialPortCombo.getValue());
            config.setBaudRate(Integer.parseInt(baudRateCombo.getValue()));
        }
        return config;
    }

    // ---- Monitoring ----

    private void startMonitoring() {
        if (connectionProvider == null) {
            return;
        }
        cpuDataIndex = 0;
        memoryDataIndex = 0;

        monitoringService = new MonitoringService(connectionProvider);
        monitoringService.setOnCpuUpdate(this::handleCpuUpdate);
        monitoringService.setOnMemoryUpdate(this::handleMemoryUpdate);
        monitoringService.setOnProcessUpdate(this::handleProcessUpdate);
        monitoringService.setOnError(error -> Platform.runLater(() ->
            statusLabel.setText(I18N.get("status.error") + ": " + error)));
        monitoringService.start();
    }

    private void stopMonitoring() {
        if (monitoringService != null) {
            monitoringService.shutdown();
            monitoringService = null;
        }
    }

    private void handleCpuUpdate(CpuInfo cpuInfo) {
        int index = cpuDataIndex++;
        Platform.runLater(() -> {
            cpuSeries.getData().add(new XYChart.Data<>(index, cpuInfo.getUsagePercent()));
            trimChartData(cpuSeries);
            currentCpuLabel.setText(
                MessageFormat.format(I18N.get("monitor.currentCpu"),
                    String.format("%.1f", cpuInfo.getUsagePercent())));
        });
        exportCpuIfRecording(cpuInfo);
    }

    private void handleMemoryUpdate(MemoryInfo memInfo) {
        int index = memoryDataIndex++;
        Platform.runLater(() -> {
            memorySeries.getData().add(new XYChart.Data<>(index, memInfo.getUsagePercent()));
            trimChartData(memorySeries);
            currentMemoryLabel.setText(
                MessageFormat.format(I18N.get("monitor.currentMemory"),
                    String.format("%.1f", memInfo.getUsagePercent())));
        });
        exportMemoryIfRecording(memInfo);
    }

    private void handleProcessUpdate(ProcessInfo procInfo) {
        Platform.runLater(() -> {
            processNameLabel.setText(procInfo.getName());
            processStateLabel.setText(procInfo.getState());
            processThreadsLabel.setText(String.valueOf(procInfo.getThreads()));
            processVmRssLabel.setText(String.valueOf(procInfo.getVmRssKb()));
            processCpuTimeLabel.setText(procInfo.getUtime() + " / " + procInfo.getStime());
        });
        exportProcessIfRecording(procInfo);
    }

    private void trimChartData(XYChart.Series<Number, Number> series) {
        int excess = series.getData().size() - maxDataPoints;
        if (excess > 0) {
            series.getData().subList(0, excess).clear();
        }
    }

    // ---- Process monitoring ----

    @FXML
    public void onMonitorProcess() {
        if (monitoringService == null) {
            return;
        }
        String pidText = pidField.getText().trim();
        if (pidText.isEmpty()) {
            return;
        }
        try {
            int pid = Integer.parseInt(pidText);
            monitoringService.setMonitoredPid(pid);
        } catch (NumberFormatException e) {
            showErrorDialog(I18N.get("dialog.error.title"), "Invalid PID: " + pidText);
        }
    }

    // ---- Recording ----

    @FXML
    public void onBrowsePath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18N.get("recording.saveTo"));
        File dir = chooser.showDialog(primaryStage);
        if (dir != null) {
            recordingPathField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    public void onStartRecording() {
        String path = recordingPathField.getText().trim();
        if (path.isEmpty()) {
            showErrorDialog(I18N.get("dialog.error.title"), "Please specify a save path.");
            return;
        }

        try {
            dataExporter = new CsvDataExporter();
            dataExporter.open(path);
            recording = true;
            sampleCount.set(0);
            startRecordingButton.setDisable(true);
            stopRecordingButton.setDisable(false);
            recordingStatusLabel.setText(I18N.get("recording.status.recording"));
            sampleCountLabel.setText(MessageFormat.format(I18N.get("recording.samples"), 0));
            statusLabel.setText(MessageFormat.format(I18N.get("recording.started"), path));
        } catch (Exception e) {
            LOG.error("Failed to start recording", e);
            showErrorDialog(I18N.get("dialog.error.title"), e.getMessage());
        }
    }

    @FXML
    public void onStopRecording() {
        recording = false;
        if (dataExporter != null) {
            try {
                dataExporter.flush();
                dataExporter.close();
            } catch (Exception e) {
                LOG.error("Error closing exporter", e);
            }
            dataExporter = null;
        }
        startRecordingButton.setDisable(false);
        stopRecordingButton.setDisable(true);
        recordingStatusLabel.setText(I18N.get("recording.status.idle"));
        int count = sampleCount.get();
        statusLabel.setText(MessageFormat.format(I18N.get("recording.stopped"), count));
    }

    private void exportCpuIfRecording(CpuInfo cpuInfo) {
        if (recording && dataExporter != null && dataExporter.isOpen()) {
            try {
                dataExporter.exportCpuInfo(cpuInfo);
                int count = sampleCount.incrementAndGet();
                Platform.runLater(() ->
                    sampleCountLabel.setText(MessageFormat.format(I18N.get("recording.samples"), count)));
            } catch (Exception e) {
                LOG.error("Failed to export CPU data", e);
            }
        }
    }

    private void exportMemoryIfRecording(MemoryInfo memInfo) {
        if (recording && dataExporter != null && dataExporter.isOpen()) {
            try {
                dataExporter.exportMemoryInfo(memInfo);
            } catch (Exception e) {
                LOG.error("Failed to export memory data", e);
            }
        }
    }

    private void exportProcessIfRecording(ProcessInfo procInfo) {
        if (recording && dataExporter != null && dataExporter.isOpen()) {
            try {
                dataExporter.exportProcessInfo(procInfo);
            } catch (Exception e) {
                LOG.error("Failed to export process data", e);
            }
        }
    }

    // ---- Settings and Window controls ----

    /**
     * Open the settings dialog.
     */
    @FXML
    public void openSettings() {
        Platform.runLater(() ->
            Window.getWindows().stream()
                .filter(w -> w instanceof Stage s && s != primaryStage && s.isShowing())
                .findFirst()
                .ifPresent(w -> {
                    Stage s = (Stage) w;
                    s.setHeight(450);
                    s.centerOnScreen();
                    Image logo = ZeroProbeApplication.loadLogo();
                    if (logo != null) {
                        s.getIcons().add(logo);
                    }
                })
        );
        AppSettings.getInstance().getPreferencesFx().show(true);
    }

    /**
     * Restart the application.
     */
    @FXML
    public void restartApplication() {
        shutdown();
        ZeroProbeApplication.restart();
    }

    /**
     * Exit the application.
     */
    @FXML
    public void exitApplication() {
        shutdown();
        closePrimaryStage();
    }

    @FXML
    public void onTitleBarMousePressed(MouseEvent event) {
        if (primaryStage == null) {
            return;
        }
        dragOffsetX = event.getScreenX() - primaryStage.getX();
        dragOffsetY = event.getScreenY() - primaryStage.getY();
    }

    @FXML
    public void onTitleBarMouseDragged(MouseEvent event) {
        if (primaryStage == null || primaryStage.isMaximized()) {
            return;
        }
        primaryStage.setX(event.getScreenX() - dragOffsetX);
        primaryStage.setY(event.getScreenY() - dragOffsetY);
    }

    @FXML
    public void onTitleBarMouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            toggleMaximizeWindow();
        }
    }

    @FXML
    public void onMinimizeWindow() {
        if (primaryStage != null) {
            primaryStage.setIconified(true);
        }
    }

    @FXML
    public void onToggleMaximizeWindow() {
        toggleMaximizeWindow();
    }

    @FXML
    public void onCloseWindow() {
        closePrimaryStage();
    }

    @FXML
    public void onWindowButtonMouseEntered(MouseEvent event) {
        if (event.getSource() instanceof Button button) {
            String baseStyle = button.getStyle();
            button.getProperties().put("baseStyle", baseStyle);
            button.setStyle(baseStyle + (button == closeButton ? TITLE_BUTTON_CLOSE_HOVER_STYLE : TITLE_BUTTON_HOVER_STYLE));
        }
    }

    @FXML
    public void onWindowButtonMouseExited(MouseEvent event) {
        if (event.getSource() instanceof Button button) {
            Object baseStyle = button.getProperties().get("baseStyle");
            button.setStyle(baseStyle instanceof String style ? style : "");
        }
    }

    /**
     * Show about dialog.
     */
    @FXML
    public void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18N.get("menu.about"));
        alert.setHeaderText(I18N.get("app.title"));
        alert.setContentText(I18N.get("about.description"));
        applyLogoToDialog(alert);
        alert.showAndWait();
    }

    /**
     * Shutdown and cleanup resources.
     */
    public void shutdown() {
        LOG.info("Application shutting down");
        saveConnectionSettings();
        stopMonitoring();
        if (connectionProvider != null) {
            connectionProvider.disconnect();
            connectionProvider = null;
        }
        if (dataExporter != null) {
            try {
                dataExporter.close();
            } catch (Exception e) {
                LOG.error("Error closing exporter during shutdown", e);
            }
            dataExporter = null;
        }
    }

    private void closePrimaryStage() {
        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    private void loadSavedConnectionSettings() {
        UserPreferences.ConnectionState state = UserPreferences.loadConnectionState();
        hostField.setText(state.host());
        portField.setText(String.valueOf(state.port()));
        usernameField.setText(state.username());
        passwordField.clear();
        serialPortCombo.setValue(state.serialPort());
        baudRateCombo.setValue(String.valueOf(state.baudRate()));
        switch (state.type()) {
            case SERIAL -> serialRadio.setSelected(true);
            case SSH -> sshRadio.setSelected(true);
        }
    }

    private void saveConnectionSettings() {
        UserPreferences.saveConnectionState(new UserPreferences.ConnectionState(
            sshRadio.isSelected() ? ConnectionConfig.ConnectionType.SSH : ConnectionConfig.ConnectionType.SERIAL,
            hostField.getText(),
            parseIntOrDefault(portField.getText(), 22),
            usernameField.getText(),
            serialPortCombo.getValue(),
            parseIntOrDefault(baudRateCombo.getValue(), 115200)
        ));
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void refreshSerialPorts() {
        String currentValue = serialPortCombo.getValue();
        var ports = Arrays.stream(SerialPort.getCommPorts())
            .map(SerialPort::getSystemPortName)
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .toList();
        var updatedItems = FXCollections.observableArrayList(ports);
        if (currentValue != null && !currentValue.isBlank()) {
            if (!updatedItems.contains(currentValue)) {
                updatedItems.add(0, currentValue);
            }
        }
        serialPortCombo.setItems(updatedItems);
        if (currentValue != null && !currentValue.isBlank()) {
            serialPortCombo.setValue(currentValue);
        } else if (!ports.isEmpty()) {
            serialPortCombo.setValue(ports.getFirst());
        }
    }

    private void toggleMaximizeWindow() {
        if (primaryStage == null) {
            return;
        }
        primaryStage.setMaximized(!primaryStage.isMaximized());
        updateMaximizeButtonText();
    }

    private void updateMaximizeButtonText() {
        if (maximizeButton != null && primaryStage != null) {
            maximizeButton.setText(primaryStage.isMaximized() ? "❐" : "□");
        }
    }

    private void showErrorDialog(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18N.get("dialog.error.title"));
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyLogoToDialog(alert);
        alert.showAndWait();
    }

    private void applyLogoToDialog(Alert alert) {
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }
    }
}
