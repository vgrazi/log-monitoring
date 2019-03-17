package com.vgrazi.monitor.eventprocessor;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import com.vgrazi.monitor.eventprocessor.domain.Record;
import com.vgrazi.monitor.eventprocessor.domain.Scorecard;
import com.vgrazi.monitor.eventprocessor.processor.FileReader;
import com.vgrazi.monitor.eventprocessor.processor.FrameProcessor;
import com.vgrazi.monitor.eventprocessor.processor.RecordProcessor;
import com.vgrazi.monitor.eventprocessor.processor.ScorecardProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

@Component
public class EventProcessLauncher implements CommandLineRunner {
    private final Logger logger = LoggerFactory.getLogger("EventProcessor");

    @Autowired
    private FileReader fileReader;

    @Autowired
    private RecordProcessor recordProcessor;

    @Autowired
    private FrameProcessor frameProcessor;

    @Autowired
    private ScorecardProcessor scorecardProcessor;

    @Override
    public void run(String[] args) {
/*
          the FileReader parses records into Record instances, then deposits them on the queue.
          They are picked up by the EventProcessor, which groups them into Frames, and deposits them on a queue.
          They are then processed by the FrameProcessor
*/
        TransferQueue<Record> recordQueue = new LinkedTransferQueue<>();
        TransferQueue<Frame> frameQueue = new LinkedTransferQueue<>();
        TransferQueue<Scorecard> scorecardQueue = new LinkedTransferQueue<>();
        // read lines, parse them, and add them to the records queue
        fileReader.tailFile(recordQueue);

        // as records appear, process them, group them into Frames, and deposit the Frames onto the frameQueue
        recordProcessor.processRecords(recordQueue, frameQueue);

        // grabs frames, forms them into "Windows" (10 minutes spans), and produces scorecards
        frameProcessor.processFrames(frameQueue, scorecardQueue);

        // grabs new scorecards and writes them to the file system. Also cleans up old files
        scorecardProcessor.processScorecard(scorecardQueue);

    }

}
