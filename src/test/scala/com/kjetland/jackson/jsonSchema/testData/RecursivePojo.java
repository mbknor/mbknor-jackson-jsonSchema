package com.kjetland.jackson.jsonSchema.testData;

import java.util.List;

public class RecursivePojo {

    public String myText;

    public List<RecursivePojo> children;

    public RecursivePojo() {
    }

    public RecursivePojo(String myText, List<RecursivePojo> children) {
        this.myText = myText;
        this.children = children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecursivePojo)) return false;

        RecursivePojo that = (RecursivePojo) o;

        if (myText != null ? !myText.equals(that.myText) : that.myText != null) return false;
        return children != null ? children.equals(that.children) : that.children == null;

    }

    @Override
    public int hashCode() {
        int result = myText != null ? myText.hashCode() : 0;
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
    }
}
