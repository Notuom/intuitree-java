package ca.etsmtl.intuitree.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class IttExecution {

    private boolean active = true;
    private String title;
    private String message;

    public IttExecution(String title, String message) {
        this.title = title;
        this.message = message;
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
