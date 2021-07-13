package com.kjetland.jackson.jsonSchema.testData.polymorphism7;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;

import java.util.Objects;

@JsonSubTypes({ @JsonSubTypes.Type(value = Child73.class) })
public class Child71 extends Parent7 {

    public String child1String;

    @JsonProperty("_child1String2")
    public String child1String2;

    @JsonProperty(value = "_child1String3", required = true)
    public String child1String3;

    public String parentString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Child71)) return false;

        Child71 child1 = (Child71) o;

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
