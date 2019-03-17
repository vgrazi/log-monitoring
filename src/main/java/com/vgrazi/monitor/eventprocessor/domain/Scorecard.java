package com.vgrazi.monitor.eventprocessor.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Scorecard {
    @JsonProperty("hitCounts")
    private List<String> hitCounts;
    @JsonProperty("startTime")
    private long startTime;

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

    @Override
    public String toString() {
        return "Scorecard{" +
                "hitCounts=" + hitCounts +
                ", startTime=" + startTime +
                '}';
    }
}
