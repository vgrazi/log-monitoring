package com.vgrazi.monitor.eventprocessor.domain;

import java.util.Map;

public class State {
    private long lastStatsReportTime;
    private Map<String, Long> hitsReport;

    public void setLastStatsReportTime(long lastStatsReportTime) {
        this.lastStatsReportTime = lastStatsReportTime;
    }

    public long getLastStatsReportTimeSecs() {
        return lastStatsReportTime;
    }

    public void setLastStatsReportTimeSecs(long lastStatsReportTime) {
        this.lastStatsReportTime = lastStatsReportTime;
    }

    public void setHitsReport(Map<String, Long> hitsReport) {
        this.hitsReport = hitsReport;
    }

    public Map<String, Long> getHitsReport() {
        return hitsReport;
    }
}
