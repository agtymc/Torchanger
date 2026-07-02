package org.agty.torchanger.torchanger.config;

import java.util.Properties;

public final class AppDefaults {
    private static final Properties PROPERTIES =
            ResourceConfig.loadProperties("/org/agty/torchanger/torchanger/config/app-defaults.properties");

    private AppDefaults() {
    }

    public static String stringValue(String key, String fallback) {
        return PROPERTIES.getProperty(key, fallback);
    }

    public static int intValue(String key, int fallback) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean booleanValue(String key, boolean fallback) {
        String value = PROPERTIES.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }
}
