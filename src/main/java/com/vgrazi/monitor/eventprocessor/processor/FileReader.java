package com.vgrazi.monitor.eventprocessor.processor;

import com.vgrazi.monitor.eventprocessor.domain.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
/**
 * Tails the event file, converting new log events to Records, and throwing them on the RecordProcessor queue
 */
public class FileReader {
    private final Logger logger = LoggerFactory.getLogger(FileReader.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    @Value("${tail-from-end}")
    private boolean tailFromEnd;

    @Value("${input-filename}")
    private String inputFileName;

    @Value("${poll-frequency-sec}")
    private long pollFrequencySeconds;

    /**
     * Tails the file, depositing parsed records on the queue for asynchronous processing
     * This will only process entire, non-empty lines; if the log stops writing, this will wait forever until the lines
     * are completed
     *
     * @param recordQueue this is our processing queue.
     */
    public void tailFile(BlockingQueue<Record> recordQueue) {
        logger.info("Reading {}", inputFileName);

        executor.submit(() -> {
            try (RandomAccessFile file = new RandomAccessFile(inputFileName, "r")) {
                {
                    if (tailFromEnd) {
                        // start a few bytes earlier
                        long eofPos = file.length() - 100;
                        if(eofPos > 0) {
                            // Skip the old news, and just start from the end of the file
                            file.seek(eofPos);
                        }
                    }
                    boolean first = true;
                    CharBuffer buffer = null;
                    while (running) {
                        // May be overkill, but who knows when the file writer decides to flush and give us half a line thank you very much.
                        // Let's use brute force to read one character at a time, and don't parse until the line is complete (ends in a new line)
                        int read = file.read();
                        if (read > 0) {
                            char ch = (char) read;
                            // unix or windows, no matter
                            if (ch != '\n' && ch != '\r') {
                                if (buffer == null) {
                                    buffer = CharBuffer.allocate(1000);
                                }
                                buffer.put(ch);
                            } else if (buffer != null) {
                                // we have a keeper!
                                String line = buffer.flip().toString();
                                if (!line.trim().equals("")) {
                                    Record record = Record.fromLine(line);
                                    // The first record is very likely incomplete, since we are coming in at a random time
                                    // so if this is first record, just ignore it
                                    if (!first || !record.isError()) {
                                        // todo: How to handle the case where the queue grows faster than we can process it.
                                        //  Need to debounce?
                                        recordQueue.put(record);

                                    }
                                    if (first) {
                                        first = false;
                                    }
                                }
                                buffer = null;
                            }
                        }
                        Thread.yield();
                    }
                }
            }
            logger.debug("File reader exiting");
            // Return null to make this a callable, for easier treatment of exceptions
            return null;
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
