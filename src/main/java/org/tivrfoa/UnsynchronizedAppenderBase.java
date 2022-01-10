package org.tivrfoa;

abstract public class UnsynchronizedAppenderBase {

    protected boolean started = false;
    private ThreadLocal<Boolean> guard = new ThreadLocal<Boolean>();
    protected String name;

    public String getName() {
        return name;
    }

    private int statusRepeatCount = 0;
    private int exceptionCount = 0;

    static final int ALLOWED_REPEATS = 3;

    public void doAppend(String msg) {
        // WARNING: The guard check MUST be the first statement in the
        // doAppend() method.

        // prevent re-entry.
        if (Boolean.TRUE.equals(guard.get())) {
            return;
        }

        try {
            guard.set(Boolean.TRUE);

            if (!this.started) {
                if (statusRepeatCount++ < ALLOWED_REPEATS) {
                    System.out.println("Attempted to append to non started appender [" + name + "].");
                }
                return;
            }

            this.append(msg);
        } catch (Exception e) {
            e.printStackTrace();
            if (exceptionCount++ < ALLOWED_REPEATS) {
                System.err.println("Appender [" + name + "] failed to append.");
            }
        } finally {
            guard.set(Boolean.FALSE);
        }
    }

    abstract protected void append(String msg);

    public void setName(String name) {
        this.name = name;
    }

    public void start() {
        started = true;
    }

    public void stop() {
        started = false;
    }

    public boolean isStarted() {
        return started;
    }

    public String toString() {
        return this.getClass().getName() + "[" + name + "]";
    }
}
