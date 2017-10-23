package LogMe;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

// https://docs.oracle.com/javase/7/docs/api/java/util/logging/Logger.html
public class IntuiLog {
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    protected class IntuiObject {
        private String executionID;
        private String section;
        private String content;

        public IntuiObject(String e, String s, String c) {
            executionID = e;
            section = s;
            content = c;
        }
    }

    // Aliases
    public static final Level LEVEL_NONE = Level.OFF; // This level is initialized to Integer.MAX_VALUE
    public static final Level LEVEL_WARNING = Level.WARNING; // This level is initialized to 900.
    public static final Level LEVEL_INFO = Level.INFO; // This level is initialized to 800.
    public static final Level LEVEL_TRACE = Level.FINEST; // This level is initialized to 300.
    public static final Level LEVEL_ERROR = Level.ALL; // This level is initialized to Integer.MIN_VALUE

    // mapper for parsing any object as json
    private ObjectMapper mapper = new ObjectMapper();

    // Logger activity
    private boolean active = true;

    // Init execution
    private String executionID;
    // Default level of logging is info
    private Level currentLevel = LEVEL_INFO;

    // Logger
    private final static Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
    private LogConfig logConfig = new LogConfig();
    private String track;

    public IntuiLog() {
        executionID = generateExecutionID();
        LOGGER.addHandler(logConfig.getFileHandler(executionID));
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public IntuiLog(String executionID) {
        this.executionID = executionID;
        LOGGER.addHandler(logConfig.getFileHandler(executionID));
    }

    public IntuiLog(String executionID, String track) {
        this.track = track;
        this.executionID = executionID;
        LOGGER.addHandler(logConfig.getFileHandler(executionID));
    }

    private String generateExecutionID() {
        return UUID.randomUUID().toString();
    }

    public void logStep(String step) {
        genericLog(step);
    }

    public void logSuccess(String success) {
        genericLog(success);
    }

    public void logEnd(String message) {
        genericLog(message);
        active = false;
    }

    private void genericLog(String message) {
        if (!active)
            return;

        try {
            IntuiObject obj = new IntuiObject(executionID, "step", message);
            LOGGER.log(currentLevel, mapper.writeValueAsString(obj));
        } catch (Exception e) {
            LOGGER.log(currentLevel, message);
        }
    }

    private void genericLog(String message, Object object) {
        if (active)
            try {
                IntuiObject obj = new IntuiObject(executionID, mapper.writeValueAsString(object), message);
                LOGGER.log(currentLevel, mapper.writeValueAsString(obj));
            } catch (Exception e) {
                LOGGER.log(currentLevel, object.toString());
            }
    }

    public void changeLevelLog(Level level) {
        currentLevel = level;
    }

    public void trace(String description, Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            try {
                genericLog(String.format("%s", description), objects[i]);
            } catch (Exception ex) {
                genericLog(String.format("%s %s", description, ex.getMessage()));
            }
        }
    }
}
