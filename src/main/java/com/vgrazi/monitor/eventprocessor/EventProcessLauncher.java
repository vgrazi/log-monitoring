package com.vgrazi.monitor.eventprocessor;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import com.vgrazi.monitor.eventprocessor.domain.Record;
import com.vgrazi.monitor.eventprocessor.processor.FileReader;
import com.vgrazi.monitor.eventprocessor.processor.FrameProcessor;
import com.vgrazi.monitor.eventprocessor.processor.RecordProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

@Component
public class EventProcessLauncher implements CommandLineRunner {
    Logger logger = LoggerFactory.getLogger("EventProcessor");

    @Autowired
    private FileReader fileReader;

    @Autowired
    private RecordProcessor recordProcessor;

    @Autowired
    private FrameProcessor frameProcessor;


    @Override
    public void run(String[] args) {
/*
          the FileReader parses records into Record instances, then deposits them on the queue.
          They are picked up by the EventProcessor, which groups them into groups, and deposits them on a queue.
          They are then processed by the GroupProcessor
*/
        TransferQueue<Record> recordQueue = new LinkedTransferQueue<>();
        TransferQueue<Frame> groupQueue = new LinkedTransferQueue<>();
        // read lines, add them to the records queue
        fileReader.tailFile(recordQueue);

        recordProcessor.processRecords(recordQueue, groupQueue);

        frameProcessor.processFrames(groupQueue);

    }

}
