package com.vgrazi.monitor.eventprocessor.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A Frame represents one second's worth of records
 */
public class Frame {
    private final List<Record> records = new LinkedList<>();
    private long frameStartTime;
    private long frameEndTime;

    public void addRecord(Record record) {
        records.add(record);
    }

    @Override
    public String toString() {
        return "Frame{" +
                records +
                '}';
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public long getStartTime() {
        return frameStartTime;
    }

    /**
     * Set the start time (in UTC epoch seconds) of this Frame, ie the time associated with the earliest record in the Frame
     */
    public void setFrameStartTime(long frameStartTime) {
        this.frameStartTime = frameStartTime;
    }

    public long getFrameEndTime() {
        return frameEndTime;
    }

    /**
     * Set the start time (in UTC epoch seconds) of this Frame, ie the time associated with the earliest record in the Frame
     */
    public void setFrameEndTime(long frameEndTime) {
        this.frameEndTime = frameEndTime;
    }

    /**
     * Returns a Stream of Record instances representing the records in this Frame
     * @return a Stream of Record instances representing the records in this Frame
     */
    public Stream<Record> stream() {
        return records.stream();
    }

    /**
     * Returns the number records in this Frame
     * @return the number records in this Frame
     */
    public int getHitCount() {
        return records.size();
    }
}
