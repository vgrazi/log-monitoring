package com.vgrazi.monitor.eventprocessor.processor;

import com.vgrazi.monitor.eventprocessor.domain.RecordGroup;
import com.vgrazi.monitor.eventprocessor.util.GroupStatsCruncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GroupProcessor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    Logger logger = LoggerFactory.getLogger(GroupProcessor.class);
    private volatile boolean running = true;

    @Autowired
    private GroupStatsCruncher groupStatsCruncher;

    /**
     * When the RecordProcessor deposits groups of seconds onto the queue, GroupProcessor processes them
     * @param groupQueue a group of all records within the configured frequency
     */
    public void processGroups(BlockingQueue<RecordGroup> groupQueue) {
        executor.submit(()-> {
            try {
                while (running) {
                    // note: groups could be empty, indicating the file is not pumping.
                    // todo: The processor should alert on empty/low volume groups
                    RecordGroup group = groupQueue.take();
                    processGroup(group);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void processGroup(RecordGroup group) {
        logger.debug("Processing group {}", group);
        calculateStats(group);
    }

    private void calculateStats(RecordGroup group) {

        int recordsPerSecond = groupStatsCruncher.getRecordsPerSecond(group);
        long startTime = group.getStartTime();
        Map<String, Long> failedResponses = groupStatsCruncher.getSectionFailedResponses(group);
        Map<String, Long> hitCounts = groupStatsCruncher.getSectionHitCounts(group);
        Map.Entry<String, Long> max = groupStatsCruncher.getMaxCountKey(hitCounts);
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

        // I AM THINKING THE GROUP IDEA IS NOT VERY GOOD. THERE COULD BE STATS WE MISS, FOR EXAMPLE IF IN 1 SEC
        // WE GOT 6 HITS PER SECOND AT THE END OF THE SECOND, AND THEN IN THE NEXT SECOND WE GOT 6 HITS IN THE BEGINNING
        // WE WOULD MISS THAT
        // RATHER, WE NEED TO KEEP A ROLLING COUNT.

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
