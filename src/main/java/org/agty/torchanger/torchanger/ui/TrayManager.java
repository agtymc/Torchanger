package org.agty.torchanger.torchanger.ui;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.agty.torchanger.torchanger.app.AppIconFactory;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;

public final class TrayManager {
    private TrayIcon trayIcon;

    public void install(
            Stage stage,
            String openLabel,
            String hideLabel,
            String startAllLabel,
            String stopAllLabel,
            String restartAllLabel,
            String updateBridgesLabel,
            String settingsLabel,
            String logsFolderLabel,
            String exitLabel,
            Runnable hideAction,
            Runnable startAllAction,
            Runnable stopAllAction,
            Runnable restartAllAction,
            Runnable updateBridgesAction,
            Runnable settingsAction,
            Runnable logsFolderAction,
            Runnable exitAction
    ) {
        if (!SystemTray.isSupported() || trayIcon != null) {
            return;
        }

        PopupMenu menu = new PopupMenu();
        MenuItem openItem = new MenuItem(openLabel);
        openItem.addActionListener(event -> Platform.runLater(() -> {
            stage.show();
            stage.toFront();
        }));
        MenuItem hideItem = new MenuItem(hideLabel);
        hideItem.addActionListener(event -> Platform.runLater(hideAction));
        MenuItem startAllItem = new MenuItem(startAllLabel);
        startAllItem.addActionListener(event -> Platform.runLater(startAllAction));
        MenuItem stopAllItem = new MenuItem(stopAllLabel);
        stopAllItem.addActionListener(event -> Platform.runLater(stopAllAction));
        MenuItem restartAllItem = new MenuItem(restartAllLabel);
        restartAllItem.addActionListener(event -> Platform.runLater(restartAllAction));
        MenuItem updateBridgesItem = new MenuItem(updateBridgesLabel);
        updateBridgesItem.addActionListener(event -> Platform.runLater(updateBridgesAction));
        MenuItem settingsItem = new MenuItem(settingsLabel);
        settingsItem.addActionListener(event -> Platform.runLater(settingsAction));
        MenuItem logsFolderItem = new MenuItem(logsFolderLabel);
        logsFolderItem.addActionListener(event -> Platform.runLater(logsFolderAction));
        MenuItem exitItem = new MenuItem(exitLabel);
        exitItem.addActionListener(event -> Platform.runLater(exitAction));
        menu.add(openItem);
        menu.add(hideItem);
        menu.addSeparator();
        menu.add(startAllItem);
        menu.add(stopAllItem);
        menu.add(restartAllItem);
        menu.addSeparator();
        menu.add(updateBridgesItem);
        menu.add(settingsItem);
        menu.add(logsFolderItem);
        menu.addSeparator();
        menu.add(exitItem);

        trayIcon = new TrayIcon(AppIconFactory.createAwtIcon(), "Torchanger", menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(event -> Platform.runLater(() -> {
            stage.show();
            stage.toFront();
        }));

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ignored) {
            trayIcon = null;
        }
    }

    public void showMessage(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    public void hideStage(Stage stage) {
        Platform.runLater(stage::hide);
    }

    public void uninstall() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

}
