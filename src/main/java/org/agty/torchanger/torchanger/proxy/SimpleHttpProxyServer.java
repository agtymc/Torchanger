package org.agty.torchanger.torchanger.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class SimpleHttpProxyServer {
    private final int httpPort;
    private final int socksPort;
    private final String proxyUsername;
    private final String proxyPassword;
    private final Consumer<String> logger;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Future<?> acceptTask;

    public SimpleHttpProxyServer(int httpPort, int socksPort, String proxyUsername, String proxyPassword, Consumer<String> logger) {
        this.httpPort = httpPort;
        this.socksPort = socksPort;
        this.proxyUsername = proxyUsername == null ? "" : proxyUsername;
        this.proxyPassword = proxyPassword == null ? "" : proxyPassword;
        this.logger = logger;
    }

    public void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", httpPort));
        running = true;
        acceptTask = executor.submit(this::acceptLoop);
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        if (acceptTask != null) {
            acceptTask.cancel(true);
        }
        executor.shutdownNow();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                executor.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (running) {
                    logger.accept("HTTP proxy accept failed: " + e.getMessage());
                }
                return;
            }
        }
    }

    private void handleClient(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
            OutputStream clientOut = client.getOutputStream();

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isBlank()) {
                client.close();
                return;
            }
            List<String> headers = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                headers.add(line);
            }
            byte[] requestBody = readRequestBody(reader, headers);
            if (!isAuthorized(headers, clientOut)) {
                logger.accept("HTTP proxy auth failed: " + describeProxyAuthorization(headers));
                client.close();
                return;
            }
            List<String> filteredHeaders = filterProxyHeaders(headers);

            if (requestLine.startsWith("CONNECT ")) {
                handleConnect(client, clientOut, requestLine);
                return;
            }
            handleHttp(clientOut, requestLine, filteredHeaders, requestBody);
        } catch (Exception e) {
            logger.accept("HTTP proxy client failed: " + e.getMessage());
            closeQuietly(client);
        }
    }

    private void handleConnect(Socket client, OutputStream clientOut, String requestLine) throws IOException {
        String target = requestLine.split(" ")[1];
        String[] hostPort = target.split(":");
        String host = hostPort[0];
        int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 443;
        Socket upstream = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", socksPort)));
        upstream.connect(new InetSocketAddress(host, port), 15000);
        clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
        clientOut.flush();
        bridgeSockets(client, upstream);
    }

    private void handleHttp(OutputStream clientOut, String requestLine, List<String> headers, byte[] requestBody) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length < 3) {
            return;
        }
        String target = parts[1];
        String method = parts[0];
        String protocol = parts[2];
        URI uri = URI.create(target);
        String host;
        int port;
        String path;

        if (uri.isAbsolute() && uri.getHost() != null) {
            host = uri.getHost();
            port = uri.getPort() > 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
            path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
                path += "?" + uri.getRawQuery();
            }
        } else {
            HostHeader hostHeader = parseHostHeader(headers);
            if (hostHeader == null) {
                throw new IOException("missing Host header for origin-form HTTP proxy request");
            }
            host = hostHeader.host();
            port = hostHeader.port();
            path = target == null || target.isBlank() ? "/" : target;
        }
        try (Socket upstream = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", socksPort)))) {
            upstream.connect(new InetSocketAddress(host, port), 15000);
            BufferedWriter upstreamWriter = new BufferedWriter(new OutputStreamWriter(upstream.getOutputStream(), StandardCharsets.ISO_8859_1));
            upstreamWriter.write(method + " " + path + " " + protocol + "\r\n");
            boolean hasHost = false;
            for (String header : headers) {
                String lower = header.toLowerCase(Locale.ROOT);
                if (lower.startsWith("host:")) {
                    hasHost = true;
                }
                upstreamWriter.write(header + "\r\n");
            }
            if (!hasHost) {
                upstreamWriter.write("Host: " + host + "\r\n");
            }
            upstreamWriter.write("\r\n");
            upstreamWriter.flush();
            if (requestBody.length > 0) {
                upstream.getOutputStream().write(requestBody);
                upstream.getOutputStream().flush();
            }
            transfer(upstream.getInputStream(), clientOut);
        }
    }

    private void bridgeSockets(Socket client, Socket upstream) throws IOException {
        tunnel(client.getInputStream(), upstream.getOutputStream(), client, upstream);
        tunnel(upstream.getInputStream(), client.getOutputStream(), client, upstream);
    }

    private void tunnel(InputStream in, OutputStream out, Socket client, Socket upstream) {
        executor.submit(() -> {
            try {
                transfer(in, out);
            } catch (SocketException ignored) {
            } catch (IOException ignored) {
            } finally {
                closeQuietly(client);
                closeQuietly(upstream);
            }
        });
    }

    private void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
            out.flush();
        }
    }

    private boolean isAuthorized(List<String> headers, OutputStream clientOut) throws IOException {
        if (proxyUsername.isBlank()) {
            return true;
        }
        String expected = proxyUsername + ":" + proxyPassword;
        for (String header : headers) {
            String lower = header.toLowerCase(Locale.ROOT);
            if (lower.startsWith("proxy-authorization:")) {
                String value = header.substring(header.indexOf(':') + 1).trim();
                int space = value.indexOf(' ');
                if (space <= 0) {
                    continue;
                }
                String scheme = value.substring(0, space).trim();
                String encoded = value.substring(space + 1).trim();
                if (!scheme.equalsIgnoreCase("Basic") || encoded.isBlank()) {
                    continue;
                }
                try {
                    String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                    if (expected.equals(decoded)) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        clientOut.write((
                "HTTP/1.1 407 Proxy Authentication Required\r\n" +
                "Proxy-Authenticate: Basic realm=\"Torchanger\"\r\n" +
                "Content-Length: 0\r\n\r\n"
        ).getBytes(StandardCharsets.ISO_8859_1));
        clientOut.flush();
        return false;
    }

    private List<String> filterProxyHeaders(List<String> headers) {
        List<String> filtered = new ArrayList<>();
        for (String header : headers) {
            String lower = header.toLowerCase(Locale.ROOT);
            if (lower.startsWith("proxy-connection") || lower.startsWith("proxy-authorization:")) {
                continue;
            }
            filtered.add(header);
        }
        return filtered;
    }

    private HostHeader parseHostHeader(List<String> headers) {
        for (String header : headers) {
            if (!header.toLowerCase(Locale.ROOT).startsWith("host:")) {
                continue;
            }
            String value = header.substring(header.indexOf(':') + 1).trim();
            if (value.isBlank()) {
                return null;
            }
            if (value.startsWith("[")) {
                int bracketEnd = value.indexOf(']');
                if (bracketEnd < 0) {
                    return null;
                }
                String host = value.substring(1, bracketEnd);
                int port = 80;
                if (value.length() > bracketEnd + 2 && value.charAt(bracketEnd + 1) == ':') {
                    port = Integer.parseInt(value.substring(bracketEnd + 2));
                }
                return new HostHeader(host, port);
            }
            int colon = value.lastIndexOf(':');
            if (colon > 0 && value.indexOf(':') == colon) {
                return new HostHeader(value.substring(0, colon), Integer.parseInt(value.substring(colon + 1)));
            }
            return new HostHeader(value, 80);
        }
        return null;
    }

    private String describeProxyAuthorization(List<String> headers) {
        for (String header : headers) {
            if (!header.toLowerCase(Locale.ROOT).startsWith("proxy-authorization:")) {
                continue;
            }
            String value = header.substring(header.indexOf(':') + 1).trim();
            int space = value.indexOf(' ');
            if (space <= 0) {
                return "authorization header is malformed";
            }
            String scheme = value.substring(0, space).trim();
            String encoded = value.substring(space + 1).trim();
            if (encoded.isBlank()) {
                return "authorization header is empty";
            }
            try {
                String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                return "Basic".equalsIgnoreCase(scheme) && (proxyUsername + ":" + proxyPassword).equals(decoded)
                        ? "authorization accepted"
                        : "username or password is incorrect";
            } catch (IllegalArgumentException e) {
                return "authorization header is invalid";
            }
        }
        return "client did not send proxy authorization; some clients do not support authenticated HTTP proxy (example: Telegram on Ubuntu). Use SOCKS5 for that client";
    }

    private byte[] readRequestBody(BufferedReader reader, List<String> headers) throws IOException {
        int contentLength = contentLength(headers);
        if (contentLength <= 0) {
            return new byte[0];
        }
        char[] bodyChars = new char[contentLength];
        int totalRead = 0;
        while (totalRead < contentLength) {
            int read = reader.read(bodyChars, totalRead, contentLength - totalRead);
            if (read < 0) {
                break;
            }
            totalRead += read;
        }
        if (totalRead <= 0) {
            return new byte[0];
        }
        return new String(bodyChars, 0, totalRead).getBytes(StandardCharsets.ISO_8859_1);
    }

    private int contentLength(List<String> headers) {
        for (String header : headers) {
            if (!header.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                continue;
            }
            try {
                return Integer.parseInt(header.substring(header.indexOf(':') + 1).trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private record HostHeader(String host, int port) {
    }
}
