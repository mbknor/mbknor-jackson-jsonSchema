package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class BoringContainer {

    @JsonProperty("child1")
    public PojoUsingJsonTypeName child1;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BoringContainer that = (BoringContainer) o;
        return Objects.equals(child1, that.child1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(child1);
    }

}
