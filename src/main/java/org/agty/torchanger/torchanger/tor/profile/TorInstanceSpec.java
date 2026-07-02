package org.agty.torchanger.torchanger.tor.profile;

import java.util.List;
import org.agty.torchanger.torchanger.bridge.BridgeCatalog;
import org.agty.torchanger.torchanger.bridge.BridgeCatalogEntry;
import org.agty.torchanger.torchanger.config.AppDefaults;

public final class TorInstanceSpec {
    private final String id;
    private final String name;
    private int socksPort;
    private int httpPort;
    private final String transportLabel;
    private final TorLaunchMode mode;
    private BridgeCatalogEntry bridgeEntry;
    private String status;
    private String ping;
    private String average;
    private boolean autoStart;

    public TorInstanceSpec(String id, String name, int socksPort, int httpPort, String transportLabel, TorLaunchMode mode, BridgeCatalogEntry bridgeEntry) {
        this.id = id;
        this.name = name;
        this.socksPort = socksPort;
        this.httpPort = httpPort;
        this.transportLabel = transportLabel;
        this.mode = mode;
        this.bridgeEntry = bridgeEntry;
        this.status = "idle";
        this.ping = "-";
        this.average = "-";
        this.autoStart = false;
    }

    public static List<TorInstanceSpec> defaults() {
        return BuiltinProfiles.defaults();
    }

    public String id() { return id; }

    public String name() {
        return name;
    }

    public int socksPort() {
        return socksPort;
    }

    public void setSocksPort(int socksPort) {
        this.socksPort = socksPort;
    }

    public int httpPort() { return httpPort; }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int controlPort() { return socksPort + 1000; }

    public String transportLabel() {
        return transportLabel;
    }

    public TorLaunchMode mode() {
        return mode;
    }

    public BridgeCatalogEntry bridgeEntry() {
        return bridgeEntry;
    }

    public void setBridgeEntry(BridgeCatalogEntry bridgeEntry) {
        this.bridgeEntry = bridgeEntry;
    }

    public String bridgeSourceDescription() {
        if (bridgeEntry == null) {
            return switch (mode) {
                case SNOWFLAKE -> ".torchanger/bridges/snowflake.txt";
                case MEEK -> ".torchanger/bridges/meek.txt";
                default -> "not required";
            };
        }
        return ".torchanger/bridge-cache/" + bridgeEntry.fileName();
    }

    public String status() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String ping() {
        return ping;
    }

    public void setPing(String ping) {
        this.ping = ping;
    }

    public String average() {
        return average;
    }

    public void setAverage(String average) {
        this.average = average;
    }

    public boolean autoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
}
