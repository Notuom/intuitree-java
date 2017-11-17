package ca.etsmtl.intuitree.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class IttExecution {

    /**
     * Represents the file format version which is used to ensure forward compatibility with the UI app.
     */
    private static final int formatVersion = 1;

    /**
     * Active is true when the execution is being logged and must be set to false when the logging is done.
     */
    private boolean active = true;

    private String title;
    private String message;

    public IttExecution(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    @JsonIgnore
    public boolean isActive() {
        return active;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

}
