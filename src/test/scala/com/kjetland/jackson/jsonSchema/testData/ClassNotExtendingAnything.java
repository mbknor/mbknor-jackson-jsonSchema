package com.kjetland.jackson.jsonSchema.testData;

public class ClassNotExtendingAnything {

    public String someString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassNotExtendingAnything child1 = (ClassNotExtendingAnything) o;

        return someString != null ? someString.equals(child1.someString) : child1.someString == null;

    }

    @Override
    public int hashCode() {
        return someString != null ? someString.hashCode() : 0;
    }
}
