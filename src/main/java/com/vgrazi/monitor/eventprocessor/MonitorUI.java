package com.vgrazi.monitor.eventprocessor;

import com.vgrazi.monitor.eventprocessor.domain.Scorecard;
import com.vgrazi.monitor.eventprocessor.util.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MonitorUI implements CommandLineRunner {

    private JFrame  frame = new JFrame();
    @Value("${scorecard-directory}")
    private String scorecardDir;
    private volatile boolean running = true;

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
        frame.setBounds(screenSize.width/8,screenSize.height/8, screenSize.width/2, screenSize.height/2);
        frame.setVisible(true);
        return frame;
    }

    private JPanel displayScorecard(Scorecard scorecard) {
        Dimension size = frame.getSize();
        int screenHeight = size.height;
        int screenWidth = size.width;

        List<String> hitCounts = scorecard.getHitCounts();
        List<SecsToHits> secsToHits = hitCounts.stream().map(SecsToHits::new).collect(Collectors.toList());
        JPanel panel;
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
            panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    graphics.setColor(Color.red);
                    int start = 0;
                    if(screenWidth < 10*secs.length) {
                        start = (10 * secs.length - screenWidth)/10 + 4;
                        if(start < 0) {
                            start = 0;
                        }
                    }
                    for (int i = start; i < secs.length; i++) {
                        int height = secs[i];
                        if (height > 0) {
                            graphics.setPaintMode();
                            int x = (i - start + 1) * 10;
                            graphics.drawLine(x, screenHeight, x, screenHeight - height * 20);
                        }
                    }
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
