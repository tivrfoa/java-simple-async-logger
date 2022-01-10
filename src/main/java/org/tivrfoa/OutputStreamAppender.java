package org.tivrfoa;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantLock;

public class OutputStreamAppender extends UnsynchronizedAppenderBase {

    /**
     * All synchronization in this class is done via the lock object.
     */
    protected final ReentrantLock lock = new ReentrantLock(false);

    /**
     * This is the {@link OutputStream outputStream} where output will be written.
     */
    private OutputStream outputStream;

    boolean immediateFlush = true;

    /**
    * The underlying output stream used by this appender.
    * 
    * @return
    */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Checks that requires parameters are set and if everything is in order,
     * activates this appender.
     */
    public void start() {
        int errors = 0;

        if (this.outputStream == null) {
            System.err.println("No output stream set for the appender named \"" + name + "\".");
            errors++;
        }
        // only error free appenders should be activated
        if (errors == 0) {
            super.start();
        }
    }

    @Override
    protected void append(String msg) {
        if (!isStarted()) {
            return;
        }

        subAppend(msg);
    }

    /**
     * Stop this appender instance. The underlying stream or writer is also
     * closed.
     * 
     * <p>
     * Stopped appenders cannot be reused.
     */
    public void stop() {
        lock.lock();
        try {
            closeOutputStream();
            super.stop();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Close the underlying {@link OutputStream}.
     */
    protected void closeOutputStream() {
        if (this.outputStream != null) {
            try {
                this.outputStream.close();
                this.outputStream = null;
            } catch (IOException e) {
                System.err.println("Could not close output stream for OutputStreamAppender.");
            }
        }
    }

    /**
     * <p>
     * Sets the @link OutputStream} where the log output will go. The specified
     * <code>OutputStream</code> must be opened by the user and be writable. The
     * <code>OutputStream</code> will be closed when the appender instance is
     * closed.
     * 
     * @param outputStream
     *          An already opened OutputStream.
     */
    public void setOutputStream(OutputStream outputStream) {
        lock.lock();
        try {
            // close any previously opened output stream
            closeOutputStream();
            this.outputStream = outputStream;
        } finally {
            lock.unlock();
        }
    }

    protected void writeOut(String msg) throws IOException {
        byte[] byteArray = msg.getBytes();
        writeBytes(byteArray);
    }

    private void writeBytes(byte[] byteArray) throws IOException {
        if(byteArray == null || byteArray.length == 0)
            return;
        
        lock.lock();
        try {
            this.outputStream.write(byteArray);
            if (immediateFlush) {
                this.outputStream.flush();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Actual writing occurs here.
     * <p>
     * Most subclasses of <code>WriterAppender</code> will need to override this
     * method.
     * 
     * @since 0.9.0
     */
    protected void subAppend(String msg) {
        if (!isStarted()) {
            return;
        }
        try {
            byte[] byteArray = msg.getBytes();
            writeBytes(byteArray);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            // as soon as an exception occurs, move to non-started state
            // and add a single ErrorStatus to the SM.
            this.started = false;
            System.err.println("IO failure in appender");
        }
    }

    public boolean isImmediateFlush() {
        return immediateFlush;
    }

    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }

}

