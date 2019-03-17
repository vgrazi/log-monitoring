package com.vgrazi.monitor.eventprocessor.util;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import com.vgrazi.monitor.eventprocessor.domain.Record;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsCruncher {
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
     * @param counts
     * @return
     */
    public Map.Entry<String, Long> getMaxCountKey(Map<String, Long> counts) {
        Map.Entry<String, Long> max = counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        return max;
    }
}
