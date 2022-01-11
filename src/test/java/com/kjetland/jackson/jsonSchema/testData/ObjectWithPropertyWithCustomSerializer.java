package com.kjetland.jackson.jsonSchema.testData;

public class ObjectWithPropertyWithCustomSerializer {

    public String s;
    public PojoWithCustomSerializer child;

    public ObjectWithPropertyWithCustomSerializer() {
    }


    public ObjectWithPropertyWithCustomSerializer(String s, PojoWithCustomSerializer child) {
        this.s = s;
        this.child = child;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectWithPropertyWithCustomSerializer that = (ObjectWithPropertyWithCustomSerializer) o;

        if (s != null ? !s.equals(that.s) : that.s != null) return false;
        return child != null ? child.equals(that.child) : that.child == null;

    }

    @Override
    public int hashCode() {
        int result = s != null ? s.hashCode() : 0;
        result = 31 * result + (child != null ? child.hashCode() : 0);
        return result;
    }
}
