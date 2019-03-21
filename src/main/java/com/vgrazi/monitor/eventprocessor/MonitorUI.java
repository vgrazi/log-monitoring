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

/**
 * Listens for new Scorecard files in the "output" directory, consumes them, renders them, and deletes them
 */
@Component
public class MonitorUI implements CommandLineRunner {

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
        JFrame frame = createJFrame();
        frame.add(new JPanel());
        watchForFiles();
    }

    private void watchForFiles() throws IOException {
        Path dir = Paths.get(scorecardDir);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        WatchKey watchKey = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        try {
            while (running && watchService.take() != null) {
                watchKey.pollEvents().forEach(event ->
                {
                    Path path = (Path) event.context();
                    System.out.printf(" Path: %s %d %s", path, event.count(), event.kind());
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
                watchKey.reset();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private JFrame createJFrame() {
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

        List<String> hitCountList = scorecard.getHitCounts();
        List<SecsToHits> secsToHits = hitCountList.stream().map(SecsToHits::new).collect(Collectors.toList());

        Map<String, Long> hitsReport = scorecard.getHitsReport();
        LinkedHashMap<String, Long> hitsReportSorted = StatsCruncher.sortByValueReverseOrder(hitsReport);

        if (!secsToHits.isEmpty()) {
            int[] hitCounts = getHitCountsForScorecard(secsToHits);
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    MonitorBuilder.clearBackground(graphics, screenWidth, screenHeight);
                    MonitorBuilder.renderAxes(graphics, screenWidth, screenHeight);
                    MonitorBuilder.renderBarGraph(graphics, screenWidth, screenHeight, hitCounts, secsToHits, alertThreshold);
                    MonitorBuilder.renderAlerts(graphics, scorecard);
                    MonitorBuilder.renderHitsReport(graphics, hitsReportSorted, reportStatsSecs);
                    MonitorBuilder.renderHistory(graphics, scorecard);

                    graphics.dispose();
                }
            };
            return panel;
        }
        return null;
    }

    /**
     * This method computes an int array based on the incoming SecsToHits list.
     * Background: Scorecard contains all of the data required for rendering, including the entire historical x and y graph data
     * The x axis are the "secs" part of SecsToHits
     * The y-axis are the "hits" part of SecsToHits
     * Iterates all of the
     */
    private int[] getHitCountsForScorecard(List<SecsToHits> secsToHits) {
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
        return secs;
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
    }
}
