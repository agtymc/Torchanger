package org.agty.torchanger.torchanger.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppInfo {
    private static final String DEFAULT_VERSION = "1.1";

    private AppInfo() {
    }

    public static String version() {
        Package appPackage = Launcher.class.getPackage();
        if (appPackage != null && appPackage.getImplementationVersion() != null && !appPackage.getImplementationVersion().isBlank()) {
            return appPackage.getImplementationVersion();
        }

        try (InputStream stream = AppInfo.class.getResourceAsStream("/META-INF/maven/org.agty.torchanger/torchanger/pom.properties")) {
            if (stream != null) {
                Properties properties = new Properties();
                properties.load(stream);
                String version = properties.getProperty("version");
                if (version != null && !version.isBlank()) {
                    return version;
                }
            }
        } catch (IOException ignored) {
        }

        return DEFAULT_VERSION;
    }

    public static String displayName() {
        return "Torchanger " + version();
    }
}
