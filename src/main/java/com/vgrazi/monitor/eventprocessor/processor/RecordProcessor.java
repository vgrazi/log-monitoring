package com.vgrazi.monitor.eventprocessor.processor;

import com.vgrazi.monitor.eventprocessor.domain.Record;
import com.vgrazi.monitor.eventprocessor.domain.RecordGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TransferQueue;

@Service
public class RecordProcessor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    Logger logger = LoggerFactory.getLogger(RecordProcessor.class);
    private volatile boolean running = true;

    @Value("${poll-frequency-sec}")
    private long pollFrequencySeconds;

    @Value("${frame-resolution-sec}")
    private long frameResolutionInSeconds;

    @Value("${use-record-times}")
    private boolean useRecordTimes;

    // we process things every "1" seconds, beginning from the start time
    // note: if there is no activity withih
    // we assume record times are correct, and in proper sequence.
    //  However best not to rely on that, so we use the real time for forming the time unit groups
    public void processRecords(BlockingQueue<Record> recordQueue, TransferQueue<RecordGroup> groupQueue) {
        executor.execute(() -> {
            try {
                // each group contains 1 seconds worth of data, starting from the groupStartTime
                RecordGroup recordGroup = null;

                while (running) {
                    Record record = recordQueue.take();
                    long recordTime = getRecordTime(record).toEpochSecond(ZoneOffset.UTC);
                    if(recordGroup == null) {
                        recordGroup = new RecordGroup();
                        recordGroup.setStartTime(recordTime);
                    }
                    else if (recordTime - recordGroup.getStartTime() > frameResolutionInSeconds) {
                        // Record belongs to the next group.
                        // Close this group and prepare for processing...
                        // Queue up the previous group...
                        if (!recordGroup.isEmpty()) {
                            // if the group is empty, don't queue it up, just reuse it. This guarantee that
                            // only non-empty groups will be processed
                            groupQueue.put(recordGroup);
                            // create the new group
                            recordGroup = new RecordGroup();
                        }
                        // bump the start time for the new group
                        recordGroup.setStartTime(recordTime);
                    }
                    // add the record to the group
                    recordGroup.addRecord(record);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Returns the actual record time or the log time depending
     * on whether the use-record-times property is true or false
     * @param record
     * @return
     */
    private LocalDateTime getRecordTime(Record record) {
        if(useRecordTimes) {
            return record.getRecordTime();
        }
        else {
            return record.getActualTime();
        }
    }

    /**
     * gracefully stop the file reader after the current line is done
     */
    public void stop() {
        running = false;
        // shut down the executor. Will wait to finish any existing tasks
        executor.shutdown();
    }

}
