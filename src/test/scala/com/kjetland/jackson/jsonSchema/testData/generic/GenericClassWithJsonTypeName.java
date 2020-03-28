package com.kjetland.jackson.jsonSchema.testData.generic;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

@JsonTypeName("GenericWithJsonTypeName")
public class GenericClassWithJsonTypeName<T> {
    public T data;

    public GenericClassWithJsonTypeName(T data) {
        this.data = data;
    }

    public GenericClassWithJsonTypeName() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericClassWithJsonTypeName<?> that = (GenericClassWithJsonTypeName<?>) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}

