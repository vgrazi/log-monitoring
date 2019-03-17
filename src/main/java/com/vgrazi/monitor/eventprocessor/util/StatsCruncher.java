package com.vgrazi.monitor.eventprocessor.util;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import com.vgrazi.monitor.eventprocessor.domain.Record;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
public class StatsCruncher {

    /**
     * Sorts the map in reverse order of value
     */
    public static LinkedHashMap<String, Long> sortByValueReverseOrder(Map<String, Long> hitsReport) {
        return hitsReport.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
    }

    /**
     * Extracts the time:hitCount for each frame in the list
     */
    public static List<String> extractHitCountList(Deque<Frame> frames) {
        List<String> hitCounts = frames.stream()
                .map(frame -> String.format("%s:%s", frame.getStartTime(), frame.getHitCount()))
                .collect(Collectors.toList());
        return hitCounts;
    }

    public static int getHitCountForLastSeconds(Deque<Frame> frames, int seconds) {
        int sum = 0;
        Iterator<Frame> iterator = frames.descendingIterator();
        for (int i = 0; iterator.hasNext() && i < seconds; i++) {
            Frame frame = iterator.next();
            sum += frame.getHitCount();
        }
        return sum;
    }

    /**
     * Iterates the records in the Frame to produce a sorted map of hit counts per section
     */
    public Map<String, Long> getSectionHitCounts(Frame frame) {
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
}
