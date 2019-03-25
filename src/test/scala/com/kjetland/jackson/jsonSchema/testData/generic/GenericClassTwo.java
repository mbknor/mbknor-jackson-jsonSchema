package com.kjetland.jackson.jsonSchema.testData.generic;

import java.util.Objects;

public class GenericClassTwo<T, U> {
    public T data1;
    public U data2;

    public GenericClassTwo(T data1, U data2) {
        this.data1 = data1;
        this.data2 = data2;
    }

    public GenericClassTwo() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericClassTwo<?, ?> that = (GenericClassTwo<?, ?>) o;
        return Objects.equals(data1, that.data1) &&
                Objects.equals(data2, that.data2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data1, data2);
    }
}

