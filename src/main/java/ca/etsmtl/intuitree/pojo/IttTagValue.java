package ca.etsmtl.intuitree.pojo;

public class IttTagValue {

    private IttTag tag;
    private String value;

    public IttTagValue(IttTag tag, String value) {
        this.tag = tag;
        this.value = value;
    }

    public String getTagName() {
        return tag.getName();
    }

    public String getValue() {
        return value;
    }

}
