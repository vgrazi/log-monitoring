package com.vgrazi.monitor.eventprocessor.processor;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import com.vgrazi.monitor.eventprocessor.domain.Scorecard;
import com.vgrazi.monitor.eventprocessor.domain.State;
import com.vgrazi.monitor.eventprocessor.util.StatsCruncher;
import com.vgrazi.monitor.eventprocessor.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Picks up Frames of 1 second's worth of records, adjusts the "State" and computes a scorecard
 * Note that all data consumed by the monitor is produced in this FrameProcessor class, and persisted to the filesystem
 * in the form of a Scorecard file.
 * Scorecards are then thrown onto the Scorecard queue where they are picked up by the ScorecardProcessor
 */
@Service
public class FrameProcessor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Logger logger = LoggerFactory.getLogger(FrameProcessor.class);
    private volatile boolean running = true;

    @Value("${report-stats-secs}")
    private int reportStatsTimeSecs;

    @Autowired
    private StatsCruncher statsCruncher;

    @Value("${seconds-of-thrashing}")
// if average hit count > alert-threshold for 120 seconds, display alert
    private int secondsOfThrashing;

    @Value("${alert-threshold}")
    private int alertThreshold;

    /**
     * Maintain the window of interest, 10 minutes by default, consisting of all frames within that time period
     */
    private final Deque<Frame> window = new LinkedList<>();

    @Autowired
    private WindowUtils windowUtils;

    private State state = new State();
    /**
     * When the RecordProcessor deposits Frame of seconds onto the queue, FrameProcessor processes them
     * @param frameQueue a queue of all Frames within the configured frequency
     * @param scorecardQueue maintains a Window of frames. The Window is the span of time we are interested in.
     */
    public void processFrames(BlockingQueue<Frame> frameQueue, BlockingQueue<Scorecard> scorecardQueue) {
        state.setLastStatsReportTime(0);
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
        List<String> hitCounts = StatsCruncher.extractHitCountList(frames);
        Scorecard scorecard = new Scorecard();
        scorecard.setStartTime(frames.getFirst().getStartTime());
        scorecard.setHitCounts(hitCounts);

        // generate hits report
        long now = statsCruncher.saveHitsReportToState(frames, state, reportStatsTimeSecs);
        scorecard.setHitsReport(state.getHitsReport());

        // generate average hit counts for last 2 minutes
        int avgHitCountForLastSeconds = StatsCruncher.getHitCountForLastSeconds(frames, secondsOfThrashing)/ frames.size();
        statsCruncher.saveHitCountAlertsToState(scorecard, now, avgHitCountForLastSeconds, state, alertThreshold, secondsOfThrashing);

        scorecard.setLastTimeOfThresholdExceededAlertSecs(state.getLastTimeOfThresholdExceededAlertSecs());
        scorecard.setFirstTimeOfThresholdExceededSecs(state.getFirstTimeOfThresholdExceededSecs());
        scorecard.setInHighActivity(state.isInHighActivity());
        scorecard.setHistory(state.getHistory());

//        Map<String, Long> failedResponses = statsCruncher.getSectionFailedResponses(frames);
//        Map<String, Long> allHitCount = statsCruncher.getSectionHitCounts(frames);
//        Map.Entry<String, Long> max = statsCruncher.getMaxCountKey(hitCounts);
        // we are guaranteed that no empty window is returned
        return scorecard;
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
