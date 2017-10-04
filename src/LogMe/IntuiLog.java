package LogMe;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

// https://docs.oracle.com/javase/7/docs/api/java/util/logging/Logger.html
public class IntuiLog {
    // Aliases
    public static final Level LEVEL_NONE = Level.OFF; // This level is initialized to Integer.MAX_VALUE
    public static final Level LEVEL_WARNING = Level.WARNING; // This level is initialized to 900.
    public static final Level LEVEL_INFO = Level.INFO; // This level is initialized to 800.
    public static final Level LEVEL_TRACE = Level.FINEST; // This level is initialized to 300.
    public static final Level LEVEL_ERROR = Level.ALL; // This level is initialized to Integer.MIN_VALUE

    // Logger activity
    private boolean active = true;

    // Init execution
    private String executionID;
    // Default level of logging is info
    private Level currentLevel = LEVEL_INFO;

    // Logger
    private final static Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
    private LogConfig logConfig = new LogConfig();

    public IntuiLog(){
        executionID = generateExecutionID();
        LOGGER.addHandler(logConfig.getFileHandler(executionID));
    }

    public IntuiLog(String executionID){
        this.executionID = executionID;
        LOGGER.addHandler(logConfig.getFileHandler(executionID));
    }

    // return
    private String generateExecutionID(){
        return UUID.randomUUID().toString();
    }

    public void logStep(String step){

    }

    public void logSuccess(String success){
        genericLog(String.format("%s\n", success));
    }

    public void logEnd(String message){
        // ...
        active = false;
    }

    private void genericLog(String message)
    {
        if(active)
            LOGGER.log(currentLevel, message);
    }

    public void changeLevelLog(Level level)
    {
        currentLevel = level;
    }
}
