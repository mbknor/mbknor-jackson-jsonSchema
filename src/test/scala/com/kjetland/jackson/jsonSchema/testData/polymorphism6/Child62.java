package com.kjetland.jackson.jsonSchema.testData.polymorphism6;

import java.util.Objects;

public class Child62 implements Parent6 {

    public Integer child2int;

    public String parentString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Child62)) return false;

        Child62 child2 = (Child62) o;

        return Objects.equals(child2int, child2.child2int)
                && Objects.equals(parentString, child2.parentString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(child2int, parentString);
    }
}
