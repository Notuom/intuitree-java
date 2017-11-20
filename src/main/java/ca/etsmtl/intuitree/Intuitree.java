package ca.etsmtl.intuitree;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A facade that gives access to a singleton instance of IttLogger, allowing for simple access through all classes,
 * which is sufficient for most common use cases.
 */
public class Intuitree {

    private static IttLogger loggerSingleton = null;

    /**
     * Create an enabled logger which sends its output to the given filename.
     *
     * @param filename Filename to output to.
     * @return An IttLogger instance with a handle on the file specified by filename.
     * @throws IOException Thrown if the file or OutputStream can't be created.
     */
    public static synchronized IttLogger create(String filename) throws IOException {
        loggerSingleton = new IttLogger(filename);
        return loggerSingleton;
    }

    /**
     * Create a logger which sends its output to the given filename.
     *
     * @param filename Filename to output to.
     * @param enabled  Initial enabled status.
     * @return An IttLogger instance with a handle on the file specified by filename.
     * @throws IOException Thrown if the file or OutputStream can't be created.
     */
    public static synchronized IttLogger create(String filename, boolean enabled) throws IOException {
        loggerSingleton = new IttLogger(filename, enabled);
        return loggerSingleton;
    }

    /**
     * Create a logger which sends its output to the given output stream.
     *
     * @param outputStream Output stream to output to.
     * @param enabled      Initial enabled status.
     * @return An IttLogger instance with a handle on the file specified by filename.
     * @throws IOException Thrown if the file or OutputStream can't be created.
     */
    public static synchronized IttLogger create(OutputStream outputStream, boolean enabled) throws IOException {
        loggerSingleton = new IttLogger(outputStream, enabled);
        return loggerSingleton;
    }

    /**
     * Get the current IttLogger instance. {@link #create(String)} must be called first.
     *
     * @return The IttLogger singleton instance.
     */
    public static IttLogger get() {
        return loggerSingleton;
    }

    /**
     * Private constructor for static class.
     */
    private Intuitree() {
    }

}
