package ca.etsmtl.intuitree;

import ca.etsmtl.intuitree.pojo.IttExecution;
import ca.etsmtl.intuitree.pojo.IttStatus;
import ca.etsmtl.intuitree.pojo.IttTag;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IttLoggerTest {

    // Format version currently used in the Execution class
    private static final int formatVersion = 1;

    private static ObjectMapper MAPPER = new ObjectMapper();
    private static JsonFactory FACTORY = new JsonFactory();

    private IttLogger logger;
    private JsonGenerator generator;
    private OutputStream outputStream;
    private ConcurrentMap<String, IttStatus> statusMapSpy;
    private ConcurrentMap<String, IttTag> tagMapSpy;

    @Before
    public void setUp() throws Exception {
        outputStream = new ByteArrayOutputStream();
        generator = FACTORY.createGenerator(outputStream, JsonEncoding.UTF8);
        statusMapSpy = new ConcurrentHashMap<>();
        tagMapSpy = new ConcurrentHashMap<>();
        logger = new IttLogger(generator, outputStream, true, false, statusMapSpy, tagMapSpy);
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

        Assert.assertEquals("{\"execution\":{\"title\":\"TestExecution\",\"message\":\"TestMessage\",\"formatVersion\":" + formatVersion + "},\"statuses\":[{\"name\":\"TestStatus\",\"color\":\"#0F0\"}],\"tags\":[{\"name\":\"TestTag\"}],\"logs\":[",
                outputStream.toString());
        Assert.assertEquals(true, execution.isActive());
    }

    @Test
    public void testEndExecution() throws IOException {
        IttExecution execution = logger.startExecution("TestExecution", "TestMessage");
        logger.endExecution();
        Assert.assertEquals("{\"execution\":{\"title\":\"TestExecution\",\"message\":\"TestMessage\",\"formatVersion\":" + formatVersion + "},\"statuses\":[],\"tags\":[],\"logs\":[]}", outputStream.toString());
        Assert.assertEquals(false, execution.isActive());
    }

    @Test
    public void testHappyPath() throws IOException {
        IttStatus redStatus = logger.addStatus("red", "#F00");
        IttStatus greenStatus = logger.addStatus("green", "#0F0");
        IttStatus blueStatus = logger.addStatus("blue", "#00F");

        IttTag fooTag = logger.addTag("foo");
        IttTag barTag = logger.addTag("bar");
        IttTag bazTag = logger.addTag("baz");

        logger.startExecution("Execution title", "Execution message");

        logger.startLogTrack();

        logger.addLog("foo1", "foo1 is blue.", blueStatus, logger.tagValue(fooTag, "1"));

        logger.addLog("foo2", "foo2 is green.", greenStatus, logger.tagValue(fooTag, "2"));
        logger.startLogTrack();

        logger.addLog("bar2-1", "bar2-1 is blue and child of foo2.", blueStatus, logger.tagValue(barTag, "2-1"));

        logger.addLog("bar2-2", "bar2-2 is green and child of foo2.", greenStatus, logger.tagValue(barTag, "2-2"));
        logger.startLogTrack();

        logger.addLog("baz2-2-1", "baz2-2-1 is red and child of bar2-2", redStatus, logger.tagValue(bazTag, "2-2-1"));

        logger.endLogTrack();

        logger.endLogTrack();

        logger.addLog("foo3", "foo3 is red.", redStatus, logger.tagValue(fooTag, "3"));
        logger.startLogTrack();

        logger.endLogTrack();

        logger.endExecution();

        Assert.assertEquals("{\"execution\":{\"title\":\"Execution title\",\"message\":\"Execution message\",\"formatVersion\":" + formatVersion + "},\"statuses\":[{\"name\":\"red\",\"color\":\"#F00\"},{\"name\":\"green\",\"color\":\"#0F0\"},{\"name\":\"blue\",\"color\":\"#00F\"}],\"tags\":[{\"name\":\"bar\"},{\"name\":\"foo\"},{\"name\":\"baz\"}],\"logs\":[{\"parentId\":0,\"id\":1,\"title\":\"foo1\",\"message\":\"foo1 is blue.\",\"tags\":[{\"value\":\"1\",\"tagName\":\"foo\"}],\"statusName\":\"blue\"},{\"parentId\":0,\"id\":2,\"title\":\"foo2\",\"message\":\"foo2 is green.\",\"tags\":[{\"value\":\"2\",\"tagName\":\"foo\"}],\"statusName\":\"green\"},{\"parentId\":2,\"id\":3,\"title\":\"bar2-1\",\"message\":\"bar2-1 is blue and child of foo2.\",\"tags\":[{\"value\":\"2-1\",\"tagName\":\"bar\"}],\"statusName\":\"blue\"},{\"parentId\":2,\"id\":4,\"title\":\"bar2-2\",\"message\":\"bar2-2 is green and child of foo2.\",\"tags\":[{\"value\":\"2-2\",\"tagName\":\"bar\"}],\"statusName\":\"green\"},{\"parentId\":4,\"id\":5,\"title\":\"baz2-2-1\",\"message\":\"baz2-2-1 is red and child of bar2-2\",\"tags\":[{\"value\":\"2-2-1\",\"tagName\":\"baz\"}],\"statusName\":\"red\"},{\"parentId\":0,\"id\":6,\"title\":\"foo3\",\"message\":\"foo3 is red.\",\"tags\":[{\"value\":\"3\",\"tagName\":\"foo\"}],\"statusName\":\"red\"}]}",
                outputStream.toString());
        System.out.println(outputStream.toString());
    }

    @Test
    public void testApiResilience() throws IOException {
        IttStatus status = logger.addStatus("status", "#F00");
        IttTag tag = logger.addTag("foo");

        logger.startExecution("Execution title", "Execution message");

        logger.startLogTrack();

        logger.addLog("foo1", "foo1--.", status, logger.tagValue(tag, "1"));

        logger.addLog("foo2", "foo2--.", status, logger.tagValue(tag, "2"));

        // Start the same track a couple of times without effect
        logger.startLogTrack();
        logger.startLogTrack();
        logger.startLogTrack();
        logger.startLogTrack();

        logger.addLog("bar2-1", "bar2-1--.", status, logger.tagValue(tag, "2-1"));

        logger.addLog("bar2-2", "bar2-2--.", status, logger.tagValue(tag, "2-2"));

        // Go crazy with starting and ending tracks without effect
        logger.startLogTrack();
        logger.endLogTrack();
        logger.startLogTrack();
        logger.endLogTrack();
        logger.startLogTrack();
        logger.endLogTrack();

        logger.startLogTrack();

        logger.addLog("baz2-2-1", "baz2-2-1--.", status, logger.tagValue(tag, "2-2-1"));

        logger.endLogTrack();

        logger.endLogTrack();

        logger.addLog("foo3", "foo3--.", status, logger.tagValue(tag, "3"));
        logger.startLogTrack();

        logger.endLogTrack();

        logger.endExecution();

        Assert.assertEquals("{\"execution\":{\"title\":\"Execution title\",\"message\":\"Execution message\",\"formatVersion\":" + formatVersion + "},\"statuses\":[{\"name\":\"status\",\"color\":\"#F00\"}],\"tags\":[{\"name\":\"foo\"}],\"logs\":[{\"parentId\":0,\"id\":1,\"title\":\"foo1\",\"message\":\"foo1--.\",\"tags\":[{\"value\":\"1\",\"tagName\":\"foo\"}],\"statusName\":\"status\"},{\"parentId\":0,\"id\":2,\"title\":\"foo2\",\"message\":\"foo2--.\",\"tags\":[{\"value\":\"2\",\"tagName\":\"foo\"}],\"statusName\":\"status\"},{\"parentId\":2,\"id\":3,\"title\":\"bar2-1\",\"message\":\"bar2-1--.\",\"tags\":[{\"value\":\"2-1\",\"tagName\":\"foo\"}],\"statusName\":\"status\"},{\"parentId\":2,\"id\":4,\"title\":\"bar2-2\",\"message\":\"bar2-2--.\",\"tags\":[{\"value\":\"2-2\",\"tagName\":\"foo\"}],\"statusName\":\"status\"},{\"parentId\":4,\"id\":5,\"title\":\"baz2-2-1\",\"message\":\"baz2-2-1--.\",\"tags\":[{\"value\":\"2-2-1\",\"tagName\":\"foo\"}],\"statusName\":\"status\"},{\"parentId\":0,\"id\":6,\"title\":\"foo3\",\"message\":\"foo3--.\",\"tags\":[{\"value\":\"3\",\"tagName\":\"foo\"}],\"statusName\":\"status\"}]}",
                outputStream.toString());
    }

}
