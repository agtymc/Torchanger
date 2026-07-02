package org.agty.torchanger.torchanger.settings;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.agty.torchanger.torchanger.bridge.BridgeCatalog;
import org.agty.torchanger.torchanger.bridge.BridgeCatalogEntry;
import org.agty.torchanger.torchanger.config.I18n;
import org.agty.torchanger.torchanger.tor.profile.TorLaunchMode;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public final class SettingsDialog {
    private SettingsDialog() {
    }

    public static Optional<AppSettings> show(Stage owner, AppSettings current, ResourceBundle bundle, String profileName, Path workspaceRoot) {
        Dialog<AppSettings> dialog = new Dialog<>();
        dialog.setTitle(bundle.getString("settings.title"));
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(SettingsDialog.class.getResource("/org/agty/torchanger/torchanger/theme.css").toExternalForm());
        pane.getStyleClass().add("settings-pane");
        pane.setPrefWidth(760);
        pane.setMaxWidth(760);
        ButtonType save = new ButtonType(bundle.getString("settings.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(bundle.getString("settings.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(save, cancel);

        ComboBox<String> languageBox = new ComboBox<>(FXCollections.observableArrayList("en", "ru"));
        languageBox.setValue(current.language());
        CheckBox autostartBox = new CheckBox(bundle.getString("settings.autostart"));
        autostartBox.setSelected(current.startWithSystem());
        CheckBox trayOnCloseBox = new CheckBox(bundle.getString("settings.minimizeOnClose"));
        trayOnCloseBox.setSelected(current.minimizeToTrayOnClose());
        CheckBox trayOnStartupBox = new CheckBox(bundle.getString("settings.minimizeOnStartup"));
        trayOnStartupBox.setSelected(current.minimizeToTrayOnStartup());
        CheckBox updateBridgesOnStartupBox = new CheckBox(bundle.getString("settings.updateBridgesOnStartup"));
        updateBridgesOnStartupBox.setSelected(current.updateBridgesOnStartup());
        Spinner<Integer> directTimeout = spinner(current.directTimeoutSeconds(), 15, 600);
        Spinner<Integer> bridgeTimeout = spinner(current.bridgeTimeoutSeconds(), 30, 900);
        Spinner<Integer> healthInterval = spinner(current.healthCheckSeconds(), 5, 120);
        ComboBox<String> logLevelBox = new ComboBox<>(FXCollections.observableArrayList("err", "warn", "notice", "info"));
        logLevelBox.setValue(current.torLogLevel());
        CheckBox hideNoisyWarningsBox = new CheckBox(bundle.getString("settings.hideNoisyWarnings"));
        hideNoisyWarningsBox.setSelected(current.hideNoisyBridgeWarnings());
        ComboBox<String> vanillaBox = new ComboBox<>(FXCollections.observableArrayList(filesFor(TorLaunchMode.VANILLA_BRIDGE)));
        vanillaBox.setValue(current.vanillaBridgeFile());
        ComboBox<String> obfs4Box = new ComboBox<>(FXCollections.observableArrayList(filesFor(TorLaunchMode.OBFS4)));
        obfs4Box.setValue(current.obfs4BridgeFile());
        ComboBox<String> webtunnelBox = new ComboBox<>(FXCollections.observableArrayList(filesFor(TorLaunchMode.WEBTUNNEL)));
        webtunnelBox.setValue(current.webtunnelBridgeFile());
        TextArea manualVanillaArea = bridgeTextArea(current.manualVanillaBridges());
        TextArea manualObfs4Area = bridgeTextArea(current.manualObfs4Bridges());
        TextArea manualWebtunnelArea = bridgeTextArea(current.manualWebtunnelBridges());
        TextField directSocksPort = portField(current.directSocksPort());
        TextField directHttpPort = portField(current.directHttpPort());
        TextField vanillaSocksPort = portField(current.vanillaSocksPort());
        TextField vanillaHttpPort = portField(current.vanillaHttpPort());
        TextField obfs4SocksPort = portField(current.obfs4SocksPort());
        TextField obfs4HttpPort = portField(current.obfs4HttpPort());
        TextField snowflakeSocksPort = portField(current.snowflakeSocksPort());
        TextField snowflakeHttpPort = portField(current.snowflakeHttpPort());
        TextField webtunnelSocksPort = portField(current.webtunnelSocksPort());
        TextField webtunnelHttpPort = portField(current.webtunnelHttpPort());
        TextField directTorSocksPort = portField(current.directTorSocksPort());
        TextField vanillaTorSocksPort = portField(current.vanillaTorSocksPort());
        TextField obfs4TorSocksPort = portField(current.obfs4TorSocksPort());
        TextField snowflakeTorSocksPort = portField(current.snowflakeTorSocksPort());
        TextField webtunnelTorSocksPort = portField(current.webtunnelTorSocksPort());
        TextField proxyUsernameField = new TextField(current.proxyUsername());
        PasswordField proxyPasswordField = new PasswordField();
        proxyPasswordField.setText(current.proxyPassword());
        GridPane generalGrid = grid();
        int row = 0;
        generalGrid.add(new Label(bundle.getString("settings.profile")), 0, row);
        generalGrid.add(new Label(profileName), 1, row++);
        generalGrid.add(new Label(bundle.getString("settings.storageDir")), 0, row);
        generalGrid.add(wrappingLabel(workspaceRoot.toAbsolutePath().toString()), 1, row++);
        generalGrid.add(new Label(bundle.getString("settings.language")), 0, row);
        generalGrid.add(languageBox, 1, row++);
        generalGrid.add(autostartBox, 0, row++, 2, 1);
        generalGrid.add(trayOnCloseBox, 0, row++, 2, 1);
        generalGrid.add(trayOnStartupBox, 0, row, 2, 1);

        GridPane startupGrid = grid();
        row = 0;
        startupGrid.add(new Label(bundle.getString("settings.directTimeout")), 0, row);
        startupGrid.add(directTimeout, 1, row++);
        startupGrid.add(new Label(bundle.getString("settings.bridgeTimeout")), 0, row);
        startupGrid.add(bridgeTimeout, 1, row++);
        startupGrid.add(new Label(bundle.getString("settings.healthInterval")), 0, row);
        startupGrid.add(healthInterval, 1, row++);
        startupGrid.add(new Label(bundle.getString("settings.logLevel")), 0, row);
        startupGrid.add(logLevelBox, 1, row++);
        startupGrid.add(updateBridgesOnStartupBox, 0, row++, 2, 1);
        startupGrid.add(hideNoisyWarningsBox, 0, row, 2, 1);

        GridPane bridgesGrid = grid();
        row = 0;
        bridgesGrid.add(new Label(bundle.getString("settings.vanillaSource")), 0, row);
        bridgesGrid.add(vanillaBox, 1, row++);
        bridgesGrid.add(new Label(bundle.getString("settings.obfs4Source")), 0, row);
        bridgesGrid.add(obfs4Box, 1, row++);
        bridgesGrid.add(new Label(bundle.getString("settings.webtunnelSource")), 0, row);
        bridgesGrid.add(webtunnelBox, 1, row++);

        Label customLabel = header(bundle.getString("settings.customBridges"));
        TabPane customBridgeTabs = new TabPane(
                new Tab(bundle.getString("settings.customVanilla"), manualVanillaArea),
                new Tab(bundle.getString("settings.customObfs4"), manualObfs4Area),
                new Tab(bundle.getString("settings.customWebtunnel"), manualWebtunnelArea)
        );
        customBridgeTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox bridgesBox = new VBox(12, bridgesGrid, customLabel, customBridgeTabs);

        GridPane portsGrid = grid();
        row = 0;
        portsGrid.add(header(bundle.getString("settings.publicProxyPorts")), 0, row++, 4, 1);
        portsGrid.add(header(bundle.getString("settings.portProfile")), 0, row);
        portsGrid.add(header(bundle.getString("settings.portSocks")), 1, row);
        portsGrid.add(header(bundle.getString("settings.portHttp")), 2, row);
        portsGrid.add(header(bundle.getString("settings.portTorSocks")), 3, row++);
        addPortRow(portsGrid, row++, bundle.getString("method.direct"), directSocksPort, directHttpPort, directTorSocksPort);
        addPortRow(portsGrid, row++, bundle.getString("method.vanilla-bridges"), vanillaSocksPort, vanillaHttpPort, vanillaTorSocksPort);
        addPortRow(portsGrid, row++, bundle.getString("method.obfs4-bridges"), obfs4SocksPort, obfs4HttpPort, obfs4TorSocksPort);
        addPortRow(portsGrid, row++, bundle.getString("method.snowflake"), snowflakeSocksPort, snowflakeHttpPort, snowflakeTorSocksPort);
        addPortRow(portsGrid, row, bundle.getString("method.webtunnel"), webtunnelSocksPort, webtunnelHttpPort, webtunnelTorSocksPort);

        GridPane proxyGrid = grid();
        row = 0;
        proxyGrid.add(new Label(bundle.getString("settings.proxyUsername")), 0, row);
        proxyGrid.add(proxyUsernameField, 1, row++);
        proxyGrid.add(new Label(bundle.getString("settings.proxyPassword")), 0, row);
        proxyGrid.add(proxyPasswordField, 1, row++);
        proxyGrid.add(wrappingLabel(bundle.getString("settings.proxyAuthHint")), 0, row, 2, 1);

        GridPane snowflakeGrid = grid();
        row = 0;
        snowflakeGrid.add(wrappingLabel(workspaceRoot.resolve("bridges").resolve("snowflake.txt").toAbsolutePath().toString()), 0, row++, 2, 1);
        snowflakeGrid.add(wrappingLabel(bundle.getString("settings.snowflakeHint")), 0, row, 2, 1);

        TabPane tabPane = new TabPane(
                new Tab(bundle.getString("settings.tabGeneral"), generalGrid),
                new Tab(bundle.getString("settings.tabStartup"), startupGrid),
                new Tab(bundle.getString("settings.tabBridges"), bridgesBox),
                new Tab(bundle.getString("settings.tabPorts"), portsGrid),
                new Tab(bundle.getString("settings.tabProxy"), proxyGrid),
                new Tab(bundle.getString("settings.tabSnowflake"), snowflakeGrid)
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pane.setContent(tabPane);

        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            AppSettings updated = AppSettings.copyOf(current);
            updated.setLanguage(languageBox.getValue());
            updated.setStartWithSystem(autostartBox.isSelected());
            updated.setMinimizeToTrayOnClose(trayOnCloseBox.isSelected());
            updated.setMinimizeToTrayOnStartup(trayOnStartupBox.isSelected());
            updated.setUpdateBridgesOnStartup(updateBridgesOnStartupBox.isSelected());
            updated.setDirectTimeoutSeconds(directTimeout.getValue());
            updated.setBridgeTimeoutSeconds(bridgeTimeout.getValue());
            updated.setHealthCheckSeconds(healthInterval.getValue());
            updated.setTorLogLevel(logLevelBox.getValue());
            updated.setHideNoisyBridgeWarnings(hideNoisyWarningsBox.isSelected());
            updated.setVanillaBridgeFile(vanillaBox.getValue());
            updated.setObfs4BridgeFile(obfs4Box.getValue());
            updated.setWebtunnelBridgeFile(webtunnelBox.getValue());
            updated.setManualVanillaBridges(manualVanillaArea.getText());
            updated.setManualObfs4Bridges(manualObfs4Area.getText());
            updated.setManualWebtunnelBridges(manualWebtunnelArea.getText());
            updated.setDirectSocksPort(parsePort(directSocksPort.getText(), current.directSocksPort()));
            updated.setDirectHttpPort(parsePort(directHttpPort.getText(), current.directHttpPort()));
            updated.setVanillaSocksPort(parsePort(vanillaSocksPort.getText(), current.vanillaSocksPort()));
            updated.setVanillaHttpPort(parsePort(vanillaHttpPort.getText(), current.vanillaHttpPort()));
            updated.setObfs4SocksPort(parsePort(obfs4SocksPort.getText(), current.obfs4SocksPort()));
            updated.setObfs4HttpPort(parsePort(obfs4HttpPort.getText(), current.obfs4HttpPort()));
            updated.setSnowflakeSocksPort(parsePort(snowflakeSocksPort.getText(), current.snowflakeSocksPort()));
            updated.setSnowflakeHttpPort(parsePort(snowflakeHttpPort.getText(), current.snowflakeHttpPort()));
            updated.setWebtunnelSocksPort(parsePort(webtunnelSocksPort.getText(), current.webtunnelSocksPort()));
            updated.setWebtunnelHttpPort(parsePort(webtunnelHttpPort.getText(), current.webtunnelHttpPort()));
            updated.setDirectTorSocksPort(parsePort(directTorSocksPort.getText(), current.directTorSocksPort()));
            updated.setVanillaTorSocksPort(parsePort(vanillaTorSocksPort.getText(), current.vanillaTorSocksPort()));
            updated.setObfs4TorSocksPort(parsePort(obfs4TorSocksPort.getText(), current.obfs4TorSocksPort()));
            updated.setSnowflakeTorSocksPort(parsePort(snowflakeTorSocksPort.getText(), current.snowflakeTorSocksPort()));
            updated.setWebtunnelTorSocksPort(parsePort(webtunnelTorSocksPort.getText(), current.webtunnelTorSocksPort()));
            updated.setProxyUsername(proxyUsernameField.getText().trim());
            updated.setProxyPassword(proxyPasswordField.getText());
            return updated;
        });
        if (owner != null) {
            dialog.setOnShown(event -> {
                if (dialog.getDialogPane().getScene().getWindow() instanceof Stage dialogStage) {
                    dialogStage.setX(owner.getX() + (owner.getWidth() - dialogStage.getWidth()) / 2.0);
                    dialogStage.setY(owner.getY() + (owner.getHeight() - dialogStage.getHeight()) / 2.0);
                }
            });
        }
        return dialog.showAndWait();
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(18));
        return grid;
    }

    private static Label header(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-label");
        return label;
    }

    private static Label wrappingLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(520);
        return label;
    }

    private static void addPortRow(GridPane grid, int row, String profile, TextField socks, TextField http, TextField torSocks) {
        grid.add(new Label(profile), 0, row);
        grid.add(socks, 1, row);
        grid.add(http, 2, row);
        grid.add(torSocks, 3, row);
    }

    private static TextField portField(int value) {
        TextField field = new TextField(Integer.toString(value));
        field.setPrefColumnCount(8);
        return field;
    }

    private static int parsePort(String value, int fallback) {
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1024 && port <= 65535 ? port : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Spinner<Integer> spinner(int value, int min, int max) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, value));
        return spinner;
    }

    private static List<String> filesFor(TorLaunchMode mode) {
        List<String> files = BridgeCatalog.allEntries().stream()
                .filter(entry -> entry.mode() == mode)
                .map(BridgeCatalogEntry::fileName)
                .toList();
        return switch (mode) {
            case VANILLA_BRIDGE -> appendManual(files, SettingsManager.MANUAL_VANILLA_FILE);
            case OBFS4 -> appendManual(files, SettingsManager.MANUAL_OBFS4_FILE);
            case WEBTUNNEL -> appendManual(files, SettingsManager.MANUAL_WEBTUNNEL_FILE);
            default -> files;
        };
    }

    private static List<String> appendManual(List<String> files, String manualFile) {
        var updated = FXCollections.<String>observableArrayList(files);
        if (!updated.contains(manualFile)) {
            updated.add(manualFile);
        }
        return List.copyOf(updated);
    }

    private static TextArea bridgeTextArea(String text) {
        TextArea area = new TextArea(text);
        area.setWrapText(false);
        area.setPrefRowCount(12);
        return area;
    }

}
