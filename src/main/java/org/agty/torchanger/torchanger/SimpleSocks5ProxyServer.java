package org.agty.torchanger.torchanger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class SimpleSocks5ProxyServer {
    private final int listenPort;
    private final int upstreamSocksPort;
    private final String proxyUsername;
    private final String proxyPassword;
    private final Consumer<String> logger;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Future<?> acceptTask;

    public SimpleSocks5ProxyServer(int listenPort, int upstreamSocksPort, String proxyUsername, String proxyPassword, Consumer<String> logger) {
        this.listenPort = listenPort;
        this.upstreamSocksPort = upstreamSocksPort;
        this.proxyUsername = proxyUsername == null ? "" : proxyUsername;
        this.proxyPassword = proxyPassword == null ? "" : proxyPassword;
        this.logger = logger;
    }

    public void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", listenPort));
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
                    logger.accept("SOCKS5 proxy accept failed: " + e.getMessage());
                }
                return;
            }
        }
    }

    private void handleClient(Socket client) {
        try {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            if (!negotiateAuthentication(in, out)) {
                client.close();
                return;
            }

            if (in.read() != 0x05) {
                client.close();
                return;
            }
            int command = in.read();
            in.read();
            int atyp = in.read();
            if (command != 0x01) {
                writeReply(out, 0x07);
                client.close();
                return;
            }

            String host = readAddress(in, atyp);
            int port = readPort(in);

            Socket upstream = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", upstreamSocksPort)));
            upstream.connect(new InetSocketAddress(host, port), 15000);
            writeReply(out, 0x00);
            bridgeSockets(client, upstream);
        } catch (Exception e) {
            if (!(e instanceof SocketException socketException) || !"Socket is closed".equals(socketException.getMessage())) {
                logger.accept("SOCKS5 proxy client failed: " + e.getMessage());
            }
            closeQuietly(client);
        }
    }

    private boolean negotiateAuthentication(InputStream in, OutputStream out) throws IOException {
        if (in.read() != 0x05) {
            return false;
        }
        int methodCount = in.read();
        byte[] methods = in.readNBytes(methodCount);
        boolean authRequired = !proxyUsername.isBlank();
        int selectedMethod = authRequired ? 0x02 : 0x00;
        if (!containsMethod(methods, selectedMethod)) {
            if (authRequired) {
                logger.accept("SOCKS5 proxy auth failed: client did not send proxy authorization; some clients do not support authenticated SOCKS5. Check client settings");
            }
            out.write(new byte[]{0x05, (byte) 0xFF});
            out.flush();
            return false;
        }
        out.write(new byte[]{0x05, (byte) selectedMethod});
        out.flush();
        if (!authRequired) {
            return true;
        }
        return verifyUserPass(in, out);
    }

    private boolean verifyUserPass(InputStream in, OutputStream out) throws IOException {
        if (in.read() != 0x01) {
            logger.accept("SOCKS5 proxy auth failed: authorization header is malformed");
            return false;
        }
        int userLength = in.read();
        String user = new String(in.readNBytes(userLength), StandardCharsets.UTF_8);
        int passwordLength = in.read();
        String password = new String(in.readNBytes(passwordLength), StandardCharsets.UTF_8);
        boolean valid = proxyUsername.equals(user) && proxyPassword.equals(password);
        out.write(new byte[]{0x01, (byte) (valid ? 0x00 : 0x01)});
        out.flush();
        if (!valid) {
            logger.accept("SOCKS5 proxy auth failed: username or password is incorrect");
        }
        return valid;
    }

    private boolean containsMethod(byte[] methods, int expected) {
        for (byte method : methods) {
            if ((method & 0xFF) == expected) {
                return true;
            }
        }
        return false;
    }

    private String readAddress(InputStream in, int atyp) throws IOException {
        return switch (atyp) {
            case 0x01 -> {
                byte[] address = in.readNBytes(4);
                yield (address[0] & 0xFF) + "." + (address[1] & 0xFF) + "." + (address[2] & 0xFF) + "." + (address[3] & 0xFF);
            }
            case 0x03 -> {
                int length = in.read();
                yield new String(in.readNBytes(length), StandardCharsets.UTF_8);
            }
            case 0x04 -> {
                byte[] address = in.readNBytes(16);
                yield java.net.InetAddress.getByAddress(address).getHostAddress();
            }
            default -> throw new IOException("unsupported SOCKS address type: " + atyp);
        };
    }

    private int readPort(InputStream in) throws IOException {
        int high = in.read();
        int low = in.read();
        return (high << 8) | low;
    }

    private void writeReply(OutputStream out, int status) throws IOException {
        out.write(new byte[]{0x05, (byte) status, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        out.flush();
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

    private void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
