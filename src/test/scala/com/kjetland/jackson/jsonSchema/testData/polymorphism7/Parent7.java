package com.kjetland.jackson.jsonSchema.testData.polymorphism7;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = Child71.class)
@JsonSubTypes({@JsonSubTypes.Type(Child72.class), @JsonSubTypes.Type(Child71.class)})
abstract public class Parent7 {
    @JsonProperty(required = true)
    public Integer id;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Parent7 parent7 = (Parent7) o;
        return Objects.equals(id, parent7.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
