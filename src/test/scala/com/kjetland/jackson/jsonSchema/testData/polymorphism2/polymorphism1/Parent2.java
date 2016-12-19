package com.kjetland.jackson.jsonSchema.testData.polymorphism2.polymorphism1;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "clazz")
public abstract class Parent2 {

    public String parentString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parent2 parent = (Parent2) o;

        return parentString != null ? parentString.equals(parent.parentString) : parent.parentString == null;

    }

    @Override
    public int hashCode() {
        return parentString != null ? parentString.hashCode() : 0;
    }
}
