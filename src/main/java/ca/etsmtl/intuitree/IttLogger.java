package ca.etsmtl.intuitree;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The main and only class which is needed to interact with the Intuitree library.
 */
public class IttLogger {

    /**
     * Jackson ObjectMapper in order to write POJOs as JSON.
     */
    private static ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Jackson JsonFactory in order to create a JsonGenerator and stream data.
     */
    private static JsonFactory FACTORY = new JsonFactory();

    /**
     * Jackson JsonGenerator in order to generate JSON in a stream.
     */
    private JsonGenerator generator;

    /**
     * OutputStream which is sent to the JsonGenerator.
     */
    private OutputStream outputStream;

    /**
     * Controls whether any logging logic and output is done.
     */
    private boolean enabled;

    /**
     * Represents the current Execution instance, holding basic information on the logging session.
     */
    private IttExecution execution;

    /**
     * Map from all status names to the corresponding status instances. Allows to refer to a status by its name in the API.
     */
    private ConcurrentMap<String, IttStatus> statusMap;

    /**
     * Map from all tag names to the corresponding tag instances. Allows to refer to a tag by its name in the API.
     */
    private ConcurrentMap<String, IttTag> tagMap;

    /**
     * A stack representing the parent IDs in the order that leads from the first parent to the root.
     */
    private Deque<Integer> parentLogIdStack = new ArrayDeque<>();

    /**
     * The current maximal log ID which determines the ID of the next generated log.
     */
    private int maxLogId;

    /**
     * The current parent ID, which is kept separately to alleviate checks when adding new logs.
     * 0, the default value, means there is no parent at the current level (root level).
     */
    private int currentParentId;

    /**
     * The current log ID, which is used when creating a new track to add the right parent ID.
     * 0, the default value, means that creating a new track will have no effect (stays on the same track).
     */
    private int currentLogId;

    /**
     * Create an enabled logger which sends its output to the given filename.
     *
     * @param filename Filename to output to.
     */
    public IttLogger(String filename) throws IOException {
        this(filename, true);
    }

    /**
     * Create a logger which sends its output to the given filename.
     *
     * @param filename Filename to output to.
     * @param enabled  Initial enabled status.
     */
    public IttLogger(String filename, boolean enabled) throws IOException {
        this(new FileOutputStream(filename), enabled);
    }

    /**
     * Create a logger which sends its output to the given output stream.
     *
     * @param outputStream Output stream to output to.
     * @param enabled      Initial enabled status.
     */
    public IttLogger(OutputStream outputStream, boolean enabled) throws IOException {
        this(FACTORY.createGenerator(outputStream, JsonEncoding.UTF8), outputStream, enabled,
                new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    /**
     * Constructor which allows for parameter injection. Used for testing purposes.
     */
    IttLogger(JsonGenerator generator, OutputStream outputStream, boolean enabled,
              ConcurrentMap<String, IttStatus> statusMap, ConcurrentMap<String, IttTag> tagMap) {
        this.generator = generator;
        this.outputStream = outputStream;
        this.enabled = enabled;
        this.statusMap = statusMap;
        this.tagMap = tagMap;

        this.generator.setCodec(MAPPER);
    }

    /**
     * Add a status to the available statuses.
     * Must be done before {@link #startExecution(java.lang.String, java.lang.String)} is called.
     *
     * @param name  The status name, which will be displayed in the UI as-is.
     * @param color The status' background color as a web color string, which will be displayed in the UI as-is.
     *              Examples: "#FFF", "#FF00FF", "black", "green", ...
     * @return The IttStatus instance which can be used directly when calling IttLogger methods.
     */
    public IttStatus addStatus(String name, String color) {
        if (!enabled) return null;

        // The concurrent map is synchronized internally
        IttStatus status = new IttStatus(name, color);
        statusMap.put(name, status);
        return status;
    }

    /**
     * Add a tag to the available tags.
     * Must be done before {@link #startExecution(java.lang.String, java.lang.String)} is called.
     *
     * @param name The tag name, which will be displayed in the UI as-is.
     * @return The IttTag instance which can be used directly when calling IttLogger methods.
     */
    public IttTag addTag(String name) {
        if (!enabled) return null;

        // The concurrent map is synchronized internally
        IttTag tag = new IttTag(name);
        tagMap.put(name, tag);
        return tag;

    }

    /**
     * Start the current execution. Must be called before any logging function and after adding at least one status
     * ({@link #addStatus(String, String)}) and one tag ({@link #addTag(String)}).
     *
     * @param title   Title for the execution.
     * @param message Message (details) for the execution.
     * @return The execution instance. Not needed for interaction with the API.
     * @throws IOException When a problem occurs while writing the JSON to the output stream.
     */
    public IttExecution startExecution(String title, String message) throws IOException {
        if (!enabled) return null;

        // Synchronize all logging logic to ensure safe state between threads.
        synchronized (this) {
            if (execution != null) {
                throw new IllegalStateException("Execution is already active. Create a new IttLogger instance " +
                        "if you wish to log different executions simultaneoulsy.");
            }

            execution = new IttExecution(title, message);

            generator.writeStartObject();

            generator.writeObjectField("execution", execution);

            generator.writeObjectField("statuses", statusMap);
            generator.writeObjectField("tags", tagMap);

            generator.writeArrayFieldStart("logs");

            return execution;
        }
    }

    /**
     * Starts a new "track" (hierarchical level) from the current log node.
     * All further {@link #addLog(String, String, IttStatus, IttTagValue...)} calls
     * until {@link #endLogTrack()} is called will add logs
     * that are children of the current (last added) log.
     */
    public void startLogTrack() {
        if (!enabled) return;

        // Synchronize all logging logic to ensure safe state between threads.
        synchronized (this) {
            if (currentLogId != 0) {
                parentLogIdStack.push(currentLogId);
                currentParentId = currentLogId;
                currentLogId = 0;
            }
        }
    }

    /**
     * Adds a log at the current "track" (hierarchical level).
     *
     * @param title   The log title (must be short; displayed in small area of the UI).
     * @param message The log message (can be very long; displayed in a large area of the UI).
     * @param status  The log status.
     * @param tags    A list of TagValues representing the tags on this node,
     *                which can be generated using the tagValue methods.
     * @throws IOException When a problem occurs while writing the JSON to the output stream.
     */
    public void addLog(String title, String message, IttStatus status, IttTagValue... tags) throws IOException {
        if (!enabled) return;

        // Synchronize all logging logic to ensure safe state between threads.
        synchronized (this) {
            IttLog log = new IttLog(currentParentId, ++maxLogId, title, message, status, Arrays.asList(tags));
            generator.writeObject(log);
            currentLogId = log.getId();
        }
    }

    /**
     * Ends the current "track", returning to the previous hierarchical level (parent log node).
     */
    public void endLogTrack() {
        if (!enabled) return;

        // Synchronize all logging logic to ensure safe state between threads.
        synchronized (this) {
            if (parentLogIdStack.size() > 0) {
                currentLogId = parentLogIdStack.pop();
                if (parentLogIdStack.size() > 0) {
                    currentParentId = parentLogIdStack.peekFirst();
                } else {
                    currentParentId = 0;
                }
            }
        }
    }

    /**
     * Ends the whole execution and closes the output stream.
     * The instance can't be used after this.
     *
     * @throws IOException When a problem occurs while writing the JSON to the output stream.
     */
    public void endExecution() throws IOException {
        if (!enabled) return;

        // Synchronize all logging logic to ensure safe state between threads.
        synchronized (this) {
            if (!execution.isActive()) {
                throw new IllegalStateException("Execution is not active. Start an execution before ending it.");
            }

            execution.setActive(false);

            generator.writeEndArray();
            generator.writeEndObject();

            generator.close();

            enabled = false;
        }
    }

    /**
     * Generate a TagValue instance from a tag name and a value. The tag must exist.
     *
     * @param tagName Name of the tag to create the TagValue from.
     * @param value Value of the tag to assign to the TagValue.
     * @return The TagValue instance.
     */
    public IttTagValue tagValue(String tagName, String value) {
        if (!enabled) return null;

        IttTag tag = tagMap.get(tagName);
        if (tag != null) {
            return tagValue(tag, value);
        } else {
            throw new IllegalArgumentException("Tag with name \"" + tagName + "\" was not registered. Register a tag with" +
                    " addTag before using it.");
        }
    }

    /**
     * Generate a TagValue instance from a tag name and a value. The tag must exist.
     *
     * @param tag Tag instance returned from {@link #addTag(String)}
     * @param value Value of the tag to assign to the TagValue.
     * @return The TagValue instance.
     */
    public IttTagValue tagValue(IttTag tag, String value) {
        if (!enabled) return null;

        return new IttTagValue(tag, value);
    }

}
