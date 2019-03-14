package com.vgrazi.monitor.eventprocessor.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A log record is in <a href="https://www.w3.org/Daemon/User/Config/Logging.html#common-logfile-format">common-logfile-format</a>, consisting of the following:
 * Host, caller, date:time, method, request, return code, byte count
 * <pre>
 * 127.0.0.1 - james [11/Mar/2019:22:08:21 +9490] "GET /report HTTP/1.0" 200 123
 * 127.0.0.1 - jill [11/Mar/2019:22:08:21 +9600] "GET /api/user HTTP/1.0" 200 234
 * </pre>
 */
public class Record {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss +SSSS");
    private final Logger logger = LoggerFactory.getLogger("Record");

    // we keep the record time and the actual time, just in case a record is missing a time, or uses hokey times
    private LocalDateTime actualTime = LocalDateTime.now();
    private LocalDateTime recordTime;
    private String logLine;
    private String host;
    private String caller;
    private String method;
    private String section;
    private String request;
    private int returnCode;
    private long byteCount;
    private String httpVersion;

    private List<String> errors;

    private final static Pattern typicalLinePattern = Pattern.compile(
            "(?x)(?<host>\\S+)\\s-\\s(?<caller>\\S+)\\s\\[" +
                    "(?<datetime>\\d\\d?/\\w{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2}\\s\\+\\d{4})]\\s\"" +
                    "(?<httpmethod>\\w+)\\s(?<section>/[^/\\s]+)?(?<request>\\S+)*\\s(?<http>\\w+)/(?<httpversion>[^\"]+)\"\\s" +
                    "(?<returncode>\\d{3})\\s(?<bytecount>\\d+)"
    );

    private final static Pattern HOST_PATTERN = Pattern.compile("^\\s*(?<host>\\S+)");
    private final static Pattern CALLER_PATTERN = Pattern.compile("- (?<caller>\\S+)");
    private final static Pattern HTTP_METHOD_PATTERN = Pattern.compile("\"(?<httpmethod>\\S+)");
    private final static Pattern REQUEST_PATTERN = Pattern.compile("\"\\S+\\s(?<request>(?<section>/[^/\\s]+)((/[^/\\s]+)*))");
    private final static Pattern HTTP_VERSION_PATTERN = Pattern.compile("(?<httpversion>\\s\\S+)\"");
    private final static Pattern RETURN_CODE_PATTERN = Pattern.compile("\"\\s(?<returncode>\\d+)");
    private final static Pattern BYTE_COUNT_PATTERN = Pattern.compile("(?<bytecount>\\d+)$");
    // todo: Do we need to make the date/time pattern configurable??
    private final static Pattern DATE_TIME_PATTERN = Pattern.compile("\\[(?<datetime>\\d\\d?/\\w+?/\\d{4}:\\d\\d?:\\d\\d?:\\d\\d?\\s\\+\\d+)]");
    /**
     * Constructor accepts the actual log line
     *
     * @param logLine the line in the log
     */
    private Record(String logLine) {
        this.logLine = logLine;
        parseLine(logLine);

    }

    public static Record fromLine(String line) {
        return new Record(line);
    }

    /**
     * makes a best effort to parse the incoming line into its time, host, caller, section, etc.
     * Classic log line looks like:
     * <pre>127.0.0.1 - james [11/Mar/2019:22:08:21 +9490] "GET /report HTTP/1.0" 200 123</pre>
     * '<pre>127.0.0.1 - jill [11/Mar/2019:22:08:21 +9600] "GET /api/user HTTP/1.0" 200 234</pre>
     *
     * @param line the log line to parse
     */
    private void parseLine(String line) {
        // todo: This assumes a rigid string format, which was stated as a requirement.
        //   for this version, we will assume it either matches or it is an exception. We can add further parsing to try
        //   smaller globs in case of no match
        Matcher matcher = typicalLinePattern.matcher(line);
        // if the line meets out requirements, then do it in one parse
        if (matcher.matches()) {
            logger.debug("Processing line: {}", line);

            host = matcher.group("host");
            caller = matcher.group("caller");
            method = matcher.group("httpmethod");
            section = matcher.group("section");
            request = matcher.group("request");
            returnCode = Integer.parseInt(matcher.group("returncode"));
            byteCount = Integer.parseInt(matcher.group("bytecount"));
            recordTime = LocalDateTime.from(FORMATTER.parse(matcher.group("datetime")));
        } else {
            logger.warn("Line was not formatted correctly: {}", line);
            // uh-oh, line is broken. Let's do our best and report issues
            matcher = HOST_PATTERN.matcher(line);
            if (matcher.find()) {
                host = matcher.group("host");
            }
            else {
                 addErrorMessage("host");
            }
            matcher = CALLER_PATTERN.matcher(line);
            if (matcher.find()) {
                caller = matcher.group("caller");
            }
            else {
                addErrorMessage("caller");
            }
            matcher = HTTP_METHOD_PATTERN.matcher(line);
            if (matcher.find()) {
                method = matcher.group("httpmethod");
            }
            else {
                addErrorMessage("http method");
            }
            matcher = REQUEST_PATTERN.matcher(line);
            if (matcher.find()) {
                section = matcher.group("section");
                request = matcher.group("request");
            }
            else {
                addErrorMessage("request/section");
            }
            matcher = HTTP_VERSION_PATTERN.matcher(line);
            if (matcher.find()) {
                httpVersion = matcher.group("httpversion");
            }
            else {
                addErrorMessage("http version");
            }
            matcher = RETURN_CODE_PATTERN.matcher(line);
            if (matcher.find()) {
                returnCode = Integer.parseInt(matcher.group("returncode"));
            }
            else {
                addErrorMessage("Http return code");
            }
            matcher = BYTE_COUNT_PATTERN.matcher(line);
            if (matcher.find()) {
                byteCount = Integer.parseInt(matcher.group("bytecount"));
            }
            else {
                addErrorMessage("byte count");
            }
            matcher = DATE_TIME_PATTERN.matcher(line);
            if (matcher.find()) {
                recordTime = LocalDateTime.from(FORMATTER.parse(matcher.group("datetime")));
            }
            else {
                addErrorMessage("date/time");
            }
        }
    }

    private void addErrorMessage(String message) {
        if(errors == null) {
            errors = new LinkedList<>();
        }
        errors.add(message);
    }

    public boolean isError() {
        return errors != null;
    }

    public LocalDateTime getRecordTime() {
        return recordTime;
    }

    public LocalDateTime getActualTime() {
        return actualTime;
    }

    public String getHost() {
        return host;
    }

    public String getCaller() {
        return caller;
    }

    public String getMethod() {
        return method;
    }

    public String getSection() {
        return section;
    }

    public String getRequest() {
        return request;
    }

    public long getByteCount() {
        return byteCount;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    @Override
    public String toString() {
        String string = "Record{'" +
                logLine + '\'' +
                '}';
        if(isError()){
            string += ", line missing the following:" + errors;
        }
        return string;
    }

    public String toDetailedString() {
        return "Record{" +
                "logLine='" + logLine + '\'' +
                ", errors=" + errors +
                ", actualTime=" + actualTime +
                ", recordTime=" + recordTime +
                ", host='" + host + '\'' +
                ", caller='" + caller + '\'' +
                ", method='" + method + '\'' +
                ", section='" + section + '\'' +
                ", request='" + request + '\'' +
                ", returnCode=" + returnCode +
                ", byteCount=" + byteCount +
                ", httpVersion='" + httpVersion + '\'' +
                '}';
    }
}
