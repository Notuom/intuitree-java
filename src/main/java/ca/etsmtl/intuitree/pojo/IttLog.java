package ca.etsmtl.intuitree.pojo;

import java.util.List;

public class IttLog {

    private int parentId;
    private int id;

    private String title;
    private String message;
    private IttStatus status;
    private List<IttTagValue> tags;

    public IttLog(int parentId, int id, String title, String message, IttStatus status, List<IttTagValue> tags) {
        this.parentId = parentId;
        this.id = id;
        this.title = title;
        this.message = message;
        this.status = status;
        this.tags = tags;
    }

    public int getParentId() {
        return parentId;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getStatusName() {
        return status.getName();
    }

    public List<IttTagValue> getTags() {
        return tags;
    }
}
