package com.vgrazi.monitor.eventprocessor.processor;

import com.vgrazi.monitor.eventprocessor.domain.Record;
import com.vgrazi.monitor.eventprocessor.domain.RecordGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
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
    // we process things every "10" seconds, beginning from the start time
    // todo: we assume record times are correct, and in proper sequence.
    //  However best not to rely on that, we will use the real time for forming the time unit groups

    public void processRecords(BlockingQueue<Record> recordQueue, TransferQueue<RecordGroup> groupQueue) {
        executor.execute(() -> {
            try {
                // each group contains 10 seconds worth of data, starting from the groupStartTime
                long groupStartTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
                RecordGroup recordGroup = new RecordGroup();

                while (running) {
                    Record record = recordQueue.take();
                    long recordActualTime = record.getActualTime().toEpochSecond(ZoneOffset.UTC);
                    if (recordActualTime - groupStartTime > pollFrequencySeconds) {
                        // Record belongs to the next group.
                        // Close this group and prepare for processing...
                        // Queue up the previous group...
                        groupQueue.put(recordGroup);
                        // bump the start time for the new group
                        groupStartTime = recordActualTime;
                        // create the new group
                        recordGroup = new RecordGroup();
                    }
                    // add the record to the group
                    recordGroup.addRecord(record);
                    logger.debug("Size:{} {}", recordQueue.size(), record.toString());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
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
