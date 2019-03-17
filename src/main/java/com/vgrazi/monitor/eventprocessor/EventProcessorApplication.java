package com.vgrazi.monitor.eventprocessor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class EventProcessorApplication {

    @Value("${scorecard-directory}")
    private String scorecardDir;

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(EventProcessorApplication.class);

        builder.headless(false);
        builder.run(args);
    }

}
