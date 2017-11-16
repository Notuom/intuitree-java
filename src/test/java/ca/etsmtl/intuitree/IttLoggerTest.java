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
    public void testStartExecution() throws IOException {
        logger.addStatus("TestStatus", "#0F0");
        logger.addTag("TestTag");

        IttExecution execution = logger.startExecution("TestExecution", "TestMessage");

        generator.flush();

        Assert.assertEquals("{\"execution\":{\"title\":\"TestExecution\",\"message\":\"TestMessage\"},\"statuses\":{\"TestStatus\":{\"name\":\"TestStatus\",\"color\":\"#0F0\"}},\"tags\":{\"TestTag\":{\"name\":\"TestTag\"}},\"logs\":[",
                outputStream.toString());
        Assert.assertEquals(true, execution.isActive());
    }

    @Test
    public void testEndExecution() throws IOException {
        IttExecution execution = logger.startExecution("TestExecution", "TestMessage");
        logger.endExecution();
        Assert.assertEquals("{\"execution\":{\"title\":\"TestExecution\",\"message\":\"TestMessage\"},\"statuses\":{},\"tags\":{},\"logs\":[]}", outputStream.toString());
        Assert.assertEquals(false, execution.isActive());
    }

    @Test
    public void testExamplePath() throws IOException {
        IttStatus redStatus = logger.addStatus("red", "#F00");
        IttStatus yellowStatus = logger.addStatus("green", "#0F0");
        IttStatus blueStatus = logger.addStatus("blue", "#00F");

        IttTag fooTag = logger.addTag("foo");
        IttTag barTag = logger.addTag("bar");
        IttTag bazTag = logger.addTag("baz");

        logger.startExecution("Execution title", "Execution message");

        logger.startLogTrack();

        logger.addLog("foo1", "foo1 is blue.", blueStatus, logger.tagValue(fooTag, "1"));

        logger.addLog("foo2", "foo2 is yellow.", yellowStatus, logger.tagValue(fooTag, "2"));
        logger.startLogTrack();

        logger.addLog("bar2-1", "bar2-1 is blue and child of foo2.", blueStatus, logger.tagValue(barTag, "2-1"));

        logger.addLog("bar2-2", "bar2-2 is yellow and child of foo2.", yellowStatus, logger.tagValue(barTag, "2-2"));
        logger.startLogTrack();

        logger.addLog("baz2-2-1", "baz2-2-1 is red and child of bar2-2", redStatus, logger.tagValue(bazTag, "2-2-1"));

        logger.endLogTrack();

        logger.endLogTrack();

        logger.addLog("foo3", "foo3 is red.", redStatus, logger.tagValue(fooTag, "3"));
        logger.startLogTrack();

        logger.endLogTrack();

        logger.endExecution();

        System.out.println(outputStream.toString());
    }

}
