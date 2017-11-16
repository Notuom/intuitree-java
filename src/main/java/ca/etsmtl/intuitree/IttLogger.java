package ca.etsmtl.intuitree;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    public IttStatus addStatus(String name, String color) {
        IttStatus status = new IttStatus(name, color);
        statusMap.put(name, status);
        return status;
    }

    public IttTag addTag(String name) {
        IttTag tag = new IttTag(name);
        tagMap.put(name, tag);
        return tag;
    }

    public synchronized IttExecution openExecution(String title) throws IOException {
        execution = new IttExecution(title);

        generator.writeStartObject();

        generator.writeObjectField("execution", execution);

        generator.writeObjectField("statuses", statusMap);
        generator.writeObjectField("tags", tagMap);

        return execution;
    }

}
