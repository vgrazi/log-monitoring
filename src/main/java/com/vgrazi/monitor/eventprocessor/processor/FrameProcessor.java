package com.vgrazi.monitor.eventprocessor.processor;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import com.vgrazi.monitor.eventprocessor.util.StatsCruncher;
import com.vgrazi.monitor.eventprocessor.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TransferQueue;

@Service
public class FrameProcessor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    Logger logger = LoggerFactory.getLogger(FrameProcessor.class);
    private volatile boolean running = true;

    @Autowired
    private StatsCruncher statsCruncher;

    /**
     * Maintain the window of interest, 10 minutes by default, consisting of all frames within that time period
     */
    private final Deque<Frame> window = new LinkedList<>();

    @Autowired
    private WindowUtils windowUtils;

    /**
     * When the RecordProcessor deposits Frame of seconds onto the queue, FrameProcessor processes them
     * @param frameQueue a queue of all Frames within the configured frequency
     * @param windowQueue maintains a Window of frames. The Window is the span of time we are interested in.
     */
    public void processFrames(BlockingQueue<Frame> frameQueue, TransferQueue<Deque<Frame>> windowQueue) {
        executor.submit(()-> {
            try {
                while (running) {
                    Frame frame = frameQueue.take();
                    processFrame(frame);
                    windowUtils.addFrameToWindow(frame, window);
                    windowQueue.transfer(window);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void processFrame(Frame frame) {
        logger.debug("Processing frame {}", frame);
        calculateStats(frame);
    }

    private void calculateStats(Frame frame) {

        int recordsPerSecond = statsCruncher.getRecordsPerSecond(frame);
        long startTime = frame.getStartTime();
        Map<String, Long> failedResponses = statsCruncher.getSectionFailedResponses(frame);
        Map<String, Long> hitCounts = statsCruncher.getSectionHitCounts(frame);Map.Entry<String, Long> max = statsCruncher.getMaxCountKey(hitCounts);
        logger.debug("recordsPerSecond: {}", recordsPerSecond);
        logger.debug("startTime: {}", LocalDateTime.ofEpochSecond(startTime, 0, ZoneOffset.UTC));
        logger.debug("Hit counts:{}", hitCounts);
        logger.debug("Failed responses:{}", failedResponses);
        logger.debug("Max:{}", max);
        // Required:
        // Sections of the site with most hits
        // Sections of the site with least hits
        // failed responses
        // large responses
        // alerts when high traffic is detected (over 10 requests per second)

        // Whenever total traffic for the past 2 minutes exceeds a certain number on average, add a message saying that “High traffic generated an alert - hits = {value}, triggered at {time}”. The default threshold should be 10 requests per second, and should be overridable.
        // Whenever the total traffic drops again below that value on average for the past 2 minutes, add another message detailing when the alert recovered.
        // Make sure all messages showing when alerting thresholds are crossed remain visible on the page for historical reasons.

        // todo: We need to capture rolling statistics in 2 categories:
        //  Seconds (eg when traffic exceeds 10 hits avg per second):
        //  Minutes (eg when traffic exceeds 10 hits avg per second for 2 minutes):
        //  to accomplish this, we can keep a LinkedHashMap of seconds to hits per second
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
