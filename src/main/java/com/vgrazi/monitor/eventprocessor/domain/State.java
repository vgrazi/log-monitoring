package com.vgrazi.monitor.eventprocessor.domain;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;


/**
 * This class preserves state between generations of the Scorecard class
 */
public class State {
    private long lastStatsReportTime;
    private Map<String, Long> hitsReport;
    private Deque<String> history = new LinkedList<>();
    private boolean inHighActivity;
    private long firstTimeOfThresholdExceededSecs;
    private long lastTimeOfThresholdExceededAlertSecs;

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

    public boolean isInHighActivity() {
        return inHighActivity;
    }

    public void setInHighActivity(boolean inHighActivity) {
        this.inHighActivity = inHighActivity;
    }

    public long getFirstTimeOfThresholdExceededSecs() {
        return firstTimeOfThresholdExceededSecs;
    }

    public void setFirstTimeOfThresholdExceededSecs(long firstTimeOfThresholdExceededSecs) {
        this.firstTimeOfThresholdExceededSecs = firstTimeOfThresholdExceededSecs;
    }

    public long getLastTimeOfThresholdExceededAlertSecs() {
        return lastTimeOfThresholdExceededAlertSecs;
    }

    public void setLastTimeOfThresholdExceededAlertSecs(long lastTimeOfThresholdExceededAlertSecs) {
        this.lastTimeOfThresholdExceededAlertSecs = lastTimeOfThresholdExceededAlertSecs;
    }

    public void addHistoryMessage(String message) {
        history.addFirst(message);
    }

    public Deque<String> getHistory() {
        return history;
    }
}
