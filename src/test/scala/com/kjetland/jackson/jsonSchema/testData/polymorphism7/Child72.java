package com.kjetland.jackson.jsonSchema.testData.polymorphism7;

import java.util.Objects;

public class Child72 extends Parent7 {
    public String middleName;
    public String lastName;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Child72 child72 = (Child72) o;
        return Objects.equals(middleName, child72.middleName) && Objects.equals(lastName, child72.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), middleName, lastName);
    }
}
