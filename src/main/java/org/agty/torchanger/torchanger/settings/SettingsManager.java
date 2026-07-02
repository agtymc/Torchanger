package org.agty.torchanger.torchanger.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import org.agty.torchanger.torchanger.bridge.BridgeCatalogEntry;

public class SettingsManager {
    public static final String MANUAL_VANILLA_FILE = "manual-vanilla-bridges.txt";
    public static final String MANUAL_OBFS4_FILE = "manual-obfs4-bridges.txt";
    public static final String MANUAL_WEBTUNNEL_FILE = "manual-webtunnel-bridges.txt";

    private final Path workspaceRoot;
    private final Path settingsFile;

    public SettingsManager(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.settingsFile = workspaceRoot.resolve("settings.properties");
    }

    public AppSettings load() throws IOException {
        Files.createDirectories(workspaceRoot);
        AppSettings settings = AppSettings.defaults();
        if (!Files.exists(settingsFile)) {
            save(settings);
            return settings;
        }
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        settings.setLanguage(properties.getProperty("language", settings.language()));
        settings.setStartWithSystem(Boolean.parseBoolean(properties.getProperty("startWithSystem", Boolean.toString(settings.startWithSystem()))));
        settings.setMinimizeToTrayOnClose(Boolean.parseBoolean(properties.getProperty("minimizeToTrayOnClose", Boolean.toString(settings.minimizeToTrayOnClose()))));
        settings.setMinimizeToTrayOnStartup(Boolean.parseBoolean(properties.getProperty("minimizeToTrayOnStartup", Boolean.toString(settings.minimizeToTrayOnStartup()))));
        settings.setUpdateBridgesOnStartup(Boolean.parseBoolean(properties.getProperty("updateBridgesOnStartup", Boolean.toString(settings.updateBridgesOnStartup()))));
        settings.setDirectTimeoutSeconds(Integer.parseInt(properties.getProperty("directTimeoutSeconds", Integer.toString(settings.directTimeoutSeconds()))));
        settings.setBridgeTimeoutSeconds(Integer.parseInt(properties.getProperty("bridgeTimeoutSeconds", Integer.toString(settings.bridgeTimeoutSeconds()))));
        settings.setHealthCheckSeconds(Integer.parseInt(properties.getProperty("healthCheckSeconds", Integer.toString(settings.healthCheckSeconds()))));
        settings.setTorLogLevel(properties.getProperty("torLogLevel", settings.torLogLevel()));
        settings.setHideNoisyBridgeWarnings(Boolean.parseBoolean(properties.getProperty("hideNoisyBridgeWarnings", Boolean.toString(settings.hideNoisyBridgeWarnings()))));
        settings.setVanillaBridgeFile(properties.getProperty("vanillaBridgeFile", settings.vanillaBridgeFile()));
        settings.setObfs4BridgeFile(properties.getProperty("obfs4BridgeFile", settings.obfs4BridgeFile()));
        settings.setWebtunnelBridgeFile(properties.getProperty("webtunnelBridgeFile", settings.webtunnelBridgeFile()));
        settings.setManualVanillaBridges(readBridgeTextFile(MANUAL_VANILLA_FILE));
        settings.setManualObfs4Bridges(readBridgeTextFile(MANUAL_OBFS4_FILE));
        settings.setManualWebtunnelBridges(readBridgeTextFile(MANUAL_WEBTUNNEL_FILE));
        settings.setDirectSocksPort(Integer.parseInt(properties.getProperty("directSocksPort", Integer.toString(settings.directSocksPort()))));
        settings.setDirectHttpPort(Integer.parseInt(properties.getProperty("directHttpPort", Integer.toString(settings.directHttpPort()))));
        settings.setVanillaSocksPort(Integer.parseInt(properties.getProperty("vanillaSocksPort", Integer.toString(settings.vanillaSocksPort()))));
        settings.setVanillaHttpPort(Integer.parseInt(properties.getProperty("vanillaHttpPort", Integer.toString(settings.vanillaHttpPort()))));
        settings.setObfs4SocksPort(Integer.parseInt(properties.getProperty("obfs4SocksPort", Integer.toString(settings.obfs4SocksPort()))));
        settings.setObfs4HttpPort(Integer.parseInt(properties.getProperty("obfs4HttpPort", Integer.toString(settings.obfs4HttpPort()))));
        settings.setSnowflakeSocksPort(Integer.parseInt(properties.getProperty("snowflakeSocksPort", Integer.toString(settings.snowflakeSocksPort()))));
        settings.setSnowflakeHttpPort(Integer.parseInt(properties.getProperty("snowflakeHttpPort", Integer.toString(settings.snowflakeHttpPort()))));
        settings.setMeekSocksPort(Integer.parseInt(properties.getProperty("meekSocksPort", Integer.toString(settings.meekSocksPort()))));
        settings.setMeekHttpPort(Integer.parseInt(properties.getProperty("meekHttpPort", Integer.toString(settings.meekHttpPort()))));
        settings.setWebtunnelSocksPort(Integer.parseInt(properties.getProperty("webtunnelSocksPort", Integer.toString(settings.webtunnelSocksPort()))));
        settings.setWebtunnelHttpPort(Integer.parseInt(properties.getProperty("webtunnelHttpPort", Integer.toString(settings.webtunnelHttpPort()))));
        settings.setDirectTorSocksPort(Integer.parseInt(properties.getProperty("directTorSocksPort", Integer.toString(settings.directTorSocksPort()))));
        settings.setVanillaTorSocksPort(Integer.parseInt(properties.getProperty("vanillaTorSocksPort", Integer.toString(settings.vanillaTorSocksPort()))));
        settings.setObfs4TorSocksPort(Integer.parseInt(properties.getProperty("obfs4TorSocksPort", Integer.toString(settings.obfs4TorSocksPort()))));
        settings.setSnowflakeTorSocksPort(Integer.parseInt(properties.getProperty("snowflakeTorSocksPort", Integer.toString(settings.snowflakeTorSocksPort()))));
        settings.setMeekTorSocksPort(Integer.parseInt(properties.getProperty("meekTorSocksPort", Integer.toString(settings.meekTorSocksPort()))));
        settings.setWebtunnelTorSocksPort(Integer.parseInt(properties.getProperty("webtunnelTorSocksPort", Integer.toString(settings.webtunnelTorSocksPort()))));
        settings.setDirectAutoStart(Boolean.parseBoolean(properties.getProperty("directAutoStart", Boolean.toString(settings.directAutoStart()))));
        settings.setVanillaAutoStart(Boolean.parseBoolean(properties.getProperty("vanillaAutoStart", Boolean.toString(settings.vanillaAutoStart()))));
        settings.setObfs4AutoStart(Boolean.parseBoolean(properties.getProperty("obfs4AutoStart", Boolean.toString(settings.obfs4AutoStart()))));
        settings.setSnowflakeAutoStart(Boolean.parseBoolean(properties.getProperty("snowflakeAutoStart", Boolean.toString(settings.snowflakeAutoStart()))));
        settings.setMeekAutoStart(Boolean.parseBoolean(properties.getProperty("meekAutoStart", Boolean.toString(settings.meekAutoStart()))));
        settings.setWebtunnelAutoStart(Boolean.parseBoolean(properties.getProperty("webtunnelAutoStart", Boolean.toString(settings.webtunnelAutoStart()))));
        settings.setProxyUsername(properties.getProperty("proxyUsername", settings.proxyUsername()));
        settings.setProxyPassword(properties.getProperty("proxyPassword", settings.proxyPassword()));
        return settings;
    }

    public void save(AppSettings settings) throws IOException {
        Files.createDirectories(workspaceRoot);
        Properties properties = new Properties();
        properties.setProperty("language", settings.language());
        properties.setProperty("startWithSystem", Boolean.toString(settings.startWithSystem()));
        properties.setProperty("minimizeToTrayOnClose", Boolean.toString(settings.minimizeToTrayOnClose()));
        properties.setProperty("minimizeToTrayOnStartup", Boolean.toString(settings.minimizeToTrayOnStartup()));
        properties.setProperty("updateBridgesOnStartup", Boolean.toString(settings.updateBridgesOnStartup()));
        properties.setProperty("directTimeoutSeconds", Integer.toString(settings.directTimeoutSeconds()));
        properties.setProperty("bridgeTimeoutSeconds", Integer.toString(settings.bridgeTimeoutSeconds()));
        properties.setProperty("healthCheckSeconds", Integer.toString(settings.healthCheckSeconds()));
        properties.setProperty("torLogLevel", settings.torLogLevel());
        properties.setProperty("hideNoisyBridgeWarnings", Boolean.toString(settings.hideNoisyBridgeWarnings()));
        properties.setProperty("vanillaBridgeFile", settings.vanillaBridgeFile());
        properties.setProperty("obfs4BridgeFile", settings.obfs4BridgeFile());
        properties.setProperty("webtunnelBridgeFile", settings.webtunnelBridgeFile());
        properties.setProperty("directSocksPort", Integer.toString(settings.directSocksPort()));
        properties.setProperty("directHttpPort", Integer.toString(settings.directHttpPort()));
        properties.setProperty("vanillaSocksPort", Integer.toString(settings.vanillaSocksPort()));
        properties.setProperty("vanillaHttpPort", Integer.toString(settings.vanillaHttpPort()));
        properties.setProperty("obfs4SocksPort", Integer.toString(settings.obfs4SocksPort()));
        properties.setProperty("obfs4HttpPort", Integer.toString(settings.obfs4HttpPort()));
        properties.setProperty("snowflakeSocksPort", Integer.toString(settings.snowflakeSocksPort()));
        properties.setProperty("snowflakeHttpPort", Integer.toString(settings.snowflakeHttpPort()));
        properties.setProperty("meekSocksPort", Integer.toString(settings.meekSocksPort()));
        properties.setProperty("meekHttpPort", Integer.toString(settings.meekHttpPort()));
        properties.setProperty("webtunnelSocksPort", Integer.toString(settings.webtunnelSocksPort()));
        properties.setProperty("webtunnelHttpPort", Integer.toString(settings.webtunnelHttpPort()));
        properties.setProperty("directTorSocksPort", Integer.toString(settings.directTorSocksPort()));
        properties.setProperty("vanillaTorSocksPort", Integer.toString(settings.vanillaTorSocksPort()));
        properties.setProperty("obfs4TorSocksPort", Integer.toString(settings.obfs4TorSocksPort()));
        properties.setProperty("snowflakeTorSocksPort", Integer.toString(settings.snowflakeTorSocksPort()));
        properties.setProperty("meekTorSocksPort", Integer.toString(settings.meekTorSocksPort()));
        properties.setProperty("webtunnelTorSocksPort", Integer.toString(settings.webtunnelTorSocksPort()));
        properties.setProperty("directAutoStart", Boolean.toString(settings.directAutoStart()));
        properties.setProperty("vanillaAutoStart", Boolean.toString(settings.vanillaAutoStart()));
        properties.setProperty("obfs4AutoStart", Boolean.toString(settings.obfs4AutoStart()));
        properties.setProperty("snowflakeAutoStart", Boolean.toString(settings.snowflakeAutoStart()));
        properties.setProperty("meekAutoStart", Boolean.toString(settings.meekAutoStart()));
        properties.setProperty("webtunnelAutoStart", Boolean.toString(settings.webtunnelAutoStart()));
        properties.setProperty("proxyUsername", settings.proxyUsername());
        properties.setProperty("proxyPassword", settings.proxyPassword());
        try (var writer = Files.newBufferedWriter(settingsFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            properties.store(writer, "Torchanger settings");
        }
        writeBridgeTextFile(MANUAL_VANILLA_FILE, settings.manualVanillaBridges());
        writeBridgeTextFile(MANUAL_OBFS4_FILE, settings.manualObfs4Bridges());
        writeBridgeTextFile(MANUAL_WEBTUNNEL_FILE, settings.manualWebtunnelBridges());
    }

    private String readBridgeTextFile(String fileName) throws IOException {
        Path file = workspaceRoot.resolve("bridges").resolve(fileName);
        if (!Files.exists(file)) {
            return "";
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private void writeBridgeTextFile(String fileName, String content) throws IOException {
        Path dir = workspaceRoot.resolve("bridges");
        Files.createDirectories(dir);
        Files.writeString(
                dir.resolve(fileName),
                content == null ? "" : content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public void applyAutostart(AppSettings settings, Path projectRoot) throws IOException {
        Path autostartDir = Path.of(System.getProperty("user.home"), ".config", "autostart");
        Path desktopFile = autostartDir.resolve("torchanger.desktop");
        if (!settings.startWithSystem()) {
            Files.deleteIfExists(desktopFile);
            return;
        }
        Files.createDirectories(autostartDir);
        String exec = "bash -lc 'cd \"" + projectRoot.toAbsolutePath() + "\" && ./mvnw -q javafx:run'";
        String content = """
                [Desktop Entry]
                Type=Application
                Name=Torchanger
                Exec=%s
                Terminal=false
                X-GNOME-Autostart-enabled=true
                """.formatted(exec);
        Files.writeString(desktopFile, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
