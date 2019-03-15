package com.vgrazi.monitor.eventprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class EventProcessorApplicationTests {
    private Logger logger = LoggerFactory.getLogger(EventProcessorApplicationTests.class);
    private volatile boolean running = true;
    private final DateTimeFormatter formatter =DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss +SSSS");
    private Random random = new Random(0);

    public static void main(String[] args) throws IOException {
        new EventProcessorApplicationTests().contextLoads();
    }
    private void contextLoads() throws IOException {
        File file = new File("log/access.log");
        File parentDir = file.getParentFile();
        if(!parentDir.exists()) {
            boolean mkdirs = parentDir.mkdirs();
            if(mkdirs) {
                logger.info("Created directory {}", parentDir);
            }
        }
        if(!file.exists()) {
            boolean created = file.createNewFile();
            if(created) {
                logger.info("Created file {}", file);
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            while (running) {
                append(writer, "127.0.0.1 - james [" ,"] \"GET /report HTTP/1.0\" 200 123");
                append(writer, "127.0.0.1 - jill [",  "] \"GET /api/user HTTP/1.0\" 200 234");
                append(writer, "127.0.0.1 - frank [", "] \"POST /api/user HTTP/1.0\" 200 34");
                append(writer, "127.0.0.1 - mary [",  "] \"POST /api/user HTTP/1.0\" 503 12");
//                append(writer, "127.0.0.1 - james [09/May/2018:16:00:39 +0000] \"GET /report HTTP/1.0\" 200 123");
//                append(writer, "127.0.0.1 - jill [09/May/2018:16:00:41 +0000] \"GET /api/user HTTP/1.0\" 200 234");
//                append(writer, "127.0.0.1 - frank [09/May/2018:16:00:42 +0000] \"POST /api/user HTTP/1.0\" 200 34");
            }
        }
    }

    private void append(BufferedWriter writer, String prefix, String suffix) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String format = formatter.format(now);
            writer.write(prefix);
            writer.write(format);
            writer.write(suffix);
            writer.write('\n');
            writer.flush();
            logger.debug("writing: {}{}{})", prefix, format, suffix);

            Thread.sleep(100+random.nextInt(401));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
    }

}
