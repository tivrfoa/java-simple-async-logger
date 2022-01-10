package org.tivrfoa;

public enum Level {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    public boolean equalOrAbove(Level other) {
        return switch (this) {
            case TRACE -> true;
            case DEBUG -> other != TRACE;
            case INFO -> other == INFO || other == WARN || other == ERROR;
            case WARN -> other == WARN || other == ERROR;
            case ERROR -> other == ERROR;
        };
    }
}