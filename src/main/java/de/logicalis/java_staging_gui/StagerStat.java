package de.logicalis.java_staging_gui;

public record StagerStat(
        String name,
        int totalDevices,
        int finishedDevices,
        int errorDevices,
        long avgDurationMs) {

    public String formattedAvg() {
        if (avgDurationMs <= 0) return "-";
        long s = avgDurationMs / 1000;
        long m = s / 60;
        s = s % 60;
        return m + "m " + s + "s";
    }
}
