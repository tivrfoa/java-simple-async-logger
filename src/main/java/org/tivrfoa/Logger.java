package org.tivrfoa;

import java.time.ZonedDateTime;

public class Logger {

    private String name;
    private AsyncAppenderBase asyncAppender;

    public Logger(String name) {
        this.name = name;

        // TODO find a better place for the code below?!
        // Should this be done every time a Logger is instantiated?
        this.asyncAppender = new AsyncAppenderBase(new FileAppender(LogConfig.OUTPUT_FILE_NAME));
        this.asyncAppender.start();
    }

    public Level getLevel() {
        return LogConfig.LEVEL;
    }

    public String getName() {
        return name;
    }

    private void log(Level level, String msg) {
        if (LogConfig.LEVEL.equalOrAbove(level)) {
            asyncAppender.doAppend(ZonedDateTime.now() + " " + level + " [" +
                   Thread.currentThread().getName() + "] " + name + ": "  + msg + "\n");
        }
    }

    public void trace(String msg) {
        log(Level.TRACE, msg);
    }

    public void debug(String msg) {
        log(Level.DEBUG, msg);
    }

    public void info(String msg) {
        log(Level.INFO, msg);
    }

    public void warn(String msg) {
        log(Level.WARN, msg);
    }

    public void error(String msg) {
        log(Level.ERROR, msg);
    }
}