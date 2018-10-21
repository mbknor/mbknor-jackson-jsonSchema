package com.kjetland.jackson.jsonSchema.testData.polymorphism5;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "clazz")
public abstract class Parent5 {

    public String parentString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parent5 parent = (Parent5) o;

        return parentString != null ? parentString.equals(parent.parentString) : parent.parentString == null;

    }

    @Override
    public int hashCode() {
        return parentString != null ? parentString.hashCode() : 0;
    }
}
