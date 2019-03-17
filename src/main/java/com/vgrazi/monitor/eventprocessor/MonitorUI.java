package com.vgrazi.monitor.eventprocessor;

import com.vgrazi.monitor.eventprocessor.domain.Scorecard;
import com.vgrazi.monitor.eventprocessor.util.IOUtils;
import com.vgrazi.monitor.eventprocessor.util.StatsCruncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MonitorUI implements CommandLineRunner {

    public static final int axisXPos = 20;
    private JFrame  frame = new JFrame();
    @Value("${scorecard-directory}")
    private String scorecardDir;
    private volatile boolean running = true;
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private final DateTimeFormatter MIN_SEC_FORMATTER = DateTimeFormatter.ofPattern("mm:ss");

    @Value("${alert-threshold}")
    private int alertThreshold;

    @Value("${report-stats-secs}")
    private String reportStatsSecs;

    private int alertYPos = 20;
    private int hitCountYPos = 30;
    private int xMargin = 20;
    private int yPosXAxis = 100;

    @Override
    public void run(String... args) throws IOException {
        JFrame frame = createFrame();
        frame.add(new JPanel());
        watchForFiles();
    }

    private void watchForFiles() throws IOException {
        Path dir = Paths.get(scorecardDir);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        WatchKey watchKey = dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        while (running) {
            watchKey.pollEvents().forEach(event->
                    {
                        Path path = (Path) event.context();
                        try {
                            Path scorecardFile = Paths.get(scorecardDir, path.getFileName().toString());
                            // give the file a chance to flush!
                            Thread.sleep(50);
                            Scorecard scorecard = IOUtils.readScorecardFile(scorecardFile);
                            JPanel panel = displayScorecard(scorecard);
                            frame.getContentPane().remove(0);
                            frame.getContentPane().add(panel);
                            frame.getContentPane().validate();

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
        }
    }

    private JFrame createFrame() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        frame.setBounds(screenSize.width/8,screenSize.height/16, screenSize.width/2, 7 * screenSize.height/8);
        frame.setVisible(true);
        return frame;
    }

    private JPanel displayScorecard(Scorecard scorecard) {
        Dimension size = frame.getSize();
        int screenHeight = size.height;
        int screenWidth = size.width;

        List<String> hitCounts = scorecard.getHitCounts();
        List<SecsToHits> secsToHits = hitCounts.stream().map(SecsToHits::new).collect(Collectors.toList());

        Map<String, Long> hitsReport = scorecard.getHitsReport();
        LinkedHashMap<String, Long> sortedByValue = StatsCruncher.sortByValueReverseOrder(hitsReport);

        long alertTime = scorecard.getLastTimeOfThresholdExceededAlertSecs();

        if(!secsToHits.isEmpty()) {
            long min = secsToHits.get(0).secs;
            long max = secsToHits.get(secsToHits.size()-1).secs;
            long count = max - min + 1;

            int[] secs = new int[(int) count];
            for(int i = 0, j=0; i < count ; i++) {
                SecsToHits secsHits = secsToHits.get(j);
                long recordSecs = secsHits.secs;
                if(recordSecs == min + i) {
                    secs[i] = secsHits.hits;
                    j++;
                }
            }
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Font font = new Font("Arial", Font.PLAIN, 12);
                    graphics.setFont(font);
                    FontMetrics fm = getFontMetrics(font);
                    int fontHeight = fm.getHeight();
                    super.paintComponent(graphics);
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, screenWidth, screenHeight);

                    int howManyTicksFitOnScreen = screenWidth / 10;

                    // add axes
                    graphics.setColor(Color.gray);
                    for (int i = 0; i < 10; i++) {
                        int y1 = i * 40;
                        graphics.drawString(String.valueOf(i * 2), axisXPos, screenHeight - y1 - yPosXAxis + fontHeight /2);
                        // draw horizontal axes
                        graphics.drawLine(axisXPos + 20, screenHeight - y1 - yPosXAxis, screenWidth, screenHeight - y1 - yPosXAxis);
                    }

                    int start = 0;
                    if (secs.length >= howManyTicksFitOnScreen) {
                        start = secs.length - howManyTicksFitOnScreen + 2;
                        if (start < 0) {
                            start = 0;
                        } else if (start >= secs.length) {
                            // must be a very small file and a narrow canvas. Just start from 0
                            start = 0;
                        }
                    }
                    boolean flip = false;
                    for (int i = start; i < secs.length; i++) {
                        int hitCount = secs[i];
                        if (hitCount >= alertThreshold) {
                            graphics.setColor(Color.red);
                        } else {
                            graphics.setColor(Color.black);
                        }

                        if (hitCount > 0) {
                            graphics.setPaintMode();
                            int x = (i - start + 1) * 10 + axisXPos + 20;
                            // draw vertical lines
                            graphics.drawLine(x, screenHeight - yPosXAxis, x, screenHeight - yPosXAxis - hitCount * 20);
                            int y = screenHeight - yPosXAxis + 20;
                            if(flip) {
                                y += 20;
                            }
                            flip = !flip;
                            graphics.setColor(Color.black);
                            graphics.drawString(MIN_SEC_FORMATTER.format(LocalDateTime.ofEpochSecond(secsToHits.get(0).secs  + i, 0, ZoneOffset.UTC)), x - 10, y);
                        }
                    }

                    graphics.setColor(Color.blue);
                    Iterator<Map.Entry<String, Long>> iterator = sortedByValue.entrySet().iterator();

                    int yPos = fontHeight + hitCountYPos;
                    graphics.drawString(String.format("Hit Counts for last %s seconds", reportStatsSecs), xMargin, yPos);
                    for (int i = 0; iterator.hasNext() && i < 10; i++) {
                        yPos+= fontHeight;
                        Map.Entry<String, Long> entry = iterator.next();
                        graphics.drawString(entry.getKey() + ":" + entry.getValue(), xMargin, yPos);
                    }

                    if(alertTime > 0) {
                        graphics.setColor(Color.red);
                        LocalDateTime time = LocalDateTime.ofEpochSecond(alertTime, 0, ZoneOffset.UTC);
                        graphics.drawString("High alert count at " + FORMATTER.format(time), xMargin, alertYPos);
                    }

                    graphics.dispose();
                }
            };
            return panel;
        }
        return null;
    }

    private class SecsToHits {
        private final long secs;
        private final int hits;
        private SecsToHits(String secsToHits) {
            String[] split = secsToHits.split(":");
            secs = Integer.parseInt(split[0]);
            hits = Integer.parseInt(split[1]);
        }
    }

    public void stop() {
        running = false;
    }
}
