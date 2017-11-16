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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IttLogger {

    private static ObjectMapper MAPPER = new ObjectMapper();
    private static JsonFactory FACTORY = new JsonFactory();

    private JsonGenerator generator;
    private OutputStream outputStream;
    private boolean enabled;
    private IttExecution execution;
    private Map<String, IttStatus> statusMap;
    private Map<String, IttTag> tagMap;
    private Deque<Integer> parentLogIdStack = new ArrayDeque<>();

    private int maxLogId;
    private int currentParentId;
    private IttLog currentLog;

    public IttLogger(String filename) throws IOException {
        this(filename, true);
    }

    public IttLogger(String filename, boolean enabled) throws IOException {
        this(new FileOutputStream(filename), enabled);
    }

    public IttLogger(OutputStream outputStream, boolean enabled) throws IOException {
        this(FACTORY.createGenerator(outputStream, JsonEncoding.UTF8), outputStream, enabled,
                new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    IttLogger(JsonGenerator generator, OutputStream outputStream, boolean enabled,
              Map<String, IttStatus> statusMap, Map<String, IttTag> tagMap) {
        this.generator = generator;
        this.outputStream = outputStream;
        this.enabled = enabled;
        this.statusMap = statusMap;
        this.tagMap = tagMap;

        this.generator.setCodec(MAPPER);
    }

    public synchronized IttStatus addStatus(String name, String color) {
        IttStatus status = new IttStatus(name, color);
        statusMap.put(name, status);
        return status;
    }

    public synchronized IttTag addTag(String name) {
        IttTag tag = new IttTag(name);
        tagMap.put(name, tag);
        return tag;
    }

    public synchronized IttExecution startExecution(String title, String message) throws IOException {
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

    public synchronized void startLogTrack() throws IOException {
        if (currentLog != null) {
            parentLogIdStack.push(currentLog.getId());
            currentParentId = currentLog.getId();
            currentLog = null;
        }
    }

    public synchronized void addLog(String title, String message, IttStatus status, IttTagValue... tags) throws IOException {
        currentLog = new IttLog(currentParentId, ++maxLogId, title, message, status, Arrays.asList(tags));
        generator.writeObject(currentLog);
    }

    public synchronized void endLogTrack() throws IOException {
        if (parentLogIdStack.size() > 0) {
            parentLogIdStack.pop();
            if (parentLogIdStack.size() > 0) {
                currentParentId = parentLogIdStack.peekFirst();
            } else {
                currentParentId = 0;
            }
        }
    }

    public synchronized void endExecution() throws IOException {
        if (!execution.isActive()) {
            throw new IllegalStateException("Execution is not active. Start an execution before ending it.");
        }

        execution.setActive(false);

        generator.writeEndArray();
        generator.writeEndObject();

        generator.close();
    }


    public IttTagValue tagValue(String tagName, String value) {
        IttTag tag = tagMap.get(tagName);

        if (tag != null) {
            return tagValue(tag, value);
        } else {
            throw new IllegalArgumentException("Tag with name \"" + tagName + "\" was not registered. Register a tag with" +
                    " addTag before using it.");
        }
    }

    public IttTagValue tagValue(IttTag tag, String value) {
        return new IttTagValue(tag, value);
    }

}
