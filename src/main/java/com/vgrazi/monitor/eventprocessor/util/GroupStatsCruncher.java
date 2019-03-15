package com.vgrazi.monitor.eventprocessor.util;

import com.vgrazi.monitor.eventprocessor.domain.Record;
import com.vgrazi.monitor.eventprocessor.domain.RecordGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.xml.ws.ServiceMode;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupStatsCruncher {
    /**
     * Itrerates the group to produce a sorted map of hit counts per section
     *
     * @param group
     */
    public Map<String, Long> getSectionHitCounts(RecordGroup group) {
        Map<String, Long> counts = group.stream().collect(Collectors.groupingBy(Record::getSection, Collectors.counting()));
        return counts;
    }

    /**
     * Itrerates the group to produce a sorted map of hit counts per section
     *
     * @param group
     */
    public Map<String, Long> getSectionFailedResponses(RecordGroup group) {
        Map<String, Long> counts = group.stream().filter(record -> record.getReturnCode() != 200).collect(Collectors.groupingBy(Record::getRequest, Collectors.counting()));
        return counts;
    }

    public int getRecordsPerSecond(RecordGroup group) {
        return group.size();
    }

    public Map.Entry<String, Long> getMaxCountKey(Map<String, Long> counts) {
        Map.Entry<String, Long> max = counts.entrySet().stream().max(Map.Entry.comparingByValue()).get();
        return max;
    }
}
