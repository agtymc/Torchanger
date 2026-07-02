package org.agty.torchanger.torchanger.tor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DependencyChecker {
    private DependencyChecker() {
    }

    public static Map<String, Boolean> checkAll() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("tor", isAvailable("tor"));
        result.put("curl", isAvailable("curl"));
        result.put("obfs4proxy", isAvailable("obfs4proxy"));
        result.put("snowflake-client", isAvailable("snowflake-client"));
        result.put("lyrebird", isAvailable("lyrebird"));
        return result;
    }

    private static boolean isAvailable(String binary) {
        try {
            Process process = new ProcessBuilder("which", binary).redirectErrorStream(true).start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
