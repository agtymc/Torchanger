package org.agty.torchanger.torchanger.settings;

import org.agty.torchanger.torchanger.app.AppEnvironment;
import org.agty.torchanger.torchanger.bridge.BridgeCatalog;
import org.agty.torchanger.torchanger.config.AppDefaults;

public final class AppSettings {
    private String language;
    private boolean startWithSystem;
    private boolean minimizeToTrayOnClose;
    private boolean minimizeToTrayOnStartup;
    private boolean updateBridgesOnStartup;
    private int directTimeoutSeconds;
    private int bridgeTimeoutSeconds;
    private int healthCheckSeconds;
    private String torLogLevel;
    private boolean hideNoisyBridgeWarnings;
    private String vanillaBridgeFile;
    private String obfs4BridgeFile;
    private String webtunnelBridgeFile;
    private String manualVanillaBridges;
    private String manualObfs4Bridges;
    private String manualWebtunnelBridges;
    private int directSocksPort;
    private int directHttpPort;
    private int vanillaSocksPort;
    private int vanillaHttpPort;
    private int obfs4SocksPort;
    private int obfs4HttpPort;
    private int snowflakeSocksPort;
    private int snowflakeHttpPort;
    private int meekSocksPort;
    private int meekHttpPort;
    private int webtunnelSocksPort;
    private int webtunnelHttpPort;
    private int directTorSocksPort;
    private int vanillaTorSocksPort;
    private int obfs4TorSocksPort;
    private int snowflakeTorSocksPort;
    private int meekTorSocksPort;
    private int webtunnelTorSocksPort;
    private boolean directAutoStart;
    private boolean vanillaAutoStart;
    private boolean obfs4AutoStart;
    private boolean snowflakeAutoStart;
    private boolean meekAutoStart;
    private boolean webtunnelAutoStart;
    private String proxyUsername;
    private String proxyPassword;

    public static AppSettings defaults() {
        AppSettings settings = new AppSettings();
        settings.language = AppDefaults.stringValue("language", "en");
        settings.startWithSystem = AppDefaults.booleanValue("startWithSystem", false);
        settings.minimizeToTrayOnClose = AppDefaults.booleanValue("minimizeToTrayOnClose", true);
        settings.minimizeToTrayOnStartup = AppDefaults.booleanValue("minimizeToTrayOnStartup", false);
        settings.updateBridgesOnStartup = AppDefaults.booleanValue("updateBridgesOnStartup", false);
        settings.directTimeoutSeconds = AppDefaults.intValue("directTimeoutSeconds", 75);
        settings.bridgeTimeoutSeconds = AppDefaults.intValue("bridgeTimeoutSeconds", 120);
        settings.healthCheckSeconds = AppDefaults.intValue("healthCheckSeconds", 20);
        settings.torLogLevel = AppDefaults.stringValue("torLogLevel", "notice");
        settings.hideNoisyBridgeWarnings = AppDefaults.booleanValue("hideNoisyBridgeWarnings", true);
        settings.vanillaBridgeFile = BridgeCatalog.vanillaTested().fileName();
        settings.obfs4BridgeFile = BridgeCatalog.obfs4Tested().fileName();
        settings.webtunnelBridgeFile = BridgeCatalog.webTunnelTested().fileName();
        settings.manualVanillaBridges = "";
        settings.manualObfs4Bridges = "";
        settings.manualWebtunnelBridges = "";
        settings.directSocksPort = AppDefaults.intValue("directSocksPort", 9060);
        settings.directHttpPort = AppDefaults.intValue("directHttpPort", 19060);
        settings.vanillaSocksPort = AppDefaults.intValue("vanillaSocksPort", 9061);
        settings.vanillaHttpPort = AppDefaults.intValue("vanillaHttpPort", 19061);
        settings.obfs4SocksPort = AppDefaults.intValue("obfs4SocksPort", 9062);
        settings.obfs4HttpPort = AppDefaults.intValue("obfs4HttpPort", 19062);
        settings.snowflakeSocksPort = AppDefaults.intValue("snowflakeSocksPort", 9063);
        settings.snowflakeHttpPort = AppDefaults.intValue("snowflakeHttpPort", 19063);
        settings.meekSocksPort = AppDefaults.intValue("meekSocksPort", 9064);
        settings.meekHttpPort = AppDefaults.intValue("meekHttpPort", 19064);
        settings.webtunnelSocksPort = AppDefaults.intValue("webtunnelSocksPort", 9065);
        settings.webtunnelHttpPort = AppDefaults.intValue("webtunnelHttpPort", 19065);
        int torSocksBase = AppEnvironment.isPackaged()
                ? AppDefaults.intValue("packagedTorSocksPortBase", 9260)
                : AppDefaults.intValue("developmentTorSocksPortBase", 9360);
        settings.directTorSocksPort = torSocksBase;
        settings.vanillaTorSocksPort = torSocksBase + 1;
        settings.obfs4TorSocksPort = torSocksBase + 2;
        settings.snowflakeTorSocksPort = torSocksBase + 3;
        settings.meekTorSocksPort = torSocksBase + 4;
        settings.webtunnelTorSocksPort = torSocksBase + 5;
        settings.directAutoStart = false;
        settings.vanillaAutoStart = false;
        settings.obfs4AutoStart = false;
        settings.snowflakeAutoStart = false;
        settings.meekAutoStart = false;
        settings.webtunnelAutoStart = false;
        settings.proxyUsername = "";
        settings.proxyPassword = "";
        return settings;
    }

    public static AppSettings copyOf(AppSettings source) {
        AppSettings copy = new AppSettings();
        copy.language = source.language;
        copy.startWithSystem = source.startWithSystem;
        copy.minimizeToTrayOnClose = source.minimizeToTrayOnClose;
        copy.minimizeToTrayOnStartup = source.minimizeToTrayOnStartup;
        copy.updateBridgesOnStartup = source.updateBridgesOnStartup;
        copy.directTimeoutSeconds = source.directTimeoutSeconds;
        copy.bridgeTimeoutSeconds = source.bridgeTimeoutSeconds;
        copy.healthCheckSeconds = source.healthCheckSeconds;
        copy.torLogLevel = source.torLogLevel;
        copy.hideNoisyBridgeWarnings = source.hideNoisyBridgeWarnings;
        copy.vanillaBridgeFile = source.vanillaBridgeFile;
        copy.obfs4BridgeFile = source.obfs4BridgeFile;
        copy.webtunnelBridgeFile = source.webtunnelBridgeFile;
        copy.manualVanillaBridges = source.manualVanillaBridges;
        copy.manualObfs4Bridges = source.manualObfs4Bridges;
        copy.manualWebtunnelBridges = source.manualWebtunnelBridges;
        copy.directSocksPort = source.directSocksPort;
        copy.directHttpPort = source.directHttpPort;
        copy.vanillaSocksPort = source.vanillaSocksPort;
        copy.vanillaHttpPort = source.vanillaHttpPort;
        copy.obfs4SocksPort = source.obfs4SocksPort;
        copy.obfs4HttpPort = source.obfs4HttpPort;
        copy.snowflakeSocksPort = source.snowflakeSocksPort;
        copy.snowflakeHttpPort = source.snowflakeHttpPort;
        copy.meekSocksPort = source.meekSocksPort;
        copy.meekHttpPort = source.meekHttpPort;
        copy.webtunnelSocksPort = source.webtunnelSocksPort;
        copy.webtunnelHttpPort = source.webtunnelHttpPort;
        copy.directTorSocksPort = source.directTorSocksPort;
        copy.vanillaTorSocksPort = source.vanillaTorSocksPort;
        copy.obfs4TorSocksPort = source.obfs4TorSocksPort;
        copy.snowflakeTorSocksPort = source.snowflakeTorSocksPort;
        copy.meekTorSocksPort = source.meekTorSocksPort;
        copy.webtunnelTorSocksPort = source.webtunnelTorSocksPort;
        copy.directAutoStart = source.directAutoStart;
        copy.vanillaAutoStart = source.vanillaAutoStart;
        copy.obfs4AutoStart = source.obfs4AutoStart;
        copy.snowflakeAutoStart = source.snowflakeAutoStart;
        copy.meekAutoStart = source.meekAutoStart;
        copy.webtunnelAutoStart = source.webtunnelAutoStart;
        copy.proxyUsername = source.proxyUsername;
        copy.proxyPassword = source.proxyPassword;
        return copy;
    }

    public String language() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean startWithSystem() { return startWithSystem; }
    public void setStartWithSystem(boolean startWithSystem) { this.startWithSystem = startWithSystem; }
    public boolean minimizeToTrayOnClose() { return minimizeToTrayOnClose; }
    public void setMinimizeToTrayOnClose(boolean minimizeToTrayOnClose) { this.minimizeToTrayOnClose = minimizeToTrayOnClose; }
    public boolean minimizeToTrayOnStartup() { return minimizeToTrayOnStartup; }
    public void setMinimizeToTrayOnStartup(boolean minimizeToTrayOnStartup) { this.minimizeToTrayOnStartup = minimizeToTrayOnStartup; }
    public boolean updateBridgesOnStartup() { return updateBridgesOnStartup; }
    public void setUpdateBridgesOnStartup(boolean updateBridgesOnStartup) { this.updateBridgesOnStartup = updateBridgesOnStartup; }
    public int directTimeoutSeconds() { return directTimeoutSeconds; }
    public void setDirectTimeoutSeconds(int directTimeoutSeconds) { this.directTimeoutSeconds = directTimeoutSeconds; }
    public int bridgeTimeoutSeconds() { return bridgeTimeoutSeconds; }
    public void setBridgeTimeoutSeconds(int bridgeTimeoutSeconds) { this.bridgeTimeoutSeconds = bridgeTimeoutSeconds; }
    public int healthCheckSeconds() { return healthCheckSeconds; }
    public void setHealthCheckSeconds(int healthCheckSeconds) { this.healthCheckSeconds = healthCheckSeconds; }
    public String torLogLevel() { return torLogLevel; }
    public void setTorLogLevel(String torLogLevel) { this.torLogLevel = torLogLevel; }
    public boolean hideNoisyBridgeWarnings() { return hideNoisyBridgeWarnings; }
    public void setHideNoisyBridgeWarnings(boolean hideNoisyBridgeWarnings) { this.hideNoisyBridgeWarnings = hideNoisyBridgeWarnings; }
    public String vanillaBridgeFile() { return vanillaBridgeFile; }
    public void setVanillaBridgeFile(String vanillaBridgeFile) { this.vanillaBridgeFile = vanillaBridgeFile; }
    public String obfs4BridgeFile() { return obfs4BridgeFile; }
    public void setObfs4BridgeFile(String obfs4BridgeFile) { this.obfs4BridgeFile = obfs4BridgeFile; }
    public String webtunnelBridgeFile() { return webtunnelBridgeFile; }
    public void setWebtunnelBridgeFile(String webtunnelBridgeFile) { this.webtunnelBridgeFile = webtunnelBridgeFile; }
    public String manualVanillaBridges() { return manualVanillaBridges; }
    public void setManualVanillaBridges(String manualVanillaBridges) { this.manualVanillaBridges = manualVanillaBridges; }
    public String manualObfs4Bridges() { return manualObfs4Bridges; }
    public void setManualObfs4Bridges(String manualObfs4Bridges) { this.manualObfs4Bridges = manualObfs4Bridges; }
    public String manualWebtunnelBridges() { return manualWebtunnelBridges; }
    public void setManualWebtunnelBridges(String manualWebtunnelBridges) { this.manualWebtunnelBridges = manualWebtunnelBridges; }
    public int directSocksPort() { return directSocksPort; }
    public void setDirectSocksPort(int directSocksPort) { this.directSocksPort = directSocksPort; }
    public int directHttpPort() { return directHttpPort; }
    public void setDirectHttpPort(int directHttpPort) { this.directHttpPort = directHttpPort; }
    public int vanillaSocksPort() { return vanillaSocksPort; }
    public void setVanillaSocksPort(int vanillaSocksPort) { this.vanillaSocksPort = vanillaSocksPort; }
    public int vanillaHttpPort() { return vanillaHttpPort; }
    public void setVanillaHttpPort(int vanillaHttpPort) { this.vanillaHttpPort = vanillaHttpPort; }
    public int obfs4SocksPort() { return obfs4SocksPort; }
    public void setObfs4SocksPort(int obfs4SocksPort) { this.obfs4SocksPort = obfs4SocksPort; }
    public int obfs4HttpPort() { return obfs4HttpPort; }
    public void setObfs4HttpPort(int obfs4HttpPort) { this.obfs4HttpPort = obfs4HttpPort; }
    public int snowflakeSocksPort() { return snowflakeSocksPort; }
    public void setSnowflakeSocksPort(int snowflakeSocksPort) { this.snowflakeSocksPort = snowflakeSocksPort; }
    public int snowflakeHttpPort() { return snowflakeHttpPort; }
    public void setSnowflakeHttpPort(int snowflakeHttpPort) { this.snowflakeHttpPort = snowflakeHttpPort; }
    public int meekSocksPort() { return meekSocksPort; }
    public void setMeekSocksPort(int meekSocksPort) { this.meekSocksPort = meekSocksPort; }
    public int meekHttpPort() { return meekHttpPort; }
    public void setMeekHttpPort(int meekHttpPort) { this.meekHttpPort = meekHttpPort; }
    public int webtunnelSocksPort() { return webtunnelSocksPort; }
    public void setWebtunnelSocksPort(int webtunnelSocksPort) { this.webtunnelSocksPort = webtunnelSocksPort; }
    public int webtunnelHttpPort() { return webtunnelHttpPort; }
    public void setWebtunnelHttpPort(int webtunnelHttpPort) { this.webtunnelHttpPort = webtunnelHttpPort; }
    public int directTorSocksPort() { return directTorSocksPort; }
    public void setDirectTorSocksPort(int directTorSocksPort) { this.directTorSocksPort = directTorSocksPort; }
    public int vanillaTorSocksPort() { return vanillaTorSocksPort; }
    public void setVanillaTorSocksPort(int vanillaTorSocksPort) { this.vanillaTorSocksPort = vanillaTorSocksPort; }
    public int obfs4TorSocksPort() { return obfs4TorSocksPort; }
    public void setObfs4TorSocksPort(int obfs4TorSocksPort) { this.obfs4TorSocksPort = obfs4TorSocksPort; }
    public int snowflakeTorSocksPort() { return snowflakeTorSocksPort; }
    public void setSnowflakeTorSocksPort(int snowflakeTorSocksPort) { this.snowflakeTorSocksPort = snowflakeTorSocksPort; }
    public int meekTorSocksPort() { return meekTorSocksPort; }
    public void setMeekTorSocksPort(int meekTorSocksPort) { this.meekTorSocksPort = meekTorSocksPort; }
    public int webtunnelTorSocksPort() { return webtunnelTorSocksPort; }
    public void setWebtunnelTorSocksPort(int webtunnelTorSocksPort) { this.webtunnelTorSocksPort = webtunnelTorSocksPort; }
    public boolean directAutoStart() { return directAutoStart; }
    public void setDirectAutoStart(boolean directAutoStart) { this.directAutoStart = directAutoStart; }
    public boolean vanillaAutoStart() { return vanillaAutoStart; }
    public void setVanillaAutoStart(boolean vanillaAutoStart) { this.vanillaAutoStart = vanillaAutoStart; }
    public boolean obfs4AutoStart() { return obfs4AutoStart; }
    public void setObfs4AutoStart(boolean obfs4AutoStart) { this.obfs4AutoStart = obfs4AutoStart; }
    public boolean snowflakeAutoStart() { return snowflakeAutoStart; }
    public void setSnowflakeAutoStart(boolean snowflakeAutoStart) { this.snowflakeAutoStart = snowflakeAutoStart; }
    public boolean meekAutoStart() { return meekAutoStart; }
    public void setMeekAutoStart(boolean meekAutoStart) { this.meekAutoStart = meekAutoStart; }
    public boolean webtunnelAutoStart() { return webtunnelAutoStart; }
    public void setWebtunnelAutoStart(boolean webtunnelAutoStart) { this.webtunnelAutoStart = webtunnelAutoStart; }
    public String proxyUsername() { return proxyUsername; }
    public void setProxyUsername(String proxyUsername) { this.proxyUsername = proxyUsername; }
    public String proxyPassword() { return proxyPassword; }
    public void setProxyPassword(String proxyPassword) { this.proxyPassword = proxyPassword; }
}
