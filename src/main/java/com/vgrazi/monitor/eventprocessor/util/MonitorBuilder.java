package com.vgrazi.monitor.eventprocessor.util;

import com.vgrazi.monitor.eventprocessor.MonitorUI;
import com.vgrazi.monitor.eventprocessor.domain.Scorecard;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class MonitorBuilder {
    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public final static DateTimeFormatter HR_MIN_SEC_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final Font labelFont = new Font("Courier", Font.PLAIN, 10);
    private static final int axisXPos = 20;
    private static final int axisYPos = 100;
    public static final int xHistoryPos = 400;
    public static int alertYPos = 20;
    public static int hitCountYPos = 30;
    public static int xMargin = 20;
    private static final Font font = new Font("Arial", Font.PLAIN, 12);
    public static void renderAlerts(Graphics graphics, Scorecard scorecard, int xMargin, int alertYPos) {
        String alert = scorecard.getAlert();
        if(alert != null) {
            graphics.setColor(Color.red);
            graphics.drawString(alert, xMargin, alertYPos );
        }
        if(scorecard.isInHighActivity()) {
            graphics.setColor(Color.red);
            graphics.drawString("High activity detected since " + DATE_TIME_FORMATTER.format(LocalDateTime.ofEpochSecond(scorecard.getFirstTimeOfThresholdExceededSecs(), 0, ZoneOffset.UTC)), xMargin, alertYPos );
        }
    }

    public static void renderBarGraph(Graphics graphics, int screenWidth, int[] secs, int screenHeight, List<MonitorUI.SecsToHits> secsToHits, int xMargin, int alertThreshold, Font labelFont, DateTimeFormatter HR_MIN_SEC_FORMATTER) {
        int howManyTicksFitOnScreen = (screenWidth - xMargin - 50) / 10;
        int start = 0;
        if (secs.length >= howManyTicksFitOnScreen) {
            start = secs.length - howManyTicksFitOnScreen + 2;
            if (start < 0) {
                start = 0;
            } else if (start >= secs.length) {
                // must be a very small file and a narrow canvas. Just start from 0
                start = 0;
            }
        }

        // plot the vertical bars
        int flip = 0;
        for (int i = start; i < secs.length; i++) {
            int hitCount = secs[i];
            if (hitCount >= alertThreshold) {
                graphics.setColor(Color.red);
            } else {
                graphics.setColor(Color.black);
            }

            if (hitCount > 0) {
                graphics.setPaintMode();
                int x = (i - start + 1) * 10 + axisXPos + 20;
                // draw vertical lines
                graphics.drawLine(x, screenHeight - axisYPos, x, screenHeight - axisYPos - hitCount * 20);
                int y = screenHeight - axisYPos + 20;
                if(flip % 3 == 1) {
                    y += 15;
                }
                else if(flip % 3 == 2) {
                    y += 30;
                }
                flip++;
                graphics.setColor(Color.black);
                graphics.setFont(labelFont);
                graphics.drawString(HR_MIN_SEC_FORMATTER.format(LocalDateTime.ofEpochSecond(secsToHits.get(0).getSecs()  + i, 0, ZoneOffset.UTC)), x - 10, y);
            }
        }
    }

    public static void renderAxes(Graphics graphics, int screenHeight, int screenWidth) {
        // add axes
        graphics.setColor(Color.gray);
        FontMetrics fm = graphics.getFontMetrics(font);
        int fontHeight = fm.getHeight();

        for (int i = 0; i < 10; i++) {
            int y1 = i * 40;
            graphics.drawString(String.valueOf(i * 2), axisXPos, screenHeight - y1 - axisYPos + fontHeight /2);
            // draw horizontal axes
            graphics.drawLine(axisXPos + 20, screenHeight - y1 - axisYPos, screenWidth, screenHeight - y1 - axisYPos);
        }
    }

    public static void renderHistory(Graphics graphics, Scorecard scorecard, int alertYPos, int xHistoryPos) {
        // render the history
        int yPos = alertYPos;
        graphics.setColor(Color.blue);
        graphics.drawString("History", xHistoryPos, yPos);
        FontMetrics fm = graphics.getFontMetrics();
        int fontHeight = fm.getHeight();
        Deque<String> history = scorecard.getHistory();
        int index = 0;
        for(String message:history) {
            yPos+= fontHeight;
            graphics.drawString(message, xHistoryPos, yPos);
            index++;
            if(index >= 10) {
                break;
            }
        }
    }

    public static void renderHitsReport(Graphics graphics, LinkedHashMap<String, Long> hitsReportSorted, int hitCountYPos, String reportStatsSecs, int xMargin) {
        // render the hits report
        graphics.setColor(Color.blue);
        Iterator<Map.Entry<String, Long>> iterator = hitsReportSorted.entrySet().iterator();
        FontMetrics fm = graphics.getFontMetrics();
        int fontHeight = fm.getHeight();
        int yPos = fontHeight + hitCountYPos;
        graphics.drawString(String.format("Highest section hit-counts for last %s seconds", reportStatsSecs), xMargin, yPos);
        for (int i = 0; iterator.hasNext() && i < 10; i++) {
            yPos+= fontHeight;
            Map.Entry<String, Long> entry = iterator.next();
            graphics.drawString(entry.getKey() + ":" + entry.getValue(), xMargin, yPos);
        }
    }

    public static void clearBackground(Graphics graphics, int screenWidth, int screenHeight) {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, screenWidth, screenHeight);
        graphics.setFont(font);
    }
}
