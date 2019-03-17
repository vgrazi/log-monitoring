package com.vgrazi.monitor.eventprocessor.processor;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import com.vgrazi.monitor.eventprocessor.domain.Scorecard;
import com.vgrazi.monitor.eventprocessor.util.StatsCruncher;
import com.vgrazi.monitor.eventprocessor.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class FrameProcessor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Logger logger = LoggerFactory.getLogger(FrameProcessor.class);
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
     * @param scorecardQueue maintains a Window of frames. The Window is the span of time we are interested in.
     */
    public void processFrames(BlockingQueue<Frame> frameQueue, BlockingQueue<Scorecard> scorecardQueue) {
        executor.submit(()-> {
            logger.info("FrameProcessor running");
            while (running) {
                Frame frame = frameQueue.take();
                windowUtils.addFrameToWindow(frame, window);
                Scorecard scorecard = createScorecard(window);
                scorecardQueue.put(scorecard);
                Thread.yield();
            }
            logger.info("FrameProcessor exiting");
            return null;
        });
    }

    private Scorecard createScorecard(Deque<Frame> frames) {
        logger.debug("Processing frame {}", frames);
        List<String> hitCounts = extractHitCountList(frames);
        Scorecard scorecard = new Scorecard();
        scorecard.setStartTime(frames.getFirst().getStartTime());
        scorecard.setHitCounts(hitCounts);
//        Map<String, Long> failedResponses = statsCruncher.getSectionFailedResponses(frames);
//        Map<String, Long> allHitCount = statsCruncher.getSectionHitCounts(frames);
//        Map.Entry<String, Long> max = statsCruncher.getMaxCountKey(hitCounts);
        // we are guaranteed that no empty window is returned
        return scorecard;
    }

    private List<String> extractHitCountList(Deque<Frame> frames) {
        List<String> hitCounts = frames.stream()
                .map(frame -> String.format("%s:%s", frame.getStartTime(), frame.getHitCount()))
                .collect(Collectors.toList());
        return hitCounts;
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
