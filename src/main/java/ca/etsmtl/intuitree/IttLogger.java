package ca.etsmtl.intuitree;

import ca.etsmtl.intuitree.pojo.*;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
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
     * Controls whether UncheckedIOException is thrown if file can't be written.
     * Constructors will always throw IOException if they happen, but logging methods will catch them
     * by default for convenience. If this is false, the logging will completely stop when an IOException happens.
     */
    private boolean throwUncheckedIoException;

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
     * @throws IOException Thrown if there is a problem writing to the specified filename.
     */
    public IttLogger(String filename) throws IOException {
        this(filename, true);
    }

    /**
     * Create a logger which sends its output to the given filename.
     *
     * @param filename Filename to output to.
     * @param enabled  Initial enabled status.
     * @throws IOException Thrown if there is a problem writing to the specified filename.
     */
    public IttLogger(String filename, boolean enabled) throws IOException {
        this(createFileOutputStream(filename), enabled);
    }

    /**
     * Create a logger which sends its output to the given output stream.
     *
     * @param outputStream Output stream to output to.
     * @param enabled      Initial enabled status.
     * @throws IOException Thrown if the JsonGenerator has a problem with the OutputStream.
     */
    public IttLogger(OutputStream outputStream, boolean enabled) throws IOException {
        this(outputStream, enabled, false);
    }

    /**
     * Create a logger which sends its output to the given output stream.
     *
     * @param outputStream              Output stream to output to.
     * @param enabled                   If true, logging will be done, if not, every method call will do nothing.
     * @param throwUncheckedIoException If true, an UncheckedIOException will be thrown if an IOException is catched.
     * @throws IOException Thrown if the JsonGenerator has a problem with the OutputStream.
     */
    public IttLogger(OutputStream outputStream, boolean enabled, boolean throwUncheckedIoException) throws IOException {
        this(FACTORY.createGenerator(outputStream, JsonEncoding.UTF8), outputStream, enabled,
                throwUncheckedIoException, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    /**
     * Constructor which allows for parameter injection. Used for testing purposes.
     */
    IttLogger(JsonGenerator generator, OutputStream outputStream, boolean enabled,
              boolean throwUncheckedIoException, ConcurrentMap<String, IttStatus> statusMap, ConcurrentMap<String, IttTag> tagMap) {
        this.generator = generator;
        this.outputStream = outputStream;
        this.enabled = enabled;
        this.throwUncheckedIoException = throwUncheckedIoException;
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
        if (name == null || color == null) {
            throw new NullPointerException("name and color can't be null");
        }
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
        if (name == null) {
            throw new NullPointerException("name can't be null");
        }
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
     */
    public IttExecution startExecution(String title, String message) {
        if (title == null || message == null) {
            throw new NullPointerException("title and message can't be null.");
        }
        if (!enabled) return null;

        // Synchronize all logging logic to ensure safe state between threads.
        synchronized (this) {
            if (execution != null) {
                throw new IllegalStateException("Execution is already active. Create a new IttLogger instance " +
                        "if you wish to log different executions simultaneoulsy.");
            }

            execution = new IttExecution(title, message);

            try {
                generator.writeStartObject();
                generator.writeObjectField("execution", execution);

                generator.writeArrayFieldStart("statuses");
                for (Map.Entry<String, IttStatus> entry : statusMap.entrySet()) {
                    generator.writeObject(entry.getValue());
                }
                generator.writeEndArray();

                generator.writeArrayFieldStart("tags");
                for (Map.Entry<String, IttTag> entry : tagMap.entrySet()) {
                    generator.writeObject(entry.getValue());
                }
                generator.writeEndArray();

                generator.writeArrayFieldStart("logs");
            } catch (IOException e) {
                handleIoException(e);
            }

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
     * @param title      The log title (must be short; displayed in small area of the UI).
     * @param message    The log message (can be very long; displayed in a large area of the UI).
     * @param statusName The string corresponding to the log status.
     *                   Must have been created with {@link #addStatus(String, String)}.
     * @param tags       A list of TagValues representing the tags on this node,
     *                   which can be generated using the tagValue methods.
     */
    public void addLog(String title, String message, String statusName, IttTagValue... tags) {
        if (statusName == null) {
            throw new NullPointerException("statusName can't be null.");
        }
        if (!enabled) return;

        IttStatus status = statusMap.get(statusName);

        if (status != null) {
            addLog(title, message, status, tags);
        } else {
            throw new IllegalArgumentException("Status with name \"" + statusName + "\" was not registered. Register a status with" +
                    " addStatus before using it.");
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
     */
    public void addLog(String title, String message, IttStatus status, IttTagValue... tags) {
        if (status == null) {
            throw new NullPointerException("status can't be null");
        }
        if (!enabled) return;

        // Synchronize all logging logic to ensure safe state between threads.
        synchronized (this) {
            IttLog log = new IttLog(currentParentId, ++maxLogId, title != null ? title : "", message != null ? message : "", status, Arrays.asList(tags));
            try {
                generator.writeObject(log);
            } catch (IOException e) {
                handleIoException(e);
            }
            currentLogId = log.getId();
        }
    }

    /*
     * TODO make the API more flexible, especially for multi-threaded environments.
     * Idea :
     * - Return the log ID when addLog is called
     * - Add a special addLog signature which allows to specify a parent ID
     *  -> When this method is called, the parent ID of the log is the one specified
     *  -> The currentLogId property is not changed for this special implementation
     *  -> This would allow to retroactively, or separately from different threads,
     *     add logs wherever in the tree, without following the main logging flow.
     *
     * In order to have a 100% multithread-capable API, we would need something more complex.
     */

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
     */
    public void endExecution() {
        if (!enabled) return;

        // Synchronize all logging logic to ensure safe state between threads.
        synchronized (this) {
            if (!execution.isActive()) {
                throw new IllegalStateException("Execution is not active. Start an execution before ending it.");
            }

            execution.setActive(false);

            try {
                generator.writeEndArray();
                generator.writeEndObject();
                generator.close();
            } catch (IOException e) {
                handleIoException(e);
            }

            enabled = false;
        }
    }

    /**
     * Generate a TagValue instance from a tag name and a value. The tag must exist.
     *
     * @param tagName Name of the tag to create the TagValue from.
     * @param value   Value of the tag to assign to the TagValue.
     * @return The TagValue instance.
     */
    public IttTagValue tagValue(String tagName, String value) {
        if (tagName == null) {
            throw new NullPointerException("statusName can't be null.");
        }
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
     * @param tag   Tag instance returned from {@link #addTag(String)}
     * @param value Value of the tag to assign to the TagValue.
     * @return The TagValue instance.
     */
    public IttTagValue tagValue(IttTag tag, String value) {
        if (tag == null) {
            throw new NullPointerException("tag can't be null.");
        }
        if (!enabled) return null;

        return new IttTagValue(tag, value != null ? value : "");
    }

    /**
     * Helper method to create a file from a given filename if it doesn't exist and return an OutputStream.
     *
     * @param filename Filename to create if it doesn't exist and turn into OutputStream.
     * @return an OutputStream to the given filename
     * @throws IOException Thrown if the file or OutputStream can't be created.
     */
    private static OutputStream createFileOutputStream(String filename) throws IOException {
        File file = new File(filename);
        file.createNewFile();
        return new FileOutputStream(file);
    }

    /**
     * Handle an IOException according to the throwUncheckedIoException parameter.
     *
     * @param ioException IOException to handle.
     */
    private void handleIoException(IOException ioException) {
        if (throwUncheckedIoException) {
            throw new UncheckedIOException(ioException);
        } else {
            ioException.printStackTrace();
        }
    }

}
