package com.kjetland.jackson.jsonSchema.testData.generic;

import java.util.Objects;

public class GenericClass<T> {
    public T data;

    public GenericClass(T data) {
        this.data = data;
    }

    public GenericClass() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericClass<?> that = (GenericClass<?>) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}

