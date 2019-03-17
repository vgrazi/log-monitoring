package com.vgrazi.monitor.eventprocessor.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jdk.nashorn.internal.ir.annotations.Ignore;

import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * The Scorecard file is passed between the server and the client.
 * It contains all of the information the client needs to render
 * It is created from scratch each time. If there is any state that is to be preserved between generations of this class,
 * that state must be preserved in the State object
 */
public class Scorecard {
    @JsonProperty("hitCounts")
    private List<String> hitCounts;
    @JsonProperty("startTime")
    private long startTime;
    @JsonProperty("hitsReport")
    private Map<String, Long> hitsReport;
    @JsonProperty("latest-time-of-threshold-exceeded-alert-secs")
    private long lastTimeOfThresholdExceededAlertSecs;
    @JsonProperty("first-time-of-threshold-exceeded-alert-secs")
    private long firstTimeOfThresholdExceededSecs;
    @JsonProperty("high-activity")
    private boolean inHighActivity;
    @JsonProperty("history")
    private Deque<String> history;
    @JsonProperty("alert")
    private String alert;

    public String getAlert() {
        return alert;
    }

    public void setHitCounts(List<String> hitCounts) {

        this.hitCounts = hitCounts;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public List<String> getHitCounts() {
        return hitCounts;
    }

    public long getStartTime() {
        return startTime;
    }

    public Map<String, Long> getHitsReport() {
        return hitsReport;
    }

    public void setHitsReport(Map<String, Long> hitsReport) {
        this.hitsReport = hitsReport;
    }

    @Override
    public String toString() {
        return "Scorecard{" +
                "hitCounts=" + hitCounts +
                ", startTime=" + startTime +
                '}';
    }

    @Ignore
    public long getLastTimeOfThresholdExceededAlertSecs() {
        return lastTimeOfThresholdExceededAlertSecs;
    }

    /**
     * Sets the most recent time (in seconds) that the threshold (10 hits per sec) was exceeded on average for last 2 minutes
     */
    @Ignore
    public void setLastTimeOfThresholdExceededAlertSecs(long lastTimeOfThresholdExceededAlertSecs) {

        this.lastTimeOfThresholdExceededAlertSecs = lastTimeOfThresholdExceededAlertSecs;
    }

    public long getFirstTimeOfThresholdExceededSecs() {
        return firstTimeOfThresholdExceededSecs;
    }

    public void setFirstTimeOfThresholdExceededSecs(long firstTimeOfThresholdExceededSecs) {
        this.firstTimeOfThresholdExceededSecs = firstTimeOfThresholdExceededSecs;
    }

    public void setInHighActivity(boolean isInHighAcivity) {
        this.inHighActivity = isInHighAcivity;
    }

    public boolean isInHighActivity() {
        return inHighActivity;
    }

    public Deque<String> getHistory() {
        return history;
    }

    public void setHistory(Deque<String> history) {

        this.history = history;
    }

    public void setAlert(String message) {
        this.alert = message;
    }
}
