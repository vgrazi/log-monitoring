package com.vgrazi.monitor.eventprocessor;

import com.vgrazi.monitor.eventprocessor.domain.Record;
import com.vgrazi.monitor.eventprocessor.domain.RecordGroup;
import com.vgrazi.monitor.eventprocessor.processor.FileReader;
import com.vgrazi.monitor.eventprocessor.processor.GroupProcessor;
import com.vgrazi.monitor.eventprocessor.processor.RecordProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.util.List;
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
    private GroupProcessor groupProcessor;


    @Override
    public void run(String[] args) {
/*
          the FileReader parses records into Record instances, then deposits them on the queue.
          They are picked up by the EventProcessor, which groups them into groups, and deposits them on a queue.
          They are then processed by the GroupProcessor
*/
        TransferQueue<Record> recordQueue = new LinkedTransferQueue<>();
        TransferQueue<RecordGroup> groupQueue = new LinkedTransferQueue<>();
        // read lines, add them to the records queue
        fileReader.tailFile(recordQueue);

        recordProcessor.processRecords(recordQueue, groupQueue);

        groupProcessor.processGroups(groupQueue);

    }

}
