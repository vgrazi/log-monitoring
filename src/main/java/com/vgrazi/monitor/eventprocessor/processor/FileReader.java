package com.vgrazi.monitor.eventprocessor.processor;

import com.vgrazi.monitor.eventprocessor.config.AppProperties;
import com.vgrazi.monitor.eventprocessor.domain.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.RandomAccessFile;
import java.nio.CharBuffer;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class FileReader {
    @Autowired
    private ApplicationContext context;
    Logger logger = LoggerFactory.getLogger(FileReader.class);
    private volatile boolean running = true;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Tails the file, depositing parsed records on the queue for asynchronous processing
     * This will only process entire, non-empty lines; if the log stops writing, this will wait forever until the lines
     * are completed
     *
     * @param recordQueue this is our processing queue.
     */
    public void tailFile(Queue<Record> recordQueue) {
        AppProperties appProperties = context.getBean(AppProperties.class);
        String fileName = appProperties.getFileName();
        logger.info("Reading {}", fileName);

        executor.submit(() -> {
            try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
                long eofPos = file.length() - 100;
                // Skip the old news, and just start from the end of the file
                file.seek(eofPos);
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
                                    // todo: How to handle the case where the queue grows faster than we can process it. Need to debounce?
                                    recordQueue.add(record);
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
