package ca.etsmtl.intuitree;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IttLoggerTest {

    private static ObjectMapper MAPPER = new ObjectMapper();
    private static JsonFactory FACTORY = new JsonFactory();

    IttLogger logger;

    JsonGenerator generator;
    OutputStream outputStream;
    Map<String, IttStatus> statusMapSpy;
    Map<String, IttTag> tagMapSpy;

    @Before
    public void setUp() throws Exception {
        outputStream = new ByteArrayOutputStream();
        generator = FACTORY.createGenerator(outputStream, JsonEncoding.UTF8);
        statusMapSpy = new ConcurrentHashMap<>();
        tagMapSpy = new ConcurrentHashMap<>();
        logger = new IttLogger(generator, outputStream, true, statusMapSpy, tagMapSpy);
    }

    @Test
    public void testAddStatus() throws Exception {
        IttStatus status = logger.addStatus("TestStatus", "My favorite color");
        Assert.assertEquals(status, statusMapSpy.get("TestStatus"));
    }

    @Test
    public void testAddTag() throws Exception {
        IttTag tag = logger.addTag("TestTag");
        Assert.assertEquals(tag, tagMapSpy.get("TestTag"));
    }

    @Test
    public void testOpenExecution() throws IOException {
        logger.addStatus("TestStatus", "#0F0");
        logger.addTag("TestTag");

        logger.openExecution("TestExecution");

        Assert.assertEquals("{\"execution\":{\"active\":true},\"statuses\":{\"TestStatus\":{\"name\":\"TestStatus\",\"color\":\"#0F0\"}},\"tags\":{\"TestTag\":{\"name\":\"TestTag\"}}",
                outputStream.toString());
    }

}
