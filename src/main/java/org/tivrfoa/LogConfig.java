package org.tivrfoa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class LogConfig {
    
    private static Properties props = loadProperties();
    public static final String OUTPUT_FILE_NAME = props.getProperty("output-file");
    public static final Level LEVEL = Level.valueOf(props.getProperty("level"));

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = LogConfig.class.getClassLoader().getResourceAsStream("log.properties")) {
            props.load(is);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return props;
    }
}
