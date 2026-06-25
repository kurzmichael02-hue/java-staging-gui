package de.logicalis.java_staging_gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    @Autowired
    private DeviceRepository repo;

    // baseline from the as-is analysis (thesis, table 5): about 25 min per device manually
    @Value("${app.manual-baseline-minutes-per-device:25}")
    private int manualBaselineMinutes;

    public ProjectMetrics computeFor(String projectTitle) {
        List<Device> devices = repo.findByProjectTitle(projectTitle);

        int total = devices.size();
        int finished = 0, errors = 0, open = 0, inProgress = 0;
        long totalDuration = 0, minDuration = Long.MAX_VALUE, maxDuration = 0;
        int durationSamples = 0;
        String fastest = null, slowest = null;

        for (Device d : devices) {
            // defensive: a record without a status must not crash the metrics page
            if (d.getStatus() == null) continue;

            switch (d.getStatus()) {
                case DONE -> finished++;
                case ERROR -> errors++;
                case OPEN -> open++;
                case IN_PROGRESS -> inProgress++;
            }

            // only count successful ones for the average - errors often stem from
            // external problems (ZTP unreachable) and would skew the average
            if (d.getStatus() == DeviceStatus.DONE
                    && d.getStartedAt() > 0
                    && d.getFinishedAt() > d.getStartedAt()) {

                long dur = d.getFinishedAt() - d.getStartedAt();
                totalDuration += dur;
                durationSamples++;

                if (dur < minDuration) {
                    minDuration = dur;
                    fastest = d.getHostname();
                }
                if (dur > maxDuration) {
                    maxDuration = dur;
                    slowest = d.getHostname();
                }
            }
        }

        // if there are no finished devices with a valid duration, set min to 0 instead of MAX_VALUE
        if (minDuration == Long.MAX_VALUE) minDuration = 0;

        // only divide by the devices with an actually measured duration - otherwise a
        // device manually set to Done (without a timestamp) drags the average down
        long avgDuration = durationSamples > 0 ? totalDuration / durationSamples : 0;

        return new ProjectMetrics(
                total, finished, durationSamples, errors, open, inProgress,
                avgDuration, minDuration, maxDuration,
                fastest, slowest,
                totalDuration,
                manualBaselineMinutes);
    }

    // breakdown by stager - who staged how much, who is the fastest.
    // intentionally per-project only, cross-project aggregation would come later if it
    // makes sense (e.g. monthly report / yearly review)
    public List<StagerStat> stagerStatsFor(String projectTitle) {
        List<Device> devices = repo.findByProjectTitle(projectTitle);

        Map<String, List<Device>> grouped = new HashMap<>();
        for (Device d : devices) {
            String key = (d.getAssignedTo() == null || d.getAssignedTo().isBlank())
                    ? "(unassigned)"
                    : d.getAssignedTo();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }

        List<StagerStat> stats = new ArrayList<>();
        for (Map.Entry<String, List<Device>> e : grouped.entrySet()) {
            stats.add(buildStat(e.getKey(), e.getValue()));
        }
        // most devices first, that is the intuitive sort order for the leaderboard
        stats.sort(Comparator.comparingInt(StagerStat::totalDevices).reversed());
        return stats;
    }

    private StagerStat buildStat(String name, List<Device> devices) {
        int total = devices.size();
        int finished = 0;
        int errors = 0;
        long sumDuration = 0;
        int durSamples = 0;

        for (Device d : devices) {
            if (d.getStatus() == DeviceStatus.ERROR) errors++;
            if (d.getStatus() == DeviceStatus.DONE) {
                finished++;
                if (d.getStartedAt() > 0 && d.getFinishedAt() > d.getStartedAt()) {
                    sumDuration += d.getFinishedAt() - d.getStartedAt();
                    durSamples++;
                }
            }
        }

        // as above: only divide by the ones with a real duration, not by all finished ones
        long avg = durSamples > 0 ? sumDuration / durSamples : 0;
        return new StagerStat(name, total, finished, errors, avg);
    }
}
