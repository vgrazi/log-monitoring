package com.vgrazi.monitor.eventprocessor;

import com.vgrazi.monitor.eventprocessor.domain.Scorecard;
import com.vgrazi.monitor.eventprocessor.util.IOUtils;
import com.vgrazi.monitor.eventprocessor.util.MonitorBuilder;
import com.vgrazi.monitor.eventprocessor.util.StatsCruncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MonitorUI implements CommandLineRunner {

    private static final Object MUTEX = new Object();
    private JFrame frame = new JFrame();
    @Value("${scorecard-directory}")
    private String scorecardDir;
    private volatile boolean running = true;

    @Value("${alert-threshold}")
    private int alertThreshold;

    @Value("${report-stats-secs}")
    private String reportStatsSecs;


    @Override
    public void run(String... args) throws IOException {
        JFrame frame = createFrame();
        frame.add(new JPanel());
        watchForFiles();
    }

    private void watchForFiles() throws IOException {
        Path dir = Paths.get(scorecardDir);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        WatchKey watchKey = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        while (running) {
            watchKey.pollEvents().forEach(event ->
            {
                Path path = (Path) event.context();
//                System.out.printf(" Path: %s %d %s", path, event.count(), event.kind());
                try {
                    Path scorecardFile = Paths.get(scorecardDir, path.getFileName().toString());
                    // give the file a chance to flush!
                    Thread.sleep(50);
                    Scorecard scorecard = IOUtils.readScorecardFile(scorecardFile);
                    Files.delete(scorecardFile);
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
            synchronized (MUTEX) {
                try {
                    MUTEX.wait(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private JFrame createFrame() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        frame.setBounds(screenSize.width / 8, screenSize.height / 16, screenSize.width / 2, 7 * screenSize.height / 8);
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
        LinkedHashMap<String, Long> hitsReportSorted = StatsCruncher.sortByValueReverseOrder(hitsReport);

        if (!secsToHits.isEmpty()) {
            long min = secsToHits.get(0).secs;
            long max = secsToHits.get(secsToHits.size() - 1).secs;
            long count = max - min + 1;

            int[] secs = new int[(int) count];
            for (int i = 0, j = 0; i < count; i++) {
                SecsToHits secsHits = secsToHits.get(j);
                long recordSecs = secsHits.secs;
                if (recordSecs == min + i) {
                    secs[i] = secsHits.hits;
                    j++;
                }
            }
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    MonitorBuilder.clearBackground(graphics, screenWidth, screenHeight);
                    MonitorBuilder.renderAxes(graphics, screenHeight, screenWidth);
                    MonitorBuilder.renderBarGraph(graphics, screenWidth, secs, screenHeight, secsToHits, MonitorBuilder.xMargin, alertThreshold, MonitorBuilder.labelFont, MonitorBuilder.HR_MIN_SEC_FORMATTER);
                    MonitorBuilder.renderAlerts(graphics, scorecard, MonitorBuilder.xMargin, MonitorBuilder.alertYPos);
                    MonitorBuilder.renderHitsReport(graphics, hitsReportSorted, MonitorBuilder.hitCountYPos, reportStatsSecs, MonitorBuilder.xMargin);
                    MonitorBuilder.renderHistory(graphics, scorecard, MonitorBuilder.alertYPos, MonitorBuilder.xHistoryPos);

                    graphics.dispose();
                }
            };
            return panel;
        }
        return null;
    }

    public class SecsToHits {
        public long getSecs() {
            return secs;
        }

        public int getHits() {
            return hits;
        }

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
        synchronized (MUTEX) {
            MUTEX.notify();
        }
    }
}
