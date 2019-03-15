package com.vgrazi.monitor.eventprocessor.processor;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import com.vgrazi.monitor.eventprocessor.domain.Record;
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

    /**
     * We process incoming records from the recordQueue into one second frames, beginning from the start time, and throw them on the frame queue
     * we assume record times are correct, and in proper sequence.
     * However best not to rely on that, so we use the real time for forming the time unit Frames
     * This is configurable. See documentation in application.properties
     */
    public void processRecords(BlockingQueue<Record> recordQueue, TransferQueue<Frame> frameQueue) {
        executor.execute(() -> {
            try {
                // each frame contains 1 seconds worth of data, starting from the frameStartTime
                Frame frame = null;

                while (running) {
                    Record record = recordQueue.take();
                    long recordTime = getRecordTime(record);
                    if(frame == null) {
                        frame = new Frame();
                        frame.setFrameStartTime(recordTime);
                    }
                    else if (recordTime - frame.getStartTime() > frameResolutionInSeconds) {
                        // Record belongs to the next Frame.
                        // Close this Frame and prepare for processing...
                        // Queue up the previous frame...
                        if (!frame.isEmpty()) {
                            // if the Frame is empty, don't queue it up, just reuse it. This guarantee that
                            // only non-empty Frames will be processed
                            frameQueue.put(frame);
                            // create the next Frame
                            frame = new Frame();
                        }
                        // bump the start time for the new Frame
                        frame.setFrameStartTime(recordTime);
                    }
                    // add the record to the Frame
                    frame.addRecord(record);
                    frame.setFrameEndTime(recordTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Returns the actual record time or the log time (in seconds) depending
     * on whether the use-record-times property is true or false
     */
    private long getRecordTime(Record record) {
        LocalDateTime recordTime;
        if(useRecordTimes) {
            recordTime = record.getRecordTime();
        }
        else {
            recordTime = record.getActualTime();
        }
        return recordTime.toEpochSecond(ZoneOffset.UTC);
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
