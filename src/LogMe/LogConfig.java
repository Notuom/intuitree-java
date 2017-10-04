package LogMe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.FileHandler;

public class LogConfig {

    private final String CONFIG_FILE = "config.properties";
    private final String PROP_LOG_PATH = "log_path";

    public Properties prop = new Properties();
    public String fileHandler;

    public LogConfig() {
        InputStream input = null;
        try {
            input = new FileInputStream(CONFIG_FILE);

            // load a properties file
            prop.load(input);
            fileHandler = prop.getProperty(PROP_LOG_PATH);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public FileHandler getFileHandler(String logFile){
        FileHandler fh = null;
        try{
             fh = new FileHandler(fileHandler + File.pathSeparator + logFile);
        } catch (IOException e){
            System.out.println(String.format("Cannot retrieve File Handler: %s", fileHandler + File.pathSeparator + logFile));
        }
        return fh;
    }
}
