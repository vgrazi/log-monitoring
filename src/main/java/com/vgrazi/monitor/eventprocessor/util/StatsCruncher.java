package com.vgrazi.monitor.eventprocessor.util;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import com.vgrazi.monitor.eventprocessor.domain.Record;
import com.vgrazi.monitor.eventprocessor.domain.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
public class StatsCruncher {
    private final Logger logger = LoggerFactory.getLogger(StatsCruncher.class);
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    @Value("${report-stats-secs}")
    private int reportStatsTimeSecs;

    @Value("${seconds-of-thrashing}")
// if average hit count > alert-threshold for 120 seconds, display alert
    private int secondsOfThrashing;

    @Value("${seconds-of-calm}")
// if average hit count < alert-threshold for 120 more seconds, cancel alert
    private int secondsOfCalm;

    @Value("${alert-threshold}")
    private int alertThreshold;

    /**
     * This is the master facade, called by FrameProcessor to generate all of the current state base on the incoming frames
     * and previous state
     */
    public void generateState(Deque<Frame> frames, State state) {
        // generate hits report
        saveHitsReportToState(frames, state, reportStatsTimeSecs);
        // generate average hit counts for last 2 minutes
        float avgHitCountForLastSeconds = getAverageHitCountForLastSeconds(frames, secondsOfThrashing);
        logger.debug("Average hit count for last {} seconds:{}", secondsOfThrashing, avgHitCountForLastSeconds);
        saveHitCountAlertsToState(frames, (int) avgHitCountForLastSeconds, state);
    }

    /**
     * Sorts the map in reverse order of value
     */
    public LinkedHashMap<String, Long> sortByValueReverseOrder(Map<String, Long> hitsReport) {
        return hitsReport.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
    }

    /**
     * Extracts the time:hitCount for each frame in the list
     */
    public List<String> extractHitCountList(Deque<Frame> frames) {
        List<String> hitCounts = frames.stream()
                .map(frame -> String.format("%s:%s", frame.getStartTime(), frame.getHitCount()))
                .collect(Collectors.toList());
        return hitCounts;
    }

    /**
     * Calculates average hit counts for last 2 minutes
     */
    private float getAverageHitCountForLastSeconds(Deque<Frame> frames, int seconds) {
        int frameCount = 0;
        int sum = 0;
        Iterator<Frame> iterator = frames.descendingIterator();
        for (int i = 0; iterator.hasNext() && i < seconds; i++) {
            frameCount++;
            Frame frame = iterator.next();
            sum += frame.getHitCount();
        }
        return sum / (float) frameCount;
    }

    /**
     * Iterates the records in the Frame to produce a sorted map of hit counts per section
     */
    private Map<String, Long> getSectionHitCounts(Frame frame) {
        Map<String, Long> counts = frame.stream().collect(Collectors.groupingBy(Record::getSection, Collectors.counting()));
        return counts;
    }

    /**
     * Iterates the frame to produce a sorted map of hit counts per section
     */
    public Map<String, Long> getSectionFailedResponses(Frame frame) {
        Map<String, Long> counts = frame.stream().filter(record -> record.getReturnCode() != 200).collect(Collectors.groupingBy(Record::getRequest, Collectors.counting()));
        return counts;
    }

    public int getRecordsPerSecond(Frame frame) {
        return frame.getHitCount();
    }

    /**
     * Finds the entry in the supplied map with the largest count. If the supplied map is empty, returns null
     *
     * @param counts
     * @return
     */
    public Map.Entry<String, Long> getMaxCountKey(Map<String, Long> counts) {
        Map.Entry<String, Long> max = counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        return max;
    }

    /**
     * Gather the usage stats for the last 10 seconds
     *
     * @param frames
     * @param reportStatsTimeSecs
     * @return
     */
    public Map<String, Long> generateHitsReport(Deque<Frame> frames, int reportStatsTimeSecs) {
        Map<String, Long> counts = new HashMap<>();
        Iterator<Frame> iterator = frames.descendingIterator();
        for (int i = 0; iterator.hasNext() && i < reportStatsTimeSecs; i++) {
            Frame frame = iterator.next();
            Map<String, Long> sectionHitCounts = getSectionHitCounts(frame);
            for (Map.Entry<String, Long> entry : sectionHitCounts.entrySet()) {
                String key = entry.getKey();
                Long value = entry.getValue();
                Long current = counts.get(key);
                if (current == null) {
                    current = 0L;
                }
                current += value;
                counts.put(key, current);
            }
        }
        return counts;
    }

    /**
     * Iterates the Frames deque to produce a hits report for each second, and then saves that hits report to the state
     */
    private void saveHitsReportToState(Deque<Frame> frames, State state, int reportStatsTimeSecs) {
        long now = frames.getLast().getFrameEndTime();
        if (state.getLastStatsReportTimeSecs() + reportStatsTimeSecs <= now) {
            state.setLastStatsReportTimeSecs(now);
            Map<String, Long> hitsReport = generateHitsReport(frames, reportStatsTimeSecs);
            state.setHitsReport(hitsReport);
        }
    }

    /**
     * If avgHitCountForLastSeconds is greater than the alert threshold,
     * sets the state to high activity
     * Also sets now as the last time the threshold activity was exceeded.
     * Otherwise, if the state is now in high activity for over the secondsOfThrashing time (2 minutes)
     * sets high activity flag
     *
     * @param avgHitCountForLastSeconds this is the average hit count over the last "10" (by default) seconds
     */
    private void saveHitCountAlertsToState(Deque<Frame> frames, int avgHitCountForLastSeconds, State state) {
        long now = frames.getLast().getFrameEndTime();
        state.setAlert(null);
        if (avgHitCountForLastSeconds > alertThreshold) {
            state.setLastTimeOfThresholdExceededAlertSecs(now);
            if(avgHitCountForLastSeconds > state.getAverageHighActivity()) {
                state.setAverageHighActivity(avgHitCountForLastSeconds);
            }
            if (!state.isInHighActivity()) {
                state.setInHighActivity(true);
                state.setFirstTimeOfThresholdExceededSecs(now);
            }
        } else // we are not currently in high activity. Check if we are coming out of a high activity state
            if (state.isInHighActivity()) {
                long deltaCalm = now - state.getLastTimeOfThresholdExceededAlertSecs();
                if (deltaCalm >= secondsOfCalm) {
                    state.setInHighActivity(false);
                    String message = String.format("State was in high alert from %s to %s. Average %d messages/second at peak",
                            FORMATTER.format(LocalDateTime.ofEpochSecond(state.getFirstTimeOfThresholdExceededSecs(), 0, ZoneOffset.UTC)),
                            FORMATTER.format(LocalDateTime.ofEpochSecond(state.getLastTimeOfThresholdExceededAlertSecs(), 0, ZoneOffset.UTC)),
                            state.getAverageHighActivity()
                    );
                    state.addHistoryMessage(message);
                    state.setAlert(message);
                }
            }
    }
}
