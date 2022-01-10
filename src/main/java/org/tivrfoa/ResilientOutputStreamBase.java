package org.tivrfoa;

import java.io.IOException;
import java.io.OutputStream;

abstract public class ResilientOutputStreamBase extends OutputStream {

    final static int STATUS_COUNT_LIMIT = 2 * 4;

    private RecoveryCoordinator recoveryCoordinator;

    protected OutputStream os;
    protected boolean presumedClean = true;

    private boolean isPresumedInError() {
        // existence of recoveryCoordinator indicates failed state
        return (recoveryCoordinator != null && !presumedClean);
    }

    public void write(byte b[], int off, int len) {
        if (isPresumedInError()) {
            if (!recoveryCoordinator.isTooSoon()) {
                attemptRecovery();
            }
            return; // return regardless of the success of the recovery attempt
        }

        try {
            os.write(b, off, len);
            postSuccessfulWrite();
        } catch (IOException e) {
            postIOFailure(e);
        }
    }

    @Override
    public void write(int b) {
        if (isPresumedInError()) {
            if (!recoveryCoordinator.isTooSoon()) {
                attemptRecovery();
            }
            return; // return regardless of the success of the recovery attempt
        }
        try {
            os.write(b);
            postSuccessfulWrite();
        } catch (IOException e) {
            postIOFailure(e);
        }
    }

    @Override
    public void flush() {
        if (os != null) {
            try {
                os.flush();
                postSuccessfulWrite();
            } catch (IOException e) {
                postIOFailure(e);
            }
        }
    }

    abstract String getDescription();

    abstract OutputStream openNewOutputStream() throws IOException;

    private void postSuccessfulWrite() {
        if (recoveryCoordinator != null) {
            recoveryCoordinator = null;
            System.out.println("Recovered from IO failure on " + getDescription());
        }
    }

    public void postIOFailure(IOException e) {
        System.err.println("IO failure while writing to " + getDescription());
        presumedClean = false;
        if (recoveryCoordinator == null) {
            recoveryCoordinator = new RecoveryCoordinator();
        }
    }

    @Override
    public void close() throws IOException {
        if (os != null) {
            os.close();
        }
    }

    void attemptRecovery() {
        try {
            close();
        } catch (IOException e) {
        }

        System.out.println("Attempting to recover from IO failure on " + getDescription());

        // subsequent writes must always be in append mode
        try {
            os = openNewOutputStream();
            presumedClean = true;
        } catch (IOException e) {
            System.err.println("Failed to open " + getDescription());
        }
    }
}
