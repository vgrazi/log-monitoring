package com.vgrazi.monitor.eventprocessor.config;

import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class AppProperties {
    // File name won't be changed without a restart. Todo: But if this is a requirement, implement it
    @Value("${filename}")
    private String fileName;
    @Value("${poll-frequency-sec}")
    private int pollFrequencySeconds;

    public String getFileName() {
        return fileName;
    }

    public int getPollFrequencySeconds() {
        return pollFrequencySeconds;
    }
}
