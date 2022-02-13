package com.kjetland.jackson.jsonSchema.testData.polymorphism7;

import java.util.Objects;

public class Child71 extends Parent7 {
    public String firstName;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Child71 child71 = (Child71) o;
        return Objects.equals(firstName, child71.firstName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), firstName);
    }
}
