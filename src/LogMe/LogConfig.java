package LogMe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class LogConfig {

    private final String CONFIG_FILE = "logging.properties";
    private final String PROP_LOG_PATH = "log_path";
    private static final String PROP_LOG_FORMAT = "format";
    private final String LOG_FORMAT = ".log";

    private static Properties prop = new Properties();
    private String fileHandlerConfig;
    private FileHandler fileHandler;

    public LogConfig() {
        InputStream input;
        try {
            input = new FileInputStream(CONFIG_FILE);

            // load a properties file
            prop.load(input);
            fileHandlerConfig = prop.getProperty(PROP_LOG_PATH);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public FileHandler getFileHandler(String logFile){
        try{
            fileHandler = new FileHandler(fileHandlerConfig + File.separator + logFile + LOG_FORMAT);
            set_format();
        } catch (IOException e){
            System.out.println(String.format("Cannot retrieve File Handler: %s", fileHandlerConfig + File.pathSeparator + logFile));
        }
        return fileHandler;
    }

    private void set_format() {
        fileHandler.setFormatter(new SimpleFormatter() {
            private final String format = prop.getProperty(PROP_LOG_FORMAT);

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        lr.getMessage().replaceAll("\\\\\"", "\"")
                );
            }
        });
    }
}
