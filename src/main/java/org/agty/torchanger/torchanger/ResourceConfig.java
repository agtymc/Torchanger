package org.agty.torchanger.torchanger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

final class ResourceConfig {
    private ResourceConfig() {
    }

    static Properties loadProperties(String resourcePath) {
        try (InputStream stream = ResourceConfig.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return new Properties();
            }
            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load resource " + resourcePath, e);
        }
    }

    static String loadText(String resourcePath, String fallback) {
        try (InputStream stream = ResourceConfig.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return fallback;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load resource " + resourcePath, e);
        }
    }

    static List<String> indexedValues(Properties properties, String prefix) {
        return properties.stringPropertyNames().stream()
                .filter(name -> name.startsWith(prefix))
                .sorted((left, right) -> Integer.compare(indexOf(left, prefix), indexOf(right, prefix)))
                .map(properties::getProperty)
                .toList();
    }

    private static int indexOf(String key, String prefix) {
        try {
            return Integer.parseInt(key.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
