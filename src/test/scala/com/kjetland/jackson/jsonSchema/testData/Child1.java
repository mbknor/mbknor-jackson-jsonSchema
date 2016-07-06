package com.kjetland.jackson.jsonSchema.testData;

public class Child1 extends Parent {

    public String child1String;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Child1 child1 = (Child1) o;

        return child1String != null ? child1String.equals(child1.child1String) : child1.child1String == null;

    }

    @Override
    public int hashCode() {
        return child1String != null ? child1String.hashCode() : 0;
    }
}
