package org.agty.torchanger.torchanger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TorManager {
    private static final String DEFAULT_SNOWFLAKE_BRIDGE = ResourceConfig.loadText(
            "/org/agty/torchanger/torchanger/config/snowflake-default-bridge.txt",
            "Bridge snowflake 192.0.2.3:80 2B280B23E1107BB62ABFC40DDCC8824814F80A72"
    );
    private static final String DEFAULT_MEEK_BRIDGE = ResourceConfig.loadText(
            "/org/agty/torchanger/torchanger/config/meek-default-bridge.txt",
            "Bridge meek_lite 0.0.2.0:2 url=https://meek.azureedge.net/ front=ajax.aspnetcdn.com"
    );
    private static final String DEFAULT_SNOWFLAKE_BROKER = "https://snowflake-broker.torproject.net/";
    private static final String DEFAULT_SNOWFLAKE_ICE = "stun:stun.l.google.com:19302,stun:stun.cloudflare.com:3478";
    private static final Pattern HOST_PORT_PATTERN = Pattern.compile("(?:with|at)\\s+([^\\s]+:\\d+)");
    private static final Pattern BOOTSTRAP_PATTERN = Pattern.compile("Bootstrapped\\s+(\\d+)%");
    private static final int MAX_BRIDGE_ATTEMPTS = 10;
    private static final int WEBTUNNEL_BRIDGES_IN_TORRC = 75;
    private static final int WEBTUNNEL_HOST_ERROR_LIMIT = 3;
    private static final int SNOWFLAKE_STALL_SECONDS = 60;
    private static final int MEEK_STALL_SECONDS = 60;
    private static final int PACKAGED_CONTROL_PORT_BASE = AppDefaults.intValue("packagedControlPortBase", 30060);
    private static final int DEVELOPMENT_CONTROL_PORT_BASE = AppDefaults.intValue("developmentControlPortBase", 31060);
    private static final List<String> NOISY_WARNING_MARKERS = List.of(
            "Tried to add bridge",
            "Proxy Client: unable to connect OR connection"
    );

    private final Path workspaceRoot;
    private final Path bridgesDir;
    private final Path logsDir;
    private final Path blacklistDir;
    private final Path successDir;
    private final TorManagerListener listener;
    private final Supplier<AppSettings> settingsSupplier;
    private final Map<String, RuntimeInstance> runtimes = new ConcurrentHashMap<>();
    private final ExecutorService processExecutor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
    private static final int BRIDGE_STALL_SECONDS = 20;

    public TorManager(Path workspaceRoot, TorManagerListener listener, Supplier<AppSettings> settingsSupplier) {
        this.workspaceRoot = workspaceRoot;
        this.bridgesDir = workspaceRoot.resolve("bridges");
        this.logsDir = workspaceRoot.resolve("logs");
        this.blacklistDir = workspaceRoot.resolve("bridge-blacklist");
        this.successDir = workspaceRoot.resolve("bridge-success");
        this.listener = listener;
        this.settingsSupplier = settingsSupplier;
    }

    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    public Path getBridgeCacheDir() {
        return workspaceRoot.resolve("bridge-cache");
    }

    public Path getLogsDir() {
        return logsDir;
    }

    public Path getBlacklistDir() {
        return blacklistDir;
    }

    public Path getSuccessDir() {
        return successDir;
    }

    public void prepareWorkspace() throws IOException {
        Files.createDirectories(workspaceRoot);
        Files.createDirectories(bridgesDir);
        Files.createDirectories(logsDir);
        Files.createDirectories(blacklistDir);
        Files.createDirectories(successDir);
        listener.onOverallLog("Workspace directory: " + workspaceRoot.toAbsolutePath());
        listener.onOverallLog("Bridge cache directory: " + getBridgeCacheDir().toAbsolutePath());
        listener.onOverallLog("Logs directory: " + logsDir.toAbsolutePath());
        listener.onOverallLog("Bridge blacklist directory: " + blacklistDir.toAbsolutePath());
        listener.onOverallLog("Bridge success directory: " + successDir.toAbsolutePath());
        BridgeCatalog.prepareCache(workspaceRoot, listener::onOverallLog);
        writeTemplateIfMissing(bridgesDir.resolve(SettingsManager.MANUAL_VANILLA_FILE), "");
        writeTemplateIfMissing(bridgesDir.resolve(SettingsManager.MANUAL_OBFS4_FILE), "");
        writeTemplateIfMissing(bridgesDir.resolve(SettingsManager.MANUAL_WEBTUNNEL_FILE), "");
        writeTemplateIfMissing(bridgesDir.resolve("snowflake.txt"), """
                # Replace with your preferred Snowflake bridge lines if needed.
                %s
                """.formatted(DEFAULT_SNOWFLAKE_BRIDGE));
        writeTemplateIfMissing(bridgesDir.resolve("meek.txt"), """
                # Replace with your preferred meek bridge lines if needed.
                %s
                """.formatted(DEFAULT_MEEK_BRIDGE));
        listener.onOverallLog("Snowflake bridge file: " + bridgesDir.resolve("snowflake.txt").toAbsolutePath());
        listener.onOverallLog("Meek bridge file: " + bridgesDir.resolve("meek.txt").toAbsolutePath());
    }

    public void updateBridgeCache() throws IOException, InterruptedException {
        BridgeCatalog.updateAll(workspaceRoot, listener::onOverallLog);
    }

    public void startAll(List<TorInstanceSpec> specs) {
        specs.forEach(this::startInstance);
    }

    public void stopAll() {
        stopAll("stopAll");
    }

    public void stopAll(String reason) {
        new ArrayList<>(runtimes.values()).forEach(runtime -> stopInstance(runtime.spec, reason));
    }

    public void restartAll(List<TorInstanceSpec> specs) {
        specs.forEach(this::restartInstance);
    }

    public void startInstance(TorInstanceSpec spec) {
        if (runtimes.containsKey(spec.name())) {
            listener.onOverallLog(spec.name() + ": already running");
            return;
        }
        processExecutor.submit(() -> startInternal(spec, 1));
    }

    public void stopInstance(TorInstanceSpec spec) {
        stopInstance(spec, "manual");
    }

    public void stopInstance(TorInstanceSpec spec, String reason) {
        RuntimeInstance runtime = runtimes.remove(spec.name());
        if (runtime == null) {
            listener.onStatus(spec.name(), "stopped");
            listener.onMetrics(spec.name(), "-", "-");
            listener.onOverallLog(spec.name() + ": stop requested but runtime was not active (" + reason + ")");
            return;
        }
        runtime.stopRequested = true;
        runtime.stopReason = reason;
        cancelFuture(runtime.timeoutTask);
        cancelFuture(runtime.healthTask);
        cancelFuture(runtime.progressTask);
        cancelFuture(runtime.transportLogTask);
        if (runtime.httpProxy != null) {
            runtime.httpProxy.stop();
        }
        if (runtime.socksProxy != null) {
            runtime.socksProxy.stop();
        }
        safeAppendLog(runtime, "Stop requested: " + reason);
        if (runtime.process != null && runtime.process.isAlive()) {
            runtime.process.destroy();
            try {
                if (!runtime.process.waitFor(5, TimeUnit.SECONDS)) {
                    runtime.process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        listener.onStatus(spec.name(), "stopped");
        listener.onMetrics(spec.name(), "-", "-");
        safeAppendLog(runtime, "Stopped");
    }

    public void restartInstance(TorInstanceSpec spec) {
        restartInstance(spec, "manual");
    }

    public void restartInstance(TorInstanceSpec spec, String reason) {
        stopInstance(spec, "restart:" + reason);
        startInstance(spec);
    }

    public int activeProcessCount() {
        return (int) runtimes.values().stream()
                .filter(runtime -> runtime.process != null && runtime.process.isAlive())
                .count();
    }

    public Optional<String> getSuccessfulBridgeLine(TorInstanceSpec spec) {
        if (spec.mode() == TorLaunchMode.DIRECT) {
            return Optional.empty();
        }
        RuntimeInstance runtime = runtimes.get(spec.name());
        if (runtime != null && runtime.connected && runtime.selectedBridgeLine != null && !runtime.selectedBridgeLine.isBlank()) {
            return Optional.of(runtime.selectedBridgeLine);
        }
        try {
            return loadPersistedSuccessfulBridgeLine(spec);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void shutdown() {
        stopAll("shutdown");
        scheduler.shutdownNow();
        processExecutor.shutdownNow();
    }

    private void startInternal(TorInstanceSpec spec) {
        startInternal(spec, 1);
    }

    private void startInternal(TorInstanceSpec spec, int attempt) {
        try {
            verifyBinary("tor");
            verifyPortAvailable(spec.socksPort());
            verifyPortAvailable(spec.httpPort());
            verifyPortAvailable(torSocksPortFor(spec));
            verifyPortAvailable(controlPortFor(spec));

            Path instanceDir = workspaceRoot.resolve(spec.id());
            Path dataDir = instanceDir.resolve("data");
            Path runDir = instanceDir.resolve("run");
            Path logFile = logsDir.resolve(spec.id() + ".log");
            Files.createDirectories(dataDir);
            Files.createDirectories(runDir);
            Files.writeString(logFile, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            RuntimeInstance runtime = new RuntimeInstance(spec, logFile, attempt);
            runtimes.put(spec.name(), runtime);
            listener.onStatus(spec.name(), "starting");
            listener.onMetrics(spec.name(), "-", "-");
            listener.onOverallLog(spec.name() + ": launch attempt " + attempt);

            List<String> torrcLines = buildTorrc(spec, dataDir, runDir, runtime);
            Path torrc = instanceDir.resolve("torrc");
            Files.writeString(torrc, String.join(System.lineSeparator(), torrcLines) + System.lineSeparator(), StandardCharsets.UTF_8);

            runtime.process = new ProcessBuilder("tor", "-f", torrc.toAbsolutePath().toString())
                .directory(workspaceRoot.toFile())
                .redirectErrorStream(true)
                .start();
            runtime.lastBootstrapAt = System.nanoTime();
            runtime.timeoutTask = scheduler.schedule(() -> handleBootstrapTimeout(runtime), timeoutFor(spec), TimeUnit.SECONDS);
            runtime.progressTask = scheduler.scheduleAtFixedRate(() -> checkBootstrapStall(runtime), 5, 5, TimeUnit.SECONDS);
            if (runtime.transportLogFile != null) {
                runtime.transportLogTask = scheduler.scheduleAtFixedRate(() -> pollTransportLog(runtime), 1, 1, TimeUnit.SECONDS);
            }
            processExecutor.submit(() -> readProcessOutput(runtime));
        } catch (Exception e) {
            listener.onStatus(spec.name(), "error");
            listener.onOverallLog(spec.name() + ": " + e.getMessage());
        }
    }

    private List<String> buildTorrc(TorInstanceSpec spec, Path dataDir, Path runDir, RuntimeInstance runtime) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("Log " + settingsSupplier.get().torLogLevel() + " stdout");
        lines.add("DataDirectory " + dataDir.toAbsolutePath());
        lines.add("GeoIPFile /usr/share/tor/geoip");
        lines.add("GeoIPv6File /usr/share/tor/geoip6");
        lines.add("SOCKSPort 127.0.0.1:" + torSocksPortFor(spec));
        lines.add("ControlPort 127.0.0.1:" + controlPortFor(spec));
        lines.add("CookieAuthentication 1");
        lines.add("ClientOnly 1");
        lines.add("AvoidDiskWrites 1");
        lines.add("UseBridges " + (spec.mode() == TorLaunchMode.DIRECT ? "0" : "1"));
        if (spec.mode() != TorLaunchMode.DIRECT) {
            lines.add("ClientUseIPv4 1");
            lines.add("ClientUseIPv6 0");
            lines.add("AllowNonRFC953Hostnames 1");
            lines.add("EnforceDistinctSubnets 0");
            lines.add("MaxClientCircuitsPending 64");
            lines.add("LearnCircuitBuildTimeout 0");
            if (spec.mode() == TorLaunchMode.WEBTUNNEL) {
                lines.add("CircuitBuildTimeout 60");
            }
        }

        switch (spec.mode()) {
            case DIRECT -> {
            }
            case VANILLA_BRIDGE -> lines.addAll(readBridgeLines(spec.bridgeEntry(), runtime));
            case OBFS4 -> {
                verifyBinary("obfs4proxy");
                lines.add("ClientTransportPlugin obfs4 exec /usr/bin/obfs4proxy");
                lines.addAll(readBridgeLines(spec.bridgeEntry(), runtime));
            }
            case SNOWFLAKE -> {
                verifyBinary("snowflake-client");
                runtime.transportLogFile = runDir.resolve("snowflake.log");
                Files.writeString(runtime.transportLogFile, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                lines.add(
                        "ClientTransportPlugin snowflake exec /usr/bin/snowflake-client"
                                + " -url " + DEFAULT_SNOWFLAKE_BROKER
                                + " -ice " + DEFAULT_SNOWFLAKE_ICE
                                + " -log " + runtime.transportLogFile.toAbsolutePath()
                );
                lines.addAll(readSnowflakeLines(runtime));
            }
            case MEEK -> {
                verifyBinary("lyrebird");
                lines.add("ClientTransportPlugin meek_lite exec /usr/bin/lyrebird");
                lines.addAll(readMeekLines(runtime));
            }
            case WEBTUNNEL -> {
                verifyBinary("lyrebird");
                lines.add("ClientTransportPlugin webtunnel exec /usr/bin/lyrebird");
                lines.addAll(readWebTunnelBridgeLines(spec.bridgeEntry(), runtime));
            }
        }
        return lines;
    }

    private void readProcessOutput(RuntimeInstance runtime) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(runtime.process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                extractBrokenBridge(runtime, line);
                updateBootstrapStatus(runtime, line);
                if (shouldDisplayLogLine(line)) {
                    safeAppendLog(runtime, line);
                }
                if (line.contains("Bootstrapped 100%")) {
                    onConnected(runtime);
                }
            }
            int exitCode = runtime.process.waitFor();
            cancelFuture(runtime.healthTask);
            cancelFuture(runtime.timeoutTask);
            cancelFuture(runtime.progressTask);
            cancelFuture(runtime.transportLogTask);
            if (!runtime.stopRequested) {
                listener.onStatus(runtime.spec.name(), exitCode == 0 ? "finished" : "exited");
                listener.onMetrics(runtime.spec.name(), "-", average(runtime.samples));
                persistBlacklist(runtime);
                if (!runtime.connected) {
                    scheduleBridgeRetry(runtime, "process exited with code " + exitCode);
                }
            }
        } catch (Exception e) {
            if (!runtime.stopRequested) {
                listener.onStatus(runtime.spec.name(), "error");
                safeAppendLog(runtime, "Reader failed: " + e.getMessage());
                persistBlacklist(runtime);
                if (!runtime.connected) {
                    scheduleBridgeRetry(runtime, "reader failure");
                }
            }
        } finally {
            if (runtime.httpProxy != null) {
                runtime.httpProxy.stop();
            }
            if (runtime.socksProxy != null) {
                runtime.socksProxy.stop();
            }
            cancelFuture(runtime.transportLogTask);
            removeRuntime(runtime);
        }
    }

    private void onConnected(RuntimeInstance runtime) {
        if (runtime.connected) {
            return;
        }
        runtime.connected = true;
        cancelFuture(runtime.timeoutTask);
        cancelFuture(runtime.progressTask);
        cancelFuture(runtime.transportLogTask);
        listener.onStatus(runtime.spec.name(), "connected");
        persistSuccessfulBridge(runtime);
        int torSocksPort = torSocksPortFor(runtime.spec);
        safeAppendLog(runtime, "Tor SOCKS5 ready on 127.0.0.1:" + torSocksPort);
        try {
            runtime.socksProxy = new SimpleSocks5ProxyServer(
                    runtime.spec.socksPort(),
                    torSocksPort,
                    settingsSupplier.get().proxyUsername(),
                    settingsSupplier.get().proxyPassword(),
                    message -> safeAppendLog(runtime, message)
            );
            runtime.socksProxy.start();
            safeAppendLog(runtime, "SOCKS5 app proxy ready on 127.0.0.1:" + runtime.spec.socksPort());
            runtime.httpProxy = new SimpleHttpProxyServer(
                    runtime.spec.httpPort(),
                    torSocksPort,
                    settingsSupplier.get().proxyUsername(),
                    settingsSupplier.get().proxyPassword(),
                    message -> safeAppendLog(runtime, message)
            );
            runtime.httpProxy.start();
            safeAppendLog(runtime, "HTTP app proxy ready on 127.0.0.1:" + runtime.spec.httpPort());
            if (!settingsSupplier.get().proxyUsername().isBlank()) {
                safeAppendLog(runtime, "SOCKS5/HTTP proxy authentication enabled for user: " + settingsSupplier.get().proxyUsername());
            }
        } catch (IOException e) {
            safeAppendLog(runtime, "App proxy failed: " + e.getMessage());
        }
        runtime.healthTask = scheduler.scheduleAtFixedRate(
                () -> runHealthCheck(runtime),
                1,
                settingsSupplier.get().healthCheckSeconds(),
                TimeUnit.SECONDS
        );
    }

    private void handleBootstrapTimeout(RuntimeInstance runtime) {
        if (runtime.connected || runtime.stopRequested) {
            return;
        }
        runtime.stopRequested = true;
        listener.onStatus(runtime.spec.name(), "timeout");
        safeAppendLog(runtime, "Bootstrap timeout");
        persistBlacklist(runtime);
        cancelFuture(runtime.progressTask);
        cancelFuture(runtime.transportLogTask);
        if (runtime.process != null && runtime.process.isAlive()) {
            runtime.process.destroyForcibly();
        }
        removeRuntime(runtime);
        scheduleBridgeRetry(runtime, "bootstrap timeout");
    }

    private void runHealthCheck(RuntimeInstance runtime) {
        if (!runtime.connected || runtime.process == null || !runtime.process.isAlive()) {
            return;
        }
        try {
            Process process = new ProcessBuilder(
                    "curl", "--socks5-hostname", "127.0.0.1:" + torSocksPortFor(runtime.spec),
                    "--max-time", "20", "-o", "/dev/null", "-s", "-w", "%{time_total}",
                    "https://check.torproject.org/api/ip"
            ).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int code = process.waitFor();
            if (code != 0 || output.isBlank()) {
                listener.onMetrics(runtime.spec.name(), "timeout", average(runtime.samples));
                return;
            }
            long ping = Math.round(Double.parseDouble(output) * 1000.0);
            runtime.samples.addLast(ping);
            while (runtime.samples.size() > 10) {
                runtime.samples.removeFirst();
            }
            listener.onMetrics(runtime.spec.name(), ping + " ms", average(runtime.samples));
        } catch (Exception e) {
            listener.onMetrics(runtime.spec.name(), "timeout", average(runtime.samples));
        }
    }

    private List<String> readBridgeLines(BridgeCatalogEntry entry, RuntimeInstance runtime) throws IOException {
        if (entry == null) {
            throw new IOException("bridge source is not configured");
        }
        List<String> candidates = loadBridgeCandidates(entry, runtime);
        if (candidates.isEmpty()) {
            throw new IOException("all bridges are blacklisted or unavailable for " + entry.fileName());
        }
        runtime.maxBridgeAttempts = Math.min(MAX_BRIDGE_ATTEMPTS, candidates.size());
        int index = Math.min(runtime.attempt - 1, candidates.size() - 1);
        String selected = candidates.get(index);
        runtime.selectedBridgeLine = selected;
        runtime.selectedBridgeEndpoint = extractHostPortFromBridgeLine(selected.substring("Bridge ".length()));
        safeAppendLog(runtime, "Using bridge " + runtime.selectedBridgeEndpoint + " (attempt " + runtime.attempt + "/" + runtime.maxBridgeAttempts + ")");
        return List.of(selected);
    }

    private List<String> readWebTunnelBridgeLines(BridgeCatalogEntry entry, RuntimeInstance runtime) throws IOException {
        if (entry == null) {
            throw new IOException("bridge source is not configured");
        }
        List<String> candidates = loadBridgeCandidates(entry, runtime);
        if (candidates.isEmpty()) {
            throw new IOException("all bridges are blacklisted or unavailable for " + entry.fileName());
        }
        runtime.maxBridgeAttempts = Math.min(MAX_BRIDGE_ATTEMPTS, candidates.size());
        int startIndex = Math.min(runtime.attempt - 1, candidates.size() - 1);
        runtime.selectedBridgeLine = candidates.get(startIndex);
        runtime.selectedBridgeEndpoint = extractHostPortFromBridgeLine(runtime.selectedBridgeLine.substring("Bridge ".length()));
        int endIndex = Math.min(startIndex + WEBTUNNEL_BRIDGES_IN_TORRC, candidates.size());
        int limit = endIndex - startIndex;
        safeAppendLog(
                runtime,
                "Using WebTunnel bridge pool with " + limit + " entries"
                        + " (offset " + (startIndex + 1) + "/" + candidates.size() + ")"
        );
        return new ArrayList<>(candidates.subList(startIndex, endIndex));
    }

    private List<String> readSnowflakeLines(RuntimeInstance runtime) throws IOException {
        Path file = bridgesDir.resolve("snowflake.txt");
        String preferredBridge = loadLastSuccessfulBridge("snowflake.txt");
        List<String> candidates = new ArrayList<>();
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String normalized = trimmed.startsWith("Bridge ") ? trimmed : "Bridge " + trimmed;
                if (preferredBridge != null && preferredBridge.equals(normalized)) {
                    candidates.add(0, normalized);
                } else {
                    candidates.add(normalized);
                }
            }
        }
        if (candidates.isEmpty()) {
            candidates = List.of(DEFAULT_SNOWFLAKE_BRIDGE);
        }
        safeAppendLog(runtime, "Using bridge file " + file.toAbsolutePath() + " (" + candidates.size() + " entries)");
        runtime.bridgeFileName = "snowflake.txt";
        runtime.bridgeCandidates = candidates;
        runtime.maxBridgeAttempts = MAX_BRIDGE_ATTEMPTS;
        int index = Math.floorMod(runtime.attempt - 1, candidates.size());
        String selected = candidates.get(index);
        runtime.selectedBridgeLine = selected;
        runtime.selectedBridgeKey = selected;
        runtime.selectedBridgeEndpoint = extractHostPortFromBridgeLine(selected.substring("Bridge ".length()));
        safeAppendLog(
                runtime,
                "Using Snowflake bridge line "
                        + indexForHumans(index) + "/" + candidates.size()
                        + " (attempt " + runtime.attempt + "/" + runtime.maxBridgeAttempts + ")"
        );
        String front = extractSnowflakeParam(selected, "front=");
        if (front != null) {
            safeAppendLog(runtime, "Snowflake front: " + front);
        }
        return List.of(selected);
    }

    private List<String> readMeekLines(RuntimeInstance runtime) throws IOException {
        Path file = bridgesDir.resolve("meek.txt");
        String preferredBridge = loadLastSuccessfulBridge("meek.txt");
        Set<String> blacklist = loadBlacklist("meek.txt");
        List<String> candidates = new ArrayList<>();
        int skippedByBlacklist = 0;
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String normalized = trimmed.startsWith("Bridge ") ? trimmed : "Bridge " + trimmed;
                if (blacklist.contains("bridge:" + normalized)) {
                    skippedByBlacklist++;
                    continue;
                }
                if (preferredBridge != null && preferredBridge.equals(normalized)) {
                    candidates.add(0, normalized);
                } else {
                    candidates.add(normalized);
                }
            }
        }
        if (candidates.isEmpty()) {
            candidates = List.of(DEFAULT_MEEK_BRIDGE);
        }
        safeAppendLog(runtime, "Using bridge file " + file.toAbsolutePath() + " (" + candidates.size() + " entries)");
        if (skippedByBlacklist > 0) {
            safeAppendLog(runtime, "Meek bridge lines skipped by blacklist: " + skippedByBlacklist);
        }
        runtime.bridgeFileName = "meek.txt";
        runtime.bridgeCandidates = candidates;
        runtime.maxBridgeAttempts = Math.min(MAX_BRIDGE_ATTEMPTS, candidates.size());
        int index = Math.min(runtime.attempt - 1, candidates.size() - 1);
        String selected = candidates.get(index);
        runtime.selectedBridgeLine = selected;
        runtime.selectedBridgeKey = selected;
        runtime.selectedBridgeEndpoint = extractHostPortFromBridgeLine(selected.substring("Bridge ".length()));
        safeAppendLog(runtime, "Using Meek bridge line " + indexForHumans(index) + "/" + candidates.size()
                + " (attempt " + runtime.attempt + "/" + runtime.maxBridgeAttempts + ")");
        String front = extractSnowflakeParam(selected, "front=");
        if (front != null) {
            safeAppendLog(runtime, "Meek front: " + front);
        }
        return List.of(selected);
    }

    private List<String> loadBridgeCandidates(BridgeCatalogEntry entry, RuntimeInstance runtime) throws IOException {
        Path file = isManualBridgeFile(entry.fileName())
                ? bridgesDir.resolve(entry.fileName())
                : BridgeCatalog.resolveCachedPath(workspaceRoot, entry);
        Set<String> blacklist = loadBlacklist(entry.fileName());
        List<String> candidates = new ArrayList<>();
        String preferredEndpoint = loadLastSuccessfulBridge(entry.fileName());
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String normalized = trimmed.startsWith("Bridge ") ? trimmed.substring("Bridge ".length()) : trimmed;
            String hostPort = extractHostPortFromBridgeLine(normalized);
            String urlHost = extractUrlHostFromBridgeLine(normalized);
            if (isBlacklisted(hostPort, urlHost, blacklist)) {
                continue;
            }
            if (preferredEndpoint != null && preferredEndpoint.equals(hostPort)) {
                candidates.add(0, "Bridge " + normalized);
            } else {
                candidates.add("Bridge " + normalized);
            }
        }
        runtime.bridgeFileName = entry.fileName();
        runtime.bridgeCandidates = candidates;
        safeAppendLog(runtime, "Using bridge file " + file.toAbsolutePath() + " (" + candidates.size() + " entries)");
        return candidates;
    }

    private boolean isManualBridgeFile(String fileName) {
        return SettingsManager.MANUAL_VANILLA_FILE.equals(fileName)
                || SettingsManager.MANUAL_OBFS4_FILE.equals(fileName)
                || SettingsManager.MANUAL_WEBTUNNEL_FILE.equals(fileName);
    }

    private String extractSnowflakeParam(String line, String keyPrefix) {
        String[] parts = line.split("\\s+");
        for (String part : parts) {
            if (part.startsWith(keyPrefix)) {
                return part.substring(keyPrefix.length());
            }
        }
        return null;
    }

    private int indexForHumans(int zeroBasedIndex) {
        return zeroBasedIndex + 1;
    }

    private void extractBrokenBridge(RuntimeInstance runtime, String line) {
        if (runtime.bridgeFileName == null) {
            return;
        }
        if (runtime.spec.mode() == TorLaunchMode.MEEK) {
            extractBrokenMeekBridge(runtime, line);
        }
        if (runtime.spec.mode() == TorLaunchMode.WEBTUNNEL) {
            extractBrokenWebTunnelBridge(runtime, line);
        }
        if (!(line.contains("[warn]") || line.contains("identity keys were not as expected") || line.contains("unable to connect"))) {
            return;
        }
        Matcher matcher = HOST_PORT_PATTERN.matcher(line);
        if (matcher.find()) {
            runtime.badEndpoints.add(matcher.group(1));
        }
    }

    private void extractBrokenMeekBridge(RuntimeInstance runtime, String line) {
        if (!line.contains("Managed proxy \"/usr/bin/lyrebird\": Failed to connect to")) {
            return;
        }
        runtime.badBridgeKeys.add(runtime.selectedBridgeKey);
        if (line.contains("status code was 403") && !runtime.meekFrontRejectedLogged) {
            runtime.meekFrontRejectedLogged = true;
            safeAppendLog(runtime, "Meek hint: the current front/url pair was rejected with HTTP 403. This usually means the bridge line is outdated or this CDN front no longer works.");
            safeAppendLog(runtime, "Meek hint: add another meek_lite bridge line to bridges/meek.txt and retry.");
        }
    }

    private void extractBrokenWebTunnelBridge(RuntimeInstance runtime, String line) {
        String sniMarker = "Managed proxy \"/usr/bin/lyrebird\": Using TLS SNI: ";
        int sniIndex = line.indexOf(sniMarker);
        if (sniIndex >= 0) {
            runtime.lastWebTunnelSni = line.substring(sniIndex + sniMarker.length()).trim();
            return;
        }
        if (!line.contains("Managed proxy \"/usr/bin/lyrebird\": Error dialing:")) {
            return;
        }
        if (runtime.lastWebTunnelSni != null && !runtime.lastWebTunnelSni.isBlank()) {
            runtime.badUrlHosts.add(runtime.lastWebTunnelSni);
            int errorCount = runtime.webTunnelHostErrors.merge(runtime.lastWebTunnelSni, 1, Integer::sum);
            if (errorCount == 1) {
                safeAppendLog(runtime, "Marked failing WebTunnel host: " + runtime.lastWebTunnelSni);
            }
            if (errorCount >= WEBTUNNEL_HOST_ERROR_LIMIT) {
                triggerWebTunnelHostFailure(runtime, runtime.lastWebTunnelSni, errorCount);
            }
        }
    }

    private void triggerWebTunnelHostFailure(RuntimeInstance runtime, String host, int errorCount) {
        if (runtime.stopRequested || runtime.connected || runtime.process == null || !runtime.process.isAlive()) {
            return;
        }
        runtime.stopRequested = true;
        listener.onStatus(runtime.spec.name(), "retrying");
        safeAppendLog(runtime, "WebTunnel host " + host + " failed " + errorCount + " times, switching bridge pool");
        persistBlacklist(runtime);
        cancelFuture(runtime.timeoutTask);
        cancelFuture(runtime.progressTask);
        cancelFuture(runtime.transportLogTask);
        if (runtime.process.isAlive()) {
            runtime.process.destroyForcibly();
        }
        removeRuntime(runtime);
        scheduleBridgeRetry(runtime, "failing WebTunnel host " + host);
    }

    private void persistBlacklist(RuntimeInstance runtime) {
        if (runtime.spec.mode() == TorLaunchMode.SNOWFLAKE) {
            return;
        }
        if (runtime.bridgeFileName == null
                || (runtime.badEndpoints.isEmpty() && runtime.badBridgeKeys.isEmpty() && runtime.badUrlHosts.isEmpty())) {
            return;
        }
        Path blacklistFile = metadataFile(blacklistDir, runtime.bridgeFileName);
        try {
            Set<String> merged = new HashSet<>(loadBlacklist(runtime.bridgeFileName));
            for (String endpoint : runtime.badEndpoints) {
                merged.add("endpoint:" + endpoint);
            }
            for (String bridgeKey : runtime.badBridgeKeys) {
                merged.add("bridge:" + bridgeKey);
            }
            for (String urlHost : runtime.badUrlHosts) {
                merged.add("urlhost:" + urlHost);
            }
            Files.writeString(
                    blacklistFile,
                    String.join(System.lineSeparator(), merged) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            listener.onOverallLog(
                    runtime.spec.name() + ": blacklisted "
                            + runtime.badEndpoints.size() + " endpoints and "
                            + runtime.badBridgeKeys.size() + " bridge keys and "
                            + runtime.badUrlHosts.size() + " WebTunnel hosts"
            );
        } catch (IOException e) {
            listener.onOverallLog(runtime.spec.name() + ": blacklist write failed: " + e.getMessage());
        }
    }

    private Set<String> loadBlacklist(String fileName) throws IOException {
        Path file = metadataFileOrLegacy(blacklistDir, fileName);
        if (!Files.exists(file)) {
            return Set.of();
        }
        return new HashSet<>(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    private String extractHostPortFromBridgeLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        return switch (parts[0]) {
            case "obfs4", "webtunnel", "snowflake", "meek", "meek_lite" -> parts[1];
            default -> parts[0];
        };
    }

    private String extractUrlHostFromBridgeLine(String line) {
        int urlIndex = line.indexOf("url=https://");
        if (urlIndex < 0) {
            return null;
        }
        int valueStart = urlIndex + "url=".length();
        int valueEnd = line.indexOf(' ', valueStart);
        String rawUrl = valueEnd >= 0 ? line.substring(valueStart, valueEnd) : line.substring(valueStart);
        try {
            return URI.create(rawUrl).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isBlacklisted(String hostPort, String urlHost, Set<String> blacklist) {
        if (hostPort != null && (blacklist.contains(hostPort) || blacklist.contains("endpoint:" + hostPort))) {
            return true;
        }
        return urlHost != null && blacklist.contains("urlhost:" + urlHost);
    }

    private boolean shouldDisplayLogLine(String line) {
        AppSettings settings = settingsSupplier.get();
        if (!settings.hideNoisyBridgeWarnings()) {
            return true;
        }
        if (!line.contains("[warn]")) {
            return true;
        }
        for (String marker : NOISY_WARNING_MARKERS) {
            if (line.contains(marker)) {
                return false;
            }
        }
        return true;
    }

    private void updateBootstrapStatus(RuntimeInstance runtime, String line) {
        Matcher matcher = BOOTSTRAP_PATTERN.matcher(line);
        if (matcher.find() && !runtime.connected) {
            int percent = Integer.parseInt(matcher.group(1));
            if (percent != runtime.lastBootstrapPercent) {
                runtime.lastBootstrapPercent = percent;
                runtime.lastBootstrapAt = System.nanoTime();
            }
            listener.onStatus(runtime.spec.name(), "starting " + percent + "%");
        }
    }

    private void checkBootstrapStall(RuntimeInstance runtime) {
        if (runtime.connected || runtime.stopRequested || runtime.process == null || !runtime.process.isAlive()) {
            return;
        }
        long stalledFor = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - runtime.lastBootstrapAt);
        int stallLimit = stallLimitFor(runtime);
        if (stalledFor < stallLimit) {
            return;
        }
        if (runtime.spec.mode() == TorLaunchMode.DIRECT) {
            return;
        }
        runtime.stopRequested = true;
        listener.onStatus(runtime.spec.name(), "timeout");
        safeAppendLog(
                runtime,
                "Bootstrap stalled at " + runtime.lastBootstrapPercent + "% for " + stalledFor + " seconds"
                        + " (limit " + stallLimit + "s)"
        );
        persistBlacklist(runtime);
        cancelFuture(runtime.timeoutTask);
        cancelFuture(runtime.progressTask);
        cancelFuture(runtime.transportLogTask);
        if (runtime.process != null && runtime.process.isAlive()) {
            runtime.process.destroyForcibly();
        }
        removeRuntime(runtime);
        scheduleBridgeRetry(runtime, "bootstrap stalled at " + runtime.lastBootstrapPercent + "%");
    }

    private void scheduleBridgeRetry(RuntimeInstance runtime, String reason) {
        if (runtime.spec.mode() == TorLaunchMode.DIRECT) {
            return;
        }
        if (runtime.attempt >= runtime.maxBridgeAttempts) {
            listener.onOverallLog(runtime.spec.name() + ": bridge retry limit reached after " + runtime.attempt + " attempts");
            return;
        }
        if (runtime.spec.mode() == TorLaunchMode.MEEK && runtime.selectedBridgeKey != null) {
            runtime.badBridgeKeys.add(runtime.selectedBridgeKey);
            persistBlacklist(runtime);
        } else if (runtime.spec.mode() != TorLaunchMode.SNOWFLAKE && runtime.selectedBridgeEndpoint != null) {
            runtime.badEndpoints.add(runtime.selectedBridgeEndpoint);
            persistBlacklist(runtime);
        }
        listener.onOverallLog(runtime.spec.name() + ": switching to another bridge after " + reason + " (attempt " + (runtime.attempt + 1) + "/" + runtime.maxBridgeAttempts + ")");
        scheduler.schedule(() -> startInternal(runtime.spec, runtime.attempt + 1), 2, TimeUnit.SECONDS);
    }

    private void pollTransportLog(RuntimeInstance runtime) {
        Path transportLogFile = runtime.transportLogFile;
        if (transportLogFile == null || !Files.exists(transportLogFile)) {
            return;
        }
        try (RandomAccessFile file = new RandomAccessFile(transportLogFile.toFile(), "r")) {
            long length = file.length();
            if (length < runtime.transportLogOffset) {
                runtime.transportLogOffset = 0;
            }
            file.seek(runtime.transportLogOffset);
            String line;
            while ((line = file.readLine()) != null) {
                String decoded = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                if (decoded.isBlank()) {
                    continue;
                }
                safeAppendLog(runtime, "[transport] " + decoded);
                handleTransportMilestones(runtime, decoded);
            }
            runtime.transportLogOffset = file.getFilePointer();
        } catch (IOException e) {
            if (!runtime.stopRequested) {
                safeAppendLog(runtime, "Transport log read failed: " + e.getMessage());
            }
        }
    }

    private String loadLastSuccessfulBridge(String fileName) throws IOException {
        Path file = metadataFileOrLegacy(successDir, fileName);
        if (!Files.exists(file)) {
            return null;
        }
        String value = Files.readString(file, StandardCharsets.UTF_8).trim();
        return value.isEmpty() ? null : value;
    }

    private void persistSuccessfulBridge(RuntimeInstance runtime) {
        if (runtime.bridgeFileName == null) {
            return;
        }
        Path file = metadataFile(successDir, runtime.bridgeFileName);
        try {
            String value = runtime.spec.mode() == TorLaunchMode.SNOWFLAKE ? runtime.selectedBridgeKey : runtime.selectedBridgeEndpoint;
            if (value == null || value.isBlank()) {
                return;
            }
            Files.writeString(
                    file,
                    value + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            listener.onOverallLog(runtime.spec.name() + ": saved last successful bridge " + value);
        } catch (IOException e) {
            listener.onOverallLog(runtime.spec.name() + ": failed to save last successful bridge: " + e.getMessage());
        }
    }

    private Path metadataFile(Path dir, String fileName) {
        return dir.resolve(fileName);
    }

    private Path metadataFileOrLegacy(Path dir, String fileName) {
        Path current = metadataFile(dir, fileName);
        if (Files.exists(current)) {
            return current;
        }
        Path legacy = dir.resolve(fileName + ".txt");
        return Files.exists(legacy) ? legacy : current;
    }

    private Optional<String> loadPersistedSuccessfulBridgeLine(TorInstanceSpec spec) throws IOException {
        if (spec.mode() == TorLaunchMode.SNOWFLAKE || spec.mode() == TorLaunchMode.MEEK) {
            String line = loadLastSuccessfulBridge(spec.mode() == TorLaunchMode.MEEK ? "meek.txt" : "snowflake.txt");
            return line == null || line.isBlank() ? Optional.empty() : Optional.of(line);
        }
        BridgeCatalogEntry entry = spec.bridgeEntry();
        if (entry == null) {
            return Optional.empty();
        }
        String endpoint = loadLastSuccessfulBridge(entry.fileName());
        if (endpoint == null || endpoint.isBlank()) {
            return Optional.empty();
        }
        Path file = isManualBridgeFile(entry.fileName())
                ? bridgesDir.resolve(entry.fileName())
                : BridgeCatalog.resolveCachedPath(workspaceRoot, entry);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String normalized = trimmed.startsWith("Bridge ") ? trimmed : "Bridge " + trimmed;
            String hostPort = extractHostPortFromBridgeLine(normalized.substring("Bridge ".length()));
            if (endpoint.equals(hostPort)) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }

    private int timeoutFor(TorInstanceSpec spec) {
        AppSettings settings = settingsSupplier.get();
        if (spec.mode() == TorLaunchMode.WEBTUNNEL) {
            return Math.max(settings.bridgeTimeoutSeconds(), 180);
        }
        return spec.mode() == TorLaunchMode.DIRECT ? settings.directTimeoutSeconds() : settings.bridgeTimeoutSeconds();
    }

    private int stallLimitFor(RuntimeInstance runtime) {
        if (runtime.spec.mode() == TorLaunchMode.SNOWFLAKE) {
            return SNOWFLAKE_STALL_SECONDS;
        }
        if (runtime.spec.mode() == TorLaunchMode.MEEK) {
            return MEEK_STALL_SECONDS;
        }
        return BRIDGE_STALL_SECONDS;
    }

    private void handleTransportMilestones(RuntimeInstance runtime, String decoded) {
        if (runtime.spec.mode() != TorLaunchMode.SNOWFLAKE || runtime.connected) {
            return;
        }
        if (decoded.contains("No messages received for 20s") || decoded.contains("no messages received, closing stale connection")) {
            runtime.snowflakeStalePeerCount++;
            runtime.snowflakePeerAssigned = false;
            safeAppendLog(runtime, "Snowflake: stale peer closed, waiting for next proxy (stale peers: " + runtime.snowflakeStalePeerCount + ")");
            return;
        }
        if (decoded.contains("timeout waiting for DataChannel.OnOpen")) {
            runtime.snowflakeOpenTimeoutCount++;
            runtime.snowflakePeerAssigned = false;
            safeAppendLog(runtime, "Snowflake: peer timed out before channel open (timeouts: " + runtime.snowflakeOpenTimeoutCount + ")");
            return;
        }
        if (decoded.contains("Connected to broker") || decoded.contains("BrokerChannel connected") || decoded.contains("HTTP rendezvous response: 200")) {
            if (runtime.transportMilestones.add("broker")) {
                safeAppendLog(runtime, "Snowflake: broker connected");
            }
        }
        if (decoded.contains("Received Answer")) {
            if (runtime.transportMilestones.add("answer")) {
                safeAppendLog(runtime, "Snowflake: received broker answer");
            }
        }
        if (decoded.contains("DataChannel created")) {
            if (runtime.transportMilestones.add("datachannel_created")) {
                safeAppendLog(runtime, "Snowflake: WebRTC channel created");
            }
        }
        boolean channelOpen = decoded.contains("DataChannel.OnOpen") || decoded.contains("webRTC connection connected");
        if (channelOpen) {
            if (runtime.transportMilestones.add("webrtc")) {
                safeAppendLog(runtime, "Snowflake: WebRTC channel opened");
            }
        }
        if (decoded.contains("Handler: snowflake assigned") || channelOpen) {
            runtime.snowflakePeerAssigned = true;
        }
    }


    private String average(Deque<Long> samples) {
        if (samples.isEmpty()) {
            return "-";
        }
        long sum = 0;
        for (Long sample : samples) {
            sum += sample;
        }
        return Math.round((double) sum / samples.size()) + " ms";
    }

    private void safeAppendLog(RuntimeInstance runtime, String line) {
        try {
            listener.onOverallLog(runtime.spec.name() + ": " + line);
            listener.onInstanceLog(runtime.spec.name(), line);
            Files.writeString(runtime.logFile, line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            listener.onOverallLog(runtime.spec.name() + ": " + line);
        }
    }

    private void verifyBinary(String binary) throws IOException {
        Process process = new ProcessBuilder("which", binary).redirectErrorStream(true).start();
        try {
            if (process.waitFor() != 0) {
                throw new IOException("binary not found: " + binary);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("binary check interrupted: " + binary, e);
        }
    }

    private void verifyPortAvailable(int port) throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress("127.0.0.1", port));
        } catch (IOException e) {
            throw new IOException("port 127.0.0.1:" + port + " is already in use", e);
        }
    }

    private void cancelFuture(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private int controlPortFor(TorInstanceSpec spec) {
        int base = AppEnvironment.isPackaged() ? PACKAGED_CONTROL_PORT_BASE : DEVELOPMENT_CONTROL_PORT_BASE;
        return base + switch (spec.id()) {
            case "direct" -> 0;
            case "vanilla-bridges" -> 1;
            case "obfs4-bridges" -> 2;
            case "snowflake" -> 3;
            case "meek" -> 4;
            case "webtunnel" -> 5;
            default -> 50;
        };
    }

    private int torSocksPortFor(TorInstanceSpec spec) {
        AppSettings settings = settingsSupplier.get();
        return switch (spec.id()) {
            case "direct" -> settings.directTorSocksPort();
            case "vanilla-bridges" -> settings.vanillaTorSocksPort();
            case "obfs4-bridges" -> settings.obfs4TorSocksPort();
            case "snowflake" -> settings.snowflakeTorSocksPort();
            case "meek" -> settings.meekTorSocksPort();
            case "webtunnel" -> settings.webtunnelTorSocksPort();
            default -> 0;
        };
    }

    private void removeRuntime(RuntimeInstance runtime) {
        runtimes.remove(runtime.spec.name(), runtime);
    }

    private void writeTemplateIfMissing(Path path, String content) throws IOException {
        if (!Files.exists(path)) {
            Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
    }

    private static final class RuntimeInstance {
        private final TorInstanceSpec spec;
        private final Path logFile;
        private final int attempt;
        private final Deque<Long> samples = new ArrayDeque<>();
        private final Set<String> badEndpoints = new HashSet<>();
        private final Set<String> badBridgeKeys = new HashSet<>();
        private final Set<String> badUrlHosts = new HashSet<>();
        private final Set<String> transportMilestones = ConcurrentHashMap.newKeySet();
        private final Map<String, Integer> webTunnelHostErrors = new ConcurrentHashMap<>();
        private volatile List<String> bridgeCandidates = List.of();
        private volatile int maxBridgeAttempts = MAX_BRIDGE_ATTEMPTS;
        private volatile String selectedBridgeLine;
        private volatile String selectedBridgeEndpoint;
        private volatile String selectedBridgeKey;
        private volatile String lastWebTunnelSni;
        private volatile int lastBootstrapPercent;
        private volatile long lastBootstrapAt;
        private volatile Process process;
        private volatile boolean connected;
        private volatile boolean stopRequested;
        private volatile String stopReason;
        private volatile ScheduledFuture<?> timeoutTask;
        private volatile ScheduledFuture<?> progressTask;
        private volatile ScheduledFuture<?> transportLogTask;
        private volatile ScheduledFuture<?> healthTask;
        private volatile SimpleSocks5ProxyServer socksProxy;
        private volatile SimpleHttpProxyServer httpProxy;
        private volatile String bridgeFileName;
        private volatile Path transportLogFile;
        private volatile long transportLogOffset;
        private volatile boolean snowflakePeerAssigned;
        private volatile int snowflakeStalePeerCount;
        private volatile int snowflakeOpenTimeoutCount;
        private volatile boolean meekFrontRejectedLogged;

        private RuntimeInstance(TorInstanceSpec spec, Path logFile, int attempt) {
            this.spec = spec;
            this.logFile = logFile;
            this.attempt = attempt;
        }
    }
}
