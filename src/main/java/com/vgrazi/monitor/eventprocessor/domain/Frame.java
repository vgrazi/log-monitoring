package com.vgrazi.monitor.eventprocessor.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A Frame represents one second's worth of records
 */
public class Frame {
    private final List<Record> recordGroup = new LinkedList<>();
    private long groupStartTime;

    public void addRecord(Record record) {
        recordGroup.add(record);
    }

    @Override
    public String toString() {
        return "Frame{" +
                recordGroup +
                '}';
    }

    public boolean isEmpty() {
        return recordGroup.isEmpty();
    }

    public long getStartTime() {
        return groupStartTime;
    }

    /**
     * Set the start time (in UTC epoch seconds) of this Frame, ie the time associated with the earliest record in the Frame
     */
    public void setStartTime(long groupStartTime) {
        this.groupStartTime = groupStartTime;
    }

    /**
     * Returns a Stream of Record instances representing the records in this Frame
     * @return a Stream of Record instances representing the records in this Frame
     */
    public Stream<Record> stream() {
        return recordGroup.stream();
    }

    /**
     * Returns the number records in this Frame
     * @return the number records in this Frame
     */
    public int size() {
        return recordGroup.size();
    }
}
