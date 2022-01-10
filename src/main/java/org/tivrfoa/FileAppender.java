package org.tivrfoa;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FileAppender extends OutputStreamAppender {

    public static final long DEFAULT_BUFFER_SIZE = 8192;
    protected boolean append = true;

    /**
     * The name of the active log file.
     */
    protected String fileName = null;

    private boolean prudent = false;

    private FileSize bufferSize = new FileSize(DEFAULT_BUFFER_SIZE);

    public FileAppender(String fileName) {
        setFile(fileName);
        start();
    }

    /**
     * The <b>File</b> property takes a string value which should be the name of
     * the file to append to.
     */
    public void setFile(String file) {
        if (file == null) {
            fileName = file;
        } else {
            // Trim spaces from both ends. The users probably does not want
            // trailing spaces in file names.
            fileName = file.trim();
        }
    }

    /**
     * Returns the value of the <b>Append</b> property.
     */
    public boolean isAppend() {
        return append;
    }

    /**
     * This method is used by derived classes to obtain the raw file property.
     * Regular users should not be calling this method.
     * 
     * @return the value of the file property
     */
    final public String rawFileProperty() {
        return fileName;
    }

    /**
     * Returns the value of the <b>File</b> property.
     * 
     * <p>
     * This method may be overridden by derived classes.
     * 
     */
    public String getFile() {
        return fileName;
    }

    /**
     * If the value of <b>File</b> is not <code>null</code>, then
     * {@link #openFile} is called with the values of <b>File</b> and
     * <b>Append</b> properties.
     */
    public void start() {
        int errors = 0;
        if (getFile() != null) {
            System.out.println("File property is set to [" + fileName + "]");

            if (prudent) {
                if (!isAppend()) {
                    setAppend(true);
                    System.out.println("Setting \"Append\" property to true on account of \"Prudent\" mode");
                }
            }

            if (checkForFileCollisionInPreviousFileAppenders()) {
                System.err.println("Collisions detected with FileAppender/RollingAppender instances defined earlier. Aborting.");
                errors++;
            } else {
                // file should be opened only if collision free
                try {
                    openFile(getFile());
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    errors++;
                    System.err.println("openFile(" + fileName + "," + append + ") call failed.");
                }
            }
        } else {
            errors++;
            System.err.println("\"File\" property not set for appender named [" + name + "].");
        }
        if (errors == 0) {
            super.start();
        }
    }

    @Override
    public void stop() {
        super.stop();

        /*Map<String, String> map = ContextUtil.getFilenameCollisionMap(context);
        if (map == null || getName() == null)
            return;

        map.remove(getName());*/
    }

    protected boolean checkForFileCollisionInPreviousFileAppenders() {
        /*boolean collisionsDetected = false;
        if (fileName == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> previousFilesMap = (Map<String, String>) context.getObject(CoreConstants.FA_FILENAME_COLLISION_MAP);
        if (previousFilesMap == null) {
            return collisionsDetected;
        }
        for (Entry<String, String> entry : previousFilesMap.entrySet()) {
            if (fileName.equals(entry.getValue())) {
                addErrorForCollision("File", entry.getValue(), entry.getKey());
                collisionsDetected = true;
            }
        }
        if (name != null) {
            previousFilesMap.put(getName(), fileName);
        }
        return collisionsDetected;*/
        return false;
    }

    protected void addErrorForCollision(String optionName, String optionValue, String appenderName) {
        System.err.println("'" + optionName + "' option has the same value \"" + optionValue + "\" as that given for appender [" + appenderName + "] defined earlier.");
    }

    /**
     * <p>
     * Sets and <i>opens</i> the file where the log output will go. The specified
     * file must be writable.
     * 
     * <p>
     * If there was already an opened file, then the previous file is closed
     * first.
     * 
     * <p>
     * <b>Do not use this method directly. To configure a FileAppender or one of
     * its subclasses, set its properties one by one and then call start().</b>
     * 
     * @param file_name
     *          The path to the log file.
     */
    public void openFile(String file_name) throws IOException {
        lock.lock();
        try {
            File file = new File(file_name);
            boolean result = FileUtil.createMissingParentDirectories(file);
            if (!result) {
                System.err.println("Failed to create parent directories for [" + file.getAbsolutePath() + "]");
            }

            ResilientFileOutputStream resilientFos = new ResilientFileOutputStream(file, append, bufferSize.getSize());
            setOutputStream(resilientFos);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @see #setPrudent(boolean)
     * 
     * @return true if in prudent mode
     */
    public boolean isPrudent() {
        return prudent;
    }

    /**
     * When prudent is set to true, file appenders from multiple JVMs can safely
     * write to the same file.
     * 
     * @param prudent
     */
    public void setPrudent(boolean prudent) {
        this.prudent = prudent;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }
    
    public void setBufferSize(FileSize bufferSize) {
        System.out.println("Setting bufferSize to ["+bufferSize.toString()+"]");
        this.bufferSize = bufferSize;
    }

    private void safeWrite(String msg) throws IOException {
        ResilientFileOutputStream resilientFOS = (ResilientFileOutputStream) getOutputStream();
        FileChannel fileChannel = resilientFOS.getChannel();
        if (fileChannel == null) {
            return;
        }

        // Clear any current interrupt (see LOGBACK-875)
        boolean interrupted = Thread.interrupted();

        FileLock fileLock = null;
        try {
            fileLock = fileChannel.lock();
            long position = fileChannel.position();
            long size = fileChannel.size();
            if (size != position) {
                fileChannel.position(size);
            }
            super.writeOut(msg);
        } catch (IOException e) {
            // Mainly to catch FileLockInterruptionExceptions (see LOGBACK-875)
            resilientFOS.postIOFailure(e);
        } finally {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
            }

            // Re-interrupt if we started in an interrupted state (see LOGBACK-875)
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected void writeOut(String msg) throws IOException {
        if (prudent) {
            safeWrite(msg);
        } else {
            super.writeOut(msg);
        }
    }
}
