package com.kjetland.jackson.jsonSchema.testData.polymorphism7;

import java.util.Objects;

public class Child72 extends Parent7 {

    public Integer child2int;

    public String parentString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Child72)) return false;

        Child72 child2 = (Child72) o;

        return Objects.equals(child2int, child2.child2int)
                && Objects.equals(parentString, child2.parentString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(child2int, parentString);
    }
}
