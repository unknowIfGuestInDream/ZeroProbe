package com.tlcsdm.zeroprobe.controller;

import com.tlcsdm.zeroprobe.ZeroProbeApplication;
import com.tlcsdm.zeroprobe.config.AppSettings;
import com.tlcsdm.zeroprobe.config.I18N;
import com.tlcsdm.zeroprobe.config.UserPreferences;
import com.tlcsdm.zeroprobe.export.DataExporter;
import com.fazecast.jSerialComm.SerialPort;
import com.tlcsdm.zeroprobe.model.ConnectionConfig;
import com.tlcsdm.zeroprobe.model.CpuInfo;
import com.tlcsdm.zeroprobe.model.EnvironmentInfo;
import com.tlcsdm.zeroprobe.model.FileEntry;
import com.tlcsdm.zeroprobe.model.MemoryInfo;
import com.tlcsdm.zeroprobe.model.ProcessInfo;
import com.tlcsdm.zeroprobe.model.ProcessListInfo;
import com.tlcsdm.zeroprobe.model.ProcessListInfo.ProcessEntry;
import com.tlcsdm.zeroprobe.model.TimeRange;
import com.tlcsdm.zeroprobe.parser.EnvironmentParser;
import com.tlcsdm.zeroprobe.parser.FileSystemParser;
import com.tlcsdm.zeroprobe.service.MonitoringService;
import com.tlcsdm.zeroprobe.transport.ConnectionProvider;
import com.tlcsdm.zeroprobe.transport.SerialConnectionProvider;
import com.tlcsdm.zeroprobe.transport.SshConnectionProvider;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Section;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main controller for the ZeroProbe application.
 */
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
    private static final TimeRange DEFAULT_TIME_RANGE = TimeRange.MIN_1;
    private static final StringConverter<TimeRange> TIME_RANGE_CONVERTER = new StringConverter<>() {
        @Override
        public String toString(TimeRange range) {
            return range == null ? "" : range.toString();
        }

        @Override
        public TimeRange fromString(String string) {
            return null;
        }
    };

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
    private Label connectionStatusLabel;

    // Monitor tab
    @FXML
    private HBox cpuGaugeContainer;
    @FXML
    private HBox memoryGaugeContainer;
    @FXML
    private Label currentCpuLabel;
    @FXML
    private Label currentMemoryLabel;
    @FXML
    private Label maxCpuLabel;
    @FXML
    private Label maxMemoryLabel;

    private Gauge cpuGauge;
    private Gauge memoryGauge;
    private double maxCpuValue;
    private double maxMemoryValue;

    // Process tab
    @FXML
    private TitledPane processChartPane;
    @FXML
    private ComboBox<TimeRange> processTimeRangeCombo;
    @FXML
    private LineChart<Number, Number> processCountChart;
    @FXML
    private NumberAxis processCountTimeAxis;
    @FXML
    private NumberAxis processCountAxis;
    @FXML
    private SplitPane processSplitPane;
    @FXML
    private TextField processFilterField;
    @FXML
    private ListView<ProcessEntry> processListView;
    @FXML
    private Label processDetailHintLabel;
    @FXML
    private GridPane processDetailsGrid;
    @FXML
    private Label processPidLabel;
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
    @FXML
    private Label processPpidLabel;
    @FXML
    private Label processUidLabel;
    @FXML
    private Label processVmSizeLabel;
    @FXML
    private Label processVmPeakLabel;
    @FXML
    private Label processVoluntaryLabel;
    @FXML
    private Label processNonvoluntaryLabel;

    // Environment tab
    @FXML
    private Button envRefreshButton;
    @FXML
    private Label envStatusLabel;
    @FXML
    private TextField envHostnameField;
    @FXML
    private TextField envKernelField;
    @FXML
    private TextField envOsField;
    @FXML
    private TextField envArchField;
    @FXML
    private TextField envUptimeField;
    @FXML
    private TableView<Map.Entry<String, String>> envVarsTable;

    // File Browser tab
    @FXML
    private Button fileBrowserRefreshButton;
    @FXML
    private CheckBox fileBrowserAutoRefreshCheck;
    @FXML
    private Label fileBrowserStatusLabel;
    @FXML
    private SplitPane fileBrowserSplitPane;
    @FXML
    private TreeView<FileEntry> fileBrowserTreeView;
    @FXML
    private Label fileBrowserDetailHintLabel;
    @FXML
    private GridPane fileBrowserDetailsGrid;
    @FXML
    private Label fileBrowserNameLabel;
    @FXML
    private Label fileBrowserPathLabel;
    @FXML
    private Label fileBrowserTypeLabel;
    @FXML
    private Label fileBrowserSizeLabel;
    @FXML
    private Label fileBrowserPermissionsLabel;
    @FXML
    private Label fileBrowserOwnerLabel;
    @FXML
    private Label fileBrowserGroupLabel;
    @FXML
    private Label fileBrowserLastModifiedLabel;
    @FXML
    private HBox fileBrowserContentButtonBox;
    @FXML
    private Button fileBrowserViewContentButton;
    @FXML
    private TitledPane fileBrowserContentPane;
    @FXML
    private TextArea fileBrowserContentArea;

    // Terminal tab
    @FXML
    private TextField terminalCommandField;
    @FXML
    private Button terminalExecuteButton;
    @FXML
    private Button terminalClearButton;
    @FXML
    private Label terminalStatusLabel;
    @FXML
    private TextArea terminalOutputArea;

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

    private XYChart.Series<Number, Number> processCountSeries;
    private int cpuDataIndex;
    private int memoryDataIndex;
    private int processCountDataIndex;
    private volatile int processMaxDataPoints = DEFAULT_TIME_RANGE.getMaxDataPoints();
    private javafx.collections.ObservableList<ProcessEntry> processListSource;
    private FilteredList<ProcessEntry> filteredProcessList;

    private Cursor resizeCursor = Cursor.DEFAULT;
    private double resizeStartX;
    private double resizeStartY;
    private double resizeStartW;
    private double resizeStartH;
    private double resizeStartStageX;
    private double resizeStartStageY;

    private final FileSystemParser fileSystemParser = new FileSystemParser();
    private ScheduledExecutorService fileAutoRefreshExecutor;
    private ScheduledFuture<?> fileAutoRefreshFuture;
    private volatile FileEntry selectedFileEntry;

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

        // Initialize gauges
        cpuGauge = createGauge(I18N.get("monitor.cpu"), "%");
        cpuGaugeContainer.getChildren().add(cpuGauge);

        memoryGauge = createGauge(I18N.get("monitor.memory"), "%");
        memoryGaugeContainer.getChildren().add(memoryGauge);

        // Initialize process count chart
        processCountSeries = new XYChart.Series<>();
        processCountSeries.setName(I18N.get("process.processCount"));
        processCountChart.getData().add(processCountSeries);
        processCountChart.setCreateSymbols(false);
        processCountChart.setAnimated(false);
        processCountChart.setLegendVisible(false);

        // Initialize process time range combo
        processTimeRangeCombo.setConverter(TIME_RANGE_CONVERTER);
        processTimeRangeCombo.setItems(FXCollections.observableArrayList(TimeRange.values()));
        processTimeRangeCombo.setValue(DEFAULT_TIME_RANGE);
        updateProcessTimeAxis();
        processTimeRangeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                processMaxDataPoints = newVal.getMaxDataPoints();
                trimChartData(processCountSeries, processMaxDataPoints);
                updateProcessTimeAxis();
            }
        });

        // Initialize process list with filtering
        processListSource = FXCollections.observableArrayList();
        filteredProcessList = new FilteredList<>(processListSource, p -> true);
        processListView.setItems(filteredProcessList);
        processListView.setPlaceholder(new Label(I18N.get("process.notConnected")));
        processFilterField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredProcessList.setPredicate(entry -> {
                if (filter.isEmpty()) {
                    return true;
                }
                return entry.name().toLowerCase().contains(filter)
                    || String.valueOf(entry.pid()).contains(filter);
            });
        });

        // Process selection listener
        processListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && monitoringService != null) {
                monitoringService.setMonitoredPid(newVal.pid());
                processDetailHintLabel.setVisible(false);
                processDetailHintLabel.setManaged(false);
                processDetailsGrid.setVisible(true);
                processDetailsGrid.setManaged(true);
            }
        });

        // Initialize environment variables table columns
        initEnvironmentTable();

        // Initialize file browser
        initFileBrowser();
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

    private static final String POWER_ICON = "\u23FB  ";
    private static final String STOP_ICON = "\u23F9  ";
    private static final String FOLDER_ICON = "\uD83D\uDCC1";
    private static final String FILE_ICON = "\uD83D\uDCC4";

    private void updateConnectButtonState() {
        if (connected) {
            connectButton.setText(STOP_ICON + I18N.get("connection.disconnect"));
            connectButton.setStyle("-fx-font-size: 14; -fx-text-fill: -color-danger-fg;");
        } else {
            connectButton.setText(POWER_ICON + I18N.get("connection.connect"));
            connectButton.setStyle("-fx-font-size: 14;");
        }
    }

    @FXML
    public void onToggleConnection() {
        if (connected) {
            onDisconnect();
        } else {
            onConnect();
        }
    }

    private void onConnect() {
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
                        connectButton.setDisable(false);
                        updateConnectButtonState();
                        connectionStatusLabel.setText(
                            MessageFormat.format(I18N.get("connection.status.connected"), config.toString()));
                        statusLabel.setText(I18N.get("status.connected"));
                        startMonitoring();
                        onRefreshEnvironment();
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

    private void onDisconnect() {
        stopMonitoring();
        if (connectionProvider != null) {
            connectionProvider.disconnect();
            connectionProvider = null;
        }
        connected = false;
        connectButton.setDisable(false);
        updateConnectButtonState();
        connectionStatusLabel.setText(I18N.get("connection.status.disconnected"));
        statusLabel.setText(I18N.get("status.disconnected"));
        clearEnvironmentTab();
        clearFileBrowserTab();
        clearTerminalTab();
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
        processCountDataIndex = 0;
        maxCpuValue = 0;
        maxMemoryValue = 0;

        monitoringService = new MonitoringService(connectionProvider);
        monitoringService.setOnCpuUpdate(this::handleCpuUpdate);
        monitoringService.setOnMemoryUpdate(this::handleMemoryUpdate);
        monitoringService.setOnProcessUpdate(this::handleProcessUpdate);
        monitoringService.setOnProcessListUpdate(this::handleProcessListUpdate);
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
        cpuDataIndex++;
        double usage = cpuInfo.getUsagePercent();
        if (usage > maxCpuValue) {
            maxCpuValue = usage;
        }
        Platform.runLater(() -> {
            cpuGauge.setValue(usage);
            currentCpuLabel.setText(
                MessageFormat.format(I18N.get("monitor.currentCpu"),
                    String.format("%.1f", usage)));
            maxCpuLabel.setText(
                MessageFormat.format(I18N.get("monitor.maxCpu"),
                    String.format("%.1f", maxCpuValue)));
        });
        exportCpuIfRecording(cpuInfo);
    }

    private void handleMemoryUpdate(MemoryInfo memInfo) {
        memoryDataIndex++;
        double usage = memInfo.getUsagePercent();
        if (usage > maxMemoryValue) {
            maxMemoryValue = usage;
        }
        Platform.runLater(() -> {
            memoryGauge.setValue(usage);
            currentMemoryLabel.setText(
                MessageFormat.format(I18N.get("monitor.currentMemory"),
                    String.format("%.1f", usage)));
            maxMemoryLabel.setText(
                MessageFormat.format(I18N.get("monitor.maxMemory"),
                    String.format("%.1f", maxMemoryValue)));
        });
        exportMemoryIfRecording(memInfo);
    }

    private void handleProcessUpdate(ProcessInfo procInfo) {
        Platform.runLater(() -> {
            processPidLabel.setText(String.valueOf(procInfo.getPid()));
            processNameLabel.setText(procInfo.getName());
            processStateLabel.setText(procInfo.getState());
            processPpidLabel.setText(String.valueOf(procInfo.getPpid()));
            processUidLabel.setText(String.valueOf(procInfo.getUid()));
            processThreadsLabel.setText(String.valueOf(procInfo.getThreads()));
            processVmRssLabel.setText(String.valueOf(procInfo.getVmRssKb()));
            processVmSizeLabel.setText(String.valueOf(procInfo.getVmSizeKb()));
            processVmPeakLabel.setText(String.valueOf(procInfo.getVmPeakKb()));
            processCpuTimeLabel.setText(procInfo.getUtime() + " / " + procInfo.getStime());
            processVoluntaryLabel.setText(String.valueOf(procInfo.getVoluntaryCtxtSwitches()));
            processNonvoluntaryLabel.setText(String.valueOf(procInfo.getNonvoluntaryCtxtSwitches()));
        });
        exportProcessIfRecording(procInfo);
    }

    private void handleProcessListUpdate(ProcessListInfo listInfo) {
        int index = processCountDataIndex++;

        // Update process list first (separate from chart update for responsiveness)
        Platform.runLater(() -> {
            ProcessEntry selected = processListView.getSelectionModel().getSelectedItem();
            processListSource.setAll(listInfo.getProcesses());
            if (selected != null) {
                for (ProcessEntry entry : filteredProcessList) {
                    if (entry.pid() == selected.pid()) {
                        processListView.getSelectionModel().select(entry);
                        break;
                    }
                }
            }
        });

        // Update chart data separately to avoid blocking list/detail refresh
        Platform.runLater(() -> {
            processCountSeries.getData().add(new XYChart.Data<>(index, listInfo.getProcessCount()));
            trimChartData(processCountSeries, processMaxDataPoints);
            updateTimeAxis(processCountTimeAxis, index, processMaxDataPoints);
        });
    }

    private void trimChartData(XYChart.Series<Number, Number> series, int maxPoints) {
        int excess = series.getData().size() - maxPoints;
        if (excess > 0) {
            series.getData().subList(0, excess).clear();
        }
    }

    private static final int TICK_DIVISIONS = 6;

    private void updateTimeAxis(NumberAxis axis, int currentIndex, int range) {
        int upper = Math.max(currentIndex, range);
        axis.setLowerBound(upper - range);
        axis.setUpperBound(upper);
        axis.setTickUnit(Math.max(1, range / TICK_DIVISIONS));
    }

    private void updateProcessTimeAxis() {
        updateTimeAxis(processCountTimeAxis, processCountDataIndex, processMaxDataPoints);
    }

    private Gauge createGauge(String title, String unit) {
        return GaugeBuilder.create()
            .skinType(Gauge.SkinType.GAUGE)
            .title(title)
            .unit(unit)
            .minValue(0)
            .maxValue(100)
            .animated(true)
            .animationDuration(500)
            .startAngle(320)
            .angleRange(280)
            .sectionsVisible(true)
            .sections(
                new Section(0, 60, Color.rgb(0, 200, 0, 0.75)),
                new Section(60, 80, Color.rgb(200, 200, 0, 0.75)),
                new Section(80, 100, Color.rgb(200, 0, 0, 0.75))
            )
            .needleColor(Color.web("#333333"))
            .tickLabelColor(Color.web("#333333"))
            .titleColor(Color.web("#333333"))
            .unitColor(Color.web("#333333"))
            .valueColor(Color.web("#333333"))
            .minSize(200, 200)
            .prefSize(300, 300)
            .build();
    }

    // ---- Environment ----

    private final EnvironmentParser environmentParser = new EnvironmentParser();

    @SuppressWarnings("unchecked")
    private void initEnvironmentTable() {
        TableColumn<Map.Entry<String, String>, String> nameCol = new TableColumn<>(I18N.get("environment.envVars.name"));
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        nameCol.setPrefWidth(200);

        TableColumn<Map.Entry<String, String>, String> valueCol = new TableColumn<>(I18N.get("environment.envVars.value"));
        valueCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValue()));
        valueCol.setPrefWidth(500);

        envVarsTable.getColumns().setAll(nameCol, valueCol);
    }

    @FXML
    public void onRefreshEnvironment() {
        if (!connected || connectionProvider == null) {
            envStatusLabel.setText(I18N.get("environment.notConnected"));
            return;
        }

        envRefreshButton.setDisable(true);
        envStatusLabel.setText(I18N.get("status.connecting"));

        Thread envThread = new Thread(() -> {
            try {
                String hostname = connectionProvider.executeCommand("hostname");
                String kernel = connectionProvider.executeCommand("uname -r");
                String osRelease = connectionProvider.executeCommand("cat /etc/os-release");
                String arch = connectionProvider.executeCommand("uname -m");
                String uptime = connectionProvider.executeCommand("cat /proc/uptime");
                String envOutput = connectionProvider.executeCommand("env");

                EnvironmentInfo info = environmentParser.buildEnvironmentInfo(
                    hostname, kernel, osRelease, arch, uptime, envOutput);

                Platform.runLater(() -> {
                    envHostnameField.setText(info.getHostname());
                    envKernelField.setText(info.getKernelVersion());
                    envOsField.setText(info.getOsName());
                    envArchField.setText(info.getArchitecture());
                    envUptimeField.setText(info.getUptime());
                    envVarsTable.setItems(FXCollections.observableArrayList(
                        info.getEnvironmentVariables().entrySet()));
                    envStatusLabel.setText("");
                    envRefreshButton.setDisable(false);
                });
            } catch (Exception e) {
                LOG.error("Failed to collect environment info", e);
                Platform.runLater(() -> {
                    envStatusLabel.setText(I18N.get("status.error") + ": " + e.getMessage());
                    envRefreshButton.setDisable(false);
                });
            }
        }, "zeroprobe-env");
        envThread.setDaemon(true);
        envThread.start();
    }

    private void clearEnvironmentTab() {
        envHostnameField.setText("");
        envKernelField.setText("");
        envOsField.setText("");
        envArchField.setText("");
        envUptimeField.setText("");
        envVarsTable.getItems().clear();
        envStatusLabel.setText(I18N.get("environment.notConnected"));
    }

    // ---- File Browser ----

    private void initFileBrowser() {
        fileBrowserTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                showFileDetails(newVal.getValue());
            }
        });

        // Lazy-load children on expand
        fileBrowserTreeView.setCellFactory(tv -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(FileEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    Label icon = new Label(item.isDirectory() ? FOLDER_ICON : FILE_ICON);
                    icon.setStyle("-fx-font-size: 14;");
                    setGraphic(icon);
                }
            }
        });

        fileBrowserAutoRefreshCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                startFileAutoRefresh();
            } else {
                stopFileAutoRefresh();
            }
        });
    }

    @FXML
    public void onRefreshFileBrowser() {
        if (!connected || connectionProvider == null) {
            fileBrowserStatusLabel.setText(I18N.get("fileBrowser.notConnected"));
            return;
        }

        fileBrowserRefreshButton.setDisable(true);
        fileBrowserStatusLabel.setText(I18N.get("fileBrowser.loading"));

        Thread thread = new Thread(() -> {
            try {
                String output = connectionProvider.executeCommand("ls -la --time-style='+%Y-%m-%d %H:%M:%S' /");
                List<FileEntry> entries = fileSystemParser.parseLsOutput(output, "/");

                Platform.runLater(() -> {
                    FileEntry rootEntry = new FileEntry("/", "/", true, 0, "", "", "", "");
                    TreeItem<FileEntry> rootItem = new TreeItem<>(rootEntry);
                    rootItem.setExpanded(true);

                    for (FileEntry entry : entries) {
                        TreeItem<FileEntry> child = createTreeItem(entry);
                        rootItem.getChildren().add(child);
                    }

                    fileBrowserTreeView.setRoot(rootItem);
                    fileBrowserStatusLabel.setText("");
                    fileBrowserRefreshButton.setDisable(false);
                });
            } catch (Exception e) {
                LOG.error("Failed to load file listing", e);
                Platform.runLater(() -> {
                    fileBrowserStatusLabel.setText(I18N.get("status.error") + ": " + e.getMessage());
                    fileBrowserRefreshButton.setDisable(false);
                });
            }
        }, "zeroprobe-filebrowser");
        thread.setDaemon(true);
        thread.start();
    }

    private TreeItem<FileEntry> createTreeItem(FileEntry entry) {
        TreeItem<FileEntry> item = new TreeItem<>(entry);
        if (entry.isDirectory()) {
            // Add a dummy child so the expand arrow shows
            item.getChildren().add(new TreeItem<>());
            item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                if (isExpanded && item.getChildren().size() == 1 && item.getChildren().getFirst().getValue() == null) {
                    loadChildren(item);
                }
            });
        }
        return item;
    }

    private void loadChildren(TreeItem<FileEntry> parentItem) {
        FileEntry parentEntry = parentItem.getValue();
        if (parentEntry == null || !connected || connectionProvider == null) {
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                String safePath = parentEntry.getPath().replace("'", "'\\''");
                String output = connectionProvider.executeCommand("ls -la --time-style='+%Y-%m-%d %H:%M:%S' '" + safePath + "'");
                List<FileEntry> entries = fileSystemParser.parseLsOutput(output, parentEntry.getPath());

                Platform.runLater(() -> {
                    parentItem.getChildren().clear();
                    for (FileEntry entry : entries) {
                        parentItem.getChildren().add(createTreeItem(entry));
                    }
                });
            } catch (Exception e) {
                LOG.error("Failed to load directory: {}", parentEntry.getPath(), e);
                Platform.runLater(() -> parentItem.getChildren().clear());
            }
        }, "zeroprobe-filebrowser-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void showFileDetails(FileEntry entry) {
        selectedFileEntry = entry;
        fileBrowserDetailHintLabel.setVisible(false);
        fileBrowserDetailHintLabel.setManaged(false);
        fileBrowserDetailsGrid.setVisible(true);
        fileBrowserDetailsGrid.setManaged(true);

        fileBrowserNameLabel.setText(entry.getName());
        fileBrowserPathLabel.setText(entry.getPath());
        fileBrowserTypeLabel.setText(entry.isDirectory()
            ? I18N.get("fileBrowser.type.directory")
            : I18N.get("fileBrowser.type.file"));
        fileBrowserSizeLabel.setText(entry.getFormattedSize());
        fileBrowserPermissionsLabel.setText(normalizePermissions(entry.getPermissions()));
        fileBrowserOwnerLabel.setText(entry.getOwner());
        fileBrowserGroupLabel.setText(entry.getGroup());
        fileBrowserLastModifiedLabel.setText(entry.getLastModified());

        boolean isFile = !entry.isDirectory() && !"/".equals(entry.getPath());
        fileBrowserContentButtonBox.setVisible(isFile);
        fileBrowserContentButtonBox.setManaged(isFile);

        // Hide content pane when switching selection
        fileBrowserContentPane.setVisible(false);
        fileBrowserContentPane.setManaged(false);
        fileBrowserContentArea.clear();
        stopFileAutoRefresh();
        fileBrowserAutoRefreshCheck.setSelected(false);
    }

    @FXML
    public void onViewFileContent() {
        if (selectedFileEntry == null || selectedFileEntry.isDirectory()) {
            return;
        }
        if (!connected || connectionProvider == null) {
            return;
        }

        fileBrowserContentPane.setVisible(true);
        fileBrowserContentPane.setManaged(true);
        fileBrowserContentArea.setText(I18N.get("fileBrowser.loading"));

        Thread thread = new Thread(() -> {
            try {
                String safePath = selectedFileEntry.getPath().replace("'", "'\\''");
                String content = connectionProvider.executeCommand("cat '" + safePath + "'");
                Platform.runLater(() -> fileBrowserContentArea.setText(content));
            } catch (Exception e) {
                LOG.error("Failed to read file: {}", selectedFileEntry.getPath(), e);
                Platform.runLater(() ->
                    fileBrowserContentArea.setText(I18N.get("status.error") + ": " + e.getMessage()));
            }
        }, "zeroprobe-fileview");
        thread.setDaemon(true);
        thread.start();
    }

    private void startFileAutoRefresh() {
        stopFileAutoRefresh();
        if (selectedFileEntry == null || selectedFileEntry.isDirectory() || !connected || connectionProvider == null) {
            return;
        }

        // Show content pane if not visible
        if (!fileBrowserContentPane.isVisible()) {
            fileBrowserContentPane.setVisible(true);
            fileBrowserContentPane.setManaged(true);
        }

        fileAutoRefreshExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "zeroprobe-file-autorefresh");
            t.setDaemon(true);
            return t;
        });
        fileAutoRefreshFuture = fileAutoRefreshExecutor.scheduleWithFixedDelay(() -> {
            try {
                FileEntry current = selectedFileEntry;
                if (current == null || current.isDirectory() || !connected || connectionProvider == null) {
                    return;
                }
                String safePath = current.getPath().replace("'", "'\\''");
                String content = connectionProvider.executeCommand("cat '" + safePath + "'");
                Platform.runLater(() -> {
                    fileBrowserContentArea.setText(content);
                    fileBrowserContentArea.setScrollTop(Double.MAX_VALUE);
                });
            } catch (Exception e) {
                LOG.debug("Auto-refresh failed for file content", e);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void stopFileAutoRefresh() {
        if (fileAutoRefreshFuture != null) {
            fileAutoRefreshFuture.cancel(false);
            fileAutoRefreshFuture = null;
        }
        if (fileAutoRefreshExecutor != null) {
            fileAutoRefreshExecutor.shutdownNow();
            fileAutoRefreshExecutor = null;
        }
    }

    private void clearFileBrowserTab() {
        fileBrowserTreeView.setRoot(null);
        fileBrowserDetailHintLabel.setVisible(true);
        fileBrowserDetailHintLabel.setManaged(true);
        fileBrowserDetailsGrid.setVisible(false);
        fileBrowserDetailsGrid.setManaged(false);
        fileBrowserContentButtonBox.setVisible(false);
        fileBrowserContentButtonBox.setManaged(false);
        fileBrowserContentPane.setVisible(false);
        fileBrowserContentPane.setManaged(false);
        fileBrowserContentArea.clear();
        fileBrowserStatusLabel.setText(I18N.get("fileBrowser.notConnected"));
        stopFileAutoRefresh();
        fileBrowserAutoRefreshCheck.setSelected(false);
        selectedFileEntry = null;
    }

    // ---- Terminal ----

    @FXML
    public void onExecuteCommand() {
        String command = terminalCommandField.getText().trim();
        if (command.isEmpty()) {
            return;
        }
        if (!connected || connectionProvider == null) {
            terminalStatusLabel.setText(I18N.get("terminal.notConnected"));
            return;
        }

        terminalExecuteButton.setDisable(true);
        terminalStatusLabel.setText(I18N.get("terminal.executing"));

        ConnectionProvider provider = connectionProvider;
        Thread thread = new Thread(() -> {
            try {
                String output = provider.executeCommand(command);
                Platform.runLater(() -> {
                    terminalOutputArea.appendText("$ " + command + "\n");
                    if (output != null && !output.isEmpty()) {
                        terminalOutputArea.appendText(output);
                        if (!output.endsWith("\n")) {
                            terminalOutputArea.appendText("\n");
                        }
                    }
                    terminalOutputArea.appendText("\n");
                    terminalStatusLabel.setText("");
                    terminalExecuteButton.setDisable(false);
                    terminalCommandField.clear();
                    terminalCommandField.requestFocus();
                });
            } catch (Exception e) {
                LOG.error("Failed to execute command: {}", command, e);
                Platform.runLater(() -> {
                    terminalOutputArea.appendText("$ " + command + "\n");
                    terminalOutputArea.appendText(I18N.get("status.error") + ": " + e.getMessage() + "\n\n");
                    terminalStatusLabel.setText(I18N.get("status.error") + ": " + e.getMessage());
                    terminalExecuteButton.setDisable(false);
                });
            }
        }, "zeroprobe-terminal");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void onClearTerminal() {
        terminalOutputArea.clear();
    }

    private void clearTerminalTab() {
        terminalOutputArea.clear();
        terminalCommandField.clear();
        terminalStatusLabel.setText(I18N.get("terminal.notConnected"));
    }

    // ---- Recording (kept for future use) ----

    private void exportCpuIfRecording(CpuInfo cpuInfo) {
        if (recording && dataExporter != null && dataExporter.isOpen()) {
            try {
                dataExporter.exportCpuInfo(cpuInfo);
                sampleCount.incrementAndGet();
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
        stopFileAutoRefresh();
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

    /**
     * Normalize a permission string to standard 10-character rwx format with dashes.
     * Expects input from {@code ls -la} output (e.g., "drwxr-xr-x", "-rw-r--r--").
     * If the string is already 10 characters, it's returned as-is.
     * Shorter strings are padded assuming missing characters represent absent permissions.
     */
    private String normalizePermissions(String permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "-";
        }
        if (permissions.length() >= 10) {
            return permissions;
        }
        // If shorter than expected, pad positions with dashes
        if (permissions.length() < 10) {
            StringBuilder sb = new StringBuilder();
            char type = permissions.charAt(0);
            sb.append(type);
            String perms = permissions.substring(1);
            // Expected positions: rwxrwxrwx (9 chars)
            String expected = "rwxrwxrwx";
            int j = 0;
            for (int i = 0; i < 9; i++) {
                if (j < perms.length() && perms.charAt(j) == expected.charAt(i)) {
                    sb.append(perms.charAt(j));
                    j++;
                } else {
                    sb.append('-');
                }
            }
            return sb.toString();
        }
        return permissions;
    }
}
