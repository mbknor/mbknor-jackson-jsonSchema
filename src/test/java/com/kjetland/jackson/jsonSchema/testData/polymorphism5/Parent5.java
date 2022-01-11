package com.kjetland.jackson.jsonSchema.testData.polymorphism5;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "clazz")
public abstract class Parent5 {

    public String parentString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parent5 parent = (Parent5) o;

        return parentString != null ? parentString.equals(parent.parentString) : parent.parentString == null;

    }

    @Override
    public int hashCode() {
        return parentString != null ? parentString.hashCode() : 0;
    }

    public static class Child51InnerClass extends Parent5 {

        public String child1String;

        @JsonProperty("_child1String2")
        public String child1String2;

        @JsonProperty(value = "_child1String3", required = true)
        public String child1String3;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Child51)) return false;
            if (!super.equals(o)) return false;

            Child51 child1 = (Child51) o;

            if (child1String != null ? !child1String.equals(child1.child1String) : child1.child1String != null)
                return false;
            if (child1String2 != null ? !child1String2.equals(child1.child1String2) : child1.child1String2 != null)
                return false;
            return child1String3 != null ? child1String3.equals(child1.child1String3) : child1.child1String3 == null;

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (child1String != null ? child1String.hashCode() : 0);
            result = 31 * result + (child1String2 != null ? child1String2.hashCode() : 0);
            result = 31 * result + (child1String3 != null ? child1String3.hashCode() : 0);
            return result;
        }
    }
}
