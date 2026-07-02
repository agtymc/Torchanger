package org.agty.torchanger.torchanger;

public interface TorManagerListener {
    void onOverallLog(String message);

    void onInstanceLog(String methodName, String message);

    void onStatus(String methodName, String status);

    void onMetrics(String methodName, String ping, String average);
}
