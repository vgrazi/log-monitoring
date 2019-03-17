package com.vgrazi.monitor.eventprocessor.util;

import com.vgrazi.monitor.eventprocessor.domain.Frame;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Deque;

@Service
public class WindowUtils {

    @Value("${window-duration-seconds}")
    private int windowDuration;

    /**
     * If Frame end time - Window start time > windowDuration, then evict oldest records
     * from the frame until the time duration is back in line
     */
    public void addFrameToWindow(Frame frame, Deque<Frame> window) {
        // first prune old records from the window
        while(!window.isEmpty()) {
            if(window.getFirst().getStartTime() - frame.getFrameEndTime() > windowDuration) {
                window.removeFirst();
            }
            else {
                break;
            }
        }

        // then add the new record
        window.addLast(frame);
    }
}
