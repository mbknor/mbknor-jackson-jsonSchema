package com.kjetland.jackson.jsonSchema.testData.polymorphism6;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Child61.class, name = "child61") })
public abstract class Parent6 {

    public String parentString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parent6 parent = (Parent6) o;

        return parentString != null ? parentString.equals(parent.parentString) : parent.parentString == null;

    }

    @Override
    public int hashCode() {
        return parentString != null ? parentString.hashCode() : 0;
    }
}
