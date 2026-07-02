package org.agty.torchanger.torchanger;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TorchangerController implements TorManagerListener {
    @FXML private Label titleLabel;
    @FXML private Button updateBridgesButton;
    @FXML private Button settingsButton;
    @FXML private Button exitButton;
    @FXML private Button startAllButton;
    @FXML private Button stopAllButton;
    @FXML private Button restartAllButton;
    @FXML private TableView<TorInstanceSpec> methodsTable;
    @FXML private TableColumn<TorInstanceSpec, String> methodColumn;
    @FXML private TableColumn<TorInstanceSpec, Integer> socksColumn;
    @FXML private TableColumn<TorInstanceSpec, Integer> httpColumn;
    @FXML private TableColumn<TorInstanceSpec, String> statusColumn;
    @FXML private TableColumn<TorInstanceSpec, String> pingColumn;
    @FXML private TableColumn<TorInstanceSpec, String> avgColumn;
    @FXML private TableColumn<TorInstanceSpec, Boolean> autoStartColumn;
    @FXML private TableColumn<TorInstanceSpec, Void> actionsColumn;
    @FXML private Label logsLabel;
    @FXML private TabPane logsTabPane;
    @FXML private TextArea overallLogArea;

    private final ObservableList<TorInstanceSpec> specs = FXCollections.observableArrayList();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, TextArea> instanceLogs = new HashMap<>();
    private final TrayManager trayManager = new TrayManager();
    private final Path workspaceRoot = AppEnvironment.workspaceRoot();
    private SettingsManager settingsManager;
    private AppSettings settings;
    private ResourceBundle bundle;
    private TorManager torManager;
    private Stage stage;
    private boolean startupSequenceStarted;

    @FXML
    public void initialize() {
        settingsManager = new SettingsManager(workspaceRoot);
        try {
            settings = settingsManager.load();
        } catch (IOException e) {
            settings = AppSettings.defaults();
        }
        bundle = I18n.bundle(settings.language());
        torManager = new TorManager(workspaceRoot, this, () -> settings);

        specs.setAll(TorInstanceSpec.defaults());
        applyPortSelection();
        applyAutoStartSelection();
        applyBridgeSelection();

        methodColumn.setCellValueFactory(cell -> new SimpleStringProperty(localizeMethod(cell.getValue())));
        socksColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().socksPort()).asObject());
        httpColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().httpPort()).asObject());
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(localizeStatus(cell.getValue().status())));
        pingColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().ping()));
        avgColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().average()));
        autoStartColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleBooleanProperty(cell.getValue().autoStart()).asObject());
        methodsTable.setItems(specs);
        methodsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        configureAutoStartColumn();
        configureActionColumns();
        configureTableContextMenu();
        createLogTabs();
        applyTexts();
        installLogContextMenu(overallLogArea, null);

        try {
            torManager.prepareWorkspace();
            onOverallLog("Workspace ready: " + workspaceRoot.toAbsolutePath());
            onOverallLog("Settings profile: " + AppEnvironment.profileName());
            onOverallLog("Bridge sources file: " + torManager.getBridgeCacheDir().resolve("sources.txt").toAbsolutePath());
        } catch (IOException e) {
            onOverallLog("Workspace preparation failed: " + e.getMessage());
        }
    }

    public void attachStage(Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(event -> {
            if (settings.minimizeToTrayOnClose()) {
                event.consume();
                trayManager.hideStage(stage);
                trayManager.showMessage("Torchanger", bundle.getString("tray.minimized"));
            } else {
                shutdown();
            }
        });
        installTray();
        if (settings.minimizeToTrayOnStartup()) {
            Platform.runLater(() -> trayManager.hideStage(stage));
        }
        startStartupSequence();
    }

    @FXML
    private void onUpdateBridgesClick() {
        updateBridgesButton.setDisable(true);
        if (!logsTabPane.getTabs().isEmpty()) {
            logsTabPane.getSelectionModel().select(0);
        }
        onOverallLog("Bridge update started");
        onOverallLog("Bridge cache directory: " + torManager.getBridgeCacheDir().toAbsolutePath());
        onOverallLog("Bridge source catalog file: " + torManager.getBridgeCacheDir().resolve("sources.txt").toAbsolutePath());
        executor.submit(() -> {
            try {
                torManager.updateBridgeCache();
                onOverallLog(bundle.getString("bridges.updated"));
                onOverallLog("Bridge update finished");
            } catch (Exception e) {
                onOverallLog(bundle.getString("bridges.updateFailed") + ": " + e.getMessage());
            } finally {
                Platform.runLater(() -> updateBridgesButton.setDisable(false));
            }
        });
    }

    @FXML
    private void onSettingsClick() {
        SettingsDialog.show(stage, settings, bundle, AppEnvironment.profileName(), workspaceRoot).ifPresent(updated -> {
            settings = updated;
            applyPortSelection();
            applyAutoStartSelection();
            bundle = I18n.bundle(settings.language());
            applyBridgeSelection();
            applyTexts();
            refreshLogContextMenus();
            refreshLogTabTitles();
            methodsTable.refresh();
            onOverallLog("Settings profile: " + AppEnvironment.profileName());
            onOverallLog("Settings storage: " + workspaceRoot.toAbsolutePath());
            if (torManager.activeProcessCount() > 0) {
                onOverallLog("Port changes apply to newly started or restarted connections");
            }
            try {
                settingsManager.save(settings);
                settingsManager.applyAutostart(settings, Path.of("").toAbsolutePath());
                trayManager.uninstall();
                installTray();
            } catch (IOException e) {
                onOverallLog("Settings save failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onStartAllClick() {
        specs.forEach(this::resetMetrics);
        executor.submit(() -> torManager.startAll(List.copyOf(specs)));
    }

    @FXML
    private void onStopAllClick() {
        executor.submit(() -> torManager.stopAll("ui:stop-all-button"));
    }

    @FXML
    private void onRestartAllClick() {
        specs.forEach(this::resetMetrics);
        executor.submit(() -> torManager.restartAll(List.copyOf(specs)));
    }

    @FXML
    private void onExitClick() {
        exitApplication();
    }

    @Override
    public void onOverallLog(String message) {
        Platform.runLater(() -> append(overallLogArea, message));
    }

    @Override
    public void onInstanceLog(String methodName, String message) {
        Platform.runLater(() -> {
            TextArea area = instanceLogs.get(methodName);
            if (area != null) {
                append(area, message);
            }
        });
    }

    @Override
    public void onStatus(String methodName, String status) {
        Platform.runLater(() -> {
            specs.stream().filter(spec -> spec.name().equals(methodName)).findFirst().ifPresent(spec -> spec.setStatus(status));
            methodsTable.refresh();
        });
    }

    @Override
    public void onMetrics(String methodName, String ping, String average) {
        Platform.runLater(() -> {
            specs.stream().filter(spec -> spec.name().equals(methodName)).findFirst().ifPresent(spec -> {
                spec.setPing(ping);
                spec.setAverage(average);
            });
            methodsTable.refresh();
        });
    }

    public void shutdown() {
        trayManager.uninstall();
        torManager.shutdown();
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void exitApplication() {
        shutdown();
        Platform.exit();
    }

    private void applyBridgeSelection() {
        specs.stream().filter(spec -> spec.mode() == TorLaunchMode.VANILLA_BRIDGE).findFirst()
                .ifPresent(spec -> spec.setBridgeEntry(BridgeCatalog.findByFileName(settings.vanillaBridgeFile())));
        specs.stream().filter(spec -> spec.mode() == TorLaunchMode.OBFS4).findFirst()
                .ifPresent(spec -> spec.setBridgeEntry(BridgeCatalog.findByFileName(settings.obfs4BridgeFile())));
        specs.stream().filter(spec -> spec.mode() == TorLaunchMode.WEBTUNNEL).findFirst()
                .ifPresent(spec -> spec.setBridgeEntry(BridgeCatalog.findByFileName(settings.webtunnelBridgeFile())));
    }

    private void applyPortSelection() {
        specs.stream().filter(spec -> spec.id().equals("direct")).findFirst().ifPresent(spec -> {
            spec.setSocksPort(settings.directSocksPort());
            spec.setHttpPort(settings.directHttpPort());
        });
        specs.stream().filter(spec -> spec.id().equals("vanilla-bridges")).findFirst().ifPresent(spec -> {
            spec.setSocksPort(settings.vanillaSocksPort());
            spec.setHttpPort(settings.vanillaHttpPort());
        });
        specs.stream().filter(spec -> spec.id().equals("obfs4-bridges")).findFirst().ifPresent(spec -> {
            spec.setSocksPort(settings.obfs4SocksPort());
            spec.setHttpPort(settings.obfs4HttpPort());
        });
        specs.stream().filter(spec -> spec.id().equals("snowflake")).findFirst().ifPresent(spec -> {
            spec.setSocksPort(settings.snowflakeSocksPort());
            spec.setHttpPort(settings.snowflakeHttpPort());
        });
        specs.stream().filter(spec -> spec.id().equals("webtunnel")).findFirst().ifPresent(spec -> {
            spec.setSocksPort(settings.webtunnelSocksPort());
            spec.setHttpPort(settings.webtunnelHttpPort());
        });
    }

    private void applyAutoStartSelection() {
        specs.stream().filter(spec -> spec.id().equals("direct")).findFirst().ifPresent(spec -> spec.setAutoStart(settings.directAutoStart()));
        specs.stream().filter(spec -> spec.id().equals("vanilla-bridges")).findFirst().ifPresent(spec -> spec.setAutoStart(settings.vanillaAutoStart()));
        specs.stream().filter(spec -> spec.id().equals("obfs4-bridges")).findFirst().ifPresent(spec -> spec.setAutoStart(settings.obfs4AutoStart()));
        specs.stream().filter(spec -> spec.id().equals("snowflake")).findFirst().ifPresent(spec -> spec.setAutoStart(settings.snowflakeAutoStart()));
        specs.stream().filter(spec -> spec.id().equals("webtunnel")).findFirst().ifPresent(spec -> spec.setAutoStart(settings.webtunnelAutoStart()));
    }

    private void applyTexts() {
        if (stage != null) {
            stage.setTitle(AppInfo.displayName());
        }
        titleLabel.setText(AppInfo.displayName());
        updateBridgesButton.setText(bundle.getString("button.updateBridges"));
        startAllButton.setText(bundle.getString("button.startAll"));
        stopAllButton.setText(bundle.getString("button.stopAll"));
        restartAllButton.setText(bundle.getString("button.restartAll"));
        settingsButton.setText(bundle.getString("button.settings"));
        exitButton.setText("⏻");
        methodColumn.setText(bundle.getString("table.method"));
        socksColumn.setText(bundle.getString("table.socks"));
        httpColumn.setText(bundle.getString("table.http"));
        statusColumn.setText(bundle.getString("table.status"));
        pingColumn.setText(bundle.getString("table.ping"));
        avgColumn.setText(bundle.getString("table.avg"));
        autoStartColumn.setText(bundle.getString("table.autoStart"));
        actionsColumn.setText(bundle.getString("table.actions"));
        logsLabel.setText(bundle.getString("logs.title"));
        if (!logsTabPane.getTabs().isEmpty()) {
            logsTabPane.getTabs().get(0).setText(bundle.getString("logs.overall"));
        }
        refreshLogTabTitles();
    }

    private void configureAutoStartColumn() {
        autoStartColumn.setCellFactory(tableColumn -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    TorInstanceSpec spec = getTableView().getItems().get(getIndex());
                    boolean selected = checkBox.isSelected();
                    spec.setAutoStart(selected);
                    applyAutoStartSetting(spec.id(), selected);
                    try {
                        settingsManager.save(settings);
                        onOverallLog("Auto-start updated for " + spec.name() + ": " + selected);
                    } catch (IOException e) {
                        onOverallLog("Settings save failed: " + e.getMessage());
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                TorInstanceSpec spec = getTableView().getItems().get(getIndex());
                checkBox.setSelected(spec.autoStart());
                setGraphic(checkBox);
            }
        });
    }

    private void configureActionColumns() {
        actionsColumn.setCellFactory(tableColumn -> new TableCell<>() {
            private final Button startButton = new Button("▶");
            private final Button stopButton = new Button("■");
            private final Button restartButton = new Button("↻");
            private final HBox box = new HBox(4, startButton, stopButton, restartButton);
            {
                box.getStyleClass().add("actions-box");
                startButton.getStyleClass().add("icon-button");
                stopButton.getStyleClass().add("icon-button");
                restartButton.getStyleClass().add("icon-button");
                startButton.setOnAction(event -> {
                    TorInstanceSpec spec = getTableView().getItems().get(getIndex());
                    resetMetrics(spec);
                    torManager.startInstance(spec);
                });
                stopButton.setOnAction(event -> torManager.stopInstance(getTableView().getItems().get(getIndex()), "ui:row-stop-button"));
                restartButton.setOnAction(event -> {
                    TorInstanceSpec spec = getTableView().getItems().get(getIndex());
                    resetMetrics(spec);
                    torManager.restartInstance(spec, "ui:row-restart-button");
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void configureTableContextMenu() {
        methodsTable.setRowFactory(tableView -> {
            TableRow<TorInstanceSpec> row = new TableRow<>();
            row.itemProperty().addListener((observable, oldSpec, spec) -> {
                if (spec == null) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(buildTableContextMenu(spec));
                }
            });
            return row;
        });
    }

    private void createLogTabs() {
        logsTabPane.getTabs().clear();
        logsTabPane.getTabs().add(new Tab(bundle.getString("logs.overall"), overallLogArea));
        overallLogArea.setEditable(false);
        overallLogArea.setWrapText(true);
        overallLogArea.setVisible(true);
        overallLogArea.setManaged(true);
        for (TorInstanceSpec spec : specs) {
            TextArea area = new TextArea();
            area.setEditable(false);
            area.setWrapText(true);
            installLogContextMenu(area, spec);
            instanceLogs.put(spec.name(), area);
            logsTabPane.getTabs().add(new Tab(localizeMethod(spec), area));
        }
    }

    private void refreshLogTabTitles() {
        for (int i = 1; i < logsTabPane.getTabs().size() && i - 1 < specs.size(); i++) {
            logsTabPane.getTabs().get(i).setText(localizeMethod(specs.get(i - 1)));
        }
    }

    private void refreshLogContextMenus() {
        installLogContextMenu(overallLogArea, null);
        for (TorInstanceSpec spec : specs) {
            TextArea area = instanceLogs.get(spec.name());
            if (area != null) {
                installLogContextMenu(area, spec);
            }
        }
    }

    private void startStartupSequence() {
        if (startupSequenceStarted) {
            return;
        }
        startupSequenceStarted = true;
        executor.submit(() -> {
            logDependencyCheck();
            updateBridgesOnStartup();
            startAutoProfiles();
        });
    }

    private void logDependencyCheck() {
        onOverallLog("Dependency check started");
        DependencyChecker.checkAll().forEach((binary, present) ->
                onOverallLog("Dependency " + binary + ": " + (present ? "OK" : "MISSING")));
        onOverallLog("Dependency check finished");
    }

    private void updateBridgesOnStartup() {
        if (!settings.updateBridgesOnStartup()) {
            onOverallLog("Startup bridge update disabled");
            return;
        }
        try {
            onOverallLog("Startup bridge update started");
            torManager.updateBridgeCache();
            onOverallLog("Startup bridge update finished");
        } catch (Exception e) {
            onOverallLog("Startup bridge update failed: " + e.getMessage());
        }
    }

    private void startAutoProfiles() {
        List<TorInstanceSpec> autoProfiles = specs.stream().filter(TorInstanceSpec::autoStart).toList();
        if (autoProfiles.isEmpty()) {
            onOverallLog("No auto-start profiles enabled");
            return;
        }
        onOverallLog("Auto-start profiles: " + autoProfiles.stream().map(TorInstanceSpec::name).toList());
        autoProfiles.forEach(this::resetMetrics);
        torManager.startAll(autoProfiles);
    }

    private void applyAutoStartSetting(String id, boolean value) {
        switch (id) {
            case "direct" -> settings.setDirectAutoStart(value);
            case "vanilla-bridges" -> settings.setVanillaAutoStart(value);
            case "obfs4-bridges" -> settings.setObfs4AutoStart(value);
            case "snowflake" -> settings.setSnowflakeAutoStart(value);
            case "webtunnel" -> settings.setWebtunnelAutoStart(value);
            default -> {
            }
        }
    }

    private void installTray() {
        if (stage != null) {
            trayManager.install(
                    stage,
                    bundle.getString("tray.open"),
                    bundle.getString("tray.hide"),
                    bundle.getString("tray.startAll"),
                    bundle.getString("tray.stopAll"),
                    bundle.getString("tray.restart"),
                    bundle.getString("tray.updateBridges"),
                    bundle.getString("tray.settings"),
                    bundle.getString("tray.logsFolder"),
                    bundle.getString("tray.exit"),
                    () -> trayManager.hideStage(stage),
                    this::onStartAllClick,
                    this::onStopAllClick,
                    this::onRestartAllClick,
                    this::onUpdateBridgesClick,
                    this::onSettingsClick,
                    this::openLogsFolder,
                    this::exitApplication
            );
        }
    }

    private void installLogContextMenu(TextArea area, TorInstanceSpec spec) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("app-context-menu");

        MenuItem copySelectionItem = new MenuItem(bundle.getString("menu.copySelection"));
        copySelectionItem.setOnAction(event -> area.copy());

        MenuItem copyAllItem = new MenuItem(bundle.getString("menu.copyAll"));
        copyAllItem.setOnAction(event -> {
            area.selectAll();
            area.copy();
            area.deselect();
        });

        MenuItem selectAllItem = new MenuItem(bundle.getString("menu.selectAll"));
        selectAllItem.setOnAction(event -> area.selectAll());

        MenuItem clearViewItem = new MenuItem(bundle.getString("menu.clearView"));
        clearViewItem.setOnAction(event -> area.clear());

        MenuItem scrollBottomItem = new MenuItem(bundle.getString("menu.scrollBottom"));
        scrollBottomItem.setOnAction(event -> area.positionCaret(area.getLength()));

        MenuItem openLogsFolderItem = new MenuItem(bundle.getString("menu.openLogsFolder"));
        openLogsFolderItem.setOnAction(event -> openLogsFolder());

        menu.getItems().addAll(copySelectionItem, copyAllItem, selectAllItem, scrollBottomItem, clearViewItem, openLogsFolderItem);

        if (spec != null) {
            MenuItem startItem = new MenuItem(bundle.getString("menu.startMethod").formatted(localizeMethod(spec)));
            startItem.setOnAction(event -> {
                resetMetrics(spec);
                torManager.startInstance(spec);
            });
            MenuItem stopItem = new MenuItem(bundle.getString("menu.stopMethod").formatted(localizeMethod(spec)));
            stopItem.setOnAction(event -> torManager.stopInstance(spec, "ui:log-context-stop"));
            MenuItem restartItem = new MenuItem(bundle.getString("menu.restartMethod").formatted(localizeMethod(spec)));
            restartItem.setOnAction(event -> {
                resetMetrics(spec);
                torManager.restartInstance(spec, "ui:log-context-restart");
            });
            menu.getItems().addAll(startItem, stopItem, restartItem);
        } else {
            MenuItem startAllItem = new MenuItem(bundle.getString("menu.startAll"));
            startAllItem.setOnAction(event -> onStartAllClick());
            MenuItem stopAllItem = new MenuItem(bundle.getString("menu.stopAll"));
            stopAllItem.setOnAction(event -> onStopAllClick());
            MenuItem restartAllItem = new MenuItem(bundle.getString("menu.restartAll"));
            restartAllItem.setOnAction(event -> onRestartAllClick());
            menu.getItems().addAll(startAllItem, stopAllItem, restartAllItem);
        }

        area.setContextMenu(menu);
    }

    private ContextMenu buildTableContextMenu(TorInstanceSpec spec) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("app-context-menu");

        MenuItem startItem = new MenuItem(bundle.getString("menu.startMethod").formatted(localizeMethod(spec)));
        startItem.setOnAction(event -> {
            resetMetrics(spec);
            torManager.startInstance(spec);
        });

        MenuItem stopItem = new MenuItem(bundle.getString("menu.stopMethod").formatted(localizeMethod(spec)));
        stopItem.setOnAction(event -> torManager.stopInstance(spec, "ui:table-context-stop"));

        MenuItem restartItem = new MenuItem(bundle.getString("menu.restartMethod").formatted(localizeMethod(spec)));
        restartItem.setOnAction(event -> {
            resetMetrics(spec);
            torManager.restartInstance(spec, "ui:table-context-restart");
        });

        MenuItem copySocksItem = new MenuItem(bundle.getString("menu.copySocksAddress"));
        copySocksItem.setOnAction(event -> copyToClipboard("127.0.0.1:" + spec.socksPort(), "SOCKS5 address copied"));

        MenuItem copyHttpItem = new MenuItem(bundle.getString("menu.copyHttpAddress"));
        copyHttpItem.setOnAction(event -> copyToClipboard("127.0.0.1:" + spec.httpPort(), "HTTP address copied"));

        MenuItem copySourceItem = new MenuItem(bundle.getString("menu.copyBridgeSource"));
        copySourceItem.setOnAction(event -> copyToClipboard(spec.bridgeSourceDescription(), "Bridge source copied"));

        MenuItem copyBridgeItem = new MenuItem(bundle.getString("menu.copySuccessfulBridge"));
        copyBridgeItem.setOnAction(event -> {
            var bridgeLine = torManager.getSuccessfulBridgeLine(spec);
            if (bridgeLine.isPresent()) {
                copyToClipboard(bridgeLine.get(), "Successful bridge copied");
            } else {
                onOverallLog(spec.name() + ": no successful bridge available to copy");
            }
        });
        copyBridgeItem.setDisable(spec.mode() == TorLaunchMode.DIRECT);

        menu.getItems().addAll(
                startItem,
                stopItem,
                restartItem,
                new SeparatorMenuItem(),
                copySocksItem,
                copyHttpItem,
                copySourceItem,
                copyBridgeItem
        );
        return menu;
    }

    private void openLogsFolder() {
        Path logsDir = workspaceRoot.resolve("logs").toAbsolutePath();
        try {
            if (!Desktop.isDesktopSupported()) {
                onOverallLog("Desktop API is not supported");
                return;
            }
            Desktop.getDesktop().open(logsDir.toFile());
        } catch (IOException e) {
            onOverallLog("Open logs folder failed: " + e.getMessage());
        }
    }

    private String localizeMethod(TorInstanceSpec spec) {
        return bundle.getString("method." + spec.id());
    }

    private String localizeStatus(String status) {
        String key = "status." + status.replace(' ', '_');
        return bundle.containsKey(key) ? bundle.getString(key) : status;
    }

    private void append(TextArea area, String message) {
        if (!area.getText().isEmpty()) {
            area.appendText(System.lineSeparator());
        }
        area.appendText(message);
    }

    private void copyToClipboard(String text, String logMessage) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        onOverallLog(logMessage);
    }

    private void resetMetrics(TorInstanceSpec spec) {
        spec.setPing("-");
        spec.setAverage("-");
    }
}
