package org.agty.torchanger.torchanger.app;

import java.nio.file.Path;

public final class AppEnvironment {
    private AppEnvironment() {
    }

    public static boolean isPackaged() {
        return System.getProperty("jpackage.app-path") != null;
    }

    public static String profileName() {
        return isPackaged() ? "packaged" : "development";
    }

    public static Path workspaceRoot() {
        return Path.of(System.getProperty("user.home"), isPackaged() ? ".torchanger" : ".torchanger-dev");
    }
}
