package com.kjetland.jackson.jsonSchema.testData.generic;

import java.util.Objects;

public class BoringClass {
    public int data;

    public BoringClass(int data) {
        this.data = data;
    }

    public BoringClass() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoringClass that = (BoringClass) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
