package com.kjetland.jackson.jsonSchema.testData.polymorphism6;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Child61 implements Parent6 {

    public String child1String;

    @JsonProperty("_child1String2")
    public String child1String2;

    @JsonProperty(value = "_child1String3", required = true)
    public String child1String3;

    public String parentString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Child61)) return false;

        Child61 child1 = (Child61) o;

        return Objects.equals(child1String, child1.child1String)
                && Objects.equals(child1String2, child1.child1String2)
                && Objects.equals(child1String3, child1.child1String3)
                && Objects.equals(parentString, child1.parentString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(child1String, child1String2, child1String3, parentString);
    }
}
