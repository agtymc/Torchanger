package org.agty.torchanger.torchanger;

import java.util.List;

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
        return List.of(
                new TorInstanceSpec("direct", "Direct", AppDefaults.intValue("directSocksPort", 9060), AppDefaults.intValue("directHttpPort", 19060), "vanilla", TorLaunchMode.DIRECT, null),
                new TorInstanceSpec("vanilla-bridges", "Vanilla Bridges", AppDefaults.intValue("vanillaSocksPort", 9061), AppDefaults.intValue("vanillaHttpPort", 19061), "bridge relay", TorLaunchMode.VANILLA_BRIDGE, BridgeCatalog.vanillaTested()),
                new TorInstanceSpec("obfs4-bridges", "obfs4 Bridges", AppDefaults.intValue("obfs4SocksPort", 9062), AppDefaults.intValue("obfs4HttpPort", 19062), "obfs4proxy", TorLaunchMode.OBFS4, BridgeCatalog.obfs4Tested()),
                new TorInstanceSpec("snowflake", "Snowflake", AppDefaults.intValue("snowflakeSocksPort", 9063), AppDefaults.intValue("snowflakeHttpPort", 19063), "snowflake-client", TorLaunchMode.SNOWFLAKE, null),
                new TorInstanceSpec("webtunnel", "WebTunnel", AppDefaults.intValue("webtunnelSocksPort", 9064), AppDefaults.intValue("webtunnelHttpPort", 19064), "lyrebird", TorLaunchMode.WEBTUNNEL, BridgeCatalog.webTunnelTested())
        );
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
            return mode == TorLaunchMode.SNOWFLAKE ? ".torchanger/bridges/snowflake.txt" : "not required";
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
