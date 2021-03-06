package com.vgrazi.monitor.eventprocessor.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vgrazi.monitor.eventprocessor.domain.Scorecard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listens for new Scorecards on the ScorecardQueue, and writes them to the file system, where they are picked up
 * by the MonitorUI, displayed, and deleted.
 */
@Service
public class ScorecardProcessor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Logger logger = LoggerFactory.getLogger(ScorecardProcessor.class);
    private volatile boolean running = true;

    @Value("${scorecard-filename}")
    private String scorecardFileFormat;

    @Value(("${date-file-pattern}"))
    private String DATE_FILE_PATTERN;

    private DateTimeFormatter FORMATTER;

    public void processScorecard(BlockingQueue<Scorecard> scorecardQueue) {
        executor.submit(()-> {
            while (running) {
                Scorecard scorecard = scorecardQueue.take();
                serializeScorecard(scorecard);
            }
            logger.info("ScorecardProcessor exiting");
            return null;
        });
    }

    private void serializeScorecard(Scorecard scorecard) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String format = FORMATTER.format(LocalDateTime.now());
        String filename = String.format(scorecardFileFormat, format);
        // Create a temp file and rename, so that the file watcher
        // only gets one modify signal
        Path tempFile = Files.createTempFile("tmp", "tmp");
        mapper.writeValue(tempFile.toFile(), scorecard);
        Path target = Paths.get(filename);
        Files.move(tempFile, target);
        logger.debug("wrote file: {}", target);
    }

    @PostConstruct
    public void postConstruct() {
        FORMATTER = DateTimeFormatter.ofPattern(DATE_FILE_PATTERN);
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
