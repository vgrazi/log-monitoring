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
     * Creates the directory tree if required, and then the file, if required
     * @throws IOException
     */
    public static void createDirectoryTree(File file) throws IOException {
        File parentDir = file.getParentFile();
        if(!parentDir.exists()) {
            boolean mkdirs = parentDir.mkdirs();
            if(mkdirs) {
                logger.info("Created directory {}", parentDir);
            }
        }
        if(!file.exists()) {
            file.createNewFile();
        }

    }

    public static Scorecard readScorecardFile(Path scorecardFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Scorecard scorecard = mapper.readValue(scorecardFile.toFile(), Scorecard.class);
        return scorecard;
    }
}
