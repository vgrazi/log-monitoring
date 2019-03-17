package com.vgrazi.monitor.eventprocessor.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vgrazi.monitor.eventprocessor.domain.Scorecard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class IOUtils {
    private final static Logger logger = LoggerFactory.getLogger(IOUtils.class);

    /**
     * Creates the directory tree if required
     */
    public static void createDirectoryTree(File parentDir) {
        if(!parentDir.exists()) {
            boolean mkdirs = parentDir.mkdirs();
            if(mkdirs) {
                logger.info("Created directory {}", parentDir);
            }
        }
    }

    public static Scorecard readScorecardFile(Path scorecardFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Scorecard scorecard = mapper.readValue(scorecardFile.toFile(), Scorecard.class);
        return scorecard;
    }
}
