package org.tivrfoa;

import java.io.File;

public class FileUtil {

    static public boolean createMissingParentDirectories(File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            // Parent directory not specified, therefore it's a request to
            // create nothing. Done! ;)
            return true;
        }

        // File.mkdirs() creates the parent directories only if they don't
        // already exist; and it's okay if they do.
        parent.mkdirs();
        return parent.exists();
    }
}
