package org.tivrfoa;


public class Logger {

    private Level level;
    private String name;
    private AsyncAppenderBase asyncAppender;

    public Logger(Level level, String name) {
        this.level = level;
        this.name = name;
        this.asyncAppender = new AsyncAppenderBase(new FileAppender("test-async.txt"));
        this.asyncAppender.start();
    }

    public Level getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public void trace(String msg) {
        if (level.equalOrAbove(Level.TRACE)) {
            // append msg
        }
    }

    public void debug(String msg) {
        if (level.equalOrAbove(Level.DEBUG)) {
            asyncAppender.doAppend(msg);
        }
    }

    public void info(String msg) {
        if (level.equalOrAbove(Level.INFO)) {
            // append msg
        }
    }

    public void warn(String msg) {
        if (level.equalOrAbove(Level.WARN)) {
            // append msg
        }
    }

    public void error(String msg) {
        if (level.equalOrAbove(Level.ERROR)) {
            // append msg
        }
    }
}