package ca.etsmtl.intuitree;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class IttExecution {

    private boolean active = true;
    private String title;

    public IttExecution(String title) {
        this.title = title;
    }

    @JsonIgnore
    public String getTitle() {
        return title;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
