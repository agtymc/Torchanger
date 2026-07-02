package org.agty.torchanger.torchanger.bridge;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.agty.torchanger.torchanger.config.ResourceConfig;
import org.agty.torchanger.torchanger.settings.SettingsManager;
import org.agty.torchanger.torchanger.tor.profile.TorLaunchMode;

public final class BridgeCatalog {
    private static final java.util.Properties CONFIG =
            ResourceConfig.loadProperties("/org/agty/torchanger/torchanger/config/bridge-catalog.properties");
    private static final List<BridgeCatalogEntry> ENTRIES = loadEntries();
    private static final Path DELTA_BRIDGES =
            Path.of(CONFIG.getProperty("localBridgeRoot", "/AGTY/WORK/work/AGTY/projects/DeltaTorUbuntu/!files/Delta-Tor/bridges"));

    private BridgeCatalog() {
    }

    public static List<BridgeCatalogEntry> allEntries() {
        return ENTRIES;
    }

    public static BridgeCatalogEntry vanillaTested() {
        return allEntries().get(0);
    }

    public static BridgeCatalogEntry obfs4Tested() {
        return allEntries().get(1);
    }

    public static BridgeCatalogEntry webTunnelTested() {
        return allEntries().get(2);
    }

    public static BridgeCatalogEntry findByFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        if (SettingsManager.MANUAL_VANILLA_FILE.equals(fileName)) {
            return new BridgeCatalogEntry("Custom Vanilla", TorLaunchMode.VANILLA_BRIDGE, fileName, "", null);
        }
        if (SettingsManager.MANUAL_OBFS4_FILE.equals(fileName)) {
            return new BridgeCatalogEntry("Custom obfs4", TorLaunchMode.OBFS4, fileName, "", null);
        }
        if (SettingsManager.MANUAL_WEBTUNNEL_FILE.equals(fileName)) {
            return new BridgeCatalogEntry("Custom WebTunnel", TorLaunchMode.WEBTUNNEL, fileName, "", null);
        }
        return allEntries().stream()
                .filter(entry -> entry.fileName().equals(fileName))
                .findFirst()
                .orElse(null);
    }

    public static void prepareCache(Path workspaceRoot, Consumer<String> logger) throws IOException {
        Path cacheDir = workspaceRoot.resolve("bridge-cache");
        Files.createDirectories(cacheDir);
        logger.accept("Bridge cache directory: " + cacheDir.toAbsolutePath());
        logger.accept("Bridge source catalog file: " + cacheDir.resolve("sources.txt").toAbsolutePath());
        if (Files.exists(DELTA_BRIDGES)) {
            logger.accept("Local bridge seed directory: " + DELTA_BRIDGES.toAbsolutePath());
        } else {
            logger.accept("Local bridge seed directory is unavailable: " + DELTA_BRIDGES.toAbsolutePath());
        }
        for (BridgeCatalogEntry entry : allEntries()) {
            Path target = cacheDir.resolve(entry.fileName());
            if (Files.exists(target)) {
                logger.accept("Bridge cache found: " + target.toAbsolutePath());
                continue;
            }
            if (Files.exists(entry.importPath())) {
                Files.copy(entry.importPath(), target, StandardCopyOption.REPLACE_EXISTING);
                logger.accept("Imported bridge cache: " + entry.importPath().toAbsolutePath() + " -> " + target.toAbsolutePath());
                continue;
            }
            try {
                downloadEntry(entry, target);
                logger.accept("Downloaded initial bridge cache: " + entry.sourceUrl() + " -> " + target.toAbsolutePath());
            } catch (IOException | InterruptedException e) {
                logger.accept("Initial bridge cache download failed for " + entry.fileName() + ": " + e.getMessage());
            }
        }
        writeSourcesFile(cacheDir);
        logger.accept("Bridge source catalog written: " + cacheDir.resolve("sources.txt").toAbsolutePath());
    }

    public static void updateAll(Path workspaceRoot, Consumer<String> logger) throws IOException, InterruptedException {
        Path cacheDir = workspaceRoot.resolve("bridge-cache");
        Files.createDirectories(cacheDir);
        logger.accept("Updating bridge cache directory: " + cacheDir.toAbsolutePath());
        for (BridgeCatalogEntry entry : allEntries()) {
            Path target = cacheDir.resolve(entry.fileName());
            logger.accept("Downloading bridge cache: " + entry.sourceUrl());
            downloadEntry(entry, target);
            logger.accept("Updated bridge cache: " + target.toAbsolutePath());
        }
        writeSourcesFile(cacheDir);
        logger.accept("Bridge source catalog updated: " + cacheDir.resolve("sources.txt").toAbsolutePath());
    }

    public static Path resolveCachedPath(Path workspaceRoot, BridgeCatalogEntry entry) {
        Objects.requireNonNull(entry);
        return workspaceRoot.resolve("bridge-cache").resolve(entry.fileName());
    }

    private static List<BridgeCatalogEntry> loadEntries() {
        String rawBase = CONFIG.getProperty("rawBase", "https://raw.githubusercontent.com/Delta-Kronecker/Tor-Bridges-Collector/refs/heads/main/bridge/");
        Path importRoot = Path.of(CONFIG.getProperty("localBridgeRoot", "/AGTY/WORK/work/AGTY/projects/DeltaTorUbuntu/!files/Delta-Tor/bridges"));
        List<BridgeCatalogEntry> loaded = ResourceConfig.indexedValues(CONFIG, "entry.").stream()
                .map(value -> parseEntry(value, rawBase, importRoot))
                .filter(Objects::nonNull)
                .toList();
        if (!loaded.isEmpty()) {
            return loaded;
        }
        return List.of(
                new BridgeCatalogEntry("Vanilla Tested", TorLaunchMode.VANILLA_BRIDGE, "Tested_and_Active_vanilla_IPv4.txt", rawBase + "vanilla_tested.txt", importRoot.resolve("Tested_and_Active_vanilla_IPv4.txt")),
                new BridgeCatalogEntry("obfs4 Tested", TorLaunchMode.OBFS4, "Tested_and_Active_obfs4_IPv4.txt", rawBase + "obfs4_tested.txt", importRoot.resolve("Tested_and_Active_obfs4_IPv4.txt")),
                new BridgeCatalogEntry("WebTunnel Tested", TorLaunchMode.WEBTUNNEL, "Tested_and_Active_webtunnel_IPv4.txt", rawBase + "webtunnel_tested.txt", importRoot.resolve("Tested_and_Active_webtunnel_IPv4.txt"))
        );
    }

    private static BridgeCatalogEntry parseEntry(String value, String rawBase, Path importRoot) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 4) {
            return null;
        }
        return new BridgeCatalogEntry(
                parts[0],
                TorLaunchMode.valueOf(parts[1]),
                parts[2],
                rawBase + parts[3],
                importRoot.resolve(parts[2])
        );
    }

    private static void writeSourcesFile(Path cacheDir) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("Bridge source catalog").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("Primary source repository: https://github.com/Delta-Kronecker/Tor-Bridges-Collector").append(System.lineSeparator());
        builder.append("Imported local seed: ").append(DELTA_BRIDGES).append(System.lineSeparator()).append(System.lineSeparator());
        for (BridgeCatalogEntry entry : allEntries()) {
            builder.append(entry.fileName()).append(" -> ").append(entry.sourceUrl()).append(System.lineSeparator());
        }
        Files.writeString(
                cacheDir.resolve("sources.txt"),
                builder.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private static void downloadEntry(BridgeCatalogEntry entry, Path target) throws IOException, InterruptedException {
        try {
            downloadEntryWithHttpClient(entry, target);
        } catch (IOException e) {
            if (!isTlsHandshakeFailure(e)) {
                throw e;
            }
            downloadEntryWithCurl(entry, target, e);
        }
    }

    private static void downloadEntryWithHttpClient(BridgeCatalogEntry entry, Path target) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(entry.sourceUrl()))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Torchanger/1.1")
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("bridge download failed for " + entry.fileName() + ": HTTP " + response.statusCode());
        }
        try (InputStream stream = response.body()) {
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void downloadEntryWithCurl(BridgeCatalogEntry entry, Path target, IOException originalError) throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile("torchanger-bridge-", ".txt");
        try {
            Process process = new ProcessBuilder(
                    "curl",
                    "--fail",
                    "--silent",
                    "--show-error",
                    "--location",
                    "--http1.1",
                    entry.sourceUrl(),
                    "--output",
                    tempFile.toAbsolutePath().toString()
            ).redirectErrorStream(true).start();
            String output;
            try (InputStream stream = process.getInputStream()) {
                output = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String message = output.isBlank() ? "curl exit code " + exitCode : output;
                throw new IOException("bridge download failed for " + entry.fileName() + " after TLS fallback: " + message, originalError);
            }
            Files.copy(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static boolean isTlsHandshakeFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("handshake_failure") || message.contains("SSLHandshakeException"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
