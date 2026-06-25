package de.logicalis.java_staging_gui;

public record ProjectMetrics(
        int totalDevices,
        int finishedCount,
        int measuredCount,
        int errorCount,
        int openCount,
        int inProgressCount,
        long avgFinishMs,
        long minFinishMs,
        long maxFinishMs,
        String fastestHostname,
        String slowestHostname,
        long totalToolMs,
        int manualBaselineMinutes
) {

    public boolean hasFinishedDevices() {
        return finishedCount > 0;
    }

    public long manualBaselineMs() {
        // baseline only over devices with an actually measured tool duration - otherwise we
        // compare baseline-for-all-finished against tool-time-for-measured-only and
        // inflate the savings (finished without a timestamp have no totalToolMs)
        return (long) measuredCount * manualBaselineMinutes * 60_000L;
    }

    public long timeSavedMs() {
        return Math.max(0, manualBaselineMs() - totalToolMs);
    }

    public double timeSavedPercent() {
        long baseline = manualBaselineMs();
        if (baseline == 0) return 0;
        return 100.0 * timeSavedMs() / baseline;
    }

    // formatted values for thymeleaf, so the template needs no inline calculation

    public String formattedAvg() { return formatMs(avgFinishMs); }

    public String formattedMin() { return formatMs(minFinishMs); }

    public String formattedMax() { return formatMs(maxFinishMs); }

    public String formattedTotalTool() { return formatMs(totalToolMs); }

    public String formattedManualBaseline() { return formatMs(manualBaselineMs()); }

    public String formattedTimeSaved() { return formatMs(timeSavedMs()); }

    private static String formatMs(long ms) {
        if (ms <= 0) return "-";
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes >= 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return hours + "h " + minutes + "m";
        }
        return minutes + "m " + seconds + "s";
    }
}
