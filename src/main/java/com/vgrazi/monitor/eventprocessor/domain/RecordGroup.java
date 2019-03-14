package com.vgrazi.monitor.eventprocessor.domain;

import java.util.LinkedList;
import java.util.List;

public class RecordGroup {
    private final List<Record> recordGroup = new LinkedList<>();

    public void addRecord(Record record) {
        recordGroup.add(record);
    }

    @Override
    public String toString() {
        return "RecordGroup{" +
                recordGroup +
                '}';
    }
}
